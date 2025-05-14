package com.example.CultureLoop.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.*;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import com.google.firebase.database.DataSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class UserChallengeService {
    // 랜덤하게 challenge 주기

    // challenge 수락 및 완료 - 파일 아니고 String 으로 바꾸기
    public ResponseEntity<?> addUserChallenge(String challengeId, boolean isComplete, String file) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            Firestore db = FirestoreClient.getFirestore();

            // challenge 문서 유효성 확인
            DocumentSnapshot challengeSnapshot = db.collection("challenges").document(challengeId).get().get();
            if (!challengeSnapshot.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Challenge not found");
            }

            // 유저 문서 참조
            DocumentReference userDocRef = db.collection("users").document(email);
            String imageUrl = "";

            // 파일이 존재하고 완료 상태인 경우에만 GCS에 업로드
            if (isComplete && file != null) {
                // imageUrl = uploadImagesToGCS(file, challengeId);
                imageUrl = file;

            }

            if (!isComplete) {
                // ongoing에 추가
                userDocRef.update("ongoing", FieldValue.arrayUnion(challengeId));
            } else {
                // ongoing에서 제거하고 completed에 추가
                userDocRef.update("ongoing", FieldValue.arrayRemove(challengeId));
                userDocRef.update("completed", FieldValue.arrayUnion(challengeId));
                userDocRef.update("badges", FieldValue.arrayUnion(imageUrl));

                // rewardId 가져오기
                String rewardId = challengeSnapshot.contains("rewardId") ?
                        challengeSnapshot.getString("rewardId") : null;

                if (rewardId != null && !rewardId.isEmpty()) {
                    // 사용자 rewards 필드가 없을 수도 있으므로 병합 방식 사용
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("rewards", FieldValue.arrayUnion(rewardId));
                    userDocRef.set(updates, SetOptions.merge());  // 필드가 없어도 자동 생성됨
                }
            }

            return ResponseEntity.ok("챌린지 " + (isComplete ? "완료" : "수락") + " 처리 완료");

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    // 챌린지 진행 cancel
    public ResponseEntity<?> cancelChallenge(String challengeId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            System.out.println(challengeId);

            Firestore db = FirestoreClient.getFirestore();
            // 유저 문서 참조
            DocumentReference userDocRef = db.collection("users").document(email);
            userDocRef.update("ongoing", FieldValue.arrayRemove(challengeId));

            return ResponseEntity.status(HttpStatus.OK).body(challengeId + "진행 취소");

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    public ResponseEntity<?> getRandomChallenges() {
        int count = 8;

        Firestore db = FirestoreClient.getFirestore();

        try {
            ApiFuture<QuerySnapshot> future = db.collection("challenges").get();

            // 수정 가능한 리스트로 복사
            List<QueryDocumentSnapshot> documents = new ArrayList<>(future.get().getDocuments());

            if (documents.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            Collections.shuffle(documents);

            List<Map<String, Object>> randomChallenges = documents.stream()
                    .limit(count)
                    .map(doc -> {
                        Map<String, Object> dataWithId = new HashMap<>(doc.getData());
                        dataWithId.put("challengeId", doc.getId());
                        return dataWithId;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(randomChallenges);

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    public ResponseEntity<?> getChallengeDetail(String ChallengeId) {
        Firestore db = FirestoreClient.getFirestore();
        try {
            DocumentSnapshot snapshot = db.collection("challenges").document(ChallengeId).get().get();
            // 기존 데이터 복사
            Map<String, Object> data = new HashMap<>(snapshot.getData());
            try {
                String status = "";
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String email = auth.getName();
                //System.out.println(email);
                DocumentSnapshot userSnapshot = db.collection("users").document(email).get().get();

                if(userSnapshot == null) {

                } else if (userSnapshot.get("ongoing") != null || userSnapshot.get("completed") != null) {
                    if (userSnapshot.get("ongoing") != null) {
                        List<String> ongoing = (ArrayList<String>) userSnapshot.get("ongoing");
                        if (ongoing.contains(ChallengeId)) {
                            status = "ongoing";
                        }
                    }
                    if (userSnapshot.get("completed") != null) {
                        List<String> completed = (ArrayList<String>) userSnapshot.get("completed");
                        if (completed.contains(ChallengeId)) {
                            status = "completed";
                        }
                    }
                    data.put("mission_status", status);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return ResponseEntity.ok(data);
        } catch (Exception e){
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("error", "Internal Server Error");
            errorBody.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    private String uploadImagesToGCS(MultipartFile image, String challengeId) throws IOException {
        String url = "";

        try {
            String bucketName = "culture_loop";

            StorageClient client = StorageClient.getInstance();

            var bucket = client.bucket();

            if (bucket == null) {
                throw new IllegalStateException("Firebase 기본 버킷이 설정되지 않았습니다.");
            }

            Storage storage = bucket.getStorage();

            if (!image.isEmpty()) {
                String filename = "badges/" + challengeId + "_" + image.getOriginalFilename();
                BlobId blobId = BlobId.of(bucketName, filename);
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(image.getContentType()).build();
                storage.create(blobInfo, image.getBytes());

                String imageUrl = "https://storage.googleapis.com/" + bucketName + "/" + filename;
                url = imageUrl;
            }

            return url;

        } catch (Exception e) {

            e.printStackTrace(); // 로그 꼭 보기!
            throw new RuntimeException("이미지 업로드 실패", e);
        }
    }

}

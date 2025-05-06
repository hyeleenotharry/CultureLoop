package com.example.CultureLoop.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.*;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserChallengeService {
    // 랜덤하게 challenge 주기


    // challenge 수락 및 완료
    public ResponseEntity<?> addUserChallenge(String challengeId, boolean isComplete, MultipartFile file) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            Firestore db = FirestoreClient.getFirestore();
            // challenge 객체 가져오기
            DocumentReference challengeRef = db.collection("challenges").document(challengeId);
            System.out.println(challengeId);
            DocumentSnapshot challengeSnapshot = challengeRef.get().get();

            if (!challengeSnapshot.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Challenge not found");
            }

            Map<String, Object> challengeData = challengeSnapshot.getData();

            // GCS에 PDF 업로드 (isCompleted == true일 때만)
            String downloadUrl = null;
            if (isComplete && file != null && !file.isEmpty()) {
                // 파일 이름 및 버킷 설정
                String filename = "badges/" + challengeId + ".pdf";
                String bucketName = StorageClient.getInstance().bucket().getName();

                BlobId blobId = BlobId.of(bucketName, filename);
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                        .setContentType("application/pdf")
                        .build();

                Storage storage = StorageOptions.getDefaultInstance().getService();

                // 존재 여부 확인
                Blob existingBlob = storage.get(blobId);
                if (existingBlob == null) {
                    // 존재하지 않으면 업로드
                    storage.create(blobInfo, file.getBytes());

                    // 다운로드 URL
                    downloadUrl = "https://storage.googleapis.com/" + bucketName + "/" + filename;
                } else {
                    // 이미 존재하면 URL만
                    downloadUrl = "https://storage.googleapis.com/" + bucketName + "/" + filename;
                }
            }


            // 문서 id : userId_challengeId
            String docId = email + "_" + challengeId;
            db.collection("users").document(email);
            DocumentReference userChallengeRef = db.collection("users_challenges").document(docId);

            // 사용자-챌린지 매핑 데이터 구성
            Map<String, Object> userChallengeData = new HashMap<>();
            userChallengeData.put("userId", email);
            userChallengeData.put("challengeId", challengeId);
            userChallengeData.put("status", isComplete ? "completed" : "ongoing");
            userChallengeData.put("challenge", challengeData);

            // 수락 누르면 firestore user - challenge(ongoing) 필드에 저장
            userChallengeRef.set(userChallengeData);

            // 완료했다면 user - challenge(completed) + 뱃지 획득
            if (isComplete) {
                DocumentReference userDoc = db.collection("users").document(email);
                userDoc.update("badges", FieldValue.arrayUnion("badge_" + challengeId));
            }

            return ResponseEntity.ok("챌린지 " + (isComplete ? "완료" : "수락") + " 처리 완료");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Firestore 작업 실패: " + e.getMessage());
        }

    }

}

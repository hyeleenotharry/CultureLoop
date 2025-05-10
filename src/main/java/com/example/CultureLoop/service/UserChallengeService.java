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

import java.io.IOException;
import java.util.*;

@Service
public class UserChallengeService {
    // 랜덤하게 challenge 주기

    // challenge 수락 및 완료
    public ResponseEntity<?> addUserChallenge(String challengeId, boolean isComplete, MultipartFile file) {
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
            if (isComplete && file != null && !file.isEmpty()) {
                imageUrl = uploadImagesToGCS(file, challengeId);
            }

            if (!isComplete) {
                // ongoing에 추가
                userDocRef.update("ongoing", FieldValue.arrayUnion(challengeId));
            } else {
                // ongoing에서 제거하고 completed에 추가
                userDocRef.update("ongoing", FieldValue.arrayRemove(challengeId));
                userDocRef.update("completed", FieldValue.arrayUnion(challengeId));
                userDocRef.update("badges", FieldValue.arrayUnion(imageUrl));

            }

            return ResponseEntity.ok("챌린지 " + (isComplete ? "완료" : "수락") + " 처리 완료");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Firestore 작업 실패: " + e.getMessage());
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

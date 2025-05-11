package com.example.CultureLoop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class CommunityService {
    String AI_URL = "https://cultureloop-ai-447979452360.asia-northeast3.run.app";

    public ResponseEntity<?> addChallenge(Map<String, Object> challenge, MultipartFile[] images) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            String name = "";
            try {
                name = db.collection("users").document(email).get().get().get("name").toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            challenge.put("host", name);

            Map<String, Object> payload = new HashMap<>();
            payload.put("title", challenge.get("title"));
            payload.put("city", challenge.get("city"));
            payload.put("description", challenge.get("description"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            RestTemplate restTemplate = new RestTemplate();
            try {

                ResponseEntity<String> response = restTemplate.postForEntity(AI_URL+"/refine/", requestEntity, String.class);

                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), new TypeReference<>() {});

                // 이미지 업로드
                List<String> imageUrls = uploadImagesToGCS(images);

                // Firestore에 챌린지 저장
                challenge.put("images", imageUrls);

                challenge.put("title", responseBody.get("title"));
                challenge.put("checklist", responseBody.get("checklist"));
                challenge.put("cultural_background", responseBody.get("cultural_background"));

                ApiFuture<DocumentReference> docRefFuture = db.collection("challenges").add(challenge);

                String challengeId = docRefFuture.get().getId();
                Map<String, Object> responseData = new HashMap<>();

                responseData.put("challengeId", challengeId);
                responseData.put("challenge", challenge);

                return new ResponseEntity<>(responseData, headers, HttpStatus.OK);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("챌린지 저장 중 오류 발생: " + e.getMessage());
        }
    }

    private List<String> uploadImagesToGCS(MultipartFile[] images) throws IOException {
        List<String> urls = new ArrayList<>();

        try {
            String bucketName = "culture_loop";

            StorageClient client = StorageClient.getInstance();

            var bucket = client.bucket();

            if (bucket == null) {
                throw new IllegalStateException("Firebase 기본 버킷이 설정되지 않았습니다.");
            }

            Storage storage = bucket.getStorage();

            for (MultipartFile image : images) {

                if (!image.isEmpty()) {
                    String filename = "challenge_imgs/" + UUID.randomUUID() + "_" + image.getOriginalFilename();
                    BlobId blobId = BlobId.of(bucketName, filename);
                    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(image.getContentType()).build();
                    storage.create(blobInfo, image.getBytes());

                    String imageUrl = "https://storage.googleapis.com/" + bucketName + "/" + filename;
                    urls.add(imageUrl);

                }
            }

            return urls;

        } catch (Exception e) {

            e.printStackTrace(); // 로그 꼭 보기!
            throw new RuntimeException("이미지 업로드 실패", e);
        }
    }
}

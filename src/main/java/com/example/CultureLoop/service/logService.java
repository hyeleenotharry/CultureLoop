package com.example.CultureLoop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import org.ietf.jgss.GSSName;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class logService {
    private String AI_URL = "https://cultureloop-ai-447979452360.asia-northeast3.run.app";

    public ResponseEntity<?> generateTravelLog(Map<String, Object> log, MultipartFile[] images){

        Firestore db = FirestoreClient.getFirestore();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        try {
            DocumentSnapshot snapshot = db.collection("challenges").document((String) log.get("challengeId")).get().get();
            String description = snapshot.get("checklist").toString();

            Map<String, Object> payload = new HashMap<>();
            payload.put("title", log.get("title"));
            payload.put("description", log.get("description"));
            payload.put("mission", description);
            payload.put("city", snapshot.get("city"));

            System.out.println(payload.toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.postForEntity(AI_URL + "/journal/", requestEntity, String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });

            List<String> imageUrls = uploadImagesToGCS(images);

            responseBody.put("user" , email);
            responseBody.put("challenge", (String) log.get("challengeId"));
            responseBody.put("imageUrls", imageUrls);

            ApiFuture<DocumentReference> docRefFuture = db.collection("users_log").add(responseBody);
            // 이미지 업로드

            return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
                    String filename = "log_imgs/" + UUID.randomUUID() + "_" + image.getOriginalFilename();
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

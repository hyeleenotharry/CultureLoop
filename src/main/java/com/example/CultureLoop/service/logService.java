package com.example.CultureLoop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
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

    public ResponseEntity<?> generateTravelLog(Map<String, Object> log, MultipartFile[] images) {
        Firestore db = FirestoreClient.getFirestore();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        try {
            // 챌린지 문서 조회
            DocumentSnapshot snapshot = db.collection("challenges").document((String) log.get("challengeId")).get().get();
            if (snapshot == null || !snapshot.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 챌린지를 찾을 수 없습니다.");
            }

            String description = snapshot.get("checklist").toString();

            // AI API 요청을 위한 payload 구성
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", log.get("title"));
            payload.put("description", log.get("description"));
            payload.put("mission", description);
            payload.put("city", snapshot.get("city"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            RestTemplate restTemplate = new RestTemplate();

            // AI 서버에 요청
            ResponseEntity<String> response = restTemplate.postForEntity(AI_URL + "/journal/", requestEntity, String.class);

            // 응답 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), new TypeReference<>() {});

            // 이미지 업로드
            List<String> imageUrls = uploadImagesToGCS(images);

            // 로그 정보에 유저 및 챌린지 정보 추가
            responseBody.put("user", email);
            responseBody.put("date", log.get("date").toString());
            responseBody.put("challengeId", (String) log.get("challengeId"));
            responseBody.put("challenge_title", snapshot.get("title").toString()); // challenge title
            responseBody.put("imageUrls", imageUrls);

            // Firestore에 로그 저장
            DocumentReference logDocRef = db.collection("users_log").add(responseBody).get();
            String logId = logDocRef.getId();

            // logId를 responseBody에도 추가해서 반환
            responseBody.put("logId", logId);

            // 유저 문서에 logId 추가
            db.collection("users").document(email)
                    .update("logs", FieldValue.arrayUnion(logId));

            return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("여행 로그 생성 중 오류 발생: " + e.getMessage());
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

    // 상세 로그
    public ResponseEntity<?> getUserLogById(String logId) {
        Firestore db = FirestoreClient.getFirestore();

        try {
            if (logId == null || logId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("logId는 필수입니다.");
            }

            // 로그 문서 조회
            DocumentSnapshot logSnapshot = db.collection("users_log").document(logId).get().get();

            if (!logSnapshot.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("해당 ID의 로그를 찾을 수 없습니다: " + logId);
            }

            Map<String, Object> logData = logSnapshot.getData();
            if (logData == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("로그 데이터가 비어 있습니다.");
            }

            logData.put("logId", logSnapshot.getId()); // ID 포함

            return ResponseEntity.ok(logData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("로그 조회 중 오류 발생: " + e.getMessage());
        }
    }
}

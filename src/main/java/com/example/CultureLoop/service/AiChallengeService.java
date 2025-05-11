package com.example.CultureLoop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import org.springframework.http.ResponseEntity;

@Service
public class AiChallengeService {
    String AI_URL = "https://cultureloop-ai-447979452360.asia-northeast3.run.app";

    Firestore db = FirestoreClient.getFirestore();

    public ResponseEntity<?> generateAiChallenge(Map<String, Object> request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            System.out.println(email);

            DocumentSnapshot snapshot = db.collection("users").document(email).get().get();
            System.out.println(snapshot.getData());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("city", request.get("city"));
            requestBody.put("style", snapshot.get("preference"));
            requestBody.put("date", request.get("date"));
            requestBody.put("count", snapshot.get("count"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.postForEntity(AI_URL+"/challenge/generate", requestEntity, String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            List<Map<String, Object>> challenges = (List<Map<String, Object>>) responseBody.get("challenges");

            List<Map<String, Object>> ai_data = new ArrayList<>();

            for (Map<String, Object> challenge : challenges) {
                Map<String, Object> challengeData = new HashMap<>();
                ArrayList<String> images = new ArrayList<>();
                images.add((String) challengeData.get("image_url"));

                challenge.put("host", "ai");
                Object imageUrlObj = challenge.remove("image_url"); // 삭제하며 추출

                if (imageUrlObj instanceof List) {
                    // image_url이 List인 경우
                    challenge.put("images", imageUrlObj);
                } else if (imageUrlObj instanceof String) {
                    // image_url이 단일 String인 경우 -> List로 감싸기
                    challenge.put("images", Collections.singletonList(imageUrlObj));
                }

                ApiFuture<DocumentReference> docRefFuture = db.collection("challenges").add(challenge);
                // 같은 제목 있으면 저장 안 하는 로직 추가...
                String challengeId = docRefFuture.get().getId();
                challengeData.put("challengeId", challengeId);
                challengeData.put("challenge", challenge);

                ai_data.add(challengeData);
            }

            return new ResponseEntity<>(ai_data, headers, HttpStatus.OK);

        } catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}

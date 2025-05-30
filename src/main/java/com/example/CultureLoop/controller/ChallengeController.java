package com.example.CultureLoop.controller;

import com.example.CultureLoop.service.AiChallengeService;
import com.example.CultureLoop.service.CommunityService;
import com.example.CultureLoop.service.UserChallengeService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.time.format.DateTimeFormatter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/challenge")
public class ChallengeController {

    private final RestTemplate restTemplate = new RestTemplate();

    private final UserChallengeService userChallengeService;
    private final CommunityService communityService;
    private final AiChallengeService aiChallengeService;

    // ai 챌린지 생성
    @PostMapping("/ai-generate")
    public ResponseEntity<?> generateChallenge(@RequestBody Map<String, Object> payload) {
        try {
            // ResponseEntity<?> responseBody = aiChallengeService.generateAiChallenge(payload);

            return aiChallengeService.generateAiChallenge(payload);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // 아무거나 8개
    @GetMapping("/init")
    public ResponseEntity<?> getRandomChallenge() {
        try {
            return userChallengeService.getRandomChallenges();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // 있는 챌린지
    @GetMapping("/random-challenge")
    public ResponseEntity<?> getAiChallengesByCity(@RequestParam String city) {
        // request param 이 없을 때는 무작위로 8개 골라서 가져오기

        // 사용자 프로필에서 count 받아오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        // city 파라미터 값 예: "seoul"
        try {
            Firestore db = FirestoreClient.getFirestore();

            // 해당 도시 챌린지 전부 불러오기
            ApiFuture<QuerySnapshot> future = db.collection("challenges").whereEqualTo("city", city).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<Map<String, Object>> challenges = new ArrayList<>();

            for (DocumentSnapshot doc : documents) {
                Map<String, Object> dataWithId = new HashMap<>(doc.getData());
                dataWithId.put("challengeId", doc.getId());
                challenges.add(dataWithId);
            }

            List<Map<String, Object>> selectedChallenges = new ArrayList<>();

            // Firestore에서 해당 유저 문서 조회
            DocumentSnapshot snapshot = db.collection("users").document(email).get().get();
            int count = snapshot.get("count", Long.class).intValue();

            // 랜덤하게 챌린지 뽑기
            Collections.shuffle(challenges);
            int avail = Math.min(count, challenges.size());
            for (int i = 0; i < avail; i++) {
                selectedChallenges.add(challenges.get(i));
            }

            int shortage = count - avail;

            if (!snapshot.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            // preference 필드를 리스트로 가져오기
            List<String> preferences = (List<String>) snapshot.get("preference");
            LocalDate now = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy/MM/dd");
            String dateStr = now.format(formatter);

            // 챌린지가 부족하다면
            if (shortage > 0) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("city", city);
                payload.put("style", preferences);
                payload.put("date", dateStr);
                payload.put("count", shortage);

                ResponseEntity<?> aiResponse = generateChallenge(payload);

                // 반환값 자체가 List<Map<String, Object>>이므로 바로 캐스팅
                List<Map<String, Object>> newChallenges = (List<Map<String, Object>>) aiResponse.getBody();


                // 이후 selectedChallenges에 추가
                selectedChallenges.addAll(newChallenges);

                // firestore 에 저장
                for (Map<String, Object> challenge : challenges) {
                    db.collection("challenges").add(challenge);
                }

            }
            return ResponseEntity.ok(Collections.singletonMap("challenges", selectedChallenges));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // /challenge?challengeId=abc123 또는 /challenge
    @GetMapping("")
    public ResponseEntity<?> getChallenge(@RequestParam(value = "challengeId", required = false) String challengeId) {
        try {
            if (challengeId == null || challengeId.isBlank()) {
                // challengeId 없으면 랜덤 챌린지 8개 반환
                return userChallengeService.getRandomChallenges();
            } else {
                // challengeId 있으면 해당 챌린지 디테일 반환
                return userChallengeService.getChallengeDetail(challengeId);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    // 호스트가 챌린지 등록
    @PostMapping(value = "/community", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addChallenge(
            @RequestPart("challenge") Map<String, Object> challenge,
            @RequestPart("images") MultipartFile[] images) throws IOException {

        try {
            ResponseEntity<?> responseData = communityService.addChallenge(challenge, images);

            return responseData;

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // 호스트의 챌린지 완료 확인
    // 호스트가 없을 시, 관리자에게
}

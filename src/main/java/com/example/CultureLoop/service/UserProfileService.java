package com.example.CultureLoop.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserProfileService {

    public ResponseEntity<?> OngoingChallenge() {
        Firestore db = FirestoreClient.getFirestore();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        try {DocumentSnapshot snapshot = db.collection("users").document(email).get().get();
            List<String> ongoingIds = (List<String>) snapshot.get("ongoing");


            if (ongoingIds == null || ongoingIds.isEmpty()) {
                return ResponseEntity.ok(Collections.singletonMap("challenges", new ArrayList<>()));
            }


            List<Map<String, Object>> ongoingChallenges = new ArrayList<>();

            for (String challengeId : ongoingIds) {
                if(challengeId == null || challengeId.isEmpty()) {
                    continue;
                }
                DocumentSnapshot challengeDoc = db.collection("challenges").document(challengeId).get().get();

                if (challengeDoc.exists()) {
                    Map<String, Object> challengeData = challengeDoc.getData();
                    challengeData.put("challengeId", challengeDoc.getId());  // id 포함하고 싶다면
                    ongoingChallenges.add(challengeData);
                }
            }

            return ResponseEntity.ok(Collections.singletonMap("challenges", ongoingChallenges));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 완료한 챌린지 모아보기
    public ResponseEntity<?> CompletedChallenge() {
        Firestore db = FirestoreClient.getFirestore();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        try {
            // 사용자 문서 조회
            DocumentSnapshot snapshot = db.collection("users").document(email).get().get();
            Object completedRaw = snapshot.get("completed");

            // completed 필드가 List 형태가 아닐 경우 대비
            List<String> completedIds = (completedRaw instanceof List)
                    ? (List<String>) completedRaw
                    : new ArrayList<>();

            if (completedIds.isEmpty()) {
                return ResponseEntity.ok(Collections.singletonMap("challenges", new ArrayList<>()));
            }

            List<Map<String, Object>> completedChallenges = new ArrayList<>();

            for (String challengeId : completedIds) {
                if (challengeId == null || challengeId.trim().isEmpty()) {
                    continue; // null 또는 빈 문자열 무시
                }

                DocumentSnapshot challengeDoc = db.collection("challenges").document(challengeId).get().get();
                if (!challengeDoc.exists()) {
                    continue; // 문서 존재하지 않음
                }

                Map<String, Object> challengeData = challengeDoc.getData();
                if (challengeData == null) {
                    continue;
                }

                challengeData.put("challengeId", challengeDoc.getId());
                completedChallenges.add(challengeData);
            }

            return ResponseEntity.ok(Collections.singletonMap("challenges", completedChallenges));

        } catch (Exception e) {
            e.printStackTrace(); // 콘솔 로그
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("CompletedChallenge 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // log 모아보기
    public ResponseEntity<?> ChallengeLog() {
        Firestore db = FirestoreClient.getFirestore();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        try {
            DocumentSnapshot snapshot = db.collection("users").document(email).get().get();
            List<String> logIds = (List<String>) snapshot.get("logs");

            if (logIds == null || logIds.isEmpty()) {
                return ResponseEntity.ok(Collections.singletonMap("logs", new ArrayList<>()));
            }

            List<Map<String, Object>> challengesLog = new ArrayList<>();

            for (String logId : logIds) {
                if(logId == null || logId.isEmpty()) {
                    continue;
                }
                DocumentSnapshot logDoc = db.collection("users_log").document(logId).get().get();

                if (logDoc.exists()) {
                    Map<String, Object> logData = logDoc.getData();
                    logData.put("logId", logDoc.getId());  // id 포함하고 싶다면
                    challengesLog.add(logData);
                }
            }


            return ResponseEntity.ok(Collections.singletonMap("logs", challengesLog));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 리워드 모아보기
    public ResponseEntity<?> getUserRewards() {
        Firestore db = FirestoreClient.getFirestore();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        try {
            // 유저 문서 조회
            DocumentSnapshot snapshot = db.collection("users").document(email).get().get();
            Object rewardsRaw = snapshot.get("rewards");

            List<String> rewardIds = (rewardsRaw instanceof List)
                    ? (List<String>) rewardsRaw
                    : new ArrayList<>();

            if (rewardIds.isEmpty()) {
                return ResponseEntity.ok(Collections.singletonMap("rewards", new ArrayList<>()));
            }

            List<Map<String, Object>> rewardDetails = new ArrayList<>();

            for (String rewardId : rewardIds) {
                if (rewardId == null || rewardId.trim().isEmpty()) continue;

                DocumentSnapshot rewardDoc = db.collection("rewards").document(rewardId).get().get();
                if (!rewardDoc.exists()) continue;

                Map<String, Object> rewardData = rewardDoc.getData();
                if (rewardData == null) continue;

                rewardData.put("rewardId", rewardDoc.getId()); // ID 포함
                rewardDetails.add(rewardData);
            }

            return ResponseEntity.ok(Collections.singletonMap("rewards", rewardDetails));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("리워드 조회 중 오류 발생: " + e.getMessage());
        }
    }

    // 뱃지 모아보기
    public ResponseEntity<?> getUserBadges() {
        Firestore db = FirestoreClient.getFirestore();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        try {
            // 유저 문서 조회
            DocumentSnapshot snapshot = db.collection("users").document(email).get().get();
            Object badgesRaw = snapshot.get("badges");

            // badges 필드가 List인지 확인
            List<String> badges = (badgesRaw instanceof List)
                    ? (List<String>) badgesRaw
                    : new ArrayList<>();

            // 반환 형식 통일
            return ResponseEntity.ok(Collections.singletonMap("badges", badges));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("뱃지 조회 중 오류 발생: " + e.getMessage());
        }
    }
}

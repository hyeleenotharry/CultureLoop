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

    public ResponseEntity<?> CompletedChallenge() {
        Firestore db = FirestoreClient.getFirestore();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        try {
            DocumentSnapshot snapshot = db.collection("users").document(email).get().get();
            List<String> completedIds = (List<String>) snapshot.get("completed");

            if (completedIds == null || completedIds.isEmpty()) {
                return ResponseEntity.ok(Collections.singletonMap("challenges", new ArrayList<>()));
            }

            List<Map<String, Object>> completedChallenges = new ArrayList<>();

            for (String challengeId : completedIds) {
                if(challengeId == null || challengeId.isEmpty()) {
                    continue;
                }
                DocumentSnapshot challengeDoc = db.collection("challenges").document(challengeId).get().get();

                if (challengeDoc.exists()) {
                    Map<String, Object> challengeData = challengeDoc.getData();
                    challengeData.put("challengeId", challengeDoc.getId());  // id 포함하고 싶다면
                    completedChallenges.add(challengeData);
                }
            }


            return ResponseEntity.ok(Collections.singletonMap("challenges", completedChallenges));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
}

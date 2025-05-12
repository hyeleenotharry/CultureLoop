package com.example.CultureLoop.controller;

import com.example.CultureLoop.DTO.PreferenceUpdateRequest;
import com.example.CultureLoop.service.CommunityService;
import com.example.CultureLoop.service.UserProfileService;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final CommunityService communityService;
    private final UserProfileService userProfileService;

    // JWT 기반 사용자 정보
    private String getEmailFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName(); // username = email로 JWT 만들었기 때문
    }

    @GetMapping("/me")
    public ResponseEntity<?> findUser() {
        try {
            String email = getEmailFromAuthentication();

            Firestore db = FirestoreClient.getFirestore();
            DocumentSnapshot snapshot = db.collection("users").document(email).get().get();

            if (!snapshot.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            return ResponseEntity.ok(snapshot.getData());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving user");
        }
    }

    // 사용자 선호도 설정
    @PostMapping("/me/preference")
    public ResponseEntity<?> updateUserPreference(@RequestBody PreferenceUpdateRequest request) {
        try {
            String email = getEmailFromAuthentication();

            Firestore db = FirestoreClient.getFirestore();
            db.collection("users").document(email).update("preference", request.getPreference());

            return ResponseEntity.ok("Preference updated");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating preference");
        }
    }

    // 사용자 개수 설정
    @PostMapping("/me/count")
    public ResponseEntity<?> updateUserCount(@RequestBody int count) {
        try {
            String email = getEmailFromAuthentication();

            Firestore db = FirestoreClient.getFirestore();
            db.collection("users").document(email).update("count", count);
            db.collection("users").document(email).update("isMember", 1);

            return ResponseEntity.ok("count updated");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // ongoing 불러오기
    @GetMapping("/me/ongoing")
    public ResponseEntity<?> getOngoingChallenge(){
        return userProfileService.OngoingChallenge();
    }

    //completed 불러오기
    @GetMapping("/me/completed")
    public ResponseEntity<?> getCompletedChallenge(){
        return userProfileService.CompletedChallenge();
    }

    // logs 불러오기
    @GetMapping("/me/logs")
    public ResponseEntity<?> getLogs(){
        return userProfileService.ChallengeLog();
    }

    // badges 불러오기
    @GetMapping("/me/badges")
    public ResponseEntity<?> getBadges(){
        return userProfileService.getUserBadges();
    }

    // rewards 불러오기
    @GetMapping("/me/rewards")
    public ResponseEntity<?> getRewards(){
        return userProfileService.getUserRewards();
    }
}

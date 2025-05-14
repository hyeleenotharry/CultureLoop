package com.example.CultureLoop.controller;

import com.example.CultureLoop.service.UserChallengeService;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/challenge")
@RequiredArgsConstructor
public class UserChallengeController {
    // challenge 상태를 바꾸는 api
    private final UserChallengeService userChallengeService;

    @PostMapping(value = "/user-challenge")
    public ResponseEntity<?> addUserChallenge(@RequestBody Map<String, Object> body) {
        String file = "";

        Boolean isCompleted = body.get("isCompleted").equals("true");
        String challengeId = body.get("challengeId").toString();
        try{
            file = body.get("file").toString();
        } catch (Exception e) {
            // nothing
        }

        // file은 isCompleted == true 일 때만 전달됨
        if (isCompleted && (file == null || file.isEmpty())) {
            return ResponseEntity.badRequest().body("완료된 챌린지는 뱃지가 필요합니다.");
        }

        userChallengeService.addUserChallenge(challengeId, isCompleted, file);

        return ResponseEntity.ok("처리 완료");
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelChallenge(@RequestBody String challengeId) {

        return userChallengeService.cancelChallenge(challengeId);
    }

    // 호스트가 있는 챌린지라면 버튼을 누를 시 호스트에게 알람이 가게
}

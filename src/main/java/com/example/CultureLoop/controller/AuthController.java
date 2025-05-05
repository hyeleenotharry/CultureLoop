package com.example.CultureLoop.controller;

import com.example.CultureLoop.service.JwtTokenProvider;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> authenticateWithFirebase(@RequestHeader("Authorization") String token) {
        try {
            // 1. Firebase ID 토큰 검증
            String idToken = token.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName(); // Google 계정 이름 (null일 수도 있음)
            // picture 필드는 getClaims()에서 추출
            String picture = null;
            Object pictureObj = decodedToken.getClaims().get("picture");
            if (pictureObj != null) {
                picture = pictureObj.toString();  // 프로필 이미지 URL
            }

            // 2. Firestore에 사용자 정보 저장
            Firestore db = FirestoreClient.getFirestore();
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", email);
            userData.put("name", name);
            userData.put("picture", picture);

            // uid 또는 email을 문서 ID로 선택 (여기선 email 사용)
            db.collection("users").document(email).set(userData);

            // 3. 서버 JWT 생성 및 반환
            String jwt = jwtTokenProvider.createToken(uid, email);
            return ResponseEntity.ok(Collections.singletonMap("token", jwt));
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Token");
        }
    }
}

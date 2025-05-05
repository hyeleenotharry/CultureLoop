package com.example.CultureLoop.service;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        Firestore db = FirestoreClient.getFirestore();
        try {
            // Firestore에 이미 존재하는지 확인
            boolean exists = db.collection("users").document(email).get().get().exists();
            if (!exists) {
                // 없으면 신규 생성
                Map<String, Object> userData = new HashMap<>();
                userData.put("email", email);
                userData.put("name", name);
                userData.put("picture", picture);
                userData.put("preference", null);  // 초기값

                db.collection("users").document(email).set(userData);
                log.info("New user registered: {}", email);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during Firestore signup", e);
        }

        return oAuth2User;
    }
}


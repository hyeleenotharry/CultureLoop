package com.example.CultureLoop.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@Configuration
public class FirebaseConfig {
    @Value("${firebase.config.path}")
    private String firebaseConfigPath;

    @PostConstruct
    public void init() throws IOException {
        log.info("Initializing firebase");
        FileInputStream serviceAccount = new FileInputStream(firebaseConfigPath);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setStorageBucket("culture_loop")
                .build();

        FirebaseApp app = FirebaseApp.initializeApp(options);
        log.info("Firebase initialized" + app.getName());
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        return FirebaseAuth.getInstance();
    }

}


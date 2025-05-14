package com.example.CultureLoop.controller;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @PostMapping("/cleanup-old-challenges")
    public ResponseEntity<?> deleteExpiredChallenges() {
        try {
            Firestore db = FirestoreClient.getFirestore();

            // 오늘 날짜
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            ApiFuture<QuerySnapshot> future = db.collection("challenges").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            int deletedCount = 0;

            for (QueryDocumentSnapshot doc : documents) {
                String startDateStr = doc.getString("start_date");

                // 유효하지 않거나 null이면 skip
                if (startDateStr == null) continue;

                try {
                    LocalDate startDate = LocalDate.parse(startDateStr, formatter);

                    // 오늘보다 이전이면 삭제
                    if (startDate.isBefore(today)) {
                        db.collection("challenges").document(doc.getId()).delete();
                        deletedCount++;
                    }
                } catch (Exception e) {
                    // 잘못된 형식 무시
                    System.err.println("Invalid date format in document " + doc.getId() + ": " + startDateStr);
                }
            }

            return ResponseEntity.ok("🧹 Deleted " + deletedCount + " expired challenges.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Cleanup failed: " + e.getMessage());
        }
    }
}

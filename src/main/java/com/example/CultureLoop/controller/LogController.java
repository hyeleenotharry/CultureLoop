package com.example.CultureLoop.controller;


import com.example.CultureLoop.service.logService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/log")
public class LogController {
    private final logService service;

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> generateLog(@RequestPart("log") Map<String, Object> log,
                                         @RequestPart("images") MultipartFile[] images) throws IOException {

        try {
            ResponseEntity<?> response = service.generateTravelLog(log, images);

            return response;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("저장 중 오류 발생: " + e.getMessage());
        }
    }

    @PostMapping(value = "/vision", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> generateLog(@RequestPart("images") MultipartFile[] images) throws IOException {

        try {
            List<String> tags = service.analyzeImagesConcurrently(images);
            ResponseEntity<?> response = (ResponseEntity<?>) tags.stream();

            return response;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("저장 중 오류 발생: " + e.getMessage());
        }
    }

    @GetMapping("")
    public ResponseEntity<?> getLog(@RequestParam String logId) {
        try {
            return service.getUserLogById(logId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

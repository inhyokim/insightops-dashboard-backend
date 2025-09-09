package com.insightops.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "service", "insightops-dashboard-backend",
            "timestamp", Instant.now(),
            "version", "1.0.0"
        ));
    }

    @GetMapping("/api")
    public ResponseEntity<Map<String, String>> api() {
        return ResponseEntity.ok(Map.of(
            "message", "InsightOps Dashboard Backend API",
            "version", "1.0.0"
        ));
    }
}


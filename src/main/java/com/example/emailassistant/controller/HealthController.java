package com.example.emailassistant.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @Value("${app.welcome-message:Welcome}")
    private String welcomeMessage;

    @GetMapping("/")
    public ResponseEntity<String> root() {
        return ResponseEntity.ok(welcomeMessage);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "Running"));
    }
}

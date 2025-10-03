package com.nelumbo.park.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SonarQubeController {

    private final List<Map<String, Object>> webhookPayloads = Collections.synchronizedList(new ArrayList<>());

    @PostMapping("/sonarqube-webhook")
    public ResponseEntity<Void> sonarqubeWebhook(@RequestBody Map<String, Object> payload) {
        webhookPayloads.add(payload);
        System.out.println("Received SonarQube webhook payload: " + payload);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sonarqube-logs")
    public ResponseEntity<List<Map<String, Object>>> getSonarQubeLogs() {
        return ResponseEntity.ok(webhookPayloads);
    }
}

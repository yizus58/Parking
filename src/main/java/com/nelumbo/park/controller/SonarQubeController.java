package com.nelumbo.park.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SonarQubeController {

    @PostMapping("/sonarqube-webhook")
    public ResponseEntity<Void> sonarqubeWebhook(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok().build();
    }
}

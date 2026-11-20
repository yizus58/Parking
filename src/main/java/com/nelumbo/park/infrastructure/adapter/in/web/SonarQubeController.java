package com.nelumbo.park.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Receives SonarQube webhook payloads and keeps only the most recent ones in
 * memory, so the endpoint cannot be used to exhaust the heap. Reading them
 * is restricted to administrators.
 */
@RestController
@RequestMapping("/api")
public class SonarQubeController {

    static final int MAX_PAYLOADS = 50;

    private final Deque<Map<String, Object>> webhookPayloads = new ArrayDeque<>();

    @PostMapping("/sonarqube-webhook")
    public ResponseEntity<Void> sonarqubeWebhook(@RequestBody Map<String, Object> payload) {
        synchronized (webhookPayloads) {
            if (webhookPayloads.size() == MAX_PAYLOADS) {
                webhookPayloads.removeFirst();
            }
            webhookPayloads.addLast(payload);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sonarqube-logs")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getSonarQubeLogs() {
        synchronized (webhookPayloads) {
            return ResponseEntity.ok(List.copyOf(webhookPayloads));
        }
    }
}

package com.example.emailassistant.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.emailassistant.service.GmailService;

@RestController
@RequestMapping("/gmail")
public class GmailController {

    private static final Logger log = LoggerFactory.getLogger(GmailController.class);
    private final GmailService gmailService;

    public GmailController(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Map<String, Object>>> unread() {
        try {
            var msgs = gmailService.getUnreadMessages();
            return ResponseEntity.ok(msgs);
        } catch (Exception e) {
            log.error("Failed to fetch unread Gmail messages", e);
            return ResponseEntity.status(500).body(List.of(Map.of("error", (Object) e.getMessage())));
        }
    }
}

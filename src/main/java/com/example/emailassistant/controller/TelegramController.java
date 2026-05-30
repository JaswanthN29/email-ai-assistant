package com.example.emailassistant.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.emailassistant.service.TelegramService;

@RestController
@RequestMapping("/telegram")
public class TelegramController {

    private final TelegramService telegramService;

    public TelegramController(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        String message = telegramService.sayHello();
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, String> payload) {
        String chatId = payload.get("chatId");
        String text = payload.get("text");

        if (chatId == null || chatId.isBlank() || text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing chatId or text"));
        }

        boolean success = telegramService.sendMessage(chatId, text);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Message sent successfully"));
        } else {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to send message via Telegram API"));
        }
    }

    @GetMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessageGet(
            @RequestParam String chatId,
            @RequestParam String text) {
        if (chatId == null || chatId.isBlank() || text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing chatId or text"));
        }

        boolean success = telegramService.sendMessage(chatId, text);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Message sent successfully"));
        } else {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to send message via Telegram API"));
        }
    }
}


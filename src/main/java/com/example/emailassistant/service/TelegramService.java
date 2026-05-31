package com.example.emailassistant.service;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);
    private final RestClient restClient = RestClient.create();

    @Value("${telegram.bot.token:}")
    private String telegramBotToken;

    @Value("${telegram.chat.id:}")
    private String defaultChatId;

    public String sayHello() {
        if (telegramBotToken == null || telegramBotToken.isBlank()) {
            log.warn("Telegram bot token is missing or empty");
            return "Hello (telegram token not configured)";
        }

        log.info("Telegram bot token is configured; returning hello message");
        return "Hello";
    }

    public boolean sendMessage(String chatId, String text) {
        String targetChatId = (chatId == null || chatId.isBlank()) ? defaultChatId : chatId;

        if (telegramBotToken == null || telegramBotToken.isBlank()) {
            log.warn("Telegram bot token is missing or empty; cannot send message");
            return false;
        }

        if (targetChatId == null || targetChatId.isBlank()) {
            log.warn("Telegram chatId is missing or empty; cannot send message");
            return false;
        }

        try {
            String url = "https://api.telegram.org/bot" + telegramBotToken + "/sendMessage";
            var response = restClient.post()
                    .uri(url)
                    .body(Map.of("chat_id", targetChatId, "text", text))
                    .retrieve()
                    .toEntity(Map.class);

            log.info("Telegram sendMessage response status: {}, body: {}", response.getStatusCode(), response.getBody());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Failed to send Telegram message", e);
            return false;
        }
    }
}


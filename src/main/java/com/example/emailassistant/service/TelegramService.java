package com.example.emailassistant.service;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.scheduling.annotation.Scheduled;

import com.example.emailassistant.model.TelegramChat;
import com.example.emailassistant.repository.TelegramChatRepository;

@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);
    private final RestClient restClient = RestClient.create();
    private final TelegramChatRepository telegramChatRepository;

    @Value("${telegram.bot.token:}")
    private String telegramBotToken;

    @Value("${telegram.chat.id:}")
    private String defaultChatId;

    public TelegramService(TelegramChatRepository telegramChatRepository) {
        this.telegramChatRepository = telegramChatRepository;
    }

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

    private long lastUpdateId = 0;

    /**
     * Polls getUpdates from the Telegram Bot API and persists any newly discovered chats in the database.
     * @return the number of new chats discovered and saved.
     */
    public int syncChats() {
        if (telegramBotToken == null || telegramBotToken.isBlank()) {
            log.warn("Telegram bot token is missing; cannot sync chats");
            return 0;
        }

        try {
            String url = "https://api.telegram.org/bot" + telegramBotToken + "/getUpdates";
            if (lastUpdateId > 0) {
                url += "?offset=" + (lastUpdateId + 1);
            }
            
            var response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
                log.warn("Telegram getUpdates response was not successful: {}", response);
                return 0;
            }

            var result = (java.util.List<?>) response.get("result");
            if (result == null || result.isEmpty()) {
                return 0;
            }

            int count = 0;
            for (Object updateObj : result) {
                if (!(updateObj instanceof Map)) continue;
                Map<?, ?> update = (Map<?, ?>) updateObj;
                
                if (update.containsKey("update_id")) {
                    long updateId = ((Number) update.get("update_id")).longValue();
                    if (updateId > lastUpdateId) {
                        lastUpdateId = updateId;
                    }
                }
                
                Map<?, ?> chat = null;
                if (update.containsKey("message")) {
                    Map<?, ?> message = (Map<?, ?>) update.get("message");
                    chat = (Map<?, ?>) message.get("chat");
                } else if (update.containsKey("my_chat_member")) {
                    Map<?, ?> myChatMember = (Map<?, ?>) update.get("my_chat_member");
                    chat = (Map<?, ?>) myChatMember.get("chat");
                }

                if (chat != null) {
                    Object idObj = chat.get("id");
                    if (idObj == null) continue;
                    String chatId = String.valueOf(idObj);
                    String firstName = (String) chat.get("first_name");
                    String lastName = (String) chat.get("last_name");

                    if (telegramChatRepository.findByChatId(chatId).isEmpty()) {
                        TelegramChat newChat = TelegramChat.builder()
                                .chatId(chatId)
                                .firstName(firstName)
                                .lastName(lastName)
                                .notificationsEnabled(true)
                                .build();
                        telegramChatRepository.save(newChat);
                        log.info("Saved new Telegram chat to DB: {} ({} {})", chatId, firstName, lastName);
                        count++;
                    }
                }
            }
            return count;
        } catch (Exception e) {
            log.error("Failed to sync Telegram chats from updates API", e);
            return 0;
        }
    }

    /**
     * Background scheduled polling task to automatically discover new chat subscriptions.
     * Runs once an hour by default (configurable via telegram.sync.interval-ms).
     */
    @Scheduled(fixedDelayString = "${telegram.sync.interval-ms:3600000}")
    public void scheduledSync() {
        log.debug("Running scheduled Telegram chat sync...");
        int synced = syncChats();
        if (synced > 0) {
            log.info("Scheduled sync completed. Synced {} new chats.", synced);
        }
    }
}


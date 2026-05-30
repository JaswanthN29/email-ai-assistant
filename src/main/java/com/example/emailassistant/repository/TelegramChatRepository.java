package com.example.emailassistant.repository;

import com.example.emailassistant.model.TelegramChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TelegramChatRepository extends JpaRepository<TelegramChat, Long> {
    Optional<TelegramChat> findByChatId(String chatId);
}

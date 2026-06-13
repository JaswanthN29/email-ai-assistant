package com.example.emailassistant.repository;

import com.example.emailassistant.model.EmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long> {
    Optional<EmailMessage> findByMessageId(String messageId);
}

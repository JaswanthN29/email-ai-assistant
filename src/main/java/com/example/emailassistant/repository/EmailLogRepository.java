package com.example.emailassistant.repository;

import com.example.emailassistant.model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    Optional<EmailLog> findByGmailMessageId(String gmailMessageId);
}

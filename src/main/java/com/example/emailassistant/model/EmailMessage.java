package com.example.emailassistant.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gmail_emails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", unique = true, nullable = false)
    private String messageId;

    @Column(name = "sender", length = 512)
    private String sender;

    @Column(name = "subject", length = 1024)
    private String subject;

    @Column(name = "snippet", length = 2048)
    private String snippet;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "last_updated_time", nullable = false)
    private LocalDateTime lastUpdatedTime = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdatedTime = LocalDateTime.now();
    }
}

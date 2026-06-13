package com.example.emailassistant.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gmail_message_id", unique = true, nullable = false)
    private String gmailMessageId;

    @Column(name = "sender")
    private String sender;

    @Column(name = "subject")
    private String subject;

    @Column(name = "forwarded_at", nullable = false)
    private LocalDateTime forwardedAt = LocalDateTime.now();

    @Column(name = "successfully_forwarded", nullable = false)
    private boolean successfullyForwarded = true;

    @PrePersist
    protected void onCreate() {
        forwardedAt = LocalDateTime.now();
    }
}

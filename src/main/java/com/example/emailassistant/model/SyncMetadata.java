package com.example.emailassistant.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sync_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncMetadata {

    @Id
    @Column(name = "metadata_key", nullable = false, unique = true)
    private String key;

    @Column(name = "metadata_value", nullable = false, length = 1024)
    private String value;

    @Column(name = "last_updated_time", nullable = false)
    private LocalDateTime lastUpdatedTime = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdatedTime = LocalDateTime.now();
    }
}

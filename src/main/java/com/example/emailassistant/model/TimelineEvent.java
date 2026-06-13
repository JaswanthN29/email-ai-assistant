package com.example.emailassistant.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "timeline_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_date", nullable = false)
    private String eventDate;

    @Column(name = "badge", nullable = false)
    private String badge;

    @Column(name = "badge_color", nullable = false)
    private String badgeColor;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "subtitle", nullable = false)
    private String subtitle;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}

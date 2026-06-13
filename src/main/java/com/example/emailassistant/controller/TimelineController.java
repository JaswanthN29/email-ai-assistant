package com.example.emailassistant.controller;

import com.example.emailassistant.model.TimelineEvent;
import com.example.emailassistant.repository.TimelineEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TimelineController {

    private final TimelineEventRepository repository;

    public TimelineController(TimelineEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/timeline")
    public ResponseEntity<List<TimelineEvent>> getTimelineEvents() {
        return ResponseEntity.ok(repository.findAllByOrderByIdDesc());
    }
}

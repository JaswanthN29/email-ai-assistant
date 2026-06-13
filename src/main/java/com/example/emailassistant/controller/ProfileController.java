package com.example.emailassistant.controller;

import com.example.emailassistant.model.ProfileImage;
import com.example.emailassistant.repository.ProfileImageRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileImageRepository profileImageRepository;

    public ProfileController(ProfileImageRepository profileImageRepository) {
        this.profileImageRepository = profileImageRepository;
    }

    @GetMapping("/image")
    public ResponseEntity<byte[]> getProfileImage() {
        List<ProfileImage> images = profileImageRepository.findAll();
        if (images.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ProfileImage img = images.get(0);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(img.getContentType()))
                .body(img.getData());
    }
}

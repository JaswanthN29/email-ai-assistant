package com.example.emailassistant.initializer;

import com.example.emailassistant.model.ProfileImage;
import com.example.emailassistant.repository.ProfileImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.InputStream;

@Component
public class ProfileImageInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProfileImageInitializer.class);
    private final ProfileImageRepository profileImageRepository;

    public ProfileImageInitializer(ProfileImageRepository profileImageRepository) {
        this.profileImageRepository = profileImageRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (profileImageRepository.count() == 0) {
            try {
                ClassPathResource resource = new ClassPathResource("profile.jpg");
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        byte[] data = is.readAllBytes();
                        ProfileImage profileImage = ProfileImage.builder()
                                .data(data)
                                .contentType("image/jpeg")
                                .build();
                        profileImageRepository.save(profileImage);
                        log.info("Successfully loaded profile image into database from resources/profile.jpg");
                    }
                } else {
                    log.warn("profile.jpg resource not found in classpath; skipping DB profile image seeding");
                }
            } catch (Exception e) {
                log.error("Failed to seed profile image to database", e);
            }
        } else {
            log.info("Profile image table already seeded in database; count={}", profileImageRepository.count());
        }
    }
}

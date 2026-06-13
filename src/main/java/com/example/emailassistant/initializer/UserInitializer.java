package com.example.emailassistant.initializer;

import com.example.emailassistant.model.User;
import com.example.emailassistant.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class UserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserInitializer.class);
    private final UserRepository userRepository;

    public UserInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User firstUser = User.builder()
                    .username("JaswanthN")
                    .password("install")
                    .build();
            userRepository.save(firstUser);
            log.info("Successfully seeded first user JaswanthN into database");
        } else {
            // Check if JaswanthN exists, if not seed it specifically
            if (userRepository.findByUsername("JaswanthN").isEmpty()) {
                User user = User.builder()
                        .username("JaswanthN")
                        .password("install")
                        .build();
                userRepository.save(user);
                log.info("Successfully seeded user JaswanthN into database");
            }
        }
    }
}

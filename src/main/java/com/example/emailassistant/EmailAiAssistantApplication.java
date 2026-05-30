package com.example.emailassistant;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EmailAiAssistantApplication {

	public static void main(String[] args) {
		createLogDirectory();
		SpringApplication.run(EmailAiAssistantApplication.class, args);
	}

	private static void createLogDirectory() {
		try {
			Path logs = Paths.get("logs");
			if (Files.notExists(logs)) {
				Files.createDirectories(logs);
			}
		} catch (Exception e) {
			System.err.println("Unable to create logs directory: " + e.getMessage());
		}
	}

}

package com.example.emailassistant.initializer;

import com.example.emailassistant.model.TimelineEvent;
import com.example.emailassistant.repository.TimelineEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class TimelineEventInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TimelineEventInitializer.class);
    private final TimelineEventRepository repository;
    private final DataSource dataSource;

    public TimelineEventInitializer(TimelineEventRepository repository, DataSource dataSource) {
        this.repository = repository;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Running database schema updates and git commit log analyzer...");

        // Ensure description column is altered to TEXT in the database
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE timeline_events ALTER COLUMN description TYPE TEXT");
            log.info("Successfully verified/altered timeline_events.description column type to TEXT.");
        } catch (Exception e) {
            log.warn("Database column alter check complete: " + e.getMessage());
        }

        try {
            // Run git log command to get YYYY-MM-DD and commit message
            ProcessBuilder pb = new ProcessBuilder("git", "log", "--date=short", "--pretty=format:%ad|%s");
            Process process = pb.start();

            List<String[]> commitsList = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split("\\|", 2);
                    if (parts.length == 2) {
                        String date = parts[0].trim();
                        String msg = parts[1].trim();
                        // Ignore standard merge commits or branch tags
                        if (msg.toLowerCase().contains("merge branch")) continue;
                        commitsList.add(new String[]{date, msg});
                    }
                }
            }

            process.waitFor();

            if (commitsList.isEmpty()) {
                log.warn("No commits found in git log. Skipping database sync.");
                return;
            }

            // Sync the database table by clearing old mock records
            repository.deleteAll();

            List<TimelineEvent> dbEvents = new ArrayList<>();
            DateTimeFormatter parseFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

            for (String[] entry : commitsList) {
                String rawDate = entry[0];
                String commitMsg = entry[1];
                
                // Format raw date
                String formattedDate = rawDate;
                try {
                    LocalDate date = LocalDate.parse(rawDate, parseFormatter);
                    formattedDate = date.format(displayFormatter);
                } catch (Exception e) {
                    // Fallback to raw date format
                }

                String badge = "Update";
                String badgeColor = "rgba(113, 113, 122, 0.1)"; // Slate gray fallback
                String title = "Daily Improvements";
                String subtitle = "Application updates and fixes";

                // Feature extraction algorithm based on keywords or commit messages
                boolean hasRag = false;
                boolean hasTelegram = false;
                boolean hasDbTimeline = false;
                boolean hasMedia = false;
                boolean hasTheme = false;
                boolean hasDocker = false;
                String featTitle = null;

                String low = commitMsg.toLowerCase();
                if (low.contains("rag") || low.contains("pega") || low.contains("vector") || low.contains("pinecone") || low.contains("embedding")) {
                    hasRag = true;
                }
                if (low.contains("telegram") || low.contains("broadcaster") || low.contains("sender") || low.contains("chat")) {
                    hasTelegram = true;
                }
                if (low.contains("timeline") && (low.contains("database") || low.contains("db") || low.contains("migration") || low.contains("jpa"))) {
                    hasDbTimeline = true;
                }
                if (low.contains("avatar") || low.contains("photo") || low.contains("image") || low.contains("upload")) {
                    hasMedia = true;
                }
                if (low.contains("theme") || low.contains("dark") || low.contains("light")) {
                    hasTheme = true;
                }
                if (low.contains("docker") || low.contains("dockerfile") || low.contains("deployment") || low.contains("railway")) {
                    hasDocker = true;
                }
                
                // General feat: extraction fallback
                if (featTitle == null && (low.startsWith("feat:") || low.startsWith("feat("))) {
                    int colonIdx = commitMsg.indexOf(":");
                    if (colonIdx != -1) {
                        String desc = commitMsg.substring(colonIdx + 1).trim();
                        if (!desc.isEmpty()) {
                            featTitle = "Feature: " + Character.toUpperCase(desc.charAt(0)) + desc.substring(1);
                        }
                    }
                }

                if (hasRag) {
                    badge = "AI Search";
                    badgeColor = "rgba(124, 58, 237, 0.1)"; // Purple
                    title = "Feature: Pega RAG Search Engine";
                    subtitle = "Retrieval-Augmented Generation using Pinecone Vector DB and Gemini Embeddings";
                } else if (hasTelegram) {
                    badge = "Telegram API";
                    badgeColor = "rgba(14, 165, 233, 0.1)"; // Sky Blue
                    title = "Feature: Telegram Contact Router";
                    subtitle = "Routing user communications, feedback, and client diagnostics to Telegram channels";
                } else if (hasDbTimeline) {
                    badge = "SQL Timeline";
                    badgeColor = "rgba(22, 163, 74, 0.1)"; // Green
                    title = "Feature: Database-Backed Evolution Timeline";
                    subtitle = "Dynamic runtime parsing of repository git log commits stored persistently in PostgreSQL";
                } else if (hasMedia) {
                    badge = "Media Storage";
                    badgeColor = "rgba(236, 72, 153, 0.1)"; // Pink
                    title = "Feature: Profile Image Media Service";
                    subtitle = "Uploading and saving user profile avatars directly to persistent database storage";
                } else if (hasTheme) {
                    badge = "Aesthetics";
                    badgeColor = "rgba(99, 102, 241, 0.1)"; // Indigo
                    title = "Feature: Light & Dark Preference Toggle";
                    subtitle = "Minimalist interface style preference engine with local persistence storage";
                } else if (hasDocker) {
                    badge = "Deployment";
                    badgeColor = "rgba(6, 182, 212, 0.1)"; // Cyan
                    title = "Feature: Multi-stage Cloud Deployment";
                    subtitle = "Optimizing Docker compilation builds and resources for Railway hosting release";
                } else if (featTitle != null) {
                    badge = "New Feature";
                    badgeColor = "rgba(37, 99, 235, 0.1)"; // Blue
                    title = featTitle;
                    subtitle = "Newly integrated backend capability in the application";
                } else {
                    badge = "System Update";
                    badgeColor = "rgba(113, 113, 122, 0.1)"; // Slate Gray
                    title = "Feature: System Stability & Architecture Optimizations";
                    subtitle = "Dependency tuning, log verbosity controls, and backend reliability patches";
                }

                // Build bullet list of git commit
                StringBuilder descBuilder = new StringBuilder();
                descBuilder.append("<ul style=\"margin-left: 1.2rem; padding-left: 0; list-style-type: disc;\">");
                descBuilder.append("<li style=\"margin-bottom: 0.3rem;\">").append(commitMsg).append("</li>");
                descBuilder.append("</ul>");

                dbEvents.add(TimelineEvent.builder()
                        .eventDate(formattedDate)
                        .badge(badge)
                        .badgeColor(badgeColor)
                        .title(title)
                        .subtitle(subtitle)
                        .description(descBuilder.toString())
                        .build());
            }

            // Sync database. Reverse list to ensure highest date gets the highest ID.
            Collections.reverse(dbEvents);
            repository.saveAll(dbEvents);
            log.info("Git commit log successfully synced to PostgreSQL database. Total days recorded: {}", dbEvents.size());

        } catch (Exception e) {
            log.error("Failed to run git log analyzer database seeder", e);
        }
    }
}

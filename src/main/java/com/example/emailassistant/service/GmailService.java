package com.example.emailassistant.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.emailassistant.model.EmailMessage;
import com.example.emailassistant.model.SyncMetadata;
import com.example.emailassistant.repository.EmailMessageRepository;
import com.example.emailassistant.repository.SyncMetadataRepository;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

@Service
public class GmailService {

    private static final Logger log = LoggerFactory.getLogger(GmailService.class);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private final EmailMessageRepository emailMessageRepository;
    private final SyncMetadataRepository syncMetadataRepository;

    public GmailService(EmailMessageRepository emailMessageRepository, SyncMetadataRepository syncMetadataRepository) {
        this.emailMessageRepository = emailMessageRepository;
        this.syncMetadataRepository = syncMetadataRepository;
    }

    @Value("${google.credentials.file:credentials.json}")
    private String credentialsFilePath;

    @Value("${google.tokens.dir:tokens}")
    private String tokensDir;

    @Value("${app.name:email-ai-assistant}")
    private String appName;

    @Value("${google.oauth.port:8888}")
    private int oauthPort;

    private Credential getCredentials() throws Exception {
        final var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        try (InputStream in = new FileInputStream(credentialsFilePath)) {
            var clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            var flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets,
                    List.of("https://www.googleapis.com/auth/gmail.readonly"))
                    .setDataStoreFactory(new FileDataStoreFactory(new File(tokensDir)))
                    .setAccessType("offline").build();

            List<Integer> ports = new ArrayList<>();
            ports.add(oauthPort);
            for (int offset = 1; offset <= 10; offset++) {
                ports.add(oauthPort + offset);
            }

            Exception lastException = null;
            for (int port : ports) {
                LocalServerReceiver receiver = null;
                try {
                    log.info("Attempting Gmail OAuth callback on port {}", port);
                    receiver = new LocalServerReceiver.Builder().setPort(port).build();
                    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
                } catch (Exception e) {
                    lastException = e;
                    log.warn("OAuth callback failed on port {}: {}", port, e.getMessage());
                } finally {
                    if (receiver != null) {
                        try {
                            receiver.stop();
                        } catch (Exception ignored) {
                            // ignore cleanup errors
                        }
                    }
                }
            }
            throw lastException != null ? lastException
                    : new IllegalStateException("Unable to start Gmail OAuth callback server on any port.");
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getUnreadMessages() throws Exception {
        Credential credential = getCredentials();

        final var transport = GoogleNetHttpTransport.newTrustedTransport();
        HttpRequestFactory requestFactory = transport.createRequestFactory(credential);

        GenericUrl listUrl = new GenericUrl("https://gmail.googleapis.com/gmail/v1/users/me/messages?q=is:unread");
        HttpRequest listReq = requestFactory.buildRequest("GET", listUrl, null);
        HttpResponse listResp = listReq.execute();

        Map<String, Object> listMap = listResp.parseAs(Map.class);
        var messages = (List<Map<String, Object>>) listMap.get("messages");
        if (messages == null || messages.isEmpty()) return new ArrayList<>();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> m : messages) {
            String id = Objects.toString(m.get("id"));
            GenericUrl msgUrl = new GenericUrl("https://gmail.googleapis.com/gmail/v1/users/me/messages/" + id + "?format=full");
            HttpRequest msgReq = requestFactory.buildRequest("GET", msgUrl, null);
            HttpResponse msgResp = msgReq.execute();
            Map<String, Object> full = msgResp.parseAs(Map.class);

            String snippet = Objects.toString(full.getOrDefault("snippet", ""));

            Map<String, Object> payload = (Map<String, Object>) full.get("payload");
            String subject = "";
            String from = "";
            if (payload != null && payload.get("headers") instanceof List) {
                var headers = (List<Map<String, Object>>) payload.get("headers");
                for (Map<String, Object> h : headers) {
                    String name = Objects.toString(h.get("name"), "");
                    String value = Objects.toString(h.get("value"), "");
                    if ("Subject".equalsIgnoreCase(name)) subject = value;
                    if ("From".equalsIgnoreCase(name)) from = value;
                }
            }

            String body = "";
            if (payload != null) {
                Map<String, Object> bodyMap = (Map<String, Object>) payload.get("body");
                if (bodyMap != null && bodyMap.get("data") != null) {
                    body = new String(Base64.getUrlDecoder().decode(Objects.toString(bodyMap.get("data"))));
                } else if (payload.get("parts") instanceof List) {
                    var parts = (List<Map<String, Object>>) payload.get("parts");
                    for (Map<String, Object> p : parts) {
                        String mime = Objects.toString(p.get("mimeType"), "");
                        if (mime.contains("text/plain") || mime.contains("text/html")) {
                            Map<String, Object> pbody = (Map<String, Object>) p.get("body");
                            if (pbody != null && pbody.get("data") != null) {
                                body = new String(Base64.getUrlDecoder().decode(Objects.toString(pbody.get("data"))));
                                break;
                            }
                        }
                    }
                }
            }

            result.add(Map.of(
                    "id", id,
                    "threadId", Objects.toString(full.get("threadId"), ""),
                    "from", from,
                    "subject", subject,
                    "snippet", snippet,
                    "body", body
            ));

            // Save to database
            if (emailMessageRepository.findByMessageId(id).isEmpty()) {
                EmailMessage emailMessage = EmailMessage.builder()
                        .messageId(id)
                        .sender(from)
                        .subject(subject)
                        .snippet(snippet)
                        .body(body)
                        .build();
                emailMessageRepository.save(emailMessage);
                log.info("Saved email message to database: {}", id);
            }
        }

        // Save last sync/update time
        SyncMetadata syncMetadata = SyncMetadata.builder()
                .key("LAST_GMAIL_SYNC_TIME")
                .value(LocalDateTime.now().toString())
                .build();
        syncMetadataRepository.save(syncMetadata);
        log.info("Updated Gmail sync metadata with current timestamp: {}", syncMetadata.getValue());

        return result;
    }
}

package com.example.emailassistant;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

@Service
public class GmailService {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    @Value("${google.credentials.file:credentials.json}")
    private String credentialsFilePath;

    @Value("${google.tokens.dir:tokens}")
    private String tokensDir;

    @Value("${app.name:email-ai-assistant}")
    private String appName;

    private Gmail getGmail() throws Exception {
        final var httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        try (InputStream in = new FileInputStream(credentialsFilePath)) {
            var clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            var flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets,
                    List.of(GmailScopes.GMAIL_READONLY))
                    .setDataStoreFactory(new FileDataStoreFactory(new File(tokensDir)))
                    .setAccessType("offline").build();

            var receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            return new Gmail.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(appName).build();
        }
    }

    public List<Map<String, String>> getUnreadMessages() throws Exception {
        Gmail service = getGmail();

        ListMessagesResponse listResponse = service.users().messages().list("me").setQ("is:unread").execute();
        var messages = listResponse.getMessages();
        if (messages == null || messages.isEmpty()) return new ArrayList<>();

        List<Map<String, String>> result = new ArrayList<>();
        for (Message m : messages) {
            Message full = service.users().messages().get("me", m.getId()).setFormat("full").execute();

            String snippet = full.getSnippet();
            String subject = full.getPayload().getHeaders().stream().filter(h -> "Subject".equalsIgnoreCase(h.getName())).map(h -> h.getValue()).findFirst().orElse("");
            String from = full.getPayload().getHeaders().stream().filter(h -> "From".equalsIgnoreCase(h.getName())).map(h -> h.getValue()).findFirst().orElse("");

            // try to get body (may be multipart)
            String body = "";
            if (full.getPayload().getBody() != null && full.getPayload().getBody().getData() != null) {
                body = new String(Base64.getUrlDecoder().decode(full.getPayload().getBody().getData()));
            } else if (full.getPayload().getParts() != null) {
                var part = full.getPayload().getParts().stream().filter(p -> p.getMimeType().contains("text/plain") || p.getMimeType().contains("text/html")).findFirst();
                if (part.isPresent() && part.get().getBody() != null && part.get().getBody().getData() != null) {
                    body = new String(Base64.getUrlDecoder().decode(part.get().getBody().getData()));
                }
            }

            result.add(Map.of(
                    "id", full.getId(),
                    "threadId", full.getThreadId(),
                    "from", from,
                    "subject", subject,
                    "snippet", snippet,
                    "body", body
            ));
        }

        return result;
    }
}

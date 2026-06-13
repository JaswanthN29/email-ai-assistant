package com.example.emailassistant.rag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;

@Service
public class DocumentationIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentationIngestionService.class);

    private final VectorStore vectorStore;
    private final Map<String, Map<String, String>> prefetchedDocs = new HashMap<>();

    public DocumentationIngestionService(java.util.Optional<VectorStore> vectorStore) {
        this.vectorStore = vectorStore.orElse(null);
    }

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource resource = new ClassPathResource("prefetched-docs.json");
            Map<String, Map<String, String>> data = mapper.readValue(
                resource.getInputStream(),
                new TypeReference<Map<String, Map<String, String>>>() {}
            );
            prefetchedDocs.putAll(data);
            logger.info("Loaded {} pre-fetched Pega documentation definitions from JSON resource", prefetchedDocs.size());
        } catch (Exception e) {
            logger.error("Failed to load pre-fetched Pega documentation JSON", e);
        }
    }

    /**
     * Scrapes a documentation URL, chunks the text, and stores it in Pinecone.
     *
     * @param url The URL of the documentation to scrape.
     * @throws IOException If scraping fails.
     */
    public void ingestDocumentationUrl(String url) throws IOException {
        if (vectorStore == null) {
            throw new IllegalStateException("Vector Store is not configured or initialized in this environment.");
        }
        String title;
        String rawText;

        if (prefetchedDocs.containsKey(url)) {
            logger.info("Using pre-fetched rich content for Pega documentation URL: {}", url);
            Map<String, String> data = prefetchedDocs.get(url);
            title = data.get("title");
            rawText = data.get("content");
        } else {
            logger.info("Starting web scraping for URL: {}", url);
            // Fetch text using Jsoup
            org.jsoup.nodes.Document htmlDoc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            title = htmlDoc.title();
            rawText = htmlDoc.body() != null ? htmlDoc.body().text() : "";
            
            if (rawText.trim().isEmpty()) {
                logger.warn("No content found on the page body for URL: {}", url);
                rawText = title;
            }
        }

        logger.info("Title: '{}'. Content length: {} characters.", title, rawText.length());

        // 2. Split into chunks (1000 characters with 200 overlap)
        List<String> textChunks = splitTextByCharacters(rawText, 1000, 200);
        logger.info("Split content into {} chunks", textChunks.size());

        // 3. Create Document objects with metadata
        List<Document> springDocs = new ArrayList<>();
        for (int i = 0; i < textChunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source_url", url);
            metadata.put("title", title);
            metadata.put("chunk_index", i);
            metadata.put("topic", "Actions and Treatments");
            metadata.put("version", "Customer Decision Hub");

            Document doc = new Document(textChunks.get(i), metadata);
            springDocs.add(doc);
        }

        // 4. Save to Vector Store
        logger.info("Storing {} chunks into Pinecone Vector Database...", springDocs.size());
        vectorStore.accept(springDocs);
        logger.info("Successfully ingested and stored embeddings in Pinecone for: {}", url);
    }

    /**
     * Helper method to split text into chunks of specified size and overlap.
     */
    private List<String> splitTextByCharacters(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int textLength = text.length();
        int start = 0;

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            chunks.add(text.substring(start, end));
            
            // Advance start position by (chunkSize - overlap)
            start += (chunkSize - overlap);
            
            // Safety break to prevent infinite loop
            if (chunkSize <= overlap) {
                break;
            }
        }
        return chunks;
    }
}

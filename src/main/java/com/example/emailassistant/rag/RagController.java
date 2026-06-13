package com.example.emailassistant.rag;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/docs")
public class RagController {

    private static final Logger logger = LoggerFactory.getLogger(RagController.class);

    private final PegaDocService docService;
    private final DocumentationIngestionService webIngestionService;
    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    public RagController(PegaDocService docService, DocumentationIngestionService webIngestionService, Optional<VectorStore> vectorStore, Optional<ChatModel> chatModel) {
        this.docService = docService;
        this.webIngestionService = webIngestionService;
        this.vectorStore = vectorStore.orElse(null);
        this.chatModel = chatModel.orElse(null);
    }

    @PostMapping("/ingest-web")
    public ResponseEntity<Map<String, String>> ingestWebDocument(@RequestParam("url") String url) {
        if (url == null || url.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL parameter 'url' must not be empty"));
        }

        try {
            logger.info("Triggered web ingestion for URL: {}", url);
            webIngestionService.ingestDocumentationUrl(url);
            return ResponseEntity.ok(Map.of("message", "Successfully ingested documentation from " + url + " into Pinecone Vector DB"));
        } catch (Exception e) {
            logger.error("Failed to ingest web document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to ingest web document: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty"));
        }
        
        if (!file.getContentType().equalsIgnoreCase("application/pdf")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only PDF files are supported currently"));
        }

        try {
            logger.info("Starting ingestion for file: {}", file.getOriginalFilename());
            docService.ingestPdf(file);
            return ResponseEntity.ok(Map.of("message", "Successfully ingested " + file.getOriginalFilename() + " and updated the vector store"));
        } catch (IOException e) {
            logger.error("Failed to ingest document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to ingest document: " + e.getMessage()));
        }
    }

    @GetMapping("/query")
    public ResponseEntity<Map<String, Object>> queryDocumentation(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Query parameter 'q' must not be empty"));
        }

        if (vectorStore == null) {
            return ResponseEntity.ok(Map.of(
                "query", query,
                "answer", "Vector Store (Pinecone) is not configured or failed to initialize in this environment.",
                "sources", List.of()
            ));
        }

        try {
            logger.info("Executing semantic search for query: {}", query);
            
            // 1. Retrieve similar documents (context)
            List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(4).build()
            );

            if (similarDocuments.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "query", query,
                    "answer", "No relevant Pega documentation could be found. Please upload documentation first.",
                    "sources", List.of()
                ));
            }

            String context = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

            // 2. Format the prompt and context
            String systemPrompt = """
                You are an expert AI assistant specializing in Pega documentation.
                Use the following retrieved context to answer the user's question.
                Provide clear, concise, and structured steps where applicable.
                If you don't know the answer or if the context doesn't provide enough information to answer, state that you don't know or ask for more specific context. Do not make up information.
                
                CONTEXT:
                {context}
                """;

            PromptTemplate template = new PromptTemplate(systemPrompt);
            Message systemMessage = template.createMessage(Map.of("context", context));
            UserMessage userMessage = new UserMessage(query);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

            // 3. Request completion from LLM (if available)
            String answer;
            if (chatModel != null) {
                logger.info("Calling chat model with prompt...");
                ChatResponse response = chatModel.call(prompt);
                answer = response.getResult().getOutput().getText();
            } else {
                logger.info("Chat model is not configured. Returning semantic search context directly.");
                answer = context;
            }

            // Extract source file names and page numbers if available in metadata
            List<Map<String, Object>> sources = similarDocuments.stream()
                .map(doc -> {
                    Map<String, Object> sourceMap = new java.util.HashMap<>();
                    sourceMap.put("id", doc.getId());
                    String text = doc.getText();
                    sourceMap.put("snippet", text.substring(0, Math.min(text.length(), 150)) + "...");
                    sourceMap.put("metadata", doc.getMetadata());
                    return sourceMap;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "query", query,
                "answer", answer,
                "sources", sources
            ));

        } catch (Exception e) {
            logger.error("Error during query processing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error processing request: " + e.getMessage()));
        }
    }
}

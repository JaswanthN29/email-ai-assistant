package com.example.emailassistant.rag;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PegaDocService {

    private static final Logger logger = LoggerFactory.getLogger(PegaDocService.class);

    private final VectorStore vectorStore;

    public PegaDocService(java.util.Optional<VectorStore> vectorStore) {
        this.vectorStore = vectorStore.orElse(null);
    }

    public void ingestPdf(MultipartFile multipartFile) throws IOException {
        if (vectorStore == null) {
            throw new IllegalStateException("Vector Store is not configured or initialized in this environment.");
        }
        // Create temporary file to load into Resource
        File tempFile = File.createTempFile("pega-doc-", ".pdf");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
        }

        try {
            Resource pdfResource = new FileSystemResource(tempFile);
            
            // 1. Read PDF
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource);
            List<Document> documents = pdfReader.get();
            logger.info("Loaded {} pages/documents from PDF: {}", documents.size(), multipartFile.getOriginalFilename());

            // 2. Split into chunks
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocuments = splitter.apply(documents);
            logger.info("Split into {} chunks", splitDocuments.size());

            // 3. Store embeddings
            vectorStore.accept(splitDocuments);
            logger.info("Added chunks to vector store");

        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}

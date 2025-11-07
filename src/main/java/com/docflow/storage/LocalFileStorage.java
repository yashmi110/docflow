package com.docflow.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local filesystem storage implementation.
 * Stores files under ./storage/{docType}/{docId}/
 */
@Component
@Slf4j
public class LocalFileStorage implements FileStorage {

    private final Path rootLocation;

    public LocalFileStorage(@Value("${file.storage.location:./storage}") String storageLocation) {
        this.rootLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
            log.info("File storage initialized at: {}", this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    @Override
    public String store(InputStream inputStream, String filename, String contentType,
                        String docType, Long docId) throws IOException {
        // Sanitize filename
        String sanitizedFilename = sanitizeFilename(filename);
        
        // Generate unique filename to avoid collisions
        String uniqueFilename = UUID.randomUUID().toString() + "_" + sanitizedFilename;
        
        // Create directory structure: storage/{docType}/{docId}/
        Path docDirectory = this.rootLocation.resolve(docType).resolve(docId.toString());
        Files.createDirectories(docDirectory);
        
        // Store file
        Path targetLocation = docDirectory.resolve(uniqueFilename);
        Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        // Return relative path from storage root
        String relativePath = this.rootLocation.relativize(targetLocation).toString();
        
        log.info("Stored file: {} (size: {} bytes)", relativePath, Files.size(targetLocation));
        
        return relativePath;
    }

    @Override
    public InputStream load(String path) throws IOException {
        Path filePath = this.rootLocation.resolve(path).normalize();
        
        if (!filePath.startsWith(this.rootLocation)) {
            throw new IOException("Cannot read file outside storage directory");
        }
        
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + path);
        }
        
        return Files.newInputStream(filePath);
    }

    @Override
    public void delete(String path) throws IOException {
        Path filePath = this.rootLocation.resolve(path).normalize();
        
        if (!filePath.startsWith(this.rootLocation)) {
            throw new IOException("Cannot delete file outside storage directory");
        }
        
        Files.deleteIfExists(filePath);
        log.info("Deleted file: {}", path);
    }

    @Override
    public boolean exists(String path) {
        Path filePath = this.rootLocation.resolve(path).normalize();
        return Files.exists(filePath) && filePath.startsWith(this.rootLocation);
    }

    @Override
    public long getFileSize(String path) throws IOException {
        Path filePath = this.rootLocation.resolve(path).normalize();
        
        if (!filePath.startsWith(this.rootLocation)) {
            throw new IOException("Cannot access file outside storage directory");
        }
        
        return Files.size(filePath);
    }

    /**
     * Sanitize filename to prevent directory traversal attacks.
     */
    private String sanitizeFilename(String filename) {
        // Remove path separators and null bytes
        return filename.replaceAll("[/\\\\\\x00]", "_");
    }
}

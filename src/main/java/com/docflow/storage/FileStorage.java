package com.docflow.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * File storage abstraction.
 * Allows swapping between local storage and cloud storage (S3, Azure Blob, etc.)
 */
public interface FileStorage {

    /**
     * Store a file.
     *
     * @param inputStream File content
     * @param filename    Original filename
     * @param contentType MIME type
     * @param docType     Document type (for path organization)
     * @param docId       Document ID (for path organization)
     * @return Stored file path
     * @throws IOException If storage fails
     */
    String store(InputStream inputStream, String filename, String contentType, 
                 String docType, Long docId) throws IOException;

    /**
     * Load a file as InputStream.
     *
     * @param path File path
     * @return File content as InputStream
     * @throws IOException If file not found or read fails
     */
    InputStream load(String path) throws IOException;

    /**
     * Delete a file.
     *
     * @param path File path
     * @throws IOException If deletion fails
     */
    void delete(String path) throws IOException;

    /**
     * Check if file exists.
     *
     * @param path File path
     * @return true if file exists
     */
    boolean exists(String path);

    /**
     * Get file size in bytes.
     *
     * @param path File path
     * @return File size in bytes
     * @throws IOException If file not found
     */
    long getFileSize(String path) throws IOException;
}

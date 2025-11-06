package com.docflow.service;

import com.docflow.domain.entity.Document;
import com.docflow.domain.entity.DocumentFile;
import com.docflow.domain.entity.User;
import com.docflow.dto.file.DocumentFileResponse;
import com.docflow.dto.file.FileUploadResponse;
import com.docflow.exception.FileStorageException;
import com.docflow.exception.ResourceNotFoundException;
import com.docflow.exception.UnauthorizedActionException;
import com.docflow.repository.DocumentFileRepository;
import com.docflow.repository.DocumentRepository;
import com.docflow.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileStorage fileStorage;
    private final DocumentFileRepository documentFileRepository;
    private final DocumentRepository documentRepository;

    @Value("${file.upload.max-size:10485760}") // 10MB default
    private long maxFileSize;

    @Value("${file.upload.allowed-types:application/pdf,image/jpeg,image/png,image/gif,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet}")
    private String allowedContentTypes;

    @Transactional
    public FileUploadResponse uploadFile(Long docId, MultipartFile file, User currentUser) {
        // Validate document exists
        Document document = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", docId));

        // Validate file
        validateFile(file);

        try {
            // Store file
            String storagePath = fileStorage.store(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    document.getDocType().name(),
                    docId
            );

            // Save metadata
            DocumentFile documentFile = DocumentFile.builder()
                    .document(document)
                    .filename(extractFilename(storagePath))
                    .originalFilename(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .storagePath(storagePath)
                    .uploadedBy(currentUser)
                    .build();

            documentFile = documentFileRepository.save(documentFile);

            log.info("File uploaded: {} for document {} by user {}",
                    file.getOriginalFilename(), docId, currentUser.getEmail());

            return FileUploadResponse.builder()
                    .fileId(documentFile.getId())
                    .filename(documentFile.getFilename())
                    .originalFilename(documentFile.getOriginalFilename())
                    .contentType(documentFile.getContentType())
                    .size(documentFile.getSize())
                    .path(storagePath)
                    .build();

        } catch (IOException e) {
            throw new FileStorageException("Failed to store file", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentFileResponse> getDocumentFiles(Long docId) {
        // Validate document exists
        if (!documentRepository.existsById(docId)) {
            throw new ResourceNotFoundException("Document", docId);
        }

        List<DocumentFile> files = documentFileRepository.findByDocumentId(docId);
        return files.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Resource downloadFile(Long docId, Long fileId) {
        DocumentFile file = documentFileRepository.findByIdAndDocumentId(fileId, docId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        try {
            InputStream inputStream = fileStorage.load(file.getStoragePath());
            return new InputStreamResource(inputStream);
        } catch (IOException e) {
            throw new FileStorageException("Failed to load file", e);
        }
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE') or #currentUser.id == #ownerId")
    public void deleteFile(Long docId, Long fileId, User currentUser, Long ownerId) {
        DocumentFile file = documentFileRepository.findByIdAndDocumentId(fileId, docId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        try {
            // Delete from storage
            fileStorage.delete(file.getStoragePath());

            // Delete metadata
            documentFileRepository.delete(file);

            log.info("File deleted: {} from document {} by user {}",
                    file.getOriginalFilename(), docId, currentUser.getEmail());

        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Validate size
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds maximum allowed size of %d bytes", maxFileSize)
            );
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new IllegalArgumentException(
                    String.format("File type %s is not allowed. Allowed types: %s",
                            contentType, allowedContentTypes)
            );
        }

        // Validate filename
        String filename = file.getOriginalFilename();
        if (filename == null || filename.contains("..")) {
            throw new IllegalArgumentException("Invalid filename");
        }
    }

    private boolean isAllowedContentType(String contentType) {
        Set<String> allowed = Arrays.stream(allowedContentTypes.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        return allowed.contains(contentType);
    }

    private String extractFilename(String path) {
        int lastSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSeparator >= 0 ? path.substring(lastSeparator + 1) : path;
    }

    private DocumentFileResponse toResponse(DocumentFile file) {
        return DocumentFileResponse.builder()
                .id(file.getId())
                .documentId(file.getDocument().getId())
                .filename(file.getFilename())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .uploadedBy(file.getUploadedBy().getId())
                .uploadedByName(file.getUploadedBy().getName())
                .uploadedByEmail(file.getUploadedBy().getEmail())
                .createdAt(file.getCreatedAt())
                .build();
    }
}

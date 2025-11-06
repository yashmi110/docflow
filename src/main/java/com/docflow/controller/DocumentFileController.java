package com.docflow.controller;

import com.docflow.domain.entity.Document;
import com.docflow.domain.entity.User;
import com.docflow.dto.file.DocumentFileResponse;
import com.docflow.dto.file.FileUploadResponse;
import com.docflow.exception.ResourceNotFoundException;
import com.docflow.repository.DocumentRepository;
import com.docflow.security.SecurityUtils;
import com.docflow.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/docs")
@RequiredArgsConstructor
public class DocumentFileController {

    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;
    private final SecurityUtils securityUtils;

    @PostMapping("/{docId}/files")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @PathVariable Long docId,
            @RequestParam("file") MultipartFile file
    ) {
        User currentUser = securityUtils.getCurrentUser();
        FileUploadResponse response = fileStorageService.uploadFile(docId, file, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{docId}/files")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentFileResponse>> getDocumentFiles(
            @PathVariable Long docId
    ) {
        List<DocumentFileResponse> files = fileStorageService.getDocumentFiles(docId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{docId}/files/{fileId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long docId,
            @PathVariable Long fileId
    ) {
        Resource resource = fileStorageService.downloadFile(docId, fileId);
        
        // Get file metadata for content type
        List<DocumentFileResponse> files = fileStorageService.getDocumentFiles(docId);
        DocumentFileResponse fileMetadata = files.stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileMetadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + fileMetadata.getOriginalFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{docId}/files/{fileId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long docId,
            @PathVariable Long fileId
    ) {
        User currentUser = securityUtils.getCurrentUser();
        
        // Get document to check ownership
        Document document = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", docId));
        
        Long ownerId = document.getOwnerUser().getId();
        
        // Delete file (RBAC enforced in service: owner, admin, or finance)
        fileStorageService.deleteFile(docId, fileId, currentUser, ownerId);
        
        return ResponseEntity.noContent().build();
    }
}

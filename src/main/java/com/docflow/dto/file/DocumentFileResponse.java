package com.docflow.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFileResponse {

    private Long id;
    private Long documentId;
    private String filename;
    private String originalFilename;
    private String contentType;
    private Long size;
    private Long uploadedBy;
    private String uploadedByName;
    private String uploadedByEmail;
    private LocalDateTime createdAt;
}

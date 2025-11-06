package com.docflow.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private Long documentId;
    private Long userId;
    private String userEmail;
    private String userName;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String note;
    private LocalDateTime createdAt;
}

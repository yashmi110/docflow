package com.docflow.controller;

import com.docflow.dto.audit.AuditLogResponse;
import com.docflow.mapper.AuditLogMapper;
import com.docflow.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final AuditLogMapper auditLogMapper;

    /**
     * Get all audit logs for a specific document.
     * Accessible by authenticated users who can view the document.
     */
    @GetMapping("/document/{documentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AuditLogResponse>> getDocumentAuditLogs(
            @PathVariable Long documentId
    ) {
        List<AuditLogResponse> auditLogs = auditLogMapper.toResponseList(
                auditLogService.getDocumentAuditLogs(documentId)
        );
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs for a document with pagination.
     */
    @GetMapping("/document/{documentId}/paginated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<AuditLogResponse>> getDocumentAuditLogsPaginated(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLogResponse> auditLogs = auditLogService.getDocumentAuditLogs(documentId, pageable)
                .map(auditLogMapper::toResponse);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs for a specific user.
     * Only accessible by ADMIN or the user themselves.
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<Page<AuditLogResponse>> getUserAuditLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLogResponse> auditLogs = auditLogService.getUserAuditLogs(userId, pageable)
                .map(auditLogMapper::toResponse);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs by action type.
     * Only accessible by ADMIN.
     */
    @GetMapping("/action/{action}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogsByAction(
            @PathVariable String action
    ) {
        List<AuditLogResponse> auditLogs = auditLogMapper.toResponseList(
                auditLogService.getAuditLogsByAction(action)
        );
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get count of audit logs for a document.
     */
    @GetMapping("/document/{documentId}/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getDocumentAuditLogCount(
            @PathVariable Long documentId
    ) {
        long count = auditLogService.countDocumentAuditLogs(documentId);
        return ResponseEntity.ok(count);
    }
}

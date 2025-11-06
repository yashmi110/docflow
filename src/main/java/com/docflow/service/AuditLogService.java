package com.docflow.service;

import com.docflow.domain.entity.AuditLog;
import com.docflow.domain.entity.Document;
import com.docflow.domain.entity.User;
import com.docflow.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing audit logs.
 * Records all document state transitions and actions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Logs a document transition.
     * Uses REQUIRES_NEW to ensure audit log is saved even if parent transaction rolls back.
     *
     * @param document   The document being transitioned
     * @param user       The user performing the action
     * @param action     The action being performed (e.g., "SUBMITTED", "APPROVED")
     * @param fromStatus The previous status
     * @param toStatus   The new status
     * @param note       Optional note about the transition
     * @return The created audit log
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logTransition(Document document, User user, String action, 
                                   String fromStatus, String toStatus, String note) {
        AuditLog auditLog = AuditLog.builder()
                .document(document)
                .user(user)
                .action(action)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .note(note)
                .build();

        auditLog = auditLogRepository.save(auditLog);

        log.info("Audit log created: Document {} - {} by {} ({} -> {})", 
                document.getId(), action, user.getEmail(), fromStatus, toStatus);

        return auditLog;
    }

    /**
     * Logs a generic document action (not a status transition).
     *
     * @param document The document
     * @param user     The user performing the action
     * @param action   The action description
     * @param note     Optional note
     * @return The created audit log
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logAction(Document document, User user, String action, String note) {
        AuditLog auditLog = AuditLog.builder()
                .document(document)
                .user(user)
                .action(action)
                .note(note)
                .build();

        auditLog = auditLogRepository.save(auditLog);

        log.info("Audit log created: Document {} - {} by {}", 
                document.getId(), action, user.getEmail());

        return auditLog;
    }

    /**
     * Gets all audit logs for a document.
     *
     * @param documentId The document ID
     * @return List of audit logs ordered by creation time (newest first)
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getDocumentAuditLogs(Long documentId) {
        return auditLogRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
    }

    /**
     * Gets audit logs for a document with pagination.
     *
     * @param documentId The document ID
     * @param pageable   Pagination parameters
     * @return Page of audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getDocumentAuditLogs(Long documentId, Pageable pageable) {
        return auditLogRepository.findByDocumentId(documentId, pageable);
    }

    /**
     * Gets audit logs for a user.
     *
     * @param userId   The user ID
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getUserAuditLogs(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Gets audit logs by action type.
     *
     * @param action The action type
     * @return List of audit logs
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByAction(String action) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action);
    }

    /**
     * Gets audit logs within a date range.
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return List of audit logs
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Gets audit logs for a specific document action.
     *
     * @param documentId The document ID
     * @param action     The action type
     * @return List of audit logs
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getDocumentAuditLogsByAction(Long documentId, String action) {
        return auditLogRepository.findByDocumentIdAndAction(documentId, action);
    }

    /**
     * Counts audit logs for a document.
     *
     * @param documentId The document ID
     * @return Count of audit logs
     */
    @Transactional(readOnly = true)
    public long countDocumentAuditLogs(Long documentId) {
        return auditLogRepository.countByDocumentId(documentId);
    }
}

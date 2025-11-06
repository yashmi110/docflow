package com.docflow.service;

import com.docflow.domain.entity.Document;
import com.docflow.domain.entity.User;
import com.docflow.domain.enums.DocumentStatus;
import com.docflow.exception.InvalidStatusTransitionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * State machine service that enforces legal document status transitions.
 * Ensures idempotency and optimistic locking via @Version.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentStatusMachine {

    private final AuditLogService auditLogService;

    // Define legal state transitions
    private static final Map<DocumentStatus, Set<DocumentStatus>> LEGAL_TRANSITIONS = new EnumMap<>(DocumentStatus.class);

    static {
        // DRAFT can go to PENDING or CANCELLED
        LEGAL_TRANSITIONS.put(DocumentStatus.DRAFT, EnumSet.of(DocumentStatus.PENDING, DocumentStatus.CANCELLED));

        // PENDING can go to APPROVED, REJECTED, or CANCELLED
        LEGAL_TRANSITIONS.put(DocumentStatus.PENDING, EnumSet.of(DocumentStatus.APPROVED, DocumentStatus.REJECTED, DocumentStatus.CANCELLED));

        // APPROVED can go to PAID (for invoices & reimbursements)
        LEGAL_TRANSITIONS.put(DocumentStatus.APPROVED, EnumSet.of(DocumentStatus.PAID));

        // REJECTED is terminal (no transitions)
        LEGAL_TRANSITIONS.put(DocumentStatus.REJECTED, EnumSet.noneOf(DocumentStatus.class));

        // PAID is terminal (no transitions)
        LEGAL_TRANSITIONS.put(DocumentStatus.PAID, EnumSet.noneOf(DocumentStatus.class));

        // CANCELLED is terminal (no transitions)
        LEGAL_TRANSITIONS.put(DocumentStatus.CANCELLED, EnumSet.noneOf(DocumentStatus.class));
    }

    /**
     * Validates if a status transition is legal.
     *
     * @param from Current status
     * @param to   Target status
     * @return true if transition is legal
     */
    public boolean isTransitionAllowed(DocumentStatus from, DocumentStatus to) {
        if (from == to) {
            // Idempotent - same status is allowed
            return true;
        }

        Set<DocumentStatus> allowedTransitions = LEGAL_TRANSITIONS.get(from);
        return allowedTransitions != null && allowedTransitions.contains(to);
    }

    /**
     * Validates and throws exception if transition is not allowed.
     *
     * @param from Current status
     * @param to   Target status
     * @throws InvalidStatusTransitionException if transition is not allowed
     */
    public void validateTransition(DocumentStatus from, DocumentStatus to) {
        if (!isTransitionAllowed(from, to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }

    /**
     * Transitions a document to a new status with audit logging.
     * Handles idempotency - if already in target status, does nothing.
     * Uses optimistic locking to prevent concurrent modifications.
     *
     * @param document   The document to transition
     * @param newStatus  The target status
     * @param user       The user performing the transition
     * @param action     The action description (e.g., "SUBMITTED", "APPROVED")
     * @param note       Optional note about the transition
     * @return The updated document
     * @throws InvalidStatusTransitionException        if transition is not allowed
     * @throws ObjectOptimisticLockingFailureException if concurrent modification detected
     */
    @Transactional
    public Document transitionStatus(Document document, DocumentStatus newStatus, User user, String action, String note) {
        DocumentStatus currentStatus = document.getStatus();

        // Idempotency check - if already in target status, log and return
        if (currentStatus == newStatus) {
            log.info("Document {} already in status {}. Idempotent operation.", document.getId(), newStatus);
            // Still log the attempt for audit purposes
            auditLogService.logTransition(document, user, action, currentStatus.name(), newStatus.name(), 
                    "Idempotent transition attempt - already in target status");
            return document;
        }

        // Validate transition
        validateTransition(currentStatus, newStatus);

        // Update status (optimistic locking via @Version will prevent concurrent modifications)
        document.setStatus(newStatus);

        // Log the transition
        auditLogService.logTransition(document, user, action, currentStatus.name(), newStatus.name(), note);

        log.info("Document {} transitioned from {} to {} by user {}", 
                document.getId(), currentStatus, newStatus, user.getEmail());

        return document;
    }

    /**
     * Submits a document (DRAFT -> PENDING).
     *
     * @param document The document to submit
     * @param user     The user submitting
     * @param note     Optional note
     * @return The updated document
     */
    @Transactional
    public Document submit(Document document, User user, String note) {
        return transitionStatus(document, DocumentStatus.PENDING, user, "SUBMITTED", note);
    }

    /**
     * Approves a document (PENDING -> APPROVED).
     *
     * @param document The document to approve
     * @param user     The user approving
     * @param note     Optional note
     * @return The updated document
     */
    @Transactional
    public Document approve(Document document, User user, String note) {
        return transitionStatus(document, DocumentStatus.APPROVED, user, "APPROVED", note);
    }

    /**
     * Rejects a document (PENDING -> REJECTED).
     *
     * @param document The document to reject
     * @param user     The user rejecting
     * @param note     Reason for rejection
     * @return The updated document
     */
    @Transactional
    public Document reject(Document document, User user, String note) {
        return transitionStatus(document, DocumentStatus.REJECTED, user, "REJECTED", note);
    }

    /**
     * Marks a document as paid (APPROVED -> PAID).
     *
     * @param document The document to mark as paid
     * @param user     The user marking as paid
     * @param note     Optional note
     * @return The updated document
     */
    @Transactional
    public Document markAsPaid(Document document, User user, String note) {
        return transitionStatus(document, DocumentStatus.PAID, user, "PAID", note);
    }

    /**
     * Cancels a document. Can be done from DRAFT or PENDING status.
     *
     * @param document The document to cancel
     * @param user     The user cancelling
     * @param note     Reason for cancellation
     * @return The updated document
     */
    @Transactional
    public Document cancel(Document document, User user, String note) {
        DocumentStatus currentStatus = document.getStatus();
        
        // Only allow cancellation from DRAFT or PENDING
        if (currentStatus != DocumentStatus.DRAFT && currentStatus != DocumentStatus.PENDING) {
            throw new InvalidStatusTransitionException(
                    String.format("Cannot cancel document in status %s. Only DRAFT or PENDING documents can be cancelled.", currentStatus)
            );
        }

        return transitionStatus(document, DocumentStatus.CANCELLED, user, "CANCELLED", note);
    }

    /**
     * Gets all legal transitions from a given status.
     *
     * @param status Current status
     * @return Set of allowed target statuses
     */
    public Set<DocumentStatus> getAllowedTransitions(DocumentStatus status) {
        return LEGAL_TRANSITIONS.getOrDefault(status, EnumSet.noneOf(DocumentStatus.class));
    }

    /**
     * Checks if a status is terminal (no further transitions allowed).
     *
     * @param status Status to check
     * @return true if terminal
     */
    public boolean isTerminalStatus(DocumentStatus status) {
        Set<DocumentStatus> transitions = LEGAL_TRANSITIONS.get(status);
        return transitions == null || transitions.isEmpty();
    }
}

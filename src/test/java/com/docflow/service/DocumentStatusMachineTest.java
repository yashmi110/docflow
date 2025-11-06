package com.docflow.service;

import com.docflow.domain.entity.Document;
import com.docflow.domain.entity.User;
import com.docflow.domain.enums.DocumentStatus;
import com.docflow.domain.enums.DocumentType;
import com.docflow.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentStatusMachineTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private DocumentStatusMachine statusMachine;

    private User testUser;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        testDocument = Document.builder()
                .id(1L)
                .docType(DocumentType.INVOICE_IN)
                .status(DocumentStatus.DRAFT)
                .ownerUser(testUser)
                .version(0)
                .build();
    }

    @Test
    void testLegalTransition_DraftToPending() {
        // Act
        Document result = statusMachine.submit(testDocument, testUser, "Test submission");

        // Assert
        assertEquals(DocumentStatus.PENDING, result.getStatus());
        verify(auditLogService).logTransition(
                eq(testDocument),
                eq(testUser),
                eq("SUBMITTED"),
                eq("DRAFT"),
                eq("PENDING"),
                eq("Test submission")
        );
    }

    @Test
    void testLegalTransition_PendingToApproved() {
        // Arrange
        testDocument.setStatus(DocumentStatus.PENDING);

        // Act
        Document result = statusMachine.approve(testDocument, testUser, "Approved");

        // Assert
        assertEquals(DocumentStatus.APPROVED, result.getStatus());
        verify(auditLogService).logTransition(
                eq(testDocument),
                eq(testUser),
                eq("APPROVED"),
                eq("PENDING"),
                eq("APPROVED"),
                eq("Approved")
        );
    }

    @Test
    void testLegalTransition_PendingToRejected() {
        // Arrange
        testDocument.setStatus(DocumentStatus.PENDING);

        // Act
        Document result = statusMachine.reject(testDocument, testUser, "Rejected due to errors");

        // Assert
        assertEquals(DocumentStatus.REJECTED, result.getStatus());
        verify(auditLogService).logTransition(
                eq(testDocument),
                eq(testUser),
                eq("REJECTED"),
                eq("PENDING"),
                eq("REJECTED"),
                eq("Rejected due to errors")
        );
    }

    @Test
    void testLegalTransition_ApprovedToPaid() {
        // Arrange
        testDocument.setStatus(DocumentStatus.APPROVED);

        // Act
        Document result = statusMachine.markAsPaid(testDocument, testUser, "Payment completed");

        // Assert
        assertEquals(DocumentStatus.PAID, result.getStatus());
        verify(auditLogService).logTransition(
                eq(testDocument),
                eq(testUser),
                eq("PAID"),
                eq("APPROVED"),
                eq("PAID"),
                eq("Payment completed")
        );
    }

    @Test
    void testIllegalTransition_DraftToApproved() {
        // Act & Assert
        assertThrows(InvalidStatusTransitionException.class,
                () -> statusMachine.approve(testDocument, testUser, "Invalid"));
        verify(auditLogService, never()).logTransition(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testIllegalTransition_PaidToPending() {
        // Arrange
        testDocument.setStatus(DocumentStatus.PAID);

        // Act & Assert
        assertThrows(InvalidStatusTransitionException.class,
                () -> statusMachine.submit(testDocument, testUser, "Invalid"));
        verify(auditLogService, never()).logTransition(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testIdempotency_SameStatusTransition() {
        // Arrange
        testDocument.setStatus(DocumentStatus.PENDING);

        // Act
        Document result = statusMachine.transitionStatus(
                testDocument,
                DocumentStatus.PENDING,
                testUser,
                "DUPLICATE",
                "Idempotent test"
        );

        // Assert
        assertEquals(DocumentStatus.PENDING, result.getStatus());
        verify(auditLogService).logTransition(
                eq(testDocument),
                eq(testUser),
                eq("DUPLICATE"),
                eq("PENDING"),
                eq("PENDING"),
                contains("Idempotent")
        );
    }

    @Test
    void testCancellation_FromDraft() {
        // Act
        Document result = statusMachine.cancel(testDocument, testUser, "Cancelled by user");

        // Assert
        assertEquals(DocumentStatus.CANCELLED, result.getStatus());
        verify(auditLogService).logTransition(
                eq(testDocument),
                eq(testUser),
                eq("CANCELLED"),
                eq("DRAFT"),
                eq("CANCELLED"),
                eq("Cancelled by user")
        );
    }

    @Test
    void testCancellation_FromPending() {
        // Arrange
        testDocument.setStatus(DocumentStatus.PENDING);

        // Act
        Document result = statusMachine.cancel(testDocument, testUser, "Cancelled");

        // Assert
        assertEquals(DocumentStatus.CANCELLED, result.getStatus());
    }

    @Test
    void testCancellation_FromApproved_ShouldFail() {
        // Arrange
        testDocument.setStatus(DocumentStatus.APPROVED);

        // Act & Assert
        assertThrows(InvalidStatusTransitionException.class,
                () -> statusMachine.cancel(testDocument, testUser, "Cannot cancel"));
    }

    @Test
    void testIsTransitionAllowed() {
        assertTrue(statusMachine.isTransitionAllowed(DocumentStatus.DRAFT, DocumentStatus.PENDING));
        assertTrue(statusMachine.isTransitionAllowed(DocumentStatus.PENDING, DocumentStatus.APPROVED));
        assertTrue(statusMachine.isTransitionAllowed(DocumentStatus.PENDING, DocumentStatus.REJECTED));
        assertTrue(statusMachine.isTransitionAllowed(DocumentStatus.APPROVED, DocumentStatus.PAID));

        assertFalse(statusMachine.isTransitionAllowed(DocumentStatus.DRAFT, DocumentStatus.APPROVED));
        assertFalse(statusMachine.isTransitionAllowed(DocumentStatus.PAID, DocumentStatus.PENDING));
        assertFalse(statusMachine.isTransitionAllowed(DocumentStatus.REJECTED, DocumentStatus.APPROVED));
    }

    @Test
    void testGetAllowedTransitions() {
        Set<DocumentStatus> draftTransitions = statusMachine.getAllowedTransitions(DocumentStatus.DRAFT);
        assertEquals(2, draftTransitions.size());
        assertTrue(draftTransitions.contains(DocumentStatus.PENDING));
        assertTrue(draftTransitions.contains(DocumentStatus.CANCELLED));

        Set<DocumentStatus> pendingTransitions = statusMachine.getAllowedTransitions(DocumentStatus.PENDING);
        assertEquals(3, pendingTransitions.size());
        assertTrue(pendingTransitions.contains(DocumentStatus.APPROVED));
        assertTrue(pendingTransitions.contains(DocumentStatus.REJECTED));
        assertTrue(pendingTransitions.contains(DocumentStatus.CANCELLED));

        Set<DocumentStatus> paidTransitions = statusMachine.getAllowedTransitions(DocumentStatus.PAID);
        assertTrue(paidTransitions.isEmpty());
    }

    @Test
    void testIsTerminalStatus() {
        assertFalse(statusMachine.isTerminalStatus(DocumentStatus.DRAFT));
        assertFalse(statusMachine.isTerminalStatus(DocumentStatus.PENDING));
        assertFalse(statusMachine.isTerminalStatus(DocumentStatus.APPROVED));

        assertTrue(statusMachine.isTerminalStatus(DocumentStatus.REJECTED));
        assertTrue(statusMachine.isTerminalStatus(DocumentStatus.PAID));
        assertTrue(statusMachine.isTerminalStatus(DocumentStatus.CANCELLED));
    }
}

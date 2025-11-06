package com.docflow.domain.enums;

public enum DocumentStatus {
    DRAFT,      // Initial state
    PENDING,    // Submitted for approval
    APPROVED,   // Approved by authorized user
    REJECTED,   // Rejected by authorized user
    PAID,       // Payment completed
    CANCELLED   // Cancelled/Voided
}

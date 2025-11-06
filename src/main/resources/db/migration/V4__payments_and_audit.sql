-- ============================================
-- DOCFLOW PAYMENTS, CREDIT NOTES, AND AUDIT
-- Payment tracking, credit notes, and audit logs
-- ============================================

-- Payments table
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    direction VARCHAR(20) NOT NULL COMMENT 'OUTBOUND (we pay), INBOUND (we receive)',
    method VARCHAR(20) NOT NULL COMMENT 'BANK, CHEQUE, CARD',
    amount DECIMAL(15, 2) NOT NULL,
    paid_at TIMESTAMP NOT NULL,
    reference VARCHAR(255) NULL COMMENT 'Transaction reference or cheque number',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_document FOREIGN KEY (doc_id) REFERENCES documents(id) ON DELETE CASCADE,
    INDEX idx_payment_doc (doc_id),
    INDEX idx_payment_direction (direction),
    INDEX idx_payment_paid_at (paid_at),
    INDEX idx_payment_method (method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Credit Notes table
CREATE TABLE credit_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    related_doc_id BIGINT NOT NULL,
    direction VARCHAR(20) NOT NULL COMMENT 'VENDOR (credit from vendor), CLIENT (credit to client)',
    amount DECIMAL(15, 2) NOT NULL,
    reason TEXT NULL,
    issued_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_credit_note_document FOREIGN KEY (related_doc_id) REFERENCES documents(id) ON DELETE CASCADE,
    INDEX idx_credit_note_doc (related_doc_id),
    INDEX idx_credit_note_direction (direction),
    INDEX idx_credit_note_issued (issued_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit Logs table (full audit trail for document state changes)
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL COMMENT 'CREATED, SUBMITTED, APPROVED, REJECTED, PAID, CANCELLED, etc.',
    from_status VARCHAR(50) NULL,
    to_status VARCHAR(50) NULL,
    note TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_log_document FOREIGN KEY (doc_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_audit_log_doc (doc_id),
    INDEX idx_audit_log_user (user_id),
    INDEX idx_audit_log_action (action),
    INDEX idx_audit_log_created (created_at),
    INDEX idx_audit_log_doc_created (doc_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

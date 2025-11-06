-- ============================================
-- DOCFLOW DOCUMENTS AND WORKFLOWS
-- Generic document header and specific document types
-- ============================================

-- Generic documents table (audit header for all document types)
CREATE TABLE documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_type VARCHAR(50) NOT NULL COMMENT 'INVOICE_IN, INVOICE_OUT, EXPENSE_CLAIM, REIMBURSEMENT',
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, PENDING, APPROVED, REJECTED, PAID, CANCELLED',
    owner_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    CONSTRAINT fk_documents_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
    INDEX idx_documents_type (doc_type),
    INDEX idx_documents_status (status),
    INDEX idx_documents_owner (owner_user_id),
    INDEX idx_documents_created (created_at),
    INDEX idx_documents_type_status (doc_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Invoice Incoming (Payable) - vendor invoices we must pay
CREATE TABLE invoice_in (
    id BIGINT PRIMARY KEY,
    vendor_id BIGINT NOT NULL,
    invoice_no VARCHAR(100) NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    subtotal DECIMAL(15, 2) NOT NULL,
    tax DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    total DECIMAL(15, 2) NOT NULL,
    CONSTRAINT fk_invoice_in_document FOREIGN KEY (id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_invoice_in_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id),
    INDEX idx_invoice_in_vendor (vendor_id),
    INDEX idx_invoice_in_invoice_no (invoice_no),
    INDEX idx_invoice_in_date (invoice_date),
    INDEX idx_invoice_in_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Invoice Outgoing (Receivable) - client invoices they must pay
CREATE TABLE invoice_out (
    id BIGINT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    invoice_no VARCHAR(100) NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    subtotal DECIMAL(15, 2) NOT NULL,
    tax DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    total DECIMAL(15, 2) NOT NULL,
    CONSTRAINT fk_invoice_out_document FOREIGN KEY (id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_invoice_out_client FOREIGN KEY (client_id) REFERENCES clients(id),
    INDEX idx_invoice_out_client (client_id),
    INDEX idx_invoice_out_invoice_no (invoice_no),
    INDEX idx_invoice_out_date (invoice_date),
    INDEX idx_invoice_out_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Purchase Order Headers
CREATE TABLE po_headers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_no VARCHAR(100) NOT NULL UNIQUE,
    vendor_id BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    total DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_po_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id),
    INDEX idx_po_no (po_no),
    INDEX idx_po_vendor (vendor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Goods Receipt Note Headers
CREATE TABLE grn_headers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    grn_no VARCHAR(100) NOT NULL UNIQUE,
    vendor_id BIGINT NOT NULL,
    received_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_grn_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id),
    INDEX idx_grn_no (grn_no),
    INDEX idx_grn_vendor (vendor_id),
    INDEX idx_grn_received_date (received_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Expense Claims
CREATE TABLE expense_claims (
    id BIGINT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    claim_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    total DECIMAL(15, 2) NOT NULL,
    CONSTRAINT fk_expense_claim_document FOREIGN KEY (id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_claim_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    INDEX idx_expense_claim_employee (employee_id),
    INDEX idx_expense_claim_date (claim_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Expense Items (line items for expense claims)
CREATE TABLE expense_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    claim_id BIGINT NOT NULL,
    category VARCHAR(100) NOT NULL COMMENT 'Travel, Meals, Accommodation, etc.',
    amount DECIMAL(15, 2) NOT NULL,
    note TEXT NULL,
    CONSTRAINT fk_expense_item_claim FOREIGN KEY (claim_id) REFERENCES expense_claims(id) ON DELETE CASCADE,
    INDEX idx_expense_item_claim (claim_id),
    INDEX idx_expense_item_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Reimbursements
CREATE TABLE reimbursements (
    id BIGINT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    requested_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    total DECIMAL(15, 2) NOT NULL,
    CONSTRAINT fk_reimbursement_document FOREIGN KEY (id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_reimbursement_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    INDEX idx_reimbursement_employee (employee_id),
    INDEX idx_reimbursement_date (requested_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

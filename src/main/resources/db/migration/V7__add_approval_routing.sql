-- ============================================
-- DOCFLOW APPROVAL ROUTING
-- Add manager hierarchy and PO approver assignment
-- ============================================

-- Add manager to employees table
ALTER TABLE employees
ADD COLUMN manager_user_id BIGINT NULL,
ADD CONSTRAINT fk_employees_manager FOREIGN KEY (manager_user_id) REFERENCES users(id);

CREATE INDEX idx_employees_manager ON employees(manager_user_id);

-- Add approver to purchase order headers
ALTER TABLE po_headers
ADD COLUMN approver_user_id BIGINT NULL,
ADD CONSTRAINT fk_po_headers_approver FOREIGN KEY (approver_user_id) REFERENCES users(id);

CREATE INDEX idx_po_headers_approver ON po_headers(approver_user_id);

-- Add PO reference to incoming invoices (optional)
ALTER TABLE invoice_in
ADD COLUMN po_header_id BIGINT NULL,
ADD CONSTRAINT fk_invoice_in_po FOREIGN KEY (po_header_id) REFERENCES po_headers(id);

CREATE INDEX idx_invoice_in_po ON invoice_in(po_header_id);

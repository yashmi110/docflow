-- ============================================
-- DOCFLOW DEV SEED DATA
-- Sample users, vendors, clients, and documents for development/testing
-- ============================================

-- Insert Users (password is 'Password@123' for all)
-- BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (name, email, password_hash, enabled, created_at, updated_at) VALUES
('Admin User', 'admin@docflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, NOW(), NOW()),
('Finance Manager', 'finance1@docflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, NOW(), NOW()),
('Department Manager', 'manager1@docflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, NOW(), NOW()),
('John Employee', 'employee1@docflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, NOW(), NOW()),
('Vendor Contact', 'vendor1@acmecorp.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, NOW(), NOW()),
('Client Contact', 'client1@techcorp.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, NOW(), NOW());

-- Assign Roles to Users
INSERT INTO user_roles (user_id, role_id) VALUES
-- Admin User: ADMIN
(1, 1),
-- Finance Manager: FINANCE
(2, 3),
-- Department Manager: MANAGER
(3, 2),
-- John Employee: EMPLOYEE
(4, 4),
-- Vendor Contact: VENDOR
(5, 5),
-- Client Contact: CLIENT
(6, 6);

-- Insert Vendors
INSERT INTO vendors (name, tax_id, contact_email, created_at, updated_at) VALUES
('Acme Corporation', 'TAX-ACME-001', 'vendor1@acmecorp.com', NOW(), NOW()),
('Global Supplies Ltd', 'TAX-GLOBAL-002', 'contact@globalsupplies.com', NOW(), NOW()),
('Tech Solutions Inc', 'TAX-TECH-003', 'sales@techsolutions.com', NOW(), NOW());

-- Insert Clients
INSERT INTO clients (name, tax_id, contact_email, created_at, updated_at) VALUES
('TechCorp Industries', 'TAX-TECH-101', 'client1@techcorp.com', NOW(), NOW()),
('Retail Enterprises', 'TAX-RETAIL-102', 'accounts@retailent.com', NOW(), NOW()),
('Manufacturing Co', 'TAX-MFG-103', 'finance@mfgco.com', NOW(), NOW());

-- Insert Employees (with manager hierarchy)
INSERT INTO employees (user_id, manager_user_id, created_at, updated_at) VALUES
-- Finance Manager (no manager)
(2, NULL, NOW(), NOW()),
-- Department Manager (reports to Finance Manager)
(3, 2, NOW(), NOW()),
-- John Employee (reports to Department Manager)
(4, 3, NOW(), NOW());

-- Insert Purchase Orders
INSERT INTO po_headers (po_no, vendor_id, currency, total, approver_user_id, created_at, updated_at) VALUES
('PO-2024-001', 1, 'USD', 5000.00, 2, NOW(), NOW()),
('PO-2024-002', 2, 'USD', 12000.00, 2, NOW(), NOW());

-- Insert Goods Receipt Notes
INSERT INTO grn_headers (grn_no, vendor_id, received_date, created_at, updated_at) VALUES
('GRN-2024-001', 1, '2024-11-01', NOW(), NOW());

-- Insert Documents (base table)
INSERT INTO documents (doc_type, status, owner_user_id, created_at, updated_at, version) VALUES
-- Invoice IN (DRAFT)
('INVOICE_IN', 'DRAFT', 2, NOW(), NOW(), 0),
-- Invoice OUT (PENDING)
('INVOICE_OUT', 'PENDING', 2, NOW(), NOW(), 0),
-- Expense Claim (PENDING)
('EXPENSE_CLAIM', 'PENDING', 4, NOW(), NOW(), 0),
-- Invoice IN (PENDING) - linked to PO
('INVOICE_IN', 'PENDING', 2, NOW(), NOW(), 0);

-- Insert Invoice IN (DRAFT)
INSERT INTO invoice_in (id, vendor_id, po_header_id, invoice_no, invoice_date, due_date, currency, subtotal, tax, total) VALUES
(1, 1, NULL, 'INV-ACME-2024-001', '2024-11-01', '2024-12-01', 'USD', 1000.00, 100.00, 1100.00);

-- Insert Invoice OUT (PENDING)
INSERT INTO invoice_out (id, client_id, invoice_no, invoice_date, due_date, currency, subtotal, tax, total) VALUES
(2, 1, 'OUT-2024-001', '2024-11-05', '2024-12-05', 'USD', 5000.00, 500.00, 5500.00);

-- Insert Expense Claim (PENDING)
INSERT INTO expense_claims (id, employee_id, claim_date, currency, total) VALUES
(3, 3, '2024-11-06', 'USD', 350.00);

-- Insert Expense Items
INSERT INTO expense_items (claim_id, category, amount, note) VALUES
(3, 'Meals & Entertainment', 150.00, 'Client dinner meeting on 2024-11-05'),
(3, 'Transportation', 45.00, 'Taxi to airport on 2024-11-06'),
(3, 'Lodging', 155.00, 'Hotel accommodation on 2024-11-06');

-- Insert Invoice IN (PENDING) - linked to PO
INSERT INTO invoice_in (id, vendor_id, po_header_id, invoice_no, invoice_date, due_date, currency, subtotal, tax, total) VALUES
(4, 1, 1, 'INV-ACME-2024-002', '2024-11-10', '2024-12-10', 'USD', 4500.00, 500.00, 5000.00);

-- Insert Audit Logs for status transitions
INSERT INTO audit_logs (doc_id, user_id, action, from_status, to_status, note, created_at) VALUES
-- Invoice OUT submitted
(2, 2, 'SUBMITTED', 'DRAFT', 'PENDING', 'Submitted for approval', NOW() - INTERVAL 1 DAY),
-- Expense Claim submitted
(3, 4, 'SUBMITTED', 'DRAFT', 'PENDING', 'Submitted expense claim for manager approval', NOW() - INTERVAL 2 DAY),
-- Invoice IN (with PO) submitted
(4, 2, 'SUBMITTED', 'DRAFT', 'PENDING', 'Invoice matches PO-2024-001', NOW() - INTERVAL 1 HOUR);

-- Add some file attachments metadata (files would be in ./storage/)
INSERT INTO doc_files (doc_id, filename, original_filename, content_type, size, storage_path, uploaded_by, created_at) VALUES
(3, 'uuid_receipt1.pdf', 'dinner_receipt.pdf', 'application/pdf', 45678, 'EXPENSE_CLAIM/3/uuid_receipt1.pdf', 4, NOW() - INTERVAL 2 DAY),
(3, 'uuid_receipt2.jpg', 'taxi_receipt.jpg', 'image/jpeg', 123456, 'EXPENSE_CLAIM/3/uuid_receipt2.jpg', 4, NOW() - INTERVAL 2 DAY);

-- Summary of seed data:
-- Users: 6 (admin, finance1, manager1, employee1, vendor1, client1)
-- Vendors: 3
-- Clients: 3
-- Employees: 3 (with manager hierarchy)
-- Purchase Orders: 2
-- GRNs: 1
-- Documents:
--   - Invoice IN (DRAFT): 1
--   - Invoice IN (PENDING with PO): 1
--   - Invoice OUT (PENDING): 1
--   - Expense Claim (PENDING): 1
-- Expense Items: 3
-- Audit Logs: 3
-- File Attachments: 2

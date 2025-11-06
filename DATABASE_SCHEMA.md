# Docflow Database Schema

## Overview

Complete database schema for the Docflow document management system with full audit trail, optimistic locking, and proper relationships.

## Flyway Migrations

### V1__initial_schema.sql
**User Management Tables**

- `users` - User accounts with email/password and Google OAuth support
  - Optimistic locking via `version` column
  - Unique constraints on `email` and `google_sub`
  - Timestamps: `created_at`, `updated_at`
  
- `roles` - System roles (ADMIN, FINANCE, MANAGER, EMPLOYEE, VENDOR, CLIENT)
  - Pre-populated with 6 default roles
  
- `user_roles` - Many-to-many junction table
  - Composite primary key (user_id, role_id)
  - Cascade delete on both sides

### V2__business_entities.sql
**Business Entity Tables**

- `vendors` - Vendor/supplier information
  - Tax ID and contact email (nullable)
  - Indexed on name, tax_id, email
  
- `clients` - Client/customer information
  - Tax ID and contact email (nullable)
  - Indexed on name, tax_id, email
  
- `employees` - Employee records linked to users
  - One-to-one relationship with users table
  - Cascade delete when user is deleted

### V3__documents_and_workflows.sql
**Document Management Tables**

- `documents` - Generic document header (audit trail)
  - Document types: INVOICE_IN, INVOICE_OUT, EXPENSE_CLAIM, REIMBURSEMENT
  - Status workflow: DRAFT → PENDING → APPROVED/REJECTED → PAID/CANCELLED
  - Optimistic locking via `version` column
  - Owner tracking via `owner_user_id`
  - Composite indexes on (doc_type, status)

- `invoice_in` - Incoming/Payable invoices
  - Inherits from documents (shared PK)
  - Links to vendors
  - Financial fields: subtotal, tax, total
  - Currency support (default USD)

- `invoice_out` - Outgoing/Receivable invoices
  - Inherits from documents (shared PK)
  - Links to clients
  - Financial fields: subtotal, tax, total
  - Currency support (default USD)

- `po_headers` - Purchase Orders
  - Links to vendors
  - Unique PO number

- `grn_headers` - Goods Receipt Notes
  - Links to vendors
  - Tracks received date

- `expense_claims` - Employee expense claims
  - Inherits from documents (shared PK)
  - Links to employees
  - Has multiple expense items

- `expense_items` - Line items for expense claims
  - Category-based (Travel, Meals, etc.)
  - Optional notes

- `reimbursements` - Employee reimbursements
  - Inherits from documents (shared PK)
  - Links to employees

### V4__payments_and_audit.sql
**Payment and Audit Tables**

- `payments` - Payment records
  - Direction: OUTBOUND (we pay), INBOUND (we receive)
  - Method: BANK, CHEQUE, CARD
  - Links to documents
  - Transaction reference tracking

- `credit_notes` - Credit note tracking
  - Direction: VENDOR (from vendor), CLIENT (to client)
  - Links to related documents
  - Reason tracking

- `audit_logs` - Full audit trail
  - Tracks all document state changes
  - Records user, action, status transitions
  - Optional notes
  - Indexed for efficient querying

## JPA Entities

### Enums
- `DocumentType` - INVOICE_IN, INVOICE_OUT, EXPENSE_CLAIM, REIMBURSEMENT
- `DocumentStatus` - DRAFT, PENDING, APPROVED, REJECTED, PAID, CANCELLED
- `PaymentDirection` - OUTBOUND, INBOUND
- `PaymentMethod` - BANK, CHEQUE, CARD
- `CreditNoteDirection` - VENDOR, CLIENT
- `RoleName` - ADMIN, FINANCE, MANAGER, EMPLOYEE, VENDOR, CLIENT

### Core Entities
- `User` - User accounts with roles (many-to-many)
- `Role` - System roles
- `Vendor` - Vendor information
- `Client` - Client information
- `Employee` - Employee records (one-to-one with User)

### Document Entities
- `Document` - Base document class (inheritance root)
- `InvoiceIn` - Incoming invoices (extends Document)
- `InvoiceOut` - Outgoing invoices (extends Document)
- `ExpenseClaim` - Expense claims (extends Document)
- `ExpenseItem` - Expense line items
- `Reimbursement` - Reimbursements (extends Document)
- `PurchaseOrderHeader` - Purchase orders
- `GoodsReceiptNoteHeader` - Goods receipt notes

### Supporting Entities
- `Payment` - Payment records
- `CreditNote` - Credit notes
- `AuditLog` - Audit trail entries

## Key Features

### Optimistic Locking
- `users.version` - Prevents concurrent user updates
- `documents.version` - Prevents concurrent document updates

### Audit Trail
- All documents track `created_at` and `updated_at`
- `audit_logs` table records all state transitions
- Owner tracking on all documents

### Relationships
- **User ↔ Role**: Many-to-many via `user_roles`
- **User ↔ Employee**: One-to-one
- **Document ↔ User**: Many-to-one (owner)
- **InvoiceIn ↔ Vendor**: Many-to-one
- **InvoiceOut ↔ Client**: Many-to-one
- **ExpenseClaim ↔ Employee**: Many-to-one
- **ExpenseClaim ↔ ExpenseItem**: One-to-many
- **Document ↔ Payment**: One-to-many
- **Document ↔ CreditNote**: One-to-many
- **Document ↔ AuditLog**: One-to-many

### Indexes
- Email and Google Sub (users)
- Document type and status combinations
- Invoice numbers and dates
- Vendor and client lookups
- Audit log queries (doc_id, user_id, created_at)

## Entity Annotations

All entities use:
- **Lombok**: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`
- **JPA**: Proper `@Entity`, `@Table`, `@Column` annotations
- **Hibernate**: `@CreationTimestamp`, `@UpdateTimestamp` for automatic timestamps
- **Optimistic Locking**: `@Version` where needed

Document inheritance uses `JOINED` strategy for proper table-per-subclass mapping.

# Sample Workflow Flows

## Overview

This document describes complete end-to-end workflows for the Docflow application with step-by-step examples using the dev seed data.

## Dev Seed Data

### Users & Credentials

All users have password: `Password@123`

| Email | Role | User ID | Description |
|-------|------|---------|-------------|
| admin@docflow.com | ADMIN | 1 | System administrator |
| finance1@docflow.com | FINANCE | 2 | Finance manager |
| manager1@docflow.com | MANAGER | 3 | Department manager |
| employee1@docflow.com | EMPLOYEE | 4 | Regular employee |
| vendor1@acmecorp.com | VENDOR | 5 | Vendor contact |
| client1@techcorp.com | CLIENT | 6 | Client contact |

### Manager Hierarchy

```
Finance Manager (finance1@docflow.com)
  └── Department Manager (manager1@docflow.com)
        └── John Employee (employee1@docflow.com)
```

### Sample Data

- **Vendors**: Acme Corporation, Global Supplies Ltd, Tech Solutions Inc
- **Clients**: TechCorp Industries, Retail Enterprises, Manufacturing Co
- **Purchase Orders**: PO-2024-001, PO-2024-002
- **GRN**: GRN-2024-001

## Flow 1: Payable Invoice (Incoming Invoice)

**Scenario**: Company receives invoice from vendor and processes payment

### Workflow States

```
DRAFT → PENDING → APPROVED → PAID
```

### Step-by-Step Process

#### Step 1: Login as Finance User

```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "finance1@docflow.com",
  "password": "Password@123"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "email": "finance1@docflow.com",
  "name": "Finance Manager",
  "roles": ["ROLE_FINANCE"]
}
```

Save the token for subsequent requests.

#### Step 2: Create Invoice (DRAFT)

```bash
POST /api/invoices/in
Authorization: Bearer {token}
Content-Type: application/json

{
  "vendorId": 1,
  "invoiceNo": "INV-ACME-2024-003",
  "invoiceDate": "2024-11-15",
  "dueDate": "2024-12-15",
  "currency": "USD",
  "subtotal": 2000.00,
  "tax": 200.00,
  "total": 2200.00
}
```

**Response**: Invoice created with `status: "DRAFT"`, `id: 5`

#### Step 3: Upload Supporting Documents

```bash
POST /api/docs/5/files
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: [invoice_scan.pdf]
```

**Response**: File uploaded successfully

#### Step 4: Submit for Approval (DRAFT → PENDING)

```bash
POST /api/invoices/in/5/submit
Authorization: Bearer {token}
Content-Type: application/json

{
  "note": "Invoice received from Acme Corporation for office supplies"
}
```

**Response**: Invoice status changed to `PENDING`

#### Step 5: Approve Invoice (PENDING → APPROVED)

**Approver**: MANAGER or FINANCE role (or PO approver if linked)

```bash
POST /api/invoices/in/5/approve
Authorization: Bearer {finance_token}
Content-Type: application/json

{
  "note": "Approved - matches delivery receipt"
}
```

**Response**: Invoice status changed to `APPROVED`

#### Step 6: Record Payment (APPROVED → PAID)

**Payer**: FINANCE or ADMIN role

```bash
POST /api/invoices/in/5/pay
Authorization: Bearer {finance_token}
Content-Type: application/json

{
  "method": "BANK",
  "amount": 2200.00,
  "paidAt": "2024-11-20T14:30:00",
  "reference": "TXN-BANK-20241120-001",
  "note": "Payment processed via wire transfer"
}
```

**Response**: Invoice status changed to `PAID`, payment record created (OUTBOUND)

#### Step 7: View Audit Trail

```bash
GET /api/audit-logs/document/5
Authorization: Bearer {token}
```

**Response**: Complete audit trail showing all transitions

### Alternative Flow: Invoice with Purchase Order

If invoice is linked to a PO with an assigned approver:

```bash
POST /api/invoices/in
{
  "vendorId": 1,
  "poHeaderId": 1,  // Links to PO-2024-001
  "invoiceNo": "INV-ACME-2024-004",
  ...
}
```

**Approval Rule**: Only the PO's assigned approver (or ADMIN) can approve this invoice.

---

## Flow 2: Receivable Invoice (Outgoing Invoice)

**Scenario**: Company issues invoice to client and records incoming payment

### Workflow States

```
DRAFT → PENDING → APPROVED → PAID (inbound payment)
```

### Step-by-Step Process

#### Step 1: Login as Finance User

```bash
POST /api/auth/login
{
  "email": "finance1@docflow.com",
  "password": "Password@123"
}
```

#### Step 2: Create Outgoing Invoice (DRAFT)

```bash
POST /api/invoices/out
Authorization: Bearer {token}
Content-Type: application/json

{
  "clientId": 1,
  "invoiceNo": "OUT-2024-002",
  "invoiceDate": "2024-11-15",
  "dueDate": "2024-12-15",
  "currency": "USD",
  "subtotal": 10000.00,
  "tax": 1000.00,
  "total": 11000.00
}
```

**Response**: Invoice created with `status: "DRAFT"`, `id: 6`

#### Step 3: Submit for Approval (DRAFT → PENDING)

```bash
POST /api/invoices/out/6/submit
Authorization: Bearer {token}
{
  "note": "Invoice for consulting services - November 2024"
}
```

**Response**: Status changed to `PENDING`

#### Step 4: Approve Invoice (PENDING → APPROVED)

**Approver**: FINANCE or ADMIN role only

```bash
POST /api/invoices/out/6/approve
Authorization: Bearer {finance_token}
{
  "note": "Approved - ready to send to client"
}
```

**Response**: Status changed to `APPROVED`

#### Step 5: Record Incoming Payment (APPROVED → PAID)

**Recorder**: FINANCE or ADMIN role

```bash
POST /api/invoices/out/6/pay
Authorization: Bearer {finance_token}
Content-Type: application/json

{
  "method": "BANK",
  "amount": 11000.00,
  "paidAt": "2024-12-10T10:15:00",
  "reference": "CLIENT-PAY-20241210-001",
  "note": "Payment received from TechCorp Industries"
}
```

**Response**: Invoice status changed to `PAID`, payment record created (INBOUND)

---

## Flow 3: Expense Claim → Reimbursement → Paid

**Scenario**: Employee submits expense claim, manager approves, finance creates reimbursement and pays

### Workflow States

```
Employee: DRAFT → PENDING → APPROVED
Finance: Create Reimbursement (PENDING) → APPROVED → PAID
```

### Part A: Expense Claim Workflow

#### Step 1: Login as Employee

```bash
POST /api/auth/login
{
  "email": "employee1@docflow.com",
  "password": "Password@123"
}
```

#### Step 2: Create Expense Claim (DRAFT)

```bash
POST /api/claims
Authorization: Bearer {employee_token}
Content-Type: application/json

{
  "claimDate": "2024-11-15",
  "currency": "USD",
  "items": [
    {
      "description": "Client dinner - Project kickoff meeting",
      "date": "2024-11-14",
      "category": "Meals & Entertainment",
      "amount": 180.00
    },
    {
      "description": "Taxi to client office",
      "date": "2024-11-14",
      "category": "Transportation",
      "amount": 35.00
    },
    {
      "description": "Parking fees",
      "date": "2024-11-14",
      "category": "Transportation",
      "amount": 15.00
    }
  ]
}
```

**Response**: Claim created with `status: "DRAFT"`, `id: 7`, `total: 230.00`

#### Step 3: Upload Receipts

```bash
POST /api/docs/7/files
Authorization: Bearer {employee_token}
Content-Type: multipart/form-data

file: [dinner_receipt.pdf]
```

```bash
POST /api/docs/7/files
file: [taxi_receipt.jpg]
```

#### Step 4: Submit Claim (DRAFT → PENDING)

```bash
POST /api/claims/7/submit
Authorization: Bearer {employee_token}
{
  "note": "Expenses for client meeting on Nov 14"
}
```

**Response**: Status changed to `PENDING`

#### Step 5: Manager Approves Claim (PENDING → APPROVED)

**Approver**: Employee's manager (manager1@docflow.com) or ADMIN

```bash
# Login as manager
POST /api/auth/login
{
  "email": "manager1@docflow.com",
  "password": "Password@123"
}

# Approve claim
POST /api/claims/7/approve
Authorization: Bearer {manager_token}
{
  "note": "Approved - valid business expenses"
}
```

**Response**: Claim status changed to `APPROVED`

**Authorization Rule**: Only the employee's direct manager (or ADMIN) can approve.

### Part B: Reimbursement Workflow

#### Step 6: Finance Creates Reimbursement

**Creator**: FINANCE or ADMIN role

```bash
# Login as finance
POST /api/auth/login
{
  "email": "finance1@docflow.com",
  "password": "Password@123"
}

# Create reimbursement from approved claim
POST /api/reimbursements
Authorization: Bearer {finance_token}
Content-Type: application/json

{
  "expenseClaimId": 7,
  "requestedDate": "2024-11-16",
  "currency": "USD",
  "total": 230.00
}
```

**Response**: Reimbursement created with `status: "PENDING"`, `id: 8`

**Validation**:
- ✅ Claim must be APPROVED
- ✅ No duplicate reimbursement for same claim
- ✅ Total must match claim total (within tolerance)
- ✅ Currency must match

#### Step 7: Finance Approves Reimbursement (PENDING → APPROVED)

```bash
POST /api/reimbursements/8/approve
Authorization: Bearer {finance_token}
{
  "note": "Approved for payment"
}
```

**Response**: Status changed to `APPROVED`

#### Step 8: Finance Pays Reimbursement (APPROVED → PAID)

```bash
POST /api/reimbursements/8/pay
Authorization: Bearer {finance_token}
Content-Type: application/json

{
  "method": "BANK",
  "amount": 230.00,
  "paidAt": "2024-11-18T16:00:00",
  "reference": "REIMB-20241118-001",
  "note": "Reimbursement paid to John Employee"
}
```

**Response**: Reimbursement status changed to `PAID`, payment record created (OUTBOUND)

---

## Flow 4: Purchase Order → Goods Receipt → Invoice Matching

**Scenario**: Complete procurement cycle with PO, GRN, and invoice matching

### Step 1: Create Purchase Order

```bash
POST /api/purchase-orders
Authorization: Bearer {finance_token}
{
  "poNo": "PO-2024-003",
  "vendorId": 2,
  "currency": "USD",
  "total": 8000.00,
  "approverUserId": 2  // Assign specific approver
}
```

### Step 2: Receive Goods

```bash
POST /api/goods-receipts
Authorization: Bearer {token}
{
  "grnNo": "GRN-2024-002",
  "poId": 3,
  "vendorId": 2,
  "currency": "USD",
  "total": 8000.00
}
```

### Step 3: Receive Invoice Linked to PO

```bash
POST /api/invoices/in
Authorization: Bearer {finance_token}
{
  "vendorId": 2,
  "poHeaderId": 3,  // Link to PO
  "invoiceNo": "INV-VENDOR-2024-005",
  "invoiceDate": "2024-11-20",
  "dueDate": "2024-12-20",
  "currency": "USD",
  "subtotal": 7272.73,
  "tax": 727.27,
  "total": 8000.00
}
```

### Step 4: Submit and Approve

**Approval Rule**: Since invoice is linked to PO with assigned approver (user_id=2), only that user (or ADMIN) can approve.

```bash
POST /api/invoices/in/9/submit
POST /api/invoices/in/9/approve  # Must be PO approver
```

---

## Common Operations

### Filtering Documents

#### Filter Pending Invoices by Date Range

```bash
POST /api/invoices/in/filter?page=0&size=20&sortBy=invoiceDate&sortDir=DESC
Authorization: Bearer {token}
Content-Type: application/json

{
  "status": "PENDING",
  "dateFrom": "2024-11-01",
  "dateTo": "2024-11-30",
  "amountMin": 100.00
}
```

#### Filter Employee's Expense Claims

```bash
POST /api/claims/filter
Authorization: Bearer {token}
{
  "employeeId": 3,
  "status": "PENDING",
  "claimDateFrom": "2024-11-01"
}
```

### Viewing Audit Logs

```bash
GET /api/audit-logs/document/{docId}
Authorization: Bearer {token}
```

### Managing Files

```bash
# Upload
POST /api/docs/{docId}/files
Content-Type: multipart/form-data
file: [document.pdf]

# List
GET /api/docs/{docId}/files

# Download
GET /api/docs/{docId}/files/{fileId}

# Delete (owner, admin, or finance only)
DELETE /api/docs/{docId}/files/{fileId}
```

---

## Authorization Summary

### Expense Claims

| Action | Authorized Users |
|--------|------------------|
| Create | Any authenticated user (must be employee) |
| Submit | Claim owner |
| Approve | Claimant's manager or ADMIN |
| Reject | Claimant's manager or ADMIN |

### Incoming Invoices

| Action | Authorized Users |
|--------|------------------|
| Create | Any authenticated user |
| Submit | Invoice owner |
| Approve (with PO) | PO approver or ADMIN |
| Approve (no PO) | MANAGER, FINANCE, or ADMIN |
| Pay | FINANCE or ADMIN |

### Outgoing Invoices

| Action | Authorized Users |
|--------|------------------|
| Create | Any authenticated user |
| Submit | Invoice owner |
| Approve | FINANCE or ADMIN |
| Pay | FINANCE or ADMIN |

### Reimbursements

| Action | Authorized Users |
|--------|------------------|
| Create | FINANCE or ADMIN |
| Approve | FINANCE or ADMIN |
| Pay | FINANCE or ADMIN |

---

## Error Scenarios

### 1. Unauthorized Approval

```bash
# Employee tries to approve their own claim
POST /api/claims/7/approve
Authorization: Bearer {employee_token}
```

**Response**: `403 Forbidden`
```json
{
  "type": "https://api.docflow.com/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Only the claimant's manager (manager1@docflow.com) or ADMIN can approve this expense claim"
}
```

### 2. Invalid Status Transition

```bash
# Try to pay invoice that's still PENDING
POST /api/invoices/in/5/pay
```

**Response**: `400 Bad Request`
```json
{
  "type": "https://api.docflow.com/errors/invalid-status-transition",
  "title": "Invalid Status Transition",
  "status": 400,
  "detail": "Invalid status transition from PENDING to PAID"
}
```

### 3. Duplicate Reimbursement

```bash
# Try to create second reimbursement for same claim
POST /api/reimbursements
{
  "expenseClaimId": 7,
  ...
}
```

**Response**: `400 Bad Request`
```json
{
  "detail": "An active reimbursement already exists for this expense claim"
}
```

---

## Testing Checklist

### Payable Invoice Flow
- [ ] Create invoice as finance user
- [ ] Upload supporting documents
- [ ] Submit for approval
- [ ] Approve as manager/finance
- [ ] Record payment
- [ ] Verify audit trail

### Receivable Invoice Flow
- [ ] Create outgoing invoice
- [ ] Submit for approval
- [ ] Approve as finance
- [ ] Record incoming payment
- [ ] Verify payment direction (INBOUND)

### Expense Claim Flow
- [ ] Create claim as employee
- [ ] Add multiple expense items
- [ ] Upload receipts
- [ ] Submit claim
- [ ] Approve as manager (not other managers)
- [ ] Create reimbursement as finance
- [ ] Approve and pay reimbursement
- [ ] Verify total matching

### PO-Based Routing
- [ ] Create PO with assigned approver
- [ ] Create invoice linked to PO
- [ ] Verify only PO approver can approve
- [ ] Verify other finance users cannot approve

### File Management
- [ ] Upload files to documents
- [ ] List files
- [ ] Download files
- [ ] Delete files (authorization check)

### Filtering
- [ ] Filter by status
- [ ] Filter by date range
- [ ] Filter by amount range
- [ ] Verify pagination
- [ ] Verify totals calculation

---

## Quick Reference: API Endpoints

### Authentication
- `POST /api/auth/signup` - Register new user
- `POST /api/auth/login` - Login and get JWT token

### Invoices IN
- `POST /api/invoices/in` - Create
- `GET /api/invoices/in/{id}` - Get by ID
- `GET /api/invoices/in` - List with filters
- `POST /api/invoices/in/filter` - Advanced filtering
- `PUT /api/invoices/in/{id}` - Update (DRAFT only)
- `POST /api/invoices/in/{id}/submit` - Submit
- `POST /api/invoices/in/{id}/approve` - Approve
- `POST /api/invoices/in/{id}/reject` - Reject
- `POST /api/invoices/in/{id}/pay` - Pay

### Invoices OUT
- Same pattern as Invoices IN under `/api/invoices/out`

### Expense Claims
- `POST /api/claims` - Create
- `GET /api/claims/{id}` - Get by ID
- `GET /api/claims` - List with filters
- `POST /api/claims/filter` - Advanced filtering
- `POST /api/claims/{id}/submit` - Submit
- `POST /api/claims/{id}/approve` - Approve
- `POST /api/claims/{id}/reject` - Reject

### Reimbursements
- `POST /api/reimbursements` - Create from claim
- `GET /api/reimbursements/{id}` - Get by ID
- `GET /api/reimbursements` - List with filters
- `POST /api/reimbursements/filter` - Advanced filtering
- `POST /api/reimbursements/{id}/approve` - Approve
- `POST /api/reimbursements/{id}/pay` - Pay

### Files
- `POST /api/docs/{docId}/files` - Upload
- `GET /api/docs/{docId}/files` - List
- `GET /api/docs/{docId}/files/{fileId}` - Download
- `DELETE /api/docs/{docId}/files/{fileId}` - Delete

### Audit Logs
- `GET /api/audit-logs/document/{docId}` - Get by document
- `GET /api/audit-logs/user/{userId}` - Get by user
- `GET /api/audit-logs/action/{action}` - Get by action

---

## Next Steps

1. Import Postman collection (see `docs/Docflow.postman_collection.json`)
2. Set up environment variables in Postman
3. Run through each flow to verify functionality
4. Test authorization rules
5. Verify audit logging
6. Test file upload/download
7. Test filtering and pagination

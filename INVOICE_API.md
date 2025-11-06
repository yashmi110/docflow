# Invoice Management API

## Overview

Complete implementation of Incoming (Payable) and Outgoing (Receivable) invoice management with full workflow, RBAC, and audit logging.

## Features

- ✅ Create, Read, Update invoices
- ✅ Workflow: DRAFT → PENDING → APPROVED → PAID
- ✅ Role-based access control (RBAC)
- ✅ Ownership validation
- ✅ Payment recording
- ✅ Automatic audit logging
- ✅ Optimistic locking
- ✅ Pagination and filtering

## Architecture

### Components

**DTOs**:
- `InvoiceInRequest/Response` - Incoming invoice data
- `InvoiceOutRequest/Response` - Outgoing invoice data
- `PaymentRequest` - Payment recording

**Mappers** (MapStruct):
- `InvoiceInMapper` - Entity ↔ DTO conversion
- `InvoiceOutMapper` - Entity ↔ DTO conversion

**Repositories**:
- `InvoiceInRepository` - Incoming invoice queries
- `InvoiceOutRepository` - Outgoing invoice queries
- `VendorRepository` - Vendor management
- `ClientRepository` - Client management
- `PaymentRepository` - Payment records

**Services**:
- `InvoiceInService` - Business logic for incoming invoices
- `InvoiceOutService` - Business logic for outgoing invoices
- `DocumentStatusMachine` - Workflow enforcement

**Controllers**:
- `InvoiceInController` - REST API for incoming invoices
- `InvoiceOutController` - REST API for outgoing invoices

**Security**:
- `SecurityUtils` - Get current authenticated user

## API Endpoints

### A) Incoming Invoices (INVOICE_IN)

Invoices we receive from vendors that we must pay.

#### Create Invoice (DRAFT)

```http
POST /api/invoices/in
Authorization: Bearer <token>
Content-Type: application/json

{
  "vendorId": 1,
  "invoiceNo": "INV-2024-001",
  "invoiceDate": "2024-11-06",
  "dueDate": "2024-12-06",
  "currency": "USD",
  "subtotal": 1000.00,
  "tax": 100.00,
  "total": 1100.00
}
```

**Response**: `201 Created`
```json
{
  "id": 1,
  "vendorId": 1,
  "vendorName": "Acme Corp",
  "invoiceNo": "INV-2024-001",
  "invoiceDate": "2024-11-06",
  "dueDate": "2024-12-06",
  "currency": "USD",
  "subtotal": 1000.00,
  "tax": 100.00,
  "total": 1100.00,
  "status": "DRAFT",
  "ownerUserId": 5,
  "ownerUserEmail": "employee@company.com",
  "ownerUserName": "John Doe",
  "createdAt": "2024-11-06T10:00:00",
  "updatedAt": "2024-11-06T10:00:00",
  "version": 0
}
```

**Authorization**: Any authenticated user

#### Get Invoice by ID

```http
GET /api/invoices/in/{id}
Authorization: Bearer <token>
```

**Authorization**: Any authenticated user

#### List Invoices (with filters)

```http
GET /api/invoices/in?status=PENDING&vendorId=1&page=0&size=20&sortBy=createdAt&sortDir=DESC
Authorization: Bearer <token>
```

**Query Parameters**:
- `status` (optional) - Filter by status (DRAFT, PENDING, APPROVED, REJECTED, PAID)
- `vendorId` (optional) - Filter by vendor
- `page` (default: 0) - Page number
- `size` (default: 20) - Page size
- `sortBy` (default: createdAt) - Sort field
- `sortDir` (default: DESC) - Sort direction (ASC/DESC)

**Authorization**: Any authenticated user

#### Update Invoice (DRAFT only)

```http
PUT /api/invoices/in/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "vendorId": 1,
  "invoiceNo": "INV-2024-001-UPDATED",
  "invoiceDate": "2024-11-06",
  "dueDate": "2024-12-06",
  "currency": "USD",
  "subtotal": 1200.00,
  "tax": 120.00,
  "total": 1320.00
}
```

**Authorization**: Invoice creator only  
**Constraint**: Only DRAFT invoices can be edited

#### Submit Invoice (DRAFT → PENDING)

```http
POST /api/invoices/in/{id}/submit
Authorization: Bearer <token>
Content-Type: application/json

{
  "note": "Ready for approval"
}
```

**Authorization**: Invoice creator only

#### Approve Invoice (PENDING → APPROVED)

```http
POST /api/invoices/in/{id}/approve
Authorization: Bearer <token>
Content-Type: application/json

{
  "note": "Budget approved"
}
```

**Authorization**: MANAGER, FINANCE, or ADMIN roles

#### Reject Invoice (PENDING → REJECTED)

```http
POST /api/invoices/in/{id}/reject
Authorization: Bearer <token>
Content-Type: application/json

{
  "note": "Missing documentation"
}
```

**Authorization**: MANAGER, FINANCE, or ADMIN roles

#### Pay Invoice (APPROVED → PAID)

```http
POST /api/invoices/in/{id}/pay
Authorization: Bearer <token>
Content-Type: application/json

{
  "method": "BANK",
  "amount": 1100.00,
  "paidAt": "2024-11-06T15:30:00",
  "reference": "TXN-12345",
  "note": "Payment processed via wire transfer"
}
```

**Payment Methods**: BANK, CHEQUE, CARD

**Authorization**: FINANCE or ADMIN roles

**Note**: Creates OUTBOUND payment record (we pay vendor)

### B) Outgoing Invoices (INVOICE_OUT)

Invoices we send to clients that they must pay us.

#### Create Invoice (DRAFT)

```http
POST /api/invoices/out
Authorization: Bearer <token>
Content-Type: application/json

{
  "clientId": 2,
  "invoiceNo": "OUT-2024-001",
  "invoiceDate": "2024-11-06",
  "dueDate": "2024-12-06",
  "currency": "USD",
  "subtotal": 5000.00,
  "tax": 500.00,
  "total": 5500.00
}
```

**Response**: Same structure as Invoice IN

#### Get Invoice by ID

```http
GET /api/invoices/out/{id}
Authorization: Bearer <token>
```

#### List Invoices (with filters)

```http
GET /api/invoices/out?status=APPROVED&clientId=2&page=0&size=20
Authorization: Bearer <token>
```

**Query Parameters**:
- `status` (optional) - Filter by status
- `clientId` (optional) - Filter by client
- `page`, `size`, `sortBy`, `sortDir` - Same as Invoice IN

#### Update Invoice (DRAFT only)

```http
PUT /api/invoices/out/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

**Authorization**: Invoice creator only

#### Submit Invoice (DRAFT → PENDING)

```http
POST /api/invoices/out/{id}/submit
Authorization: Bearer <token>
```

#### Approve Invoice (PENDING → APPROVED)

```http
POST /api/invoices/out/{id}/approve
Authorization: Bearer <token>
```

**Authorization**: MANAGER, FINANCE, or ADMIN roles

#### Reject Invoice (PENDING → REJECTED)

```http
POST /api/invoices/out/{id}/reject
Authorization: Bearer <token>
```

**Authorization**: MANAGER, FINANCE, or ADMIN roles

#### Record Payment (APPROVED → PAID)

```http
POST /api/invoices/out/{id}/pay
Authorization: Bearer <token>
Content-Type: application/json

{
  "method": "BANK",
  "amount": 5500.00,
  "paidAt": "2024-11-06T16:00:00",
  "reference": "CLIENT-PAY-789",
  "note": "Payment received from client"
}
```

**Authorization**: FINANCE or ADMIN roles

**Note**: Creates INBOUND payment record (client pays us)

## Workflow

### Status Transitions

```
DRAFT → PENDING → APPROVED → PAID
              ↓
           REJECTED
```

### Role-Based Permissions

| Action | Roles | Additional Constraints |
|--------|-------|------------------------|
| Create | Any authenticated user | - |
| View | Any authenticated user | - |
| Update | Any authenticated user | Must be owner, DRAFT only |
| Submit | Any authenticated user | Must be owner |
| Approve | MANAGER, FINANCE, ADMIN | - |
| Reject | MANAGER, FINANCE, ADMIN | - |
| Pay | FINANCE, ADMIN | - |

## Payment Direction

- **Invoice IN** (Payable): Payment direction = **OUTBOUND** (we pay vendor)
- **Invoice OUT** (Receivable): Payment direction = **INBOUND** (client pays us)

## Validation

### Invoice Request Validation

- `vendorId`/`clientId`: Required, must exist
- `invoiceNo`: Required, max 100 chars, must be unique
- `invoiceDate`: Required
- `dueDate`: Optional
- `currency`: Required, exactly 3 characters
- `subtotal`: Required, > 0
- `tax`: Optional, >= 0 (defaults to 0)
- `total`: Required, > 0

### Payment Request Validation

- `method`: Required (BANK, CHEQUE, CARD)
- `amount`: Required, > 0
- `paidAt`: Required
- `reference`: Optional, max 255 chars
- `note`: Optional

## Error Responses

### Resource Not Found (404)

```json
{
  "type": "https://api.docflow.com/errors/not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Invoice not found with id: 123"
}
```

### Forbidden (403)

```json
{
  "type": "https://api.docflow.com/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Only the invoice creator can edit it"
}
```

### Invalid Status Transition (400)

```json
{
  "type": "https://api.docflow.com/errors/invalid-status-transition",
  "title": "Invalid Status Transition",
  "status": 400,
  "detail": "Invalid status transition from PAID to PENDING"
}
```

### Validation Error (400)

```json
{
  "type": "https://api.docflow.com/errors/validation-failed",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed",
  "errors": {
    "invoiceNo": "Invoice number is required",
    "total": "Total must be greater than 0"
  }
}
```

### Concurrent Modification (409)

```json
{
  "type": "https://api.docflow.com/errors/optimistic-locking-failure",
  "title": "Concurrent Modification Detected",
  "status": 409,
  "detail": "The document was modified by another user. Please refresh and try again."
}
```

## Audit Logging

All status transitions are automatically logged. See audit logs:

```http
GET /api/audit-logs/document/{invoiceId}
Authorization: Bearer <token>
```

## Testing Examples

### Create and Submit Invoice

```bash
# 1. Login
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@docflow.com","password":"Admin@123"}' \
  | jq -r '.token')

# 2. Create invoice
INVOICE_ID=$(curl -X POST http://localhost:8080/api/invoices/in \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "vendorId": 1,
    "invoiceNo": "INV-2024-001",
    "invoiceDate": "2024-11-06",
    "dueDate": "2024-12-06",
    "currency": "USD",
    "subtotal": 1000.00,
    "tax": 100.00,
    "total": 1100.00
  }' | jq -r '.id')

# 3. Submit for approval
curl -X POST "http://localhost:8080/api/invoices/in/$INVOICE_ID/submit" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"note":"Ready for approval"}'

# 4. Approve
curl -X POST "http://localhost:8080/api/invoices/in/$INVOICE_ID/approve" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"note":"Approved"}'

# 5. Pay
curl -X POST "http://localhost:8080/api/invoices/in/$INVOICE_ID/pay" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "method": "BANK",
    "amount": 1100.00,
    "paidAt": "2024-11-06T15:30:00",
    "reference": "TXN-12345",
    "note": "Payment processed"
  }'

# 6. View audit logs
curl -X GET "http://localhost:8080/api/audit-logs/document/$INVOICE_ID" \
  -H "Authorization: Bearer $TOKEN"
```

## Database Schema

### invoice_in Table

- `id` (PK)
- `vendor_id` (FK → vendors)
- `invoice_no` (unique)
- `invoice_date`
- `due_date`
- `currency`
- `subtotal`
- `tax`
- `total`
- Inherits from `documents` table (doc_type, status, owner_user_id, etc.)

### invoice_out Table

- `id` (PK)
- `client_id` (FK → clients)
- `invoice_no` (unique)
- `invoice_date`
- `due_date`
- `currency`
- `subtotal`
- `tax`
- `total`
- Inherits from `documents` table

### payments Table

- `id` (PK)
- `doc_id` (FK → documents)
- `direction` (OUTBOUND/INBOUND)
- `method` (BANK/CHEQUE/CARD)
- `amount`
- `paid_at`
- `reference`
- `created_at`

## Next Steps

1. **Seed Data**: Create vendors and clients for testing
2. **Integration Tests**: Add comprehensive test coverage
3. **Postman Collection**: Export API collection
4. **Frontend Integration**: Connect React frontend
5. **Reports**: Add invoice reports and analytics
6. **Email Notifications**: Send notifications on status changes
7. **File Attachments**: Support invoice document uploads
8. **Bulk Operations**: Approve/reject multiple invoices

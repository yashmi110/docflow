# Expense Claims & Reimbursements API

## Overview

Complete implementation of Employee Expense Claims and Reimbursements with full workflow, validation, and audit logging.

## Features

- ✅ Employee-owned expense claims with line items
- ✅ Manager approval workflow
- ✅ Reimbursement creation from approved claims
- ✅ Finance approval and payment
- ✅ Duplicate reimbursement prevention
- ✅ Total amount validation with tolerance
- ✅ Currency matching validation
- ✅ Automatic audit logging
- ✅ Optimistic locking

## Architecture

### Expense Claims Workflow

```
Employee creates claim (DRAFT)
  ↓
Employee submits (DRAFT → PENDING)
  ↓
Manager approves/rejects (PENDING → APPROVED/REJECTED)
  ↓
Finance creates reimbursement (from APPROVED claim)
```

### Reimbursements Workflow

```
Finance creates reimbursement (PENDING)
  ↓
Finance approves (PENDING → APPROVED)
  ↓
Finance pays (APPROVED → PAID with OUTBOUND payment)
```

## API Endpoints

### A) Expense Claims

#### Create Expense Claim (DRAFT)

```http
POST /api/claims
Authorization: Bearer <token>
Content-Type: application/json

{
  "claimDate": "2024-11-06",
  "currency": "USD",
  "items": [
    {
      "description": "Client dinner",
      "date": "2024-11-05",
      "category": "Meals",
      "amount": 150.00
    },
    {
      "description": "Taxi to airport",
      "date": "2024-11-06",
      "category": "Transportation",
      "amount": 45.00
    }
  ]
}
```

**Response**: `201 Created`
```json
{
  "id": 1,
  "employeeId": 5,
  "employeeName": "John Doe",
  "employeeEmail": "john.doe@company.com",
  "claimDate": "2024-11-06",
  "currency": "USD",
  "total": 195.00,
  "status": "DRAFT",
  "items": [
    {
      "id": 1,
      "description": "Client dinner",
      "date": "2024-11-05",
      "category": "Meals",
      "amount": 150.00
    },
    {
      "id": 2,
      "description": "Taxi to airport",
      "date": "2024-11-06",
      "category": "Transportation",
      "amount": 45.00
    }
  ],
  "ownerUserId": 10,
  "ownerUserEmail": "john.doe@company.com",
  "ownerUserName": "John Doe",
  "createdAt": "2024-11-06T10:00:00",
  "updatedAt": "2024-11-06T10:00:00",
  "version": 0
}
```

**Authorization**: Any authenticated user (must be linked to Employee record)

**Validation**:
- At least one expense item required
- All amounts must be > 0
- Currency must be 3 characters
- Categories and descriptions required

#### Get Expense Claim by ID

```http
GET /api/claims/{id}
Authorization: Bearer <token>
```

#### List Expense Claims (with filters)

```http
GET /api/claims?status=PENDING&employeeId=5&page=0&size=20
Authorization: Bearer <token>
```

**Query Parameters**:
- `status` (optional) - Filter by status
- `employeeId` (optional) - Filter by employee
- `page`, `size`, `sortBy`, `sortDir` - Pagination

#### Submit Expense Claim (DRAFT → PENDING)

```http
POST /api/claims/{id}/submit
Authorization: Bearer <token>
Content-Type: application/json

{
  "note": "Submitted for manager approval"
}
```

**Authorization**: Claim owner only

#### Approve Expense Claim (PENDING → APPROVED)

```http
POST /api/claims/{id}/approve
Authorization: Bearer <token>
Content-Type: application/json

{
  "note": "Approved - valid business expenses"
}
```

**Authorization**: MANAGER or ADMIN roles

#### Reject Expense Claim (PENDING → REJECTED)

```http
POST /api/claims/{id}/reject
Authorization: Bearer <token>
Content-Type: application/json

{
  "note": "Missing receipts for meals"
}
```

**Authorization**: MANAGER or ADMIN roles

### B) Reimbursements

#### Create Reimbursement (from approved claim)

```http
POST /api/reimbursements
Authorization: Bearer <token>
Content-Type: application/json

{
  "expenseClaimId": 1,
  "requestedDate": "2024-11-06",
  "currency": "USD",
  "total": 195.00
}
```

**Response**: `201 Created`
```json
{
  "id": 1,
  "employeeId": 5,
  "employeeName": "John Doe",
  "employeeEmail": "john.doe@company.com",
  "expenseClaimId": 1,
  "requestedDate": "2024-11-06",
  "currency": "USD",
  "total": 195.00,
  "status": "PENDING",
  "ownerUserId": 15,
  "ownerUserEmail": "finance@company.com",
  "ownerUserName": "Finance User",
  "createdAt": "2024-11-06T14:00:00",
  "updatedAt": "2024-11-06T14:00:00",
  "version": 0
}
```

**Authorization**: FINANCE or ADMIN roles

**Validation**:
- ✅ Expense claim must exist and be APPROVED
- ✅ No active reimbursement already exists for this claim
- ✅ Total must match claim total (within tolerance, default 0.01)
- ✅ Currency must match claim currency

**Tolerance Configuration**:
```properties
reimbursement.tolerance=0.01
```

#### Get Reimbursement by ID

```http
GET /api/reimbursements/{id}
Authorization: Bearer <token>
```

#### List Reimbursements (with filters)

```http
GET /api/reimbursements?status=PENDING&employeeId=5&page=0&size=20
Authorization: Bearer <token>
```

#### Approve Reimbursement (PENDING → APPROVED)

```http
POST /api/reimbursements/{id}/approve
Authorization: Bearer <token>
Content-Type: application/json

{
  "note": "Approved for payment"
}
```

**Authorization**: FINANCE or ADMIN roles

#### Pay Reimbursement (APPROVED → PAID)

```http
POST /api/reimbursements/{id}/pay
Authorization: Bearer <token>
Content-Type: application/json

{
  "method": "BANK",
  "amount": 195.00,
  "paidAt": "2024-11-06T15:30:00",
  "reference": "TXN-REIMB-001",
  "note": "Paid via bank transfer"
}
```

**Authorization**: FINANCE or ADMIN roles

**Note**: Creates OUTBOUND payment record (company pays employee)

## Validation Rules

### Expense Claim Validation

1. **Items Required**: At least one expense item must be provided
2. **Amount Validation**: All amounts must be > 0
3. **Currency Format**: Must be exactly 3 characters (e.g., USD, EUR)
4. **Description**: Required, max 255 characters
5. **Category**: Required, max 100 characters
6. **Date**: Required for each item

### Reimbursement Validation

1. **Claim Status**: Only APPROVED claims can have reimbursements
2. **Duplicate Prevention**: Only one active reimbursement per claim
3. **Total Matching**: Reimbursement total must match claim total (within tolerance)
4. **Currency Matching**: Reimbursement currency must match claim currency
5. **Tolerance**: Default 0.01, configurable via `reimbursement.tolerance`

**Example Tolerance Check**:
```
Claim Total: 195.00
Reimbursement Total: 195.01
Difference: 0.01
Tolerance: 0.01
Result: ✅ VALID (difference <= tolerance)

Claim Total: 195.00
Reimbursement Total: 195.05
Difference: 0.05
Tolerance: 0.01
Result: ❌ INVALID (difference > tolerance)
```

## Role-Based Access Control

| Action | Roles | Constraints |
|--------|-------|-------------|
| **Create Claim** | Any authenticated user | Must be linked to Employee |
| **View Claims** | Any authenticated user | - |
| **Submit Claim** | Any authenticated user | Must be claim owner |
| **Approve/Reject Claim** | MANAGER, ADMIN | - |
| **Create Reimbursement** | FINANCE, ADMIN | Claim must be APPROVED |
| **View Reimbursements** | Any authenticated user | - |
| **Approve Reimbursement** | FINANCE, ADMIN | - |
| **Pay Reimbursement** | FINANCE, ADMIN | - |

## Error Responses

### Claim Not Approved (400)

```json
{
  "type": "https://api.docflow.com/errors/bad-request",
  "title": "Bad Request",
  "status": 400,
  "detail": "Can only create reimbursement for APPROVED expense claims"
}
```

### Duplicate Reimbursement (400)

```json
{
  "type": "https://api.docflow.com/errors/bad-request",
  "title": "Bad Request",
  "status": 400,
  "detail": "An active reimbursement already exists for this expense claim"
}
```

### Total Mismatch (400)

```json
{
  "type": "https://api.docflow.com/errors/bad-request",
  "title": "Bad Request",
  "status": 400,
  "detail": "Reimbursement total 195.50 does not match approved claim total 195.00 (tolerance: 0.01)"
}
```

### Currency Mismatch (400)

```json
{
  "type": "https://api.docflow.com/errors/bad-request",
  "title": "Bad Request",
  "status": 400,
  "detail": "Reimbursement currency EUR does not match claim currency USD"
}
```

### Not Employee (400)

```json
{
  "type": "https://api.docflow.com/errors/bad-request",
  "title": "Bad Request",
  "status": 400,
  "detail": "User is not associated with an employee record"
}
```

## Audit Logging

All status transitions are automatically logged:

```http
GET /api/audit-logs/document/{claimId}
GET /api/audit-logs/document/{reimbursementId}
```

## Testing Examples

### Complete Workflow

```bash
# 1. Login as employee
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"employee@company.com","password":"password"}' \
  | jq -r '.token')

# 2. Create expense claim
CLAIM_ID=$(curl -X POST http://localhost:8080/api/claims \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "claimDate": "2024-11-06",
    "currency": "USD",
    "items": [
      {
        "description": "Client dinner",
        "date": "2024-11-05",
        "category": "Meals",
        "amount": 150.00
      }
    ]
  }' | jq -r '.id')

# 3. Submit claim
curl -X POST "http://localhost:8080/api/claims/$CLAIM_ID/submit" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"note":"Ready for approval"}'

# 4. Login as manager
MANAGER_TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"manager@company.com","password":"password"}' \
  | jq -r '.token')

# 5. Approve claim
curl -X POST "http://localhost:8080/api/claims/$CLAIM_ID/approve" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -d '{"note":"Approved"}'

# 6. Login as finance
FINANCE_TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"finance@company.com","password":"password"}' \
  | jq -r '.token')

# 7. Create reimbursement
REIMB_ID=$(curl -X POST http://localhost:8080/api/reimbursements \
  -H "Authorization: Bearer $FINANCE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "expenseClaimId": '$CLAIM_ID',
    "requestedDate": "2024-11-06",
    "currency": "USD",
    "total": 150.00
  }' | jq -r '.id')

# 8. Approve reimbursement
curl -X POST "http://localhost:8080/api/reimbursements/$REIMB_ID/approve" \
  -H "Authorization: Bearer $FINANCE_TOKEN" \
  -d '{"note":"Approved for payment"}'

# 9. Pay reimbursement
curl -X POST "http://localhost:8080/api/reimbursements/$REIMB_ID/pay" \
  -H "Authorization: Bearer $FINANCE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "method": "BANK",
    "amount": 150.00,
    "paidAt": "2024-11-06T15:30:00",
    "reference": "TXN-REIMB-001",
    "note": "Paid"
  }'
```

## Database Schema

### expense_claims Table

- Inherits from `documents` table
- `employee_id` (FK → employees)
- `claim_date`
- `currency`
- `total` (calculated from items)

### expense_items Table

- `id` (PK)
- `claim_id` (FK → expense_claims)
- `description`
- `date`
- `category`
- `amount`

### reimbursements Table

- Inherits from `documents` table
- `employee_id` (FK → employees)
- `requested_date`
- `currency`
- `total`

## Components Created

**DTOs** (6 files):
- `ExpenseItemRequest/Response`
- `ExpenseClaimRequest/Response`
- `ReimbursementRequest/Response`

**Repositories** (3 files):
- `ExpenseClaimRepository`
- `ReimbursementRepository`
- `EmployeeRepository`

**Mappers** (2 files):
- `ExpenseClaimMapper`
- `ReimbursementMapper`

**Services** (2 files):
- `ExpenseClaimService`
- `ReimbursementService`

**Controllers** (2 files):
- `ExpenseClaimController`
- `ReimbursementController`

## Next Steps

1. **Seed Data**: Create employee records linked to users
2. **Receipt Attachments**: Add file upload for receipts
3. **Approval Delegation**: Allow managers to delegate approval
4. **Expense Categories**: Create category master data
5. **Spending Limits**: Add per-category spending limits
6. **Bulk Approval**: Approve multiple claims at once
7. **Reports**: Expense reports by employee, category, period
8. **Email Notifications**: Notify on claim submission/approval

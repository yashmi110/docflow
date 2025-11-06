# Approval Routing & Authorization

## Overview

Comprehensive approver assignment rules with manager hierarchy and PO-based routing. Enforces strict authorization checks to ensure only designated approvers can approve/reject documents.

## Features

- ✅ Manager hierarchy for expense claims
- ✅ PO-based routing for incoming invoices
- ✅ Role-based approval for invoices
- ✅ ADMIN override capability
- ✅ 403 Forbidden responses with ProblemDetails
- ✅ Centralized approval logic
- ✅ Audit logging of all approvals

## Approval Rules

### 1. Expense Claims

**Approver Assignment**:
- **Primary**: Claimant's manager (stored in `employees.manager_user_id`)
- **Override**: ADMIN role

**Rules**:
1. Employee must have an assigned manager
2. Only the manager can approve/reject the claim
3. ADMIN can always approve/reject (override)

**Database Schema**:
```sql
ALTER TABLE employees
ADD COLUMN manager_user_id BIGINT NULL,
ADD CONSTRAINT fk_employees_manager FOREIGN KEY (manager_user_id) REFERENCES users(id);
```

**Example**:
```
Employee: John Doe (user_id=5)
Manager: Jane Smith (user_id=10)

✅ Jane Smith can approve John's claims
❌ Other managers cannot approve John's claims
✅ ADMIN can approve any claim
```

### 2. Incoming Invoices (Payable)

**Approver Assignment**:
- **With PO**: PO's assigned approver (stored in `po_headers.approver_user_id`)
- **Without PO**: MANAGER or FINANCE roles
- **Override**: ADMIN role

**Rules**:
1. If invoice is linked to a PO with an assigned approver:
   - Only that specific approver can approve/reject
   - ADMIN can override
2. If no PO or PO has no approver:
   - Any user with MANAGER or FINANCE role can approve/reject
   - ADMIN can approve/reject

**Database Schema**:
```sql
ALTER TABLE po_headers
ADD COLUMN approver_user_id BIGINT NULL,
ADD CONSTRAINT fk_po_headers_approver FOREIGN KEY (approver_user_id) REFERENCES users(id);

ALTER TABLE invoice_in
ADD COLUMN po_header_id BIGINT NULL,
ADD CONSTRAINT fk_invoice_in_po FOREIGN KEY (po_header_id) REFERENCES po_headers(id);
```

**Example**:
```
Scenario 1: Invoice with PO
PO: PO-2024-001
PO Approver: Finance Manager (user_id=15)

✅ Finance Manager (user_id=15) can approve
❌ Other FINANCE/MANAGER users cannot approve
✅ ADMIN can approve

Scenario 2: Invoice without PO
✅ Any MANAGER can approve
✅ Any FINANCE can approve
✅ ADMIN can approve
```

### 3. Outgoing Invoices (Receivable)

**Approver Assignment**:
- **Primary**: FINANCE role
- **Override**: ADMIN role

**Rules**:
1. Only users with FINANCE role can approve/reject
2. ADMIN can always approve/reject (override)

**Example**:
```
✅ FINANCE user can approve
❌ MANAGER cannot approve
✅ ADMIN can approve
```

## API Behavior

### Success Response

```http
POST /api/claims/1/approve
Authorization: Bearer <manager-token>

{
  "note": "Approved"
}
```

**Response**: `200 OK`
```json
{
  "id": 1,
  "status": "APPROVED",
  ...
}
```

### Authorization Failure Response

```http
POST /api/claims/1/approve
Authorization: Bearer <non-manager-token>

{
  "note": "Approved"
}
```

**Response**: `403 Forbidden`
```json
{
  "type": "https://api.docflow.com/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Only the claimant's manager (jane.smith@company.com) or ADMIN can approve this expense claim"
}
```

## Error Messages

### Expense Claim Errors

**No Manager Assigned**:
```json
{
  "type": "https://api.docflow.com/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Expense claim cannot be approved: claimant has no assigned manager"
}
```

**Wrong Manager**:
```json
{
  "type": "https://api.docflow.com/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Only the claimant's manager (jane.smith@company.com) or ADMIN can approve this expense claim"
}
```

### Invoice IN Errors

**PO Approver Required**:
```json
{
  "type": "https://api.docflow.com/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "This invoice is linked to PO PO-2024-001 which requires approval from finance.manager@company.com"
}
```

**Insufficient Role**:
```json
{
  "type": "https://api.docflow.com/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Only MANAGER, FINANCE, or ADMIN roles can approve incoming invoices"
}
```

### Invoice OUT Errors

**Insufficient Role**:
```json
{
  "type": "https://api.docflow.com/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Only FINANCE or ADMIN roles can approve outgoing invoices"
}
```

## Implementation

### ApprovalService

Centralized service for all approval authorization logic:

```java
@Service
public class ApprovalService {
    
    // Validate expense claim approver
    public void validateExpenseClaimApprover(ExpenseClaim claim, User approver) {
        // ADMIN can always approve
        if (hasRole(approver, RoleName.ADMIN)) {
            return;
        }
        
        // Check if approver is the claimant's manager
        Employee claimant = claim.getEmployee();
        if (claimant.getManager() == null) {
            throw new UnauthorizedActionException(
                "Expense claim cannot be approved: claimant has no assigned manager"
            );
        }
        
        if (!claimant.getManager().getId().equals(approver.getId())) {
            throw new UnauthorizedActionException(
                String.format("Only the claimant's manager (%s) or ADMIN can approve",
                    claimant.getManager().getEmail())
            );
        }
    }
    
    // Validate invoice IN approver
    public void validateInvoiceInApprover(InvoiceIn invoice, User approver) {
        // ADMIN can always approve
        if (hasRole(approver, RoleName.ADMIN)) {
            return;
        }
        
        // If PO exists with assigned approver, only that approver can approve
        if (invoice.getPurchaseOrder() != null && 
            invoice.getPurchaseOrder().getApprover() != null) {
            User poApprover = invoice.getPurchaseOrder().getApprover();
            if (!poApprover.getId().equals(approver.getId())) {
                throw new UnauthorizedActionException(
                    String.format("This invoice is linked to PO %s which requires approval from %s",
                        invoice.getPurchaseOrder().getPoNo(),
                        poApprover.getEmail())
                );
            }
            return;
        }
        
        // Otherwise, MANAGER or FINANCE can approve
        if (hasRole(approver, RoleName.MANAGER) || hasRole(approver, RoleName.FINANCE)) {
            return;
        }
        
        throw new UnauthorizedActionException(
            "Only MANAGER, FINANCE, or ADMIN roles can approve incoming invoices"
        );
    }
    
    // Validate invoice OUT approver
    public void validateInvoiceOutApprover(InvoiceOut invoice, User approver) {
        // ADMIN can always approve
        if (hasRole(approver, RoleName.ADMIN)) {
            return;
        }
        
        // FINANCE can approve
        if (hasRole(approver, RoleName.FINANCE)) {
            return;
        }
        
        throw new UnauthorizedActionException(
            "Only FINANCE or ADMIN roles can approve outgoing invoices"
        );
    }
}
```

### Service Integration

Services call ApprovalService before performing state transitions:

```java
@Transactional
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public ExpenseClaimResponse approveClaim(Long id, User currentUser, String note) {
    ExpenseClaim claim = expenseClaimRepository.findByIdWithDetails(id)
        .orElseThrow(() -> new ResourceNotFoundException("Expense claim", id));
    
    // Validate approver authorization
    approvalService.validateExpenseClaimApprover(claim, currentUser);
    
    // Transition PENDING -> APPROVED
    statusMachine.approve(claim, currentUser, note);
    claim = expenseClaimRepository.save(claim);
    
    return toResponseWithItems(claim);
}
```

## Database Schema Changes

### Migration V7: Add Approval Routing

```sql
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

-- Add PO reference to incoming invoices
ALTER TABLE invoice_in
ADD COLUMN po_header_id BIGINT NULL,
ADD CONSTRAINT fk_invoice_in_po FOREIGN KEY (po_header_id) REFERENCES po_headers(id);

CREATE INDEX idx_invoice_in_po ON invoice_in(po_header_id);
```

## Entity Updates

### Employee Entity

```java
@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_user_id")
    private User manager;  // NEW
}
```

### PurchaseOrderHeader Entity

```java
@Entity
@Table(name = "po_headers")
public class PurchaseOrderHeader {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "po_no", nullable = false, unique = true)
    private String poNo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_user_id")
    private User approver;  // NEW
}
```

### InvoiceIn Entity

```java
@Entity
@Table(name = "invoice_in")
public class InvoiceIn extends Document {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_header_id")
    private PurchaseOrderHeader purchaseOrder;  // NEW
}
```

## Usage Examples

### Example 1: Expense Claim with Manager Approval

```bash
# 1. Create employee with manager
# Employee: John Doe (user_id=5)
# Manager: Jane Smith (user_id=10)

# 2. John creates expense claim
curl -X POST http://localhost:8080/api/claims \
  -H "Authorization: Bearer $JOHN_TOKEN" \
  -d '{
    "claimDate": "2024-11-06",
    "currency": "USD",
    "items": [{"description": "Dinner", "date": "2024-11-05", "category": "Meals", "amount": 150}]
  }'

# 3. John submits claim
curl -X POST http://localhost:8080/api/claims/1/submit \
  -H "Authorization: Bearer $JOHN_TOKEN"

# 4. Jane (manager) approves - SUCCESS
curl -X POST http://localhost:8080/api/claims/1/approve \
  -H "Authorization: Bearer $JANE_TOKEN" \
  -d '{"note":"Approved"}'

# 5. Other manager tries to approve - FAILURE (403)
curl -X POST http://localhost:8080/api/claims/1/approve \
  -H "Authorization: Bearer $OTHER_MANAGER_TOKEN" \
  -d '{"note":"Approved"}'
# Response: 403 Forbidden - Only Jane can approve
```

### Example 2: Invoice with PO Routing

```bash
# 1. Create PO with assigned approver
# PO: PO-2024-001
# Approver: Finance Manager (user_id=15)

# 2. Create invoice linked to PO
curl -X POST http://localhost:8080/api/invoices/in \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "vendorId": 1,
    "poHeaderId": 1,
    "invoiceNo": "INV-001",
    ...
  }'

# 3. Finance Manager approves - SUCCESS
curl -X POST http://localhost:8080/api/invoices/in/1/approve \
  -H "Authorization: Bearer $FINANCE_MANAGER_TOKEN"

# 4. Other FINANCE user tries to approve - FAILURE (403)
curl -X POST http://localhost:8080/api/invoices/in/1/approve \
  -H "Authorization: Bearer $OTHER_FINANCE_TOKEN"
# Response: 403 Forbidden - Only PO approver can approve
```

### Example 3: Outgoing Invoice (FINANCE Only)

```bash
# 1. Create outgoing invoice
curl -X POST http://localhost:8080/api/invoices/out \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"clientId": 2, "invoiceNo": "OUT-001", ...}'

# 2. FINANCE approves - SUCCESS
curl -X POST http://localhost:8080/api/invoices/out/1/approve \
  -H "Authorization: Bearer $FINANCE_TOKEN"

# 3. MANAGER tries to approve - FAILURE (403)
curl -X POST http://localhost:8080/api/invoices/out/1/approve \
  -H "Authorization: Bearer $MANAGER_TOKEN"
# Response: 403 Forbidden - Only FINANCE can approve
```

## Authorization Matrix

| Document Type | Approver | Condition | ADMIN Override |
|---------------|----------|-----------|----------------|
| **Expense Claim** | Claimant's Manager | Manager assigned | ✅ Yes |
| **Invoice IN (with PO)** | PO Approver | PO has approver | ✅ Yes |
| **Invoice IN (no PO)** | MANAGER or FINANCE | No PO or no approver | ✅ Yes |
| **Invoice OUT** | FINANCE | Always | ✅ Yes |

## Benefits

1. **Accountability**: Clear approval responsibility
2. **Security**: Prevents unauthorized approvals
3. **Flexibility**: PO-based routing for complex workflows
4. **Hierarchy**: Manager-employee relationships enforced
5. **Override**: ADMIN can handle exceptions
6. **Audit**: All approvals logged with approver identity
7. **Clarity**: Clear error messages guide users

## Components Created

**Service** (1 file):
- `ApprovalService` - Centralized approval authorization

**Database Migration** (1 file):
- `V7__add_approval_routing.sql` - Schema changes

**Entity Updates** (3 files):
- `Employee` - Added manager field
- `PurchaseOrderHeader` - Added approver field
- `InvoiceIn` - Added PO reference

**Service Updates** (3 files):
- `ExpenseClaimService` - Manager validation
- `InvoiceInService` - PO routing validation
- `InvoiceOutService` - FINANCE-only validation

## Next Steps

1. **Seed Data**: Create manager-employee relationships
2. **PO Management**: Create PO CRUD endpoints
3. **Delegation**: Allow managers to delegate approval
4. **Notifications**: Email approvers when action needed
5. **Escalation**: Auto-escalate if not approved in X days
6. **Reports**: Approval metrics and pending approvals dashboard

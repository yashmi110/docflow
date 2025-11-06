# Document Status State Machine

## Overview

The `DocumentStatusMachine` service enforces legal status transitions for all documents in the system. It ensures data integrity through:

1. **Legal Transition Validation** - Only allowed state changes
2. **Idempotency** - Same status transitions are safe
3. **Optimistic Locking** - Prevents concurrent modifications via `@Version`
4. **Audit Logging** - Every transition is recorded

## State Diagram

```
┌─────────┐
│  DRAFT  │
└────┬────┘
     │
     ▼
┌─────────┐     ┌──────────┐
│ PENDING ├────►│ APPROVED │
└────┬────┘     └────┬─────┘
     │               │
     │               ▼
     │          ┌────────┐
     │          │  PAID  │ (Terminal)
     │          └────────┘
     │
     ▼
┌──────────┐
│ REJECTED │ (Terminal)
└──────────┘

CANCELLED (Can be reached from DRAFT or PENDING)
```

## Legal Transitions

| From Status | To Status(es) | Description |
|------------|---------------|-------------|
| **DRAFT** | PENDING | Submit for approval |
| **PENDING** | APPROVED, REJECTED | Approval decision |
| **APPROVED** | PAID | Mark as paid (invoices/reimbursements) |
| **REJECTED** | _(none)_ | Terminal state |
| **PAID** | _(none)_ | Terminal state |
| **CANCELLED** | _(none)_ | Terminal state |

### Special Rules

- **Cancellation**: Only allowed from DRAFT or PENDING status
- **Idempotency**: Transitioning to the same status is allowed (no-op)
- **Terminal States**: REJECTED, PAID, CANCELLED have no outgoing transitions

## Service Methods

### Core Transition Method

```java
Document transitionStatus(
    Document document, 
    DocumentStatus newStatus, 
    User user, 
    String action, 
    String note
)
```

**Features**:
- Validates transition legality
- Handles idempotency (logs but doesn't fail)
- Uses optimistic locking (`@Version`)
- Creates audit log entry
- Returns updated document

**Throws**:
- `InvalidStatusTransitionException` - Illegal transition
- `ObjectOptimisticLockingFailureException` - Concurrent modification

### Convenience Methods

```java
// DRAFT -> PENDING
Document submit(Document document, User user, String note)

// PENDING -> APPROVED
Document approve(Document document, User user, String note)

// PENDING -> REJECTED
Document reject(Document document, User user, String note)

// APPROVED -> PAID
Document markAsPaid(Document document, User user, String note)

// DRAFT/PENDING -> CANCELLED
Document cancel(Document document, User user, String note)
```

### Query Methods

```java
// Check if transition is allowed
boolean isTransitionAllowed(DocumentStatus from, DocumentStatus to)

// Validate and throw exception if not allowed
void validateTransition(DocumentStatus from, DocumentStatus to)

// Get all allowed transitions from a status
Set<DocumentStatus> getAllowedTransitions(DocumentStatus status)

// Check if status is terminal
boolean isTerminalStatus(DocumentStatus status)
```

## Audit Logging

Every status transition is automatically logged via `AuditLogService`.

### Audit Log Entry

```java
{
  "id": 1,
  "documentId": 123,
  "userId": 45,
  "userEmail": "manager@company.com",
  "userName": "John Manager",
  "action": "APPROVED",
  "fromStatus": "PENDING",
  "toStatus": "APPROVED",
  "note": "Budget approved by finance",
  "createdAt": "2024-11-06T18:30:00"
}
```

### Audit Log Features

- **REQUIRES_NEW Transaction**: Logs are saved even if parent transaction rolls back
- **Idempotency Tracking**: Even idempotent attempts are logged
- **Full History**: Complete audit trail for compliance

## API Endpoints

### Get Document Audit Logs

```http
GET /api/audit-logs/document/{documentId}
Authorization: Bearer <token>
```

**Response**:
```json
[
  {
    "id": 3,
    "documentId": 123,
    "userId": 45,
    "userEmail": "manager@company.com",
    "userName": "John Manager",
    "action": "APPROVED",
    "fromStatus": "PENDING",
    "toStatus": "APPROVED",
    "note": "Budget approved",
    "createdAt": "2024-11-06T18:30:00"
  },
  {
    "id": 2,
    "documentId": 123,
    "userId": 12,
    "userEmail": "employee@company.com",
    "userName": "Jane Employee",
    "action": "SUBMITTED",
    "fromStatus": "DRAFT",
    "toStatus": "PENDING",
    "note": "Ready for review",
    "createdAt": "2024-11-06T10:00:00"
  }
]
```

### Get Paginated Audit Logs

```http
GET /api/audit-logs/document/{documentId}/paginated?page=0&size=20
Authorization: Bearer <token>
```

### Get User Audit Logs

```http
GET /api/audit-logs/user/{userId}?page=0&size=20
Authorization: Bearer <token>
```

**Authorization**: ADMIN or the user themselves

### Get Audit Logs by Action

```http
GET /api/audit-logs/action/APPROVED
Authorization: Bearer <token>
```

**Authorization**: ADMIN only

### Get Audit Log Count

```http
GET /api/audit-logs/document/{documentId}/count
Authorization: Bearer <token>
```

## Usage Examples

### Example 1: Submit Document for Approval

```java
@Service
@RequiredArgsConstructor
public class InvoiceService {
    
    private final DocumentStatusMachine statusMachine;
    private final DocumentRepository documentRepository;
    
    @Transactional
    public InvoiceIn submitInvoice(Long invoiceId, User user) {
        InvoiceIn invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new NotFoundException("Invoice not found"));
        
        // Submit for approval (DRAFT -> PENDING)
        statusMachine.submit(invoice, user, "Submitted for approval");
        
        return documentRepository.save(invoice);
    }
}
```

### Example 2: Approve Document

```java
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
@Transactional
public InvoiceIn approveInvoice(Long invoiceId, User user, String note) {
    InvoiceIn invoice = invoiceRepository.findById(invoiceId)
        .orElseThrow(() -> new NotFoundException("Invoice not found"));
    
    // Approve (PENDING -> APPROVED)
    statusMachine.approve(invoice, user, note);
    
    return documentRepository.save(invoice);
}
```

### Example 3: Handle Idempotency

```java
// First call - transitions DRAFT -> PENDING
statusMachine.submit(document, user, "First submission");

// Second call - idempotent, logs but doesn't fail
statusMachine.submit(document, user, "Duplicate submission");
// Document remains in PENDING status
// Audit log records: "Idempotent transition attempt - already in target status"
```

### Example 4: Concurrent Modification Protection

```java
try {
    // User A and User B both load document version 1
    Document docA = documentRepository.findById(123L).get();
    Document docB = documentRepository.findById(123L).get();
    
    // User A approves first (version 1 -> 2)
    statusMachine.approve(docA, userA, "Approved by A");
    documentRepository.save(docA);
    
    // User B tries to approve (still has version 1)
    statusMachine.approve(docB, userB, "Approved by B");
    documentRepository.save(docB); // Throws ObjectOptimisticLockingFailureException
    
} catch (ObjectOptimisticLockingFailureException ex) {
    // Handle: Refresh document and retry
    return ProblemDetail with 409 CONFLICT
}
```

## Error Responses

### Invalid Transition (400)

```json
{
  "type": "https://api.docflow.com/errors/invalid-status-transition",
  "title": "Invalid Status Transition",
  "status": 400,
  "detail": "Invalid status transition from PAID to PENDING"
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

## Testing

### Unit Test Example

```java
@Test
void testLegalTransition() {
    // Arrange
    Document doc = Document.builder()
        .status(DocumentStatus.DRAFT)
        .version(0)
        .build();
    
    // Act
    Document result = statusMachine.submit(doc, user, "Test submission");
    
    // Assert
    assertEquals(DocumentStatus.PENDING, result.getStatus());
    verify(auditLogService).logTransition(any(), any(), eq("SUBMITTED"), 
        eq("DRAFT"), eq("PENDING"), anyString());
}

@Test
void testIllegalTransition() {
    // Arrange
    Document doc = Document.builder()
        .status(DocumentStatus.PAID)
        .build();
    
    // Act & Assert
    assertThrows(InvalidStatusTransitionException.class, 
        () -> statusMachine.submit(doc, user, "Invalid"));
}

@Test
void testIdempotency() {
    // Arrange
    Document doc = Document.builder()
        .status(DocumentStatus.PENDING)
        .build();
    
    // Act - transition to same status
    Document result = statusMachine.transitionStatus(
        doc, DocumentStatus.PENDING, user, "TEST", "Idempotent test"
    );
    
    // Assert - status unchanged, but audit log created
    assertEquals(DocumentStatus.PENDING, result.getStatus());
    verify(auditLogService).logTransition(any(), any(), eq("TEST"), 
        eq("PENDING"), eq("PENDING"), contains("Idempotent"));
}
```

## Best Practices

1. **Always use StatusMachine**: Never set document status directly
2. **Provide meaningful notes**: Help with audit trail and debugging
3. **Handle OptimisticLockingFailure**: Refresh and retry on conflict
4. **Check terminal states**: Use `isTerminalStatus()` before attempting transitions
5. **Use convenience methods**: `submit()`, `approve()`, etc. are clearer than generic `transitionStatus()`
6. **RBAC on transitions**: Use `@PreAuthorize` to control who can approve/reject
7. **Audit log queries**: Use for compliance reports and debugging

## Compliance & Audit

The state machine and audit logging system provides:

- **Complete audit trail** for regulatory compliance
- **Non-repudiation** - All actions are logged with user and timestamp
- **Idempotency tracking** - Even duplicate attempts are recorded
- **Concurrent modification detection** - Prevents lost updates
- **Immutable logs** - Audit logs use REQUIRES_NEW transaction propagation

## Future Enhancements

- [ ] Add workflow engine for multi-step approvals
- [ ] Implement delegation (approve on behalf of)
- [ ] Add notification system for status changes
- [ ] Support custom transitions per document type
- [ ] Add bulk status transitions
- [ ] Implement scheduled status changes
- [ ] Add rollback capability for certain transitions

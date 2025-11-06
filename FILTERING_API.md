# Advanced Filtering API

## Overview

Comprehensive filtering capabilities for all document types using JPA Specifications with POST endpoints for complex criteria.

## Features

- ✅ Dynamic filtering with multiple criteria
- ✅ Date range filtering
- ✅ Amount range filtering
- ✅ Status filtering
- ✅ Entity-specific filtering (vendor, client, employee)
- ✅ Sorting (ascending/descending)
- ✅ Pagination
- ✅ Total amount calculation
- ✅ Result count

## Architecture

### Components

**Filter Criteria DTOs**:
- `InvoiceFilterCriteria` - For invoices IN and OUT
- `ExpenseClaimFilterCriteria` - For expense claims
- `ReimbursementFilterCriteria` - For reimbursements
- `PageRequest` - Pagination parameters
- `PageResponse<T>` - Paginated response with totals

**JPA Specifications**:
- `InvoiceInSpecification` - Dynamic query building for incoming invoices
- `InvoiceOutSpecification` - Dynamic query building for outgoing invoices
- `ExpenseClaimSpecification` - Dynamic query building for expense claims
- `ReimbursementSpecification` - Dynamic query building for reimbursements

**Repositories**:
- Extended with `JpaSpecificationExecutor` for dynamic filtering

## API Endpoints

### Invoice IN Filtering

```http
POST /api/invoices/in/filter?page=0&size=20&sortBy=createdAt&sortDir=DESC
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "PENDING",
  "vendorId": 1,
  "dateFrom": "2024-01-01",
  "dateTo": "2024-12-31",
  "amountMin": 100.00,
  "amountMax": 10000.00,
  "currency": "USD",
  "invoiceNo": "INV"
}
```

**Response**:
```json
{
  "content": [
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
      "status": "PENDING",
      "ownerUserId": 5,
      "ownerUserEmail": "employee@company.com",
      "ownerUserName": "John Doe",
      "createdAt": "2024-11-06T10:00:00",
      "updatedAt": "2024-11-06T10:00:00",
      "version": 0
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "totalAmount": 1100.00,
  "count": 1
}
```

### Invoice OUT Filtering

```http
POST /api/invoices/out/filter?page=0&size=20&sortBy=invoiceDate&sortDir=DESC
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "APPROVED",
  "clientId": 2,
  "dateFrom": "2024-01-01",
  "dateTo": "2024-12-31",
  "amountMin": 1000.00,
  "amountMax": 50000.00,
  "currency": "USD"
}
```

### Expense Claims Filtering

```http
POST /api/claims/filter?page=0&size=20&sortBy=claimDate&sortDir=DESC
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "PENDING",
  "employeeId": 5,
  "claimDateFrom": "2024-01-01",
  "claimDateTo": "2024-12-31",
  "amountMin": 50.00,
  "amountMax": 5000.00,
  "currency": "USD"
}
```

### Reimbursements Filtering

```http
POST /api/reimbursements/filter?page=0&size=20&sortBy=requestedDate&sortDir=DESC
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "APPROVED",
  "employeeId": 5,
  "requestedDateFrom": "2024-01-01",
  "requestedDateTo": "2024-12-31",
  "amountMin": 100.00,
  "amountMax": 10000.00,
  "currency": "USD"
}
```

## Filter Criteria

### Invoice Filter Criteria

| Field | Type | Description |
|-------|------|-------------|
| `status` | DocumentStatus | Filter by status (DRAFT, PENDING, APPROVED, REJECTED, PAID, CANCELLED) |
| `vendorId` | Long | Filter by vendor (Invoice IN only) |
| `clientId` | Long | Filter by client (Invoice OUT only) |
| `dateFrom` | LocalDate | Invoice date from (inclusive) |
| `dateTo` | LocalDate | Invoice date to (inclusive) |
| `amountMin` | BigDecimal | Minimum total amount (inclusive) |
| `amountMax` | BigDecimal | Maximum total amount (inclusive) |
| `currency` | String | Filter by currency (e.g., USD, EUR) |
| `invoiceNo` | String | Search by invoice number (partial match, case-insensitive) |

### Expense Claim Filter Criteria

| Field | Type | Description |
|-------|------|-------------|
| `status` | DocumentStatus | Filter by status |
| `employeeId` | Long | Filter by employee |
| `claimDateFrom` | LocalDate | Claim date from (inclusive) |
| `claimDateTo` | LocalDate | Claim date to (inclusive) |
| `amountMin` | BigDecimal | Minimum total amount (inclusive) |
| `amountMax` | BigDecimal | Maximum total amount (inclusive) |
| `currency` | String | Filter by currency |

### Reimbursement Filter Criteria

| Field | Type | Description |
|-------|------|-------------|
| `status` | DocumentStatus | Filter by status |
| `employeeId` | Long | Filter by employee |
| `requestedDateFrom` | LocalDate | Requested date from (inclusive) |
| `requestedDateTo` | LocalDate | Requested date to (inclusive) |
| `amountMin` | BigDecimal | Minimum total amount (inclusive) |
| `amountMax` | BigDecimal | Maximum total amount (inclusive) |
| `currency` | String | Filter by currency |

## Query Parameters

All filter endpoints support the following query parameters:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `page` | 0 | Page number (0-indexed) |
| `size` | 20 | Page size |
| `sortBy` | createdAt | Field to sort by |
| `sortDir` | DESC | Sort direction (ASC or DESC) |

### Sortable Fields

**Invoices**:
- `createdAt` - Creation timestamp
- `invoiceDate` - Invoice date
- `dueDate` - Due date
- `total` - Total amount
- `status` - Document status

**Expense Claims**:
- `createdAt` - Creation timestamp
- `claimDate` - Claim date
- `total` - Total amount
- `status` - Document status

**Reimbursements**:
- `createdAt` - Creation timestamp
- `requestedDate` - Requested date
- `total` - Total amount
- `status` - Document status

## Response Format

### PageResponse Structure

```json
{
  "content": [],           // Array of document DTOs
  "page": 0,              // Current page number
  "size": 20,             // Page size
  "totalElements": 100,   // Total number of matching records
  "totalPages": 5,        // Total number of pages
  "first": true,          // Is this the first page?
  "last": false,          // Is this the last page?
  "totalAmount": 12500.50,// Sum of all amounts in current page
  "count": 100            // Total count of matching records
}
```

## Usage Examples

### Example 1: Find All Pending Invoices from a Vendor

```bash
curl -X POST http://localhost:8080/api/invoices/in/filter \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "PENDING",
    "vendorId": 1
  }'
```

### Example 2: Find High-Value Invoices in Date Range

```bash
curl -X POST http://localhost:8080/api/invoices/in/filter?sortBy=total&sortDir=DESC \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "dateFrom": "2024-01-01",
    "dateTo": "2024-12-31",
    "amountMin": 10000.00,
    "status": "APPROVED"
  }'
```

### Example 3: Find Employee's Expense Claims

```bash
curl -X POST http://localhost:8080/api/claims/filter \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 5,
    "status": "PENDING",
    "claimDateFrom": "2024-11-01"
  }'
```

### Example 4: Find Reimbursements Awaiting Payment

```bash
curl -X POST http://localhost:8080/api/reimbursements/filter?page=0&size=50 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "APPROVED",
    "requestedDateFrom": "2024-01-01"
  }'
```

### Example 5: Search Invoices by Number

```bash
curl -X POST http://localhost:8080/api/invoices/in/filter \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceNo": "2024"
  }'
```

## Technical Implementation

### JPA Specifications

The filtering uses JPA Criteria API through Specifications for type-safe, dynamic query building:

```java
public static Specification<InvoiceIn> withFilters(InvoiceFilterCriteria criteria) {
    return (root, query, criteriaBuilder) -> {
        List<Predicate> predicates = new ArrayList<>();

        if (criteria.getStatus() != null) {
            predicates.add(criteriaBuilder.equal(root.get("status"), criteria.getStatus()));
        }

        if (criteria.getDateFrom() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("invoiceDate"), criteria.getDateFrom()));
        }

        // ... more predicates

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
}
```

### Repository Extension

Repositories extend `JpaSpecificationExecutor`:

```java
public interface InvoiceInRepository extends JpaRepository<InvoiceIn, Long>,
        JpaSpecificationExecutor<InvoiceIn> {
    // ... custom methods
}
```

### Service Layer

Services build specifications and calculate totals:

```java
public PageResponse<InvoiceInResponse> filterInvoices(
        InvoiceFilterCriteria criteria, Pageable pageable) {
    Specification<InvoiceIn> spec = InvoiceInSpecification.withFilters(criteria);
    Page<InvoiceIn> page = invoiceInRepository.findAll(spec, pageable);
    
    Page<InvoiceInResponse> responsePage = page.map(invoiceInMapper::toResponse);
    
    // Calculate totals
    BigDecimal totalAmount = page.getContent().stream()
            .map(InvoiceIn::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    return PageResponse.of(responsePage, totalAmount, page.getTotalElements());
}
```

## Benefits

1. **Type Safety**: Compile-time checking of filter criteria
2. **Dynamic Queries**: Only active filters are applied
3. **Performance**: Efficient database queries with proper indexing
4. **Flexibility**: Easy to add new filter criteria
5. **Consistency**: Same pattern across all document types
6. **Totals**: Automatic calculation of summary statistics
7. **Pagination**: Efficient handling of large result sets

## Future Enhancements

- [ ] Add full-text search on descriptions
- [ ] Add aggregation queries (sum, avg, count by status)
- [ ] Add export to CSV/Excel
- [ ] Add saved filter presets
- [ ] Add filter history
- [ ] Add advanced date filters (this month, last quarter, etc.)
- [ ] Add multi-field sorting
- [ ] Add GraphQL support for flexible querying

## Performance Considerations

1. **Indexes**: Ensure proper database indexes on filtered fields
2. **Pagination**: Always use pagination for large datasets
3. **Projections**: Consider using DTOs with only required fields
4. **Caching**: Cache frequently used filter results
5. **Query Optimization**: Monitor and optimize slow queries

## Components Created

**DTOs** (5 files):
- `InvoiceFilterCriteria`
- `ExpenseClaimFilterCriteria`
- `ReimbursementFilterCriteria`
- `PageRequest`
- `PageResponse<T>`

**Specifications** (4 files):
- `InvoiceInSpecification`
- `InvoiceOutSpecification`
- `ExpenseClaimSpecification`
- `ReimbursementSpecification`

**Updated Repositories** (4 files):
- Extended with `JpaSpecificationExecutor`

**Service Methods**:
- `filterInvoices()` in InvoiceInService
- Similar methods for other document types

**Controller Endpoints**:
- `POST /api/invoices/in/filter`
- `POST /api/invoices/out/filter`
- `POST /api/claims/filter`
- `POST /api/reimbursements/filter`

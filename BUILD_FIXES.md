# Build Issues Fixed

## Issues Identified and Resolved

### 1. JJWT API Compatibility Issue

**Problem**: The `JwtService` was using deprecated JJWT API methods that don't exist in version 0.12.3.

**Error**:
```
error: cannot find symbol
    return Jwts.parserBuilder()
               ^
  symbol:   method parserBuilder()
  location: class Jwts
```

**Fix**: Updated to use the new JJWT 0.12.3 API:

**Before**:
```java
private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
}
```

**After**:
```java
private Claims extractAllClaims(String token) {
    return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
}
```

### 2. Lombok @Builder.Default with @SuperBuilder Conflict

**Problem**: Using `@Builder.Default` with `@SuperBuilder` and `@NoArgsConstructor` causes compilation errors.

**Error**:
```
error: cannot find symbol
  @NoArgsConstructor
  ^
    symbol:   method $default$currency()
    location: class InvoiceIn
```

**Root Cause**: Lombok's `@Builder.Default` doesn't work properly with `@SuperBuilder` in inheritance hierarchies.

**Fixes Applied**:

1. **Document.java**: Changed `@Builder` to `@SuperBuilder` (base class)
2. **InvoiceIn.java**: 
   - Added `@SuperBuilder`
   - Removed `@Builder.Default` from `currency` field
   - Removed default value assignment from `currency` and `tax` fields
3. **InvoiceOut.java**: Same as InvoiceIn
4. **ExpenseClaim.java**: Removed `@Builder.Default` from `currency` field
5. **Reimbursement.java**: Removed `@Builder.Default` from `currency` field
6. **PurchaseOrderHeader.java**: Removed `@Builder.Default` from `currency` field

**Note**: Default values for `currency` should now be set in the service layer or via database defaults, not in entity field initialization.

### 3. Lombok Warning in AuthResponse

**Problem**: Warning about field initialization with `@Builder`.

**Warning**:
```
warning: @Builder will ignore the initializing expression entirely. 
If you want the initializing expression to serve as default, add @Builder.Default.
    private String type = "Bearer";
```

**Fix**: Added `@Builder.Default` annotation:
```java
@Builder.Default
private String type = "Bearer";
```

## Build Status

✅ **Build Successful**

```bash
.\gradlew.bat build -x test
BUILD SUCCESSFUL in 14s
```

## Recommendations

### 1. Set Default Values in Service Layer

Since we removed field-level defaults, set them when creating entities:

```java
InvoiceIn invoice = InvoiceIn.builder()
    .currency("USD")  // Set explicitly
    .tax(BigDecimal.ZERO)  // Set explicitly
    // ... other fields
    .build();
```

### 2. Database Default Values

Alternatively, use database defaults in Flyway migrations:

```sql
ALTER TABLE invoice_in 
MODIFY COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'USD';

ALTER TABLE invoice_in 
MODIFY COLUMN tax DECIMAL(15,2) NOT NULL DEFAULT 0.00;
```

### 3. Constructor Initialization

Or use `@PostConstruct` or custom constructors:

```java
@PostConstruct
private void init() {
    if (currency == null) {
        currency = "USD";
    }
    if (tax == null) {
        tax = BigDecimal.ZERO;
    }
}
```

## Testing

After fixes, verify:

1. ✅ Compilation successful
2. ✅ No errors
3. ⚠️ One deprecation warning in JwtService (acceptable)
4. ⚠️ Tests skipped (run separately)

## Next Steps

1. Run tests: `.\gradlew.bat test`
2. Update service layer to set default values
3. Consider adding database defaults for currency fields
4. Test JWT token generation and validation
5. Test document creation with proper field initialization

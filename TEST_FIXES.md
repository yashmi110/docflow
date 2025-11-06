# Test Fixes Summary

## Issues Fixed

### 1. DocumentStatusMachine Cancellation Tests Failing

**Problem**: Tests for cancellation from DRAFT and PENDING states were failing because CANCELLED was not included in the allowed transitions.

**Error**:
```
InvalidStatusTransitionException: Invalid status transition from DRAFT to CANCELLED
InvalidStatusTransitionException: Invalid status transition from PENDING to CANCELLED
```

**Fix**: Updated the state machine to allow CANCELLED transitions from DRAFT and PENDING states.

**File**: `DocumentStatusMachine.java`

**Changes**:
```java
// Before
LEGAL_TRANSITIONS.put(DocumentStatus.DRAFT, EnumSet.of(DocumentStatus.PENDING));
LEGAL_TRANSITIONS.put(DocumentStatus.PENDING, EnumSet.of(DocumentStatus.APPROVED, DocumentStatus.REJECTED));

// After
LEGAL_TRANSITIONS.put(DocumentStatus.DRAFT, EnumSet.of(DocumentStatus.PENDING, DocumentStatus.CANCELLED));
LEGAL_TRANSITIONS.put(DocumentStatus.PENDING, EnumSet.of(DocumentStatus.APPROVED, DocumentStatus.REJECTED, DocumentStatus.CANCELLED));
```

### 2. Test Expectations Updated

**Problem**: Test `testGetAllowedTransitions()` expected 1 transition from DRAFT and 2 from PENDING, but after adding CANCELLED support, these numbers changed.

**Fix**: Updated test expectations.

**File**: `DocumentStatusMachineTest.java`

**Changes**:
```java
// DRAFT transitions: 1 -> 2 (PENDING, CANCELLED)
assertEquals(2, draftTransitions.size());
assertTrue(draftTransitions.contains(DocumentStatus.CANCELLED));

// PENDING transitions: 2 -> 3 (APPROVED, REJECTED, CANCELLED)
assertEquals(3, pendingTransitions.size());
assertTrue(pendingTransitions.contains(DocumentStatus.CANCELLED));
```

### 3. Circular Dependency in Spring Context

**Problem**: Circular dependency between `SecurityConfig`, `OAuth2AuthenticationSuccessHandler`, `AuthService`, and `AuthenticationManager`.

**Error**:
```
BeanCurrentlyInCreationException: Error creating bean with name 'securityConfig': 
Requested bean is currently in creation: Is there an unresolvable circular reference?
```

**Dependency Chain**:
```
SecurityConfig 
  → OAuth2AuthenticationSuccessHandler 
    → AuthService 
      → AuthenticationManager 
        → SecurityConfig (circular!)
```

**Fix**: Used `@Lazy` annotation on `AuthenticationManager` in `AuthService` and `AuthService` in `OAuth2AuthenticationSuccessHandler` to break the circular dependency.

**Note**: `@Lazy` doesn't work with `@RequiredArgsConstructor`, so manual constructors were required.

**Files Modified**:

1. **AuthService.java**:
```java
// Before
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    // ...
}

// After
@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    
    public AuthService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        @Lazy AuthenticationManager authenticationManager  // Lazy injection
    ) {
        // Manual field assignment
    }
}
```

2. **OAuth2AuthenticationSuccessHandler.java**:
```java
// Before
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final AuthService authService;
    // ...
}

// After
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final AuthService authService;
    
    public OAuth2AuthenticationSuccessHandler(
        @Lazy AuthService authService,  // Lazy injection
        JwtService jwtService,
        ObjectMapper objectMapper
    ) {
        // Manual field assignment
    }
}
```

### 4. Missing JWT Properties in Test Configuration

**Problem**: `JwtService` bean creation failed in tests because JWT configuration properties were missing.

**Error**:
```
Error creating bean with name 'jwtService': Injection of autowired dependencies failed
```

**Fix**: Added JWT and OAuth2 configuration properties to test profile.

**File**: `src/test/resources/application-test.properties`

**Added**:
```properties
# JWT Configuration for tests
jwt.secret=test-secret-key-for-testing-purposes-only-minimum-256-bits
jwt.expiration=86400000

# OAuth2 (dummy values for tests)
spring.security.oauth2.client.registration.google.client-id=test-client-id
spring.security.oauth2.client.registration.google.client-secret=test-client-secret
```

## Updated State Machine Diagram

```
DRAFT ──────→ PENDING ──────→ APPROVED ──────→ PAID (Terminal)
  │              │                │
  │              ↓                │
  │          REJECTED (Terminal)  │
  │                               │
  └──────→ CANCELLED (Terminal) ←─┘
```

**Allowed Transitions**:
- DRAFT → PENDING, CANCELLED
- PENDING → APPROVED, REJECTED, CANCELLED
- APPROVED → PAID
- REJECTED → (none - terminal)
- PAID → (none - terminal)
- CANCELLED → (none - terminal)

## Test Results

✅ **All Tests Passing**

```
DocflowApplicationTests > contextLoads() PASSED
DocumentStatusMachineTest > testIdempotency_SameStatusTransition() PASSED
DocumentStatusMachineTest > testIllegalTransition_DraftToApproved() PASSED
DocumentStatusMachineTest > testGetAllowedTransitions() PASSED
DocumentStatusMachineTest > testLegalTransition_PendingToRejected() PASSED
DocumentStatusMachineTest > testIsTerminalStatus() PASSED
DocumentStatusMachineTest > testIllegalTransition_PaidToPending() PASSED
DocumentStatusMachineTest > testLegalTransition_ApprovedToPaid() PASSED
DocumentStatusMachineTest > testCancellation_FromApproved_ShouldFail() PASSED
DocumentStatusMachineTest > testLegalTransition_DraftToPending() PASSED
DocumentStatusMachineTest > testLegalTransition_PendingToApproved() PASSED
DocumentStatusMachineTest > testCancellation_FromDraft() PASSED
DocumentStatusMachineTest > testIsTransitionAllowed() PASSED
DocumentStatusMachineTest > testCancellation_FromPending() PASSED

BUILD SUCCESSFUL
```

## Files Modified

1. ✅ `DocumentStatusMachine.java` - Added CANCELLED to allowed transitions
2. ✅ `DocumentStatusMachineTest.java` - Updated test expectations
3. ✅ `AuthService.java` - Manual constructor with @Lazy AuthenticationManager
4. ✅ `OAuth2AuthenticationSuccessHandler.java` - Manual constructor with @Lazy AuthService
5. ✅ `application-test.properties` - Added JWT and OAuth2 test configuration

## Lessons Learned

1. **@Lazy with @RequiredArgsConstructor**: Lombok's `@RequiredArgsConstructor` doesn't support `@Lazy` on individual fields. Use manual constructors when lazy injection is needed.

2. **Circular Dependencies**: Spring Boot's circular dependency detection is strict. Use `@Lazy` strategically to break cycles, typically on the less critical dependency.

3. **Test Configuration**: Always ensure test profiles have all required configuration properties, even if they're dummy values.

4. **State Machine Design**: When adding new terminal states or transitions, remember to update both the state machine logic AND the tests.

## Build Command

```bash
.\gradlew.bat test
BUILD SUCCESSFUL in 1m 43s
```

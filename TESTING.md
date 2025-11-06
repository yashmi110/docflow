# Docflow Testing Guide

## Overview

This document describes the integration test suite for the Docflow application. All tests use **Testcontainers** with MySQL to ensure tests run against a real database in an isolated environment.

## Test Categories

### 1. Authentication Tests (`AuthIntegrationTest`)

Tests user signup, login, and JWT token generation.

**Coverage**:
- ✅ Successful user signup
- ✅ Duplicate email rejection
- ✅ Invalid email validation
- ✅ Weak password rejection
- ✅ Successful login with valid credentials
- ✅ Wrong password rejection
- ✅ Non-existent user rejection
- ✅ Login with seed data users

**Key Test**:
```java
@Test
void testLoginSeedDataUser() {
    // Tests login with admin@docflow.com (from V8 migration)
    // Verifies JWT token generation and user roles
}
```

### 2. Status Transition Tests (`StatusTransitionIntegrationTest`)

Tests document state machine and optimistic locking.

**Coverage**:
- ✅ Invoice transitions: DRAFT → PENDING → APPROVED → PAID
- ✅ Invalid transition rejection
- ✅ Optimistic locking conflict detection
- ✅ Expense claim workflow
- ✅ Rejection workflow

**Key Test**:
```java
@Test
void testInvoiceStatusTransitions() {
    // Creates invoice in DRAFT
    // Submits to PENDING
    // Approves to APPROVED
    // Pays to PAID
    // Verifies version increments at each step
}
```

### 3. RBAC Tests (`RBACIntegrationTest`)

Tests role-based access control and authorization rules.

**Coverage**:
- ✅ FINANCE can create invoices
- ✅ EMPLOYEE cannot create invoices (403)
- ✅ MANAGER cannot create invoices (403)
- ✅ ADMIN can override permissions
- ✅ MANAGER cannot approve outgoing invoices (403)
- ✅ FINANCE can approve outgoing invoices
- ✅ Wrong manager cannot approve claims (403)
- ✅ Correct manager can approve claims
- ✅ ADMIN can override manager hierarchy
- ✅ Unauthenticated requests rejected (401)
- ✅ Invalid token rejected (401)
- ✅ EMPLOYEE cannot view all invoices (403)
- ✅ FINANCE can view all invoices

**Key Test**:
```java
@Test
void testWrongManagerCannotApproveClaim() {
    // employee1 reports to manager1
    // Finance user tries to approve (not the manager)
    // Expects 403 Forbidden
}
```

### 4. Audit Log Tests (`AuditLogIntegrationTest`)

Tests audit trail creation for all state transitions.

**Coverage**:
- ✅ Audit log created on document creation
- ✅ Audit log for each status transition
- ✅ User recorded for each action
- ✅ Rejection reason captured
- ✅ Payment details captured
- ✅ Chronological order maintained
- ✅ All required fields present

**Key Test**:
```java
@Test
void testAuditLogForStatusTransitions() {
    // Creates invoice, submits, approves
    // Verifies 3 audit log entries
    // Checks fromStatus and toStatus for each transition
}
```

### 5. File Upload Tests (`FileUploadIntegrationTest`)

Tests file attachment functionality.

**Coverage**:
- ✅ Upload PDF file
- ✅ Upload image file (JPEG, PNG)
- ✅ Reject disallowed file types
- ✅ Reject oversized files (>10MB)
- ✅ List all files for document
- ✅ Download file
- ✅ Delete file
- ✅ Upload multiple files
- ✅ Upload to non-existent document (404)
- ✅ Upload without authentication (401)
- ✅ Upload Excel files
- ✅ Upload Word documents
- ✅ Preserve original filename

**Key Test**:
```java
@Test
void testUploadPdfFile() {
    // Uploads PDF to invoice
    // Verifies file metadata (name, size, type)
    // Verifies uploader recorded
}
```

## Running Tests

### Prerequisites

- **Java 17**
- **Docker** (for Testcontainers)
- **Gradle**

### Run All Tests

```bash
# Windows
.\gradlew.bat test

# Linux/Mac
./gradlew test
```

### Run Specific Test Class

```bash
.\gradlew.bat test --tests "com.docflow.integration.AuthIntegrationTest"
```

### Run Specific Test Method

```bash
.\gradlew.bat test --tests "com.docflow.integration.AuthIntegrationTest.testLoginSuccess"
```

### Run with Coverage Report

```bash
.\gradlew.bat test jacocoTestReport
```

Coverage report will be generated at:
```
build/reports/jacoco/test/html/index.html
```

### Run Tests in Debug Mode

```bash
.\gradlew.bat test --debug
```

## Test Configuration

### Testcontainers Setup

All integration tests extend `BaseIntegrationTest` which configures:

```java
@Container
protected static final MySQLContainer<?> mysqlContainer = 
    new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("docflow_test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);
```

**Benefits**:
- ✅ Real MySQL database (not H2)
- ✅ Isolated test environment
- ✅ Automatic cleanup
- ✅ Container reuse for faster tests

### Test Properties

Located at `src/test/resources/application-test.properties`:

```properties
spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver
spring.datasource.url=jdbc:tc:mysql:8.0:///docflow_test?TC_DAEMON=true
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

### Seed Data

Tests use seed data from Flyway migration `V8__dev_seed_data.sql`:

| Email | Password | Role |
|-------|----------|------|
| admin@docflow.com | Password@123 | ADMIN |
| finance1@docflow.com | Password@123 | FINANCE |
| manager1@docflow.com | Password@123 | MANAGER |
| employee1@docflow.com | Password@123 | EMPLOYEE |

**Manager Hierarchy**:
```
Finance Manager (finance1)
  └── Department Manager (manager1)
        └── John Employee (employee1)
```

## Test Patterns

### 1. Authentication Helper

```java
private String loginAs(String email, String password) throws Exception {
    String loginRequest = String.format("""
            {
                "email": "%s",
                "password": "%s"
            }
            """, email, password);

    MvcResult result = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
    return response.get("token").asText();
}
```

### 2. Document Creation

```java
String createRequest = """
        {
            "vendorId": 1,
            "invoiceNo": "INV-TEST-001",
            "invoiceDate": "2024-11-15",
            "dueDate": "2024-12-15",
            "currency": "USD",
            "subtotal": 1000.00,
            "tax": 100.00,
            "total": 1100.00
        }
        """;

MvcResult createResult = mockMvc.perform(post("/api/invoices/in")
                .header("Authorization", "Bearer " + financeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
        .andExpect(status().isOk())
        .andReturn();
```

### 3. File Upload

```java
MockMultipartFile file = new MockMultipartFile(
        "file",
        "invoice.pdf",
        "application/pdf",
        "Test PDF content".getBytes()
);

mockMvc.perform(multipart("/api/docs/" + invoiceId + "/files")
                .file(file)
                .header("Authorization", "Bearer " + financeToken))
        .andExpect(status().isOk());
```

## Troubleshooting

### Docker Not Running

**Error**:
```
Could not start container
```

**Solution**:
```bash
# Start Docker Desktop
# Or on Linux:
sudo systemctl start docker
```

### Port Conflicts

**Error**:
```
Port 3306 already in use
```

**Solution**:
Testcontainers uses random ports, but if you have MySQL running locally, it shouldn't conflict. If issues persist, stop local MySQL:

```bash
# Windows
Stop-Service MySQL80

# Linux
sudo systemctl stop mysql
```

### Out of Memory

**Error**:
```
java.lang.OutOfMemoryError
```

**Solution**:
Increase heap size in `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
```

### Slow Tests

**Cause**: Container startup time

**Solution**:
- Use container reuse: `.withReuse(true)` (already configured)
- Run tests in parallel (Gradle 7+):

```bash
.\gradlew.bat test --parallel --max-workers=4
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'corretto'
      - name: Run tests
        run: ./gradlew test
      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./build/reports/jacoco/test/jacocoTestReport.xml
```

### GitLab CI

```yaml
test:
  image: gradle:7.6-jdk17
  services:
    - docker:dind
  script:
    - gradle test
  artifacts:
    reports:
      junit: build/test-results/test/TEST-*.xml
```

## Best Practices

1. **Use Testcontainers**: Real database, not H2
2. **Clean State**: Each test is independent
3. **Descriptive Names**: `testEmployeeCannotCreateInvoiceIn`
4. **Arrange-Act-Assert**: Clear test structure
5. **Test Edge Cases**: Invalid inputs, boundary conditions
6. **Verify Responses**: Check status codes and JSON structure
7. **Use Seed Data**: Leverage V8 migration data
8. **Test Authorization**: Verify 403 responses
9. **Test Audit Logs**: Ensure all actions are logged

## Coverage Goals

- **Line Coverage**: > 80%
- **Branch Coverage**: > 75%
- **Integration Tests**: All critical paths
- **Unit Tests**: Business logic and utilities

## Next Steps

1. Add performance tests
2. Add load tests with JMeter
3. Add contract tests with Pact
4. Add E2E tests with Selenium
5. Add mutation testing with PIT

## Resources

- [Testcontainers Documentation](https://www.testcontainers.org/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [MockMvc Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)

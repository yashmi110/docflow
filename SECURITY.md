# Docflow Security Implementation

## Overview

Complete security implementation with JWT authentication, Google OAuth2, and role-based access control (RBAC).

## Authentication Methods

### 1. Email/Password Authentication

**Signup Endpoint**: `POST /api/auth/signup`

Request:
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass@123"
}
```

Password Requirements:
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character (@$!%*?&)

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "email": "john@example.com",
  "name": "John Doe",
  "roles": ["EMPLOYEE"]
}
```

**Login Endpoint**: `POST /api/auth/login`

Request:
```json
{
  "email": "john@example.com",
  "password": "SecurePass@123"
}
```

Response: Same as signup

### 2. Google OAuth2 Authentication

**Initiate OAuth Flow**: `GET /oauth2/authorization/google`

This redirects to Google's OAuth consent screen.

**Callback**: `GET /login/oauth2/code/google`

After successful authentication, returns JSON response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "email": "user@gmail.com",
  "name": "User Name",
  "roles": ["EMPLOYEE"]
}
```

**User Linking**:
- If `google_sub` exists → Login existing user
- If email exists → Link Google account to existing user
- Otherwise → Create new user with EMPLOYEE role

## JWT Configuration

**Algorithm**: HS256 (HMAC with SHA-256)

**Token Structure**:
```json
{
  "sub": "user@example.com",
  "roles": ["ROLE_ADMIN", "ROLE_FINANCE"],
  "iat": 1699000000,
  "exp": 1699086400
}
```

**Expiration**: 24 hours (86400000 ms)

**Usage**: Include in Authorization header:
```
Authorization: Bearer <token>
```

## Roles & Permissions

### Available Roles

1. **ADMIN** - Full system access
2. **FINANCE** - Financial operations
3. **MANAGER** - Approval workflows
4. **EMPLOYEE** - Basic document operations
5. **VENDOR** - Vendor-specific access
6. **CLIENT** - Client-specific access

### Default Role Assignment

- **Signup**: EMPLOYEE role
- **OAuth2**: EMPLOYEE role
- **Seeded Admin**: ADMIN role

### RBAC with @PreAuthorize

Use on service methods:

```java
@PreAuthorize("hasRole('ADMIN')")
public void adminOnlyMethod() { }

@PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
public void financeMethod() { }

@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
public void approvalMethod() { }
```

## Public Endpoints

No authentication required:
- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /oauth2/authorization/google`
- `GET /login/oauth2/code/google`
- `GET /actuator/health`
- `GET /actuator/info`

## Protected Endpoints

All other endpoints require valid JWT token.

## Security Components

### JwtService
- Generates JWT tokens
- Validates tokens
- Extracts claims (username, roles, expiration)

### JwtAuthenticationFilter
- Intercepts requests
- Extracts and validates JWT from Authorization header
- Sets Spring Security context

### CustomUserDetailsService
- Loads user by email
- Converts User entity to Spring Security UserDetails
- Includes roles with "ROLE_" prefix

### OAuth2AuthenticationSuccessHandler
- Handles successful OAuth2 authentication
- Creates or links user account
- Generates JWT token
- Returns JSON response (no UI redirect)

### AuthenticationEntryPointImpl
- Handles unauthorized access (401)
- Returns ProblemDetail JSON response

### GlobalExceptionHandler
- Handles validation errors
- Handles authentication failures
- Returns RFC 7807 ProblemDetail responses

## Password Security

- **Hashing**: BCrypt with strength 10
- **Storage**: Only hashed passwords stored in database
- **OAuth Users**: No password stored (password_hash is NULL)

## Seed Data (Development Only)

**Admin User**:
- Email: `admin@docflow.com`
- Password: `Admin@123`
- Role: ADMIN

Created by migration: `V5__seed_data_dev.sql`

## Configuration

### Development (application-dev.properties)

```properties
jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437
jwt.expiration=86400000

spring.security.oauth2.client.registration.google.client-id=your-google-client-id
spring.security.oauth2.client.registration.google.client-secret=your-google-client-secret
```

### Production (application-prod.properties)

```properties
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION:86400000}

spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
```

## Google OAuth2 Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable Google+ API
4. Create OAuth 2.0 credentials
5. Add authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`
6. Copy Client ID and Client Secret
7. Set environment variables or update application-dev.properties

## Testing Authentication

### Signup
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "Test@123"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@docflow.com",
    "password": "Admin@123"
  }'
```

### Access Protected Endpoint
```bash
curl -X GET http://localhost:8080/api/protected \
  -H "Authorization: Bearer <your-jwt-token>"
```

## Error Responses

All errors return RFC 7807 ProblemDetail format:

**Validation Error (400)**:
```json
{
  "type": "https://api.docflow.com/errors/validation-failed",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed",
  "errors": {
    "email": "Email must be valid",
    "password": "Password must contain at least one uppercase letter..."
  }
}
```

**Authentication Failed (401)**:
```json
{
  "type": "https://api.docflow.com/errors/authentication-failed",
  "title": "Authentication Failed",
  "status": 401,
  "detail": "Invalid email or password"
}
```

**Unauthorized (401)**:
```json
{
  "type": "https://api.docflow.com/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication required. Please provide a valid JWT token."
}
```

## Security Best Practices

1. **JWT Secret**: Use strong, random secret in production (min 256 bits)
2. **HTTPS**: Always use HTTPS in production
3. **Token Storage**: Store JWT in httpOnly cookies or secure storage (not localStorage)
4. **Token Refresh**: Implement refresh token mechanism for long-lived sessions
5. **Rate Limiting**: Add rate limiting to auth endpoints
6. **Account Lockout**: Implement after N failed login attempts
7. **Password Reset**: Add password reset flow with email verification
8. **2FA**: Consider adding two-factor authentication
9. **Audit Logging**: Log all authentication attempts
10. **CORS**: Restrict allowed origins in production

## Next Steps

1. Configure Google OAuth2 credentials
2. Test signup and login flows
3. Implement password reset functionality
4. Add refresh token mechanism
5. Implement rate limiting
6. Add audit logging for security events

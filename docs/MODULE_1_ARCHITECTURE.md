# Module 1: Architecture & Request Flow

## Modular Monolith & Package-by-Feature Structure
```text
src/main/java/com/placement/campus_placement_system/
├── CampusPlacementSystemApplication.java   # Spring Boot entry point
├── auth/
│   ├── controller/
│   │   └── AuthController.java             # HTTP endpoints (/api/auth/register, /login, /me)
│   ├── dto/
│   │   ├── request/
│   │   │   ├── LoginRequest.java           # Validated login input
│   │   │   └── RegisterRequest.java        # Validated registration input
│   │   └── response/
│   │       ├── ApiErrorResponse.java       # Standard JSON error envelope
│   │       ├── AuthResponse.java           # Login response with JWT + user info
│   │       └── UserResponse.java           # Safe user representation (no password)
│   ├── entity/
│   │   └── User.java                       # JPA entity mapped to PostgreSQL 'users' table
│   ├── enums/
│   │   └── Role.java                       # STUDENT, TPO, RECRUITER
│   ├── repository/
│   │   └── UserRepository.java             # Spring Data JPA data access
│   ├── security/
│   │   ├── CustomUserDetails.java          # Bridges User entity to Spring Security UserDetails
│   │   ├── CustomUserDetailsService.java   # Loads UserDetails by email from PostgreSQL
│   │   ├── JwtAuthenticationFilter.java    # Validates Bearer JWT on every request
│   │   ├── JwtProperties.java              # Startup-validated JWT_SECRET & JWT_EXPIRATION
│   │   ├── JwtService.java                 # HS256 JWT generation and claim extraction
│   │   ├── RestAccessDeniedHandler.java    # JSON 403 handler for insufficient role
│   │   └── RestAuthenticationEntryPoint.java # JSON 401 handler for missing/bad/expired JWT
│   └── service/
│       └── AuthService.java                # Core registration, login, and /me business logic
├── config/
│   ├── JpaAuditingConfig.java              # Enables @CreatedDate and @LastModifiedDate
│   └── SecurityConfig.java                 # Filter chain, RBAC rules, PasswordEncoder, AuthManager
└── exception/
    ├── DuplicateEmailException.java        # 409 Conflict exception
    ├── GlobalExceptionHandler.java         # @RestControllerAdvice centralized error handler
    ├── InvalidRoleRegistrationException.java # 403 Forbidden for public TPO/RECRUITER attempts
    └── ResourceNotFoundException.java      # 404 Not Found exception
```

## Controller → Service → Repository Workflow
1. **Controller (`AuthController`):** Accepts JSON requests, triggers Jakarta Bean Validation (`@Valid`), delegates to `AuthService`, and returns response DTOs. Controllers never touch JPA repositories or entities directly.
2. **Service (`AuthService`):** Enforces business rules (allowing only `Role.STUDENT` on public registration, normalizing email to lowercase, checking duplicate emails, encoding passwords with BCrypt, invoking `AuthenticationManager`, and calling `JwtService`).
3. **Repository (`UserRepository`):** Extends `JpaRepository<User, Long>` and provides `findByEmailIgnoreCase` and `existsByEmailIgnoreCase`.

## JWT Authentication Workflow
1. **Login (`POST /api/auth/login`):**
   - `AuthService` passes email and raw password to `AuthenticationManager`.
   - `DaoAuthenticationProvider` calls `CustomUserDetailsService.loadUserByUsername(email)` and verifies the BCrypt hash and `isEnabled()` (`active == true`).
   - On success, `JwtService.generateToken(user)` creates an `HS256` token containing `sub` (email), `userId`, `role`, `iat`, and `exp`.
2. **Protected Request (`GET /api/auth/me` or `/api/student/**`):**
   - `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`.
   - If `Authorization: Bearer <token>` is present, `JwtService` verifies the signature and expiration.
   - If valid and the user is active, `SecurityContextHolder` is populated with `CustomUserDetails` and `ROLE_<ROLE>` authority.
   - If the token is missing, invalid, or expired on a protected endpoint, `RestAuthenticationEntryPoint` returns HTTP `401 Unauthorized`.
   - If the user is authenticated but lacks the required role, `RestAccessDeniedHandler` returns HTTP `403 Forbidden`.

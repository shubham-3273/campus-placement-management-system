# Module 1: Authentication and User Management — Overview

## Purpose
Module 1 establishes the security foundation of the **Campus Placement Management System** using a **Modular Monolith Architecture**. Every subsequent module (`student`, `company`, `job`, `eligibility`, `application`, `recruitment`, `notification`, `analytics`) will rely on the user accounts, roles, and stateless JWT authentication provided by this module.

## Key Features Implemented
- **User Account Model:** `User` entity with `STUDENT`, `TPO`, and `RECRUITER` roles, BCrypt password hashing, active/inactive status flag, and automatic `createdAt`/`updatedAt` timestamps.
- **Public Student Registration (`POST /api/auth/register`):** Validates input, enforces unique email, hashes passwords using `BCryptPasswordEncoder`, and blocks public `TPO` and `RECRUITER` registrations.
- **JWT Login (`POST /api/auth/login`):** Verifies credentials via Spring Security's `AuthenticationManager`, rejects inactive accounts, and issues an `HS256`-signed JWT containing `userId`, `email` (`sub`), and `role`.
- **Current User Retrieval (`GET /api/auth/me`):** Reads the authenticated principal directly from `SecurityContextHolder` (never trusting a client-supplied `userId`).
- **Role-Based Access Control (RBAC):** Secures `/api/student/**`, `/api/tpo/**`, and `/api/recruiter/**` routes by role and protects all other non-public routes by default.
- **Centralized JSON Error Handling:** Consistent `ApiErrorResponse` payloads across validation errors, duplicate emails, invalid credentials, expired/invalid/missing JWTs (`401`), and insufficient permissions (`403`).
- **Migration-Controlled PostgreSQL Schema:** Managed exclusively by Flyway (`V1__create_users_table.sql`) with `spring.jpa.hibernate.ddl-auto=validate`.
- **Public Health Check (`GET /actuator/health`):** Exposes basic `UP`/`DOWN` status without leaking environment variables or sensitive internals.

## Stateless Logout Explanation
Because this API uses **stateless JWT authentication** (`SessionCreationPolicy.STATELESS`), the server does **not** store login sessions in memory or in a database.
- **Basic Logout Behavior:** When a user logs out, the frontend application deletes its stored JWT (e.g., from memory, `localStorage`, or secure cookie).
- **Important Security Reality:** Deleting the token on the client **does not invalidate the JWT on the server**. The token remains cryptographically valid until its `exp` (`JWT_EXPIRATION`) timestamp is reached.
- **Future Hardening Option:** True server-side token invalidation before expiration requires a token denylist (e.g., Redis) or rotating refresh tokens, which can be added if needed without changing the core authentication flow.

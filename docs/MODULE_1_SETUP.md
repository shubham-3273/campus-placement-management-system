# Module 1: Environment Setup & Run Guide

## 1. Prerequisites
- **Java 21+**
- **PostgreSQL** running locally with a database created (e.g., `campus-placement-system`)
- **Maven Wrapper** (`mvnw` / `mvnw.cmd` included in repository)

## 2. Generate a Secure JWT Secret
`JWT_SECRET` must be at least **32 characters (256 bits)** for `HS256` signing. Generate a strong secret using one of these commands:

- **Git Bash / Linux / macOS (OpenSSL):**
  ```bash
  openssl rand -base64 48
  ```
- **PowerShell (Windows):**
  ```powershell
  [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
  ```

> [!IMPORTANT]
> Never commit real passwords or JWT secrets to Git. Keep secrets in environment variables or a local `.env` file (already listed in `.gitignore`).

## 3. Required Environment Variables
See `.env.example` in the project root:
- `DB_URL` — e.g. `jdbc:postgresql://localhost:5432/campus-placement-system`
- `DB_USERNAME` — your PostgreSQL username
- `DB_PASSWORD` — your PostgreSQL password
- `JWT_SECRET` — strong random secret (>= 32 characters)
- `JWT_EXPIRATION` — token lifetime in milliseconds (e.g. `86400000` for 24 hours)

If `JWT_SECRET` or `JWT_EXPIRATION` is missing or invalid, `JwtProperties` fails startup immediately with a descriptive validation error instead of an obscure runtime exception.

## 4. IntelliJ IDEA Environment Variable Setup
1. Open **Run → Edit Configurations...** in IntelliJ IDEA.
2. Select `CampusPlacementSystemApplication`.
3. Locate the **Environment variables** field (click **Modify options → Environment variables** if hidden).
4. Add:
   ```text
   DB_URL=jdbc:postgresql://localhost:5432/campus-placement-system;DB_USERNAME=postgres;DB_PASSWORD=YOUR_PASSWORD;JWT_SECRET=YOUR_GENERATED_SECRET;JWT_EXPIRATION=86400000
   ```
   *(Or click the edit icon on the right of the field to enter each key/value pair on its own row.)*
5. Click **Apply → OK** and run the application.

## 5. Running from PowerShell
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/campus-placement-system"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="YOUR_PASSWORD"
$env:JWT_SECRET="YOUR_GENERATED_32_CHAR_PLUS_SECRET"
$env:JWT_EXPIRATION="86400000"

./mvnw spring-boot:run
```

## 6. Running Tests
Unit tests and PostgreSQL-backed integration tests (`test` profile) can be executed with:
```powershell
$env:DB_PASSWORD="YOUR_PASSWORD"
./mvnw test
```

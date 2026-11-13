# Module 1: API Reference & Postman Testing

Base URL: `http://localhost:8080`

## Access Control Summary
| Endpoint / Pattern | Access Level |
| :--- | :--- |
| `POST /api/auth/register` | Public |
| `POST /api/auth/login` | Public |
| `GET /api/auth/me` | Authenticated (valid JWT) |
| `GET /actuator/health`, `GET /api/health` | Public |
| `/api/student/**` | `STUDENT` role |
| `/api/tpo/**` | `TPO` role |
| `/api/recruiter/**` | `RECRUITER` role |
| All other endpoints | Authenticated by default |

---

## 1. Register Student (`POST /api/auth/register`)
Only `STUDENT` is allowed via public registration.

**Request Body:**
```json
{
  "name": "Test Student",
  "email": "student@example.com",
  "password": "Student@123",
  "role": "STUDENT"
}
```

**Success Response (`201 Created`):**
```json
{
  "userId": 1,
  "name": "Test Student",
  "email": "student@example.com",
  "role": "STUDENT"
}
```

**Error Responses:**
- `400 Bad Request` — Invalid name, email format, or password shorter than 8 characters (`validationErrors` map included).
- `403 Forbidden` — Attempting to register with `"role": "TPO"` or `"role": "RECRUITER"`.
- `409 Conflict` — Email already exists.

---

## 2. Login (`POST /api/auth/login`)

**Request Body:**
```json
{
  "email": "student@example.com",
  "password": "Student@123"
}
```

**Success Response (`200 OK`):**
```json
{
  "userId": 1,
  "name": "Test Student",
  "email": "student@example.com",
  "role": "STUDENT",
  "token": "<signed-jwt-token>",
  "tokenType": "Bearer"
}
```

**Error Responses:**
- `401 Unauthorized` — Wrong email or wrong password.
- `403 Forbidden` — Account exists but `active = false`.

---

## 3. Get Current Authenticated User (`GET /api/auth/me`)

**Headers:**
```text
Authorization: Bearer <signed-jwt-token>
```

**Success Response (`200 OK`):**
```json
{
  "userId": 1,
  "name": "Test Student",
  "email": "student@example.com",
  "role": "STUDENT"
}
```

**Error Responses:**
- `401 Unauthorized` — Missing `Authorization` header, invalid token, or expired token.

---

## 4. Health Check (`GET /actuator/health`)

**Success Response (`200 OK`):**
```json
{
  "status": "UP"
}
```

---

## Postman Collection Workflow
1. **Create a Postman Environment** with variables:
   - `baseUrl` = `http://localhost:8080`
   - `jwtToken` = *(leave blank)*
2. **Request 1 — Register Student:** `POST {{baseUrl}}/api/auth/register` with JSON body above → expect `201 Created`.
3. **Request 2 — Public TPO Rejection:** `POST {{baseUrl}}/api/auth/register` with `"role": "TPO"` → expect `403 Forbidden`.
4. **Request 3 — Login:** `POST {{baseUrl}}/api/auth/login` → expect `200 OK`. Add a Postman **Tests** script to save the token automatically:
   ```javascript
   const data = pm.response.json();
   if (data.token) {
       pm.environment.set("jwtToken", data.token);
   }
   ```
5. **Request 4 — Current User:** `GET {{baseUrl}}/api/auth/me` with Auth Type **Bearer Token** set to `{{jwtToken}}` → expect `200 OK`.
6. **Request 5 — Unauthorized Probe:** `GET {{baseUrl}}/api/auth/me` with no token → expect `401 Unauthorized`.
7. **Request 6 — Health Check:** `GET {{baseUrl}}/actuator/health` → expect `200 OK` with `{"status":"UP"}`.

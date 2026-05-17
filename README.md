# Auth Service

Part of the [TV Tracker](../README.md) microservices backend. Handles all authentication and user identity concerns — registration, login, JWT issuance, token refresh, and password reset.

---

## Responsibilities

- User registration (email + username + password)
- Login via email or username
- Issues short-lived **access tokens** (15 min) signed with an RSA private key
- Issues long-lived **refresh tokens** (30 days) stored in the database
- Refresh token rotation — old token is revoked on every refresh
- Forgot password / reset password via email (Resend)
- Designed for future **OAuth 2.0** support (Google) — `auth_provider` and `provider_id` fields are already in the schema

---

## Tech Stack

| Concern | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security + JJWT 0.12.3 |
| Database | PostgreSQL (via Spring Data JPA) |
| JWT Signing | RSA asymmetric (RS256) |
| Email | Resend API |
| Build | Maven |

---

## Architecture Notes

### Asymmetric JWT Signing

This service holds the **RSA private key** and is the only service that can issue tokens. All other services (including the API Gateway) verify tokens using the **public key only** — they can validate but never forge a token.

```
Login → auth-service signs JWT with private key
Request → api-gateway verifies JWT with public key → forwards X-User-Id header
Refresh → client calls /api/auth/refresh → auth-service issues new access token
```

### Token Strategy

| Token | Lifetime | Storage |
|---|---|---|
| Access token | 15 minutes | Client memory (not localStorage) |
| Refresh token | 30 days | HttpOnly cookie + database |

Refresh tokens are rotated on every use — the old token is revoked and a new one is issued. This limits the damage of a stolen refresh token.

---

## API Endpoints

All endpoints are prefixed with `/api/auth`.

| Method | Endpoint | Auth required | Description |
|---|---|---|---|
| `POST` | `/register` | No | Create a new account |
| `POST` | `/login` | No | Login, returns access + refresh token |
| `POST` | `/refresh` | No | Exchange refresh token for new access token |
| `POST` | `/forgot-password` | No | Sends password reset email |
| `POST` | `/reset-password` | No | Resets password using token from email |
| `GET` | `/me` | Yes | Returns current user profile |

---

## Database Schema

Schema: `user_schema`

### `users`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| email | VARCHAR | Unique |
| username | VARCHAR | Unique |
| password | VARCHAR | Nullable — null for OAuth users |
| auth_provider | ENUM | `LOCAL` or `GOOGLE` |
| provider_id | VARCHAR | Nullable — Google user ID for OAuth |
| email_verified | BOOLEAN | Default false |
| created_at | TIMESTAMP | Auto-managed |
| updated_at | TIMESTAMP | Auto-managed |

### `refresh_tokens`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| token | VARCHAR | Unique, opaque random string |
| user_id | UUID | Foreign key → users |
| expires_at | TIMESTAMP | |
| revoked | BOOLEAN | Default false |
| created_at | TIMESTAMP | |

### `password_reset_tokens`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| token | VARCHAR | Unique |
| user_id | UUID | Foreign key → users |
| expires_at | TIMESTAMP | 1 hour |
| used | BOOLEAN | Default false |
| created_at | TIMESTAMP | |

---

## Environment Variables

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_PRIVATE_KEY` | Base64-encoded RSA private key (PEM) |
| `JWT_PUBLIC_KEY` | Base64-encoded RSA public key (PEM) |
| `RESEND_API_KEY` | Resend API key for sending emails |
| `RESEND_FROM_EMAIL` | Sender email address |
| `FRONTEND_URL` | Used to generate password reset links |

### Generating RSA Keys

```bash
# Generate private key
openssl genrsa -out private.pem 2048

# Extract public key
openssl rsa -in private.pem -pubout -out public.pem

# Base64 encode for environment variables
base64 -w 0 private.pem
base64 -w 0 public.pem
```

---

## Running Locally

**Prerequisites:** Java 25, Maven, PostgreSQL running locally or via Docker

```bash
# Clone the repo
git clone git@github.com:YOUR_USERNAME/tv-tracker.git
cd tv-tracker/auth-service

# Set environment variables (create a .env file or export them)
cp .env.example .env
# Fill in your values

# Run
./mvnw spring-boot:run
```

Or with Docker:

```bash
docker build -t auth-service .
docker run -p 8081:8081 --env-file .env auth-service
```

---

## Running Tests

```bash
./mvnw test
```

---

## Part of TV Tracker

This service is one of five in the TV Tracker backend:

| Service | Port | Description |
|---|---|---|
| **auth-service** | 8081 | Authentication & user identity |
| api-gateway | 8080 | Single entry point, JWT validation, routing |
| show-service | 8082 | TMDB integration, show search & caching |
| tracking-service | 8083 | Episode tracking, Kafka event publishing |
| notification-service | 8084 | New episode alerts via email |

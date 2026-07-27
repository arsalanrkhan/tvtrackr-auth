# tvtrackr-auth

Handles user registration, login, JWT issuance, refresh-token rotation, password reset, and email verification for the TVTrackr backend (port `8081`). The only service that holds the RSA **private** key — every other service (starting with the gateway) verifies tokens with the public key only.

> Code below reflects the `develop` branch, where the actual implementation lives.

## API

All endpoints are prefixed `/api/auth/v1` and are public at this service's own security layer — the gateway is expected to be the actual authorization boundary for anything downstream.

| Method | Path | Description |
|---|---|---|
| `POST` | `/register` | Create account, issue access token + refresh cookie, add username to Bloom filter, send verification email |
| `POST` | `/login` | Login by email **or** username + password |
| `POST` | `/logout` | Revoke the refresh token from the cookie and clear it |
| `POST` | `/refresh` | Rotate refresh token, issue new access token |
| `POST` | `/forgot-password` | Send password reset email (silently no-ops if email doesn't exist; 60s cooldown) |
| `POST` | `/reset-password` | Consume one-time reset token, set new password, revoke all refresh tokens |
| `GET` | `/verify-email?token=` | Consume one-time verification token, mark email verified, return a fresh access token |
| `POST` | `/resend-verification-email` | Re-send verification email (silently no-ops if not found/already verified; 60s cooldown) |
| `GET` | `/usernames/{username}/availability` | Two-tier Bloom filter + DB username availability check |

## JWT strategy

- **Access token** — 15 min (`app.jwt.access-token-expiry-ms`, default `900000`), signed with an RSA private key (RS256), carries `sub` (user UUID), `email`, and `emailVerified` claims. Stateless — never persisted.
- **Refresh token** — 30 days (`app.refresh-token.expiry-days`), an opaque 64-byte random string (not a JWT), persisted in Postgres, sent as an `HttpOnly` + `Secure` + `SameSite=Strict` cookie named `refreshToken`.
- **Rotation** — every `/refresh` call revokes the old token and issues a new one. Both `/refresh` and `/logout` fetch the token via `findByTokenForUpdate` (`SELECT ... FOR UPDATE`), so a concurrent refresh-and-logout (or two concurrent refreshes) on the same token can't both succeed.
- **Password reset** revokes *all* of a user's refresh tokens, not just the current one.
- **Email verification** returns a brand-new access token with `emailVerified=true` in the same response, so the client doesn't need to re-login to unlock gated features.

## Two-tier username availability

`usernameAvailability()` in `AuthServiceImpl`:
1. **Bloom filter first** (`UsernameBFService.mightExist`) — backed by a Redis bitmap (`bloom:usernames`), populated via double hashing (Guava MurmurHash3, two seeds) into `bloom-filter.hash-count` (default 7) bit offsets over a `bloom-filter.bit-size` (default ~9.58M) bit array. Bit reads/writes go through `RedisService.getBitsPipelined` / `setBitsPipelined` — all *k* bits in a single Redis round trip.
2. If the filter says "definitely not present," the check returns `available = true` with **no DB query**.
3. Only on a probable-positive does it fall through to `UserRepository.existsByUsernameCaseInsensitive` (backed by a `LOWER(username)` unique index) to resolve the ~1% false-positive rate.

`UsernameBFSeeder` (an `ApplicationRunner`) seeds the filter from all existing usernames on startup, guarded by a Redis flag (`bloom:usernames:seeded`) so a multi-instance deployment doesn't reseed redundantly — code comments note the seeded-check-then-set isn't atomic, but this is judged safe in practice since instances start sequentially, and note `saveStringPermanentIfAbsent` as the fix if that ever changes. `UnverifiedUserCleanupScheduler` calls `clear()` then `seed()` after purging stale users; there's a documented brief window mid-rebuild where the filter is empty and every username reads as available — the DB's unique index is the actual safety net during that window.

## Database (`auth_db`, Flyway-managed)

- **`users`** — `BIGSERIAL` internal PK, public `uuid`, unique `email`, case-insensitive-unique `username` (via `LOWER(username)` index), `display_name`, `email_verified`, plus `version`/`created_at`/`updated_at` from `BaseEntity`. Partial index on `created_at` where `email_verified = false` speeds up the unverified-cleanup scan.
- **`user_auth_providers`** — one row per (user, provider) pair; `provider` is `LOCAL` or `GOOGLE` (DB `CHECK` constraint), `password_hash` only populated for `LOCAL`. This design keeps room for OAuth without a nullable password column on `users` itself.
- **`refresh_tokens`** — token string (unique), `revoked` flag, `expires_at`; partial indexes for the cleanup job's revoked/expired scans.

## Redis usage

| Key pattern | TTL | Purpose |
|---|---|---|
| `auth:passwordReset:{token}` | 15 min | One-time password reset token → user ID |
| `auth:emailVerification:{token}` | 15 min | One-time verification token → user ID |
| `auth:cooldown:passwordReset:{userId}` | 60 s | Rate limit on reset requests |
| `auth:cooldown:emailVerification:{userId}` | 60 s | Rate limit on resend requests |
| `bloom:usernames` | none | Bloom filter bitmap |
| `bloom:usernames:seeded` | none | Seeding guard flag |

One-time tokens are consumed via `RedisService.getAndDeleteString` (atomic `GETDEL`) — the delete *is* the validity check, so a token can't be replayed even under concurrent requests. Cooldowns use `saveStringIfAbsent` (`SET NX`) rather than a check-then-set, closing the same class of race.

## Scheduled jobs

Both use ShedLock (Redis-backed) so only one instance runs them in a multi-instance deployment, and both delete in chunks of 1000 rather than a single unbounded statement:

- **`UnverifiedUserCleanupScheduler`** — daily at midnight. Deletes unverified accounts older than `app.scheduler.cleanup.after-days` (default 7), then rebuilds the Bloom filter.
- **`RefreshTokenCleanupScheduler`** — daily at 1am. Deletes revoked or expired refresh tokens.

## Email

Registration, password reset, and email verification all send through `AuthEmailService` → Thymeleaf template (`email/password-reset.html`, `email/email-verification.html`) → `EmailService` → Resend's REST API via a plain `RestClient` (no SDK).

**Note:** in the current code, email sending happens **synchronously inline** during the request (register/forgot-password/resend calls straight through to Resend). `AuthServiceImpl.register()` has a `TODO` to move the verification email and Bloom-filter update onto a Kafka event once Kafka is wired up — so the "async via Kafka from day one" design in the broader project plan isn't in this service's code yet.

## Security notes

- Passwords hashed with BCrypt, strength 12.
- Login accepts email *or* username via a single case-insensitive query (`findByEmailOrUsernameCaseInsensitive`) that also `LEFT JOIN FETCH`es auth providers to avoid an N+1 lookup.
- `forgot-password` and `resend-verification-email` return identical `200` responses whether or not the account exists, to avoid leaking which emails are registered.
- A `DataIntegrityViolationException` during registration (a duplicate slipping past the pre-check under concurrent signups) is caught and turned into a single `EMAIL_OR_USERNAME_ALREADY_EXISTS` error rather than a raw 500.
- Swagger/OpenAPI (SpringDoc) is available at `/swagger-ui.html` but disabled by default (`SWAGGER_ENABLED=false`, `API_DOCS_ENABLED=false`).

## Tech stack

Java 25 · Spring Boot 4.0.6 · Spring Security + Spring Data JPA/Hibernate · PostgreSQL 16 + Flyway · Redis (Spring Data Redis) · JJWT 0.12.6 (RS256) · ShedLock 6.9.2 (Redis provider) · Guava 33.4.0 (Bloom filter hashing) · Thymeleaf · Resend (via `RestClient`) · SpringDoc OpenAPI 2.8.8 · `tvtrackr-common`

## Configuration (env vars)

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | Postgres connection |
| `REDIS_HOST/PORT/PASSWORD` | Redis connection |
| `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` | Base64-encoded RSA keypair for signing/verifying access tokens |
| `JWT_ACCESS_TOKEN_EXPIRY_MS` | Default `900000` (15 min) |
| `FRONTEND_BASE_URL` | Used to build verification/reset links in emails |
| `RESEND_API_KEY`, `RESEND_FROM_EMAIL`, `RESEND_FROM_NAME` | Outbound email |
| `CLEANUP_UNVERIFIED_USERS_AFTER_DAYS` | Default 7 |
| `BF_BIT_SIZE`, `BF_HASH_COUNT` | Bloom filter sizing, defaults `9585059` / `7` |
| `SWAGGER_ENABLED`, `API_DOCS_ENABLED` | Both default `false` |

## Running locally

`docker-compose.yml` brings up Postgres, Redis, and the service together:

```bash
docker compose up --build
```

Generate an RSA keypair for local testing:

```bash
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
base64 -w 0 private.pem   # → JWT_PRIVATE_KEY
base64 -w 0 public.pem    # → JWT_PUBLIC_KEY
```

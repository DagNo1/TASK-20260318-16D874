# Practice Platform Backend Design

## Overview

This service is a Spring Boot 3 monolith running on Java 17 with MySQL as the primary datastore. It is designed for an offline environment and provides backend APIs for a "Pet Supplies Trading and Cooking Practice Management Platform" spanning catalog management, inventory, practice sessions, reporting, notifications, instant messaging, approval workflows, and sensitive-profile protection.

The backend uses a layered architecture:

- `api/`: REST controllers and WebSocket message handlers
- `service/`: business rules and orchestration
- `repository/`: Spring Data JPA persistence access
- `domain/`: entities and enums
- `security/`: JWT auth, filters, masking, encryption helpers
- `config/`: HTTP security, OpenAPI, WebSocket, and application config
- `ops/backup/`: backup scripts and restore runbook

## Goals

- Support four user roles: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`, and `REVIEWER`
- Provide real CRUD and workflow endpoints rather than demo-only stubs
- Enforce role and scope checks at controller and service boundaries
- Keep the deployment model simple with a single service and a local database
- Support operational compliance features such as retention, masking, encryption, TLS profile support, and local backup procedures

## Architecture

## Runtime components

- Spring Boot HTTP API on port `8080`
- Swagger/OpenAPI metadata exposed at `/swagger-ui.html` and `/v3/api-docs`
- JWT-based stateless authentication
- STOMP/WebSocket messaging on `/ws`
- MySQL with Flyway migrations
- Scheduled jobs for report generation and IM message retention

## Request flow

1. A client authenticates through `/api/auth/login`.
2. The server returns a JWT containing the authenticated username and role list.
3. The client sends `Authorization: Bearer <token>` on protected REST calls.
4. `JwtAuthenticationFilter` restores the security context.
5. `@PreAuthorize` checks role-level permissions on controllers.
6. Services apply object-level and business-rule checks before persistence.

## Security model

### Authentication

- Login is handled by `AuthService` through Spring Security's `AuthenticationManager`.
- Passwords are stored as BCrypt hashes.
- Password policy requires at least 8 characters with both letters and numbers.
- Accounts are locked for 15 minutes after 5 consecutive failed attempts.

### Authorization

- Public routes:
  - `/api/auth/**`
  - `/ws/**`
  - `/swagger-ui.html`
  - `/swagger-ui/**`
  - `/v3/api-docs/**`
- All other routes require authentication.
- Controller methods use role-based guards such as:
  - `PLATFORM_ADMIN` for approvals and manual report generation
  - `PLATFORM_ADMIN` or `MERCHANT_OPERATOR` for product and inventory management
  - `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`, `REVIEWER` for read-only practice/reporting views where appropriate

### Sensitive data protection

- Sensitive profile data is stored encrypted in the database.
- Sensitive values are returned to clients in masked form.
- Sensitive values are logged in masked form rather than raw form.

### Transport security

- A `tls` Spring profile enables HTTPS server-side SSL configuration.
- Keystore path, password, type, and alias are supplied by environment variables.

### Critical operation protection

- Approval workflows require platform-admin access.
- Approval execution uses a dual-admin pattern for protected operations.
- Audit views are exposed for approval request history.

## Error handling

REST errors are normalized as:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "productCode is required",
  "traceId": "corr-123"
}
```

The main codes used by the global exception handler are:

- `NOT_FOUND`
- `BUSINESS_RULE`
- `VALIDATION_ERROR`
- `BAD_REQUEST`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `INTERNAL_ERROR`

Unhandled exceptions are logged server-side and returned to clients as a sanitized `INTERNAL_ERROR`.

## Core domain modules

## 1. Authentication and session access

- JWT login endpoint
- BCrypt password hashing
- Password complexity enforcement
- Failed-attempt lockout window

## 2. Product catalog

- Product and SKU create/update/get/list flows
- Listing and delisting workflows
- CSV export
- Batch import from structured request rows
- Category, brand, and attribute relationships enforced in service/data layers

Key business constraints:

- Unique product codes
- Unique SKU barcodes
- Category depth limit
- Inventory thresholds tied to SKU stock data

## 3. Inventory

- Upsert inventory items
- Adjust stock quantities
- Track inventory logs
- Surface low-stock alerts
- Default alert threshold configured in application settings

## 4. Practice sessions

- Create practice sessions from nested steps and timers
- Multiple parallel timers per session
- Manual and automatic checkpoint support
- Step completion workflow
- Reviewer read access to session state and latest checkpoint

## 5. Instant messaging

- STOMP/WebSocket-based session establishment
- Message send, recall, and read receipt flows
- Session topic broadcasting
- Anti-spam and image-rule enforcement in the service layer
- Retention job for old IM messages with a default of 180 days

## 6. Notifications

- Internal notification delivery
- Read tracking
- Subscription listing and upsert
- Supported event types:
  - `PRACTICE_STEP_COMPLETED`
  - `IM_MESSAGE_RECALLED`
  - `ORDER_STATUS_UPDATED`
  - `REVIEW_RESULT_PUBLISHED`
  - `REPORT_HANDLING_UPDATED`

## 7. Reporting

- Indicator catalog
- Aggregation queries by organization and time window
- Drill-down retrieval of source records
- XLSX export
- Daily scheduled report generation
- Manual trigger for platform admins

## 8. Practice achievements

- Versioned achievement records
- Attachment versioning
- Assessment form upsert
- Completion-certificate PDF export
- Assessment-form XLSX export

## 9. Sensitive profile

- Authenticated upsert of phone number and ID number
- Encrypted persistence
- Masked retrieval

## Data and persistence

- MySQL is the primary backing store.
- Flyway initializes schema and seeded auth data from `src/main/resources/db/migration/V1__init_schema.sql`.
- JPA/Hibernate validates schema at startup.
- Time handling is configured in UTC.

## Background jobs and operations

### Scheduled behavior

- IM retention archive job uses the configured retention cron and retention-days value.
- Reporting has a daily generation workflow aligned with the default scheduled-report requirement.

### Backup and recovery

- Daily full backup script: `ops/backup/backup_full_daily.sh`
- Hourly incremental backup script: `ops/backup/backup_incremental_hourly.sh`
- Restore runbook: `ops/backup/README.md`
- Intended retention window: 30 days

## Configuration

Important runtime settings include:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `JWT_SECRET`
- `IM_RETENTION_DAYS`
- `IM_RETENTION_CRON`
- `REPORT_TIMEZONE`
- `TLS_KEYSTORE_PATH`
- `TLS_KEYSTORE_PASSWORD`
- `TLS_KEYSTORE_TYPE`
- `TLS_KEY_ALIAS`

## Testing strategy

The repository includes unit and web-layer integration-style tests for:

- authentication and lockout
- role-based authorization
- object-scope reporting access
- product import/export boundaries
- reporting generation
- inventory and notification flows
- retention behavior
- encryption and masking helpers

Tests can be run in Dockerized Maven form to keep local host setup minimal.

## Trade-offs

- The monolith keeps deployment and debugging simple, but places more responsibility on internal module boundaries.
- Stateless JWT auth reduces session storage complexity, but requires clients to manage tokens explicitly.
- Backup/restore verification is documented operationally, not fully automated end-to-end.
- TLS is profile-driven, which is flexible for local development but requires deployment discipline.

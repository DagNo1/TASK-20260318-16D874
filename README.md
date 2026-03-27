# Practice Platform Backend API

Spring Boot 3 monolith backend (Java 17) for the “Pet Supplies Trading and Cooking Practice Management Platform”.

## Documentation

- Design document: `docs/design.md`
- API specification: `docs/api-spec.md`

## Start Command (How to Run)

1. Open a terminal in `pure_backend/`
2. Run:
   ```bash
   docker compose up
   ```

The service exposes the API on port `8080`.

## Service Address (Services List)

From your host machine:

- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- MySQL (optional, for debugging): `localhost:3306`
  - DB: `practice_platform`
  - User: `app`
  - Password: `app123`

## Verification Method

1. Confirm the server is running by opening Swagger UI:
   - `http://localhost:8080/swagger-ui.html`
2. Run the test suite in Docker:
   ```bash
   docker run --rm --platform=linux/arm64 -v "${PWD}:/workspace" -w /workspace maven:3.9.8-eclipse-temurin-17 mvn test
   ```
   - Expected result: `BUILD SUCCESS`
3. Verify the login endpoint:
   - `POST /api/auth/login`
   - Seeded demo users in `pure_backend/src/main/resources/db/migration/V1__init_schema.sql`:
     - `platform_admin`
     - `merchant_operator`
     - `regular_buyer`
     - `reviewer`
   - Seed comment documents password `password` for all demo users
   - If login fails in your local environment, verify your seeded auth data matches the current password policy
4. After login, call any authenticated endpoint using:
   - Header: `Authorization: Bearer <accessToken>`
5. Quick endpoint checks (once you have a JWT):
   - Products: `POST /api/merchant/products`
   - Product export: `GET /api/merchant/products/export`
   - Sensitive profile: `POST /api/profile/sensitive`
   - Cooking Timer (practice sessions): `POST /api/practice/sessions`
   - Cooking Timer (step timers): `POST /api/practice/sessions/{sessionId}/timers/{timerId}/command`
   - Cooking Timer (checkpoint load): `GET /api/practice/sessions/{sessionId}/checkpoints/latest`
   - Notifications: `GET /api/notifications?unreadOnly=false`

WebSocket (STOMP):

- Connect to: `ws://localhost:8080/ws`
- Send/subscribe topics depend on your client flow; example message mappings exist under:
  - `com.pettrade.practiceplatform.api.im.ImWebSocketController`

## Operational Notes

- Backup and recovery runbook: `pure_backend/ops/backup/README.md`
- Backup scripts:
  - `pure_backend/ops/backup/backup_full_daily.sh`
  - `pure_backend/ops/backup/backup_incremental_hourly.sh`
- Sensitive profile data is stored encrypted and returned/logged in masked form via the sensitive profile service flow.
- TLS support is available through the `tls` Spring profile and requires the keystore environment variables referenced in `pure_backend/src/main/resources/application.yml`.
- The main API surface and example payloads are documented in `docs/api-spec.md`.

## Project Structure (`pure_backend/`)

The backend code is organized as a standard layered Spring Boot monolith:

- `pure_backend/docker-compose.yml`: MySQL + backend container wiring (the one-click startup)
- `pure_backend/Dockerfile`: Builds the Spring Boot JAR into the runtime image
- `pure_backend/src/main/java/com/pettrade/practiceplatform/`
  - `api/`: REST controllers (and STOMP message endpoints)
  - `service/`: business logic (timers, checkpoints, reporting, messaging, approvals, etc.)
  - `repository/`: persistence layer (Spring Data JPA repositories)
  - `domain/`: entities and core domain models
  - `config/`: Spring configuration (OpenAPI, security config, WebSocket broker config)
  - `security/`: JWT/auth helpers and filters used by Spring Security
  - `websocket/`: WebSocket/STOMP specific helpers (if applicable)
  - `exception/`: application exceptions
- `pure_backend/src/main/resources/application.yml`: application configuration (server port, JWT settings, Flyway settings)
- `pure_backend/src/main/resources/db/migration/`: Flyway migrations (schema initialization and seeded auth data in `V1__init_schema.sql`)
- `pure_backend/src/test/java/`: unit/integration tests for the core business logic
- `pure_backend/ops/backup/`: backup scripts and restore runbook

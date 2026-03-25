# Practice Platform Backend API

Spring Boot 3 monolith backend (Java 17) for the “Pet Supplies Trading and Cooking Practice Management Platform”.

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
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- MySQL (optional, for debugging): `localhost:3306`
  - DB: `practice_platform`
  - User: `app`
  - Password: `app123`

## Verification Method

1. Confirm the server is running by opening Swagger UI:
   - `http://localhost:8080/swagger-ui.html`
2. Verify the anonymous login endpoint:
   - `POST /api/auth/login`
   - Seeded username: `admin`
   - Seeded password: `password`
   - Seed data source: `pure_backend/src/main/resources/db/migration/V2__seed_auth_data.sql`
3. After login, call any authenticated endpoint using:
   - Header: `Authorization: Bearer <accessToken>`
4. Quick endpoint checks (once you have a JWT):
   - Cooking Timer (practice sessions): `POST /api/practice/sessions`
   - Cooking Timer (step timers): `POST /api/practice/sessions/{sessionId}/timers/{timerId}/command`
   - Cooking Timer (checkpoint load): `GET /api/practice/sessions/{sessionId}/checkpoints/latest`
   - Notifications: `GET /api/notifications?unreadOnly=false`

WebSocket (STOMP):

- Connect to: `ws://localhost:8080/ws`
- Send/subscribe topics depend on your client flow; example message mappings exist under:
  - `com.pettrade.practiceplatform.api.im.ImWebSocketController`

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
- `pure_backend/src/main/resources/db/migration/`: Flyway migrations (schema + seed data such as `V2__seed_auth_data.sql`)
- `pure_backend/src/test/java/`: unit/integration tests for the core business logic


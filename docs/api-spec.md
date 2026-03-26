# Practice Platform API Specification

## Overview

- Base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Auth scheme: JWT bearer token
- Content type: `application/json` unless otherwise noted

## Authentication

Protected endpoints require:

```http
Authorization: Bearer <accessToken>
```

Public endpoints:

- `POST /api/auth/login`
- `/ws/**`
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs/**`

## Roles

- `PLATFORM_ADMIN`
- `MERCHANT_OPERATOR`
- `REGULAR_BUYER`
- `REVIEWER`

## Standard error response

```json
{
  "code": "FORBIDDEN",
  "message": "Access denied",
  "traceId": "corr-123"
}
```

Common HTTP and error-code mappings:

- `400` -> `VALIDATION_ERROR` or `BAD_REQUEST`
- `401` -> `UNAUTHORIZED`
- `403` -> `FORBIDDEN`
- `404` -> `NOT_FOUND`
- `409` -> `BUSINESS_RULE`
- `500` -> `INTERNAL_ERROR`

## REST endpoints

## Auth

### `POST /api/auth/login`

- Access: public
- Purpose: authenticate a user and return a JWT

Request body:

```json
{
  "username": "admin",
  "password": "password"
}
```

Response body:

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "username": "admin",
  "roles": ["ROLE_PLATFORM_ADMIN"]
}
```

## Products

Base path: `/api/merchant/products`  
Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`

### `POST /api/merchant/products`

- Create a product with one or more SKUs

Request body:

```json
{
  "productCode": "P-1001",
  "name": "Dog Food",
  "brandId": 2,
  "categoryId": 3,
  "skus": [
    {
      "skuBarcode": "BC-1001-S",
      "name": "Dog Food Small",
      "stockQuantity": 9,
      "alertThreshold": 10,
      "attributeSpecIds": [11, 12]
    }
  ]
}
```

### `PUT /api/merchant/products/{productId}`

- Update a product and its SKU set

### `GET /api/merchant/products/{productId}`

- Get one product by ID

### `GET /api/merchant/products?listed=true`

- List products filtered by listing state

### `POST /api/merchant/products/{productId}/list`

- Mark a product as listed

### `POST /api/merchant/products/{productId}/delist`

- Mark a product as delisted

### `POST /api/merchant/products/import`

- Batch import product rows

Request body:

```json
{
  "rows": [
    {
      "productCode": "P-1001",
      "name": "Dog Food",
      "brandId": 2,
      "categoryId": 3,
      "skuBarcode": "BC-1001-S",
      "skuName": "Dog Food Small",
      "stockQuantity": 9,
      "alertThreshold": 10
    }
  ]
}
```

### `GET /api/merchant/products/export`

- Export products as CSV
- Response content type: `text/csv`

## Inventory

Base path: `/api/merchant/inventory`  
Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`

### `POST /api/merchant/inventory/items`

- Create or update an inventory item

Request body:

```json
{
  "sku": "BC-1001-S",
  "name": "Dog Food Small",
  "stockQuantity": 9,
  "alertThreshold": 10
}
```

### `POST /api/merchant/inventory/items/{itemId}/stock`

- Adjust stock for an existing inventory item

Request body:

```json
{
  "newQuantity": 12,
  "reason": "cycle count"
}
```

### `GET /api/merchant/inventory/alerts`

- List low-stock alert history

### `GET /api/merchant/inventory/logs`

- List inventory adjustment history

## Notifications

Base path: `/api/notifications`  
Access: any authenticated user

### `GET /api/notifications?unreadOnly=false`

- List notifications for the current user

### `POST /api/notifications/read`

- Mark a delivered notification as read

Request body:

```json
{
  "deliveryId": 123
}
```

### `GET /api/notifications/subscriptions`

- List current user's notification subscriptions

### `POST /api/notifications/subscriptions`

- Create or update a notification subscription

Request body:

```json
{
  "eventType": "ORDER_STATUS_UPDATED",
  "enabled": true
}
```

Supported notification event types:

- `PRACTICE_STEP_COMPLETED`
- `IM_MESSAGE_RECALLED`
- `ORDER_STATUS_UPDATED`
- `REVIEW_RESULT_PUBLISHED`
- `REPORT_HANDLING_UPDATED`

## Reporting

Base path: `/api/reporting`

### `GET /api/reporting/indicators`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REVIEWER`
- List indicator definitions

### `POST /api/reporting/query`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REVIEWER`
- Query aggregate values

Request body:

```json
{
  "indicatorCode": "INVENTORY_CHANGE_EVENTS",
  "organizationUserId": 1,
  "businessDimension": null,
  "periodStart": "2026-03-25T00:00:00",
  "periodEnd": "2026-03-26T00:00:00"
}
```

### `POST /api/reporting/drill-down`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REVIEWER`
- Retrieve source records behind an aggregate query

### `POST /api/reporting/export`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REVIEWER`
- Export query results to XLSX
- Response content type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

Request body:

```json
{
  "indicatorCode": "INVENTORY_CHANGE_EVENTS",
  "organizationUserId": 1,
  "businessDimension": null,
  "periodStart": "2026-03-25T00:00:00",
  "periodEnd": "2026-03-26T00:00:00"
}
```

### `POST /api/reporting/generate-daily`

- Access: `PLATFORM_ADMIN`
- Trigger daily report generation manually

## Practice sessions

Base path: `/api/practice/sessions`

### `POST /api/practice/sessions`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`
- Create a practice session

Request body:

```json
{
  "title": "Braised Chicken Practice",
  "steps": [
    {
      "name": "Prep ingredients",
      "instruction": "Slice vegetables and measure seasoning.",
      "estimatedSeconds": 300,
      "techniqueCardIds": [101, 102],
      "nodeReminders": [
        {
          "offsetSecondsAfterStepStart": 60,
          "message": "Check prep completeness"
        }
      ],
      "timers": [
        {
          "timerKey": "marinade",
          "durationSeconds": 900,
          "reminders": [
            {
              "offsetSecondsBeforeDue": 60,
              "message": "Prepare next step"
            }
          ]
        }
      ]
    }
  ]
}
```

### `GET /api/practice/sessions/{sessionId}`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`, `REVIEWER`
- Get one practice session

### `POST /api/practice/sessions/{sessionId}/timers/{timerId}/command`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`
- Command a timer with `START`, `PAUSE`, or `RESUME`

Request body:

```json
{
  "action": "START"
}
```

### `POST /api/practice/sessions/{sessionId}/steps/{stepId}/complete`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`
- Mark a step as completed

### `POST /api/practice/sessions/{sessionId}/checkpoints`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`
- Save a checkpoint

Request body:

```json
{
  "reason": "manual"
}
```

### `GET /api/practice/sessions/{sessionId}/checkpoints/latest`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`, `REVIEWER`
- Load the latest checkpoint snapshot

## Practice achievements

Base path: `/api/practice-achievements`

### `GET /api/practice-achievements`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`, `REVIEWER`
- List achievements visible to the current user

### `POST /api/practice-achievements`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`
- Create an achievement

Request body:

```json
{
  "versionNo": 1,
  "title": "March Cooking Practice",
  "periodStart": "2026-03-01",
  "periodEnd": "2026-03-31",
  "responsiblePerson": "Chef Li",
  "conclusion": "Completed successfully"
}
```

### `PUT /api/practice-achievements/{achievementId}`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`
- Update an achievement with strict version incrementing

### `POST /api/practice-achievements/{achievementId}/attachments`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`
- Add a versioned attachment

### `GET /api/practice-achievements/{achievementId}/attachments`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`, `REVIEWER`
- List attachment versions

### `POST /api/practice-achievements/{achievementId}/assessment-form`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`
- Create or update the assessment form

### `GET /api/practice-achievements/{achievementId}/export/completion-certificate`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`, `REVIEWER`
- Export a PDF completion certificate
- Response content type: `application/pdf`

### `GET /api/practice-achievements/{achievementId}/export/assessment-form`

- Access: `PLATFORM_ADMIN`, `MERCHANT_OPERATOR`, `REGULAR_BUYER`, `REVIEWER`
- Export an XLSX assessment form
- Response content type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

## Approvals

Base path: `/api/approvals`  
Access: `PLATFORM_ADMIN`

### `POST /api/approvals`

- Create a protected-operation approval request

Request body:

```json
{
  "requestType": "INVENTORY_ADJUST",
  "targetResource": "inventory:item:1",
  "targetAction": "ADJUST",
  "payloadJson": "{\"newQuantity\":12}"
}
```

### `POST /api/approvals/{requestId}/approve`

- Record an approval decision
- Two distinct admins are required before execution

### `POST /api/approvals/{requestId}/reject`

- Record a rejection decision

### `POST /api/approvals/{requestId}/execute`

- Execute an already approved protected operation

### `GET /api/approvals/pending`

- List pending approval requests

### `GET /api/approvals/{requestId}/audit`

- List immutable audit entries for a request

## Sensitive profile

Base path: `/api/profile/sensitive`  
Access: any authenticated user

### `POST /api/profile/sensitive`

- Create or update the caller's encrypted sensitive profile

Request body:

```json
{
  "phoneNumber": "13800138000",
  "idNumber": "110101199001010011"
}
```

### `GET /api/profile/sensitive`

- Return the caller's masked sensitive profile

## WebSocket / STOMP endpoints

### Connection

- WebSocket endpoint: `ws://localhost:8080/ws`
- Message authentication is based on the user principal established for the session

### Message mappings

#### `/app/im/{sessionId}/establish`

- Establish an IM session for the connected user
- Server sends a user-scoped event to `/user/queue/im/session`

#### `/app/im/{sessionId}/send`

- Send a message to a session

Request body:

```json
{
  "type": "TEXT",
  "text": "hello",
  "imageFingerprint": null,
  "imageMimeType": null,
  "imageSizeBytes": null,
  "imageUrl": null,
  "clientMessageId": "msg-001"
}
```

- Server broadcasts message events to `/topic/im/{sessionId}/messages`

#### `/app/im/{sessionId}/recall`

- Recall a previously sent message

Request body:

```json
{
  "messageId": 1001
}
```

- Server broadcasts updated message state to `/topic/im/{sessionId}/messages`

#### `/app/im/{sessionId}/read`

- Mark messages as read up to a given message ID

Request body:

```json
{
  "lastReadMessageId": 1001
}
```

- Server broadcasts read events to `/topic/im/{sessionId}/reads`

## Notes for clients

- Use Swagger/OpenAPI for exact generated schemas when integrating.
- Expect validation failures for blank required fields and invalid enums.
- Product, reporting, and approval flows may return `409 BUSINESS_RULE` when business constraints are violated.
- Reporting scope is role-sensitive: non-admin users should only query/export their own organization scope.

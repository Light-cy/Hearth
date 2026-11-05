# Hisaab — REST API Contract (Frozen)

All money amounts in JSON are represented as numbers with at most 2 decimal places (e.g., `333.34`, `4200.00`).
Under the hood, all monetary values are stored and calculated strictly in integer paisa (`BIGINT`).
Ascending member ID order (`M01`, `M02`, `M03`) drives deterministic remainder allocation.

All endpoints except `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, and `/api/health` require a Bearer token:
`Authorization: Bearer <access_token>`

POST endpoints accept an `Idempotency-Key: <uuid>` header for replay safety.

Error Envelope format for all non-2xx responses:
```json
{
  "error": {
    "code": "VALIDATION_ERROR | UNAUTHENTICATED | FORBIDDEN | NOT_FOUND | CONFLICT | BUSINESS_RULE | RATE_LIMITED | INTERNAL",
    "message": "Human readable explanation",
    "fields": { "fieldName": "field error description" }
  }
}
```

---

## 1. Authentication & Household

### `POST /api/auth/register`
- **Request Body**:
  ```json
  {
    "name": "Ali",
    "email": "ali@example.com",
    "password": "password123",
    "invite_code": "LHR-4B-9872"
  }
  ```
  *(Note: The very first account registered becomes Admin `M01` and requires no `invite_code`)*
- **Response (201 Created)**:
  ```json
  {
    "user": { "id": "M01", "name": "Ali", "email": "ali@example.com", "role": "admin" },
    "access_token": "jwt...",
    "refresh_token": "hex...",
    "expires_in": 900
  }
  ```

### `POST /api/auth/login`
- **Request Body**:
  ```json
  { "email": "ali@example.com", "password": "password123" }
  ```
- **Response (200 OK)**:
  Same shape as register.

### `POST /api/auth/refresh`
- **Request Body**:
  ```json
  { "refresh_token": "hex..." }
  ```
- **Response (200 OK)**:
  New token pair with rotation and reuse detection.

### `POST /api/auth/logout`
- **Request Body**:
  ```json
  { "refresh_token": "hex..." }
  ```
- **Response (204 No Content)**

### `GET /api/auth/me`
- **Response (200 OK)**:
  ```json
  { "user": { "id": "M01", "name": "Ali", "email": "ali@example.com", "role": "admin" } }
  ```

### `POST /api/auth/password`
- **Request Body**:
  ```json
  { "current_password": "old...", "new_password": "new..." }
  ```
- **Response (204 No Content)**

### `GET /api/household`
- **Response (200 OK)**:
  ```json
  { "name": "Roommates Flat 4B, Lahore", "member_count": 3, "invite_code": "LHR-4B-9872" }
  ```

### `POST /api/household/invite/rotate` *(Admin only)*
- **Response (200 OK)**:
  ```json
  { "invite_code": "LHR-4B-1234" }
  ```

---

## 2. Business Endpoints

### `GET /api/members`
- **Response (200 OK)**:
  ```json
  [
    { "id": "M01", "name": "Ali" },
    { "id": "M02", "name": "Ayesha" },
    { "id": "M03", "name": "Bilal" }
  ]
  ```

### `GET /api/shopping?status=pending|bought`
- **Response (200 OK)**:
  ```json
  [
    { "id": "SHOP-001", "title": "Full Cream Milk", "unit": "pack", "requested_qty": 3, "status": "pending", "bought_at": null },
    { "id": "SHOP-004", "title": "Farm Fresh Eggs", "unit": "dozen", "requested_qty": 2, "status": "bought", "bought_at": 1730764800000 }
  ]
  ```

### `POST /api/shopping`
- **Request Body**:
  ```json
  { "title": "Cooking Oil 5L", "unit": "bottle", "requested_qty": 1 }
  ```
- **Response (201 Created)**: Shopping item object. Returns 409 if pending title exists.

### `POST /api/shopping/checkout`
- **Request Body**:
  ```json
  {
    "item_ids": ["SHOP-001", "SHOP-002"],
    "total_paid": 1200.00,
    "payer_id": "M01",
    "split_members": ["M01", "M02", "M03"],
    "tax_percent": 0.0,
    "discount_amount": 0.0
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "expense": {
      "id": "EXP-001",
      "title": "Groceries: Full Cream Milk, Super Basmati Rice",
      "category": "Groceries",
      "amount": 1200.00,
      "payer_id": "M01",
      "split_type": "EQUAL",
      "splits": [
        { "member_id": "M01", "amount": 400.00 },
        { "member_id": "M02", "amount": 400.00 },
        { "member_id": "M03", "amount": 400.00 }
      ]
    },
    "items": ["SHOP-001", "SHOP-002"]
  }
  ```

### `POST /api/expenses`
- **Request Body**:
  ```json
  {
    "title": "Apartment Cleaning Supplies",
    "category": "Cleaning",
    "amount": 300.00,
    "payer_id": "M01",
    "split_type": "CUSTOM",
    "splits": [
      { "member_id": "M01", "amount": 80.00 },
      { "member_id": "M02", "amount": 70.00 },
      { "member_id": "M03", "amount": 150.00 }
    ],
    "expense_date": "2026-11-03",
    "is_recurring": false
  }
  ```
- **Response (201 Created)**: Expense object with computed splits.

### `GET /api/expenses?month=YYYY-MM`
- **Response (200 OK)**: Array of expenses for month, newest first.

### `GET /api/balances`
- **Response (200 OK)**:
  ```json
  {
    "balances": [
      { "member_id": "M01", "name": "Ali", "paid": 1680.00, "owed": 1430.00, "sent": 0.00, "received": 0.00, "net": 250.00 },
      { "member_id": "M02", "name": "Ayesha", "paid": 0.00, "owed": 450.00, "sent": 0.00, "received": 0.00, "net": -450.00 },
      { "member_id": "M03", "name": "Bilal", "paid": 2520.00, "owed": 2320.00, "sent": 0.00, "received": 0.00, "net": 200.00 }
    ],
    "sum": 0.00
  }
  ```

### `POST /api/settle`
- **Request Body**:
  ```json
  { "sender_id": "M02", "recipient_id": "M01", "amount": 250.00 }
  ```
- **Response (201 Created)**:
  ```json
  {
    "settlement": { "id": "SETTLE-001", "sender_id": "M02", "recipient_id": "M01", "amount": 250.00, "recorded_by": "M02" },
    "balances": [ ... ]
  }
  ```

### `GET /api/balances/simplify` *(Surprise Option 1)*
- **Response (200 OK)**:
  ```json
  [
    { "from": "M02", "to": "M01", "amount": 250.00 },
    { "from": "M02", "to": "M03", "amount": 200.00 }
  ]
  ```

### `GET /api/budget?month=YYYY-MM`
- **Response (200 OK)**:
  ```json
  {
    "month": "2026-11",
    "monthly_budget": 5000.00,
    "total_spent": 4200.00,
    "remaining": 800.00,
    "percent_used": 84,
    "status": "NEAR_LIMIT"
  }
  ```

### `GET /api/budget/categories` *(Surprise Option 3)*
- **Response (200 OK)**:
  ```json
  [
    { "category": "Groceries", "total": 1200.00, "percent_of_budget": 24, "exceeds_threshold": false },
    { "category": "Utilities", "total": 2520.00, "percent_of_budget": 50.4, "exceeds_threshold": true },
    { "category": "Cleaning", "total": 300.00, "percent_of_budget": 6, "exceeds_threshold": false },
    { "category": "Dining", "total": 180.00, "percent_of_budget": 3.6, "exceeds_threshold": false }
  ]
  ```

### `POST /api/expenses/recurring/apply` *(Surprise Option 4)*
- **Response (200 OK)**:
  ```json
  { "created": 1, "skipped": 0 }
  ```

### `GET /api/health`
- **Response (200 OK)**:
  ```json
  { "status": "ok", "db": "connected" }
  ```

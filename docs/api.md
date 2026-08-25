# 🌐 FintechLedger — REST API Reference

All requests and responses use JSON (`Content-Type: application/json`).
Protected endpoints require a JWT Bearer token supplied in the `Authorization` HTTP header:
```http
Authorization: Bearer <jwt_access_token>
```

---

## 1. Authentication Endpoints

### Register User
`POST /api/v1/auth/register` (Public)

**Request Body:**
```json
{
  "email": "alice@fintech.com",
  "password": "Password123!",
  "firstName": "Alice",
  "lastName": "Smith"
}
```

**Response (201 Created):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "alice@fintech.com",
  "firstName": "Alice",
  "lastName": "Smith",
  "role": "ROLE_USER"
}
```

---

### Login User
`POST /api/v1/auth/login` (Public)

**Request Body:**
```json
{
  "email": "alice@fintech.com",
  "password": "Password123!"
}
```

---

## 2. Wallet Endpoints

### Create Wallet
`POST /api/v1/wallets` (Requires `ROLE_USER`)

**Request Body:**
```json
{
  "currency": "INR"
}
```

**Response (201 Created):**
```json
{
  "id": "e7b0a708-3a81-4475-ba7c-7d92f58e1bbf",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "currency": "INR",
  "balance": 0.0000,
  "status": "ACTIVE",
  "createdAt": "2026-08-26T00:00:00Z"
}
```

---

### Deposit Funds (Mock Inbound Gateway)
`POST /api/v1/wallets/{walletId}/deposit` (Requires `ROLE_USER`)

**Request Body:**
```json
{
  "amount": 1000.00,
  "currency": "INR"
}
```

---

### Get Balance
`GET /api/v1/wallets/{walletId}/balance` (Requires `ROLE_USER`)

**Response (200 OK):**
```json
{
  "walletId": "e7b0a708-3a81-4475-ba7c-7d92f58e1bbf",
  "currency": "INR",
  "balance": 1000.0000
}
```

---

## 3. Payment & Transfer Endpoints

### Transfer Funds (Idempotent)
`POST /api/v1/payments/transfer` (Requires `ROLE_USER`)

**Headers:**
```http
Idempotency-Key: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d
```

**Request Body:**
```json
{
  "senderWalletId": "e7b0a708-3a81-4475-ba7c-7d92f58e1bbf",
  "receiverWalletId": "4c3b318d-6d5b-4228-a477-8c3da985a9bc",
  "amount": 300.00,
  "currency": "INR"
}
```

**Response (200 OK):**
```json
{
  "id": "2d1f11c7-8cbb-46ae-83e9-0efb38e2194b",
  "referenceId": "PAY-A4E2-81D2-99FA",
  "senderWalletId": "e7b0a708-3a81-4475-ba7c-7d92f58e1bbf",
  "receiverWalletId": "4c3b318d-6d5b-4228-a477-8c3da985a9bc",
  "amount": 300.0000,
  "currency": "INR",
  "status": "COMPLETED",
  "createdAt": "2026-08-26T00:00:00Z",
  "completedAt": "2026-08-26T00:00:01Z"
}
```

---

## 4. RFC-7807 Standard Error Responses

When an error occurs, the server returns a structured **ProblemDetail** payload:

```json
{
  "type": "https://api.fintech.com/errors/insufficient-funds",
  "title": "Insufficient Funds",
  "status": 422,
  "detail": "Wallet e7b0a708 has insufficient balance (100.0000 INR) for requested debit of 500.0000 INR",
  "instance": "/api/v1/payments/transfer",
  "timestamp": "2026-08-26T00:00:00.123456Z",
  "traceId": "c89b76e1-9fa3-4c91"
}
```

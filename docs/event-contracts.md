# 📨 FintechLedger — Event Contracts & Kafka Streaming Specifications

This document defines the schema definitions, topics, partitioning keys, and consumer group contracts for all Apache Kafka events published by the **Transactional Outbox Publisher**.

---

## 1. Topic Topology & Partitioning Strategy

| Topic Name | Partitions | Key Strategy | Purpose |
| :--- | :---: | :--- | :--- |
| `fintech.payment.completed` | 3 | `paymentId` / `senderWalletId` | Emitted when a transfer is committed to the ledger |
| `fintech.payment.failed` | 3 | `paymentId` | Emitted when a payment fails validation or fund verification |
| `fintech.wallet.created` | 3 | `userId` | Emitted when a new multi-currency wallet is opened |
| `fintech.fraud.alert` | 3 | `walletId` | Emitted when high-velocity or anomaly triggers fire |

---

## 2. Event Envelope & Payload Schemas

All event payloads are wrapped in standard domain envelopes serialized in JSON.

### 2.1 `PaymentCompletedEvent`
**Topic**: `fintech.payment.completed`

```json
{
  "eventId": "e9b21f3a-84dc-438f-a2e6-6df798b3c0a1",
  "eventType": "PAYMENT_COMPLETED",
  "paymentId": "7b8f9e01-23a4-45b6-c7d8-9e0f1a2b3c4d",
  "referenceId": "PAY-A4E2-81D2-99FA",
  "senderWalletId": "e7b0a708-3a81-4475-ba7c-7d92f58e1bbf",
  "receiverWalletId": "4c3b318d-6d5b-4228-a477-8c3da985a9bc",
  "amount": 300.0000,
  "currency": "INR",
  "timestamp": "2026-08-26T00:00:01.450Z"
}
```

---

### 2.2 `PaymentFailedEvent`
**Topic**: `fintech.payment.failed`

```json
{
  "eventId": "f109283a-44dc-418f-a2e6-6df798b3c999",
  "eventType": "PAYMENT_FAILED",
  "paymentId": "8c9f0e12-34b5-46c7-d8e9-0f1a2b3c4d5e",
  "senderWalletId": "e7b0a708-3a81-4475-ba7c-7d92f58e1bbf",
  "receiverWalletId": "4c3b318d-6d5b-4228-a477-8c3da985a9bc",
  "amount": 50000.0000,
  "currency": "INR",
  "reason": "Insufficient funds in source wallet",
  "timestamp": "2026-08-26T00:00:01.600Z"
}
```

---

### 2.3 `WalletCreatedEvent`
**Topic**: `fintech.wallet.created`

```json
{
  "eventId": "d8a11f2a-73cc-427e-91d5-5ce687a2b990",
  "eventType": "WALLET_CREATED",
  "walletId": "e7b0a708-3a81-4475-ba7c-7d92f58e1bbf",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "currency": "INR",
  "initialBalance": 0.0000,
  "timestamp": "2026-08-26T00:00:00.120Z"
}
```

---

## 3. Consumer Groups & Idempotent Processing

Consumers are organized into isolated consumer groups to allow independent, parallel event streaming:
- `fintech-audit-group`: Ingests all events for regulatory compliance and audit logs.
- `fintech-fraud-group`: Inspects transaction velocity and anomaly patterns.
- `fintech-notification-group`: Dispatches customer SMS, push, or email receipts.

Consumers enforce idempotency using `eventId` deduping to ensure at-least-once deliveries produce strictly once side-effects.

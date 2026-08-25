# 🏛️ FintechLedger — System Architecture & Engineering Principles

This document provides a comprehensive technical breakdown of the architecture, concurrency design, transaction boundaries, idempotency mechanics, and outbox messaging model powering **FintechLedger**.

---

## 1. High-Level Architecture Overview

```
                         ┌────────────────────────┐
                         │   Client Application   │
                         │  (Web / Mobile / CLI)  │
                         └───────────┬────────────┘
                                     │ HTTPS
                                     ▼
                      ┌──────────────────────────────┐
                      │    Spring Security 6 Gateway │
                      │  - JWT Bearer Authentication │
                      │  - Role-Based Access Control │
                      │  - Token-Bucket Rate Limiter │
                      └──────────────┬───────────────┘
                                     │
                                     ▼
                      ┌──────────────────────────────┐
                      │   Wallet & Payment Engine    │
                      │  - Idempotency Interceptor   │
                      │  - Pessimistic Row Locking   │
                      │  - Double-Entry Bookkeeper   │
                      │  - Transactional Outbox Gen  │
                      └───────┬──────────────┬───────┘
                              │              │
                    ACID Commit              │ Background Poller (Scheduled)
                              │              │
                              ▼              ▼
                     ┌────────────────┐   ┌───────────────────────────┐
                     │   PostgreSQL   │   │ Outbox Publisher Service  │
                     │  - Wallets     │   └─────────────┬─────────────┘
                     │  - Ledger      │                 │
                     │  - Payments    │                 ▼
                     │  - Idempotency │          ┌─────────────┐
                     │  - Outbox Log  │          │ Apache Kafka│
                     └────────────────┘          └──────┬──────┘
                                                        │
                                    ┌───────────────────┼───────────────────┐
                                    ▼                   ▼                   ▼
                             ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
                             │ Audit Trail │     │ Fraud Guard │     │Notification │
                             │  Consumer   │     │  Consumer   │     │  Consumer   │
                             └─────────────┘     └─────────────┘     └─────────────┘
```

---

## 2. Core Architectural Guarantees

### 2.1 Double-Entry Bookkeeping Invariant

The engine enforces the fundamental accounting identity:

$$\sum \text{Debits} \equiv \sum \text{Credits}$$

For every financial movement between wallets, balanced ledger entries are atomically generated within the same database transaction:
- **Debit Entry**: Decreases source wallet balance.
- **Credit Entry**: Increases destination wallet balance.
- **Invariant Assertion**: The sum of all debits minus credits in the transaction must equal exactly zero:
  $$\text{net\_balance} = \sum (\text{debit} - \text{credit}) = 0$$

If this invariant is violated, the transaction is rejected and rolled back immediately.

---

### 2.2 Deadlock-Free Pessimistic Concurrency Control

High-volume transfer operations between concurrent threads present two major risks:
1. **Phantom Balance Overdrafts (Race Conditions)**: Two concurrent transfers reading balance before either commits.
2. **Deadlocks**: Thread A locks Wallet 1 then attempts to lock Wallet 2, while Thread B locks Wallet 2 then attempts to lock Wallet 1.

#### The Solution: Ordered Pessimistic Locking (`PESSIMISTIC_WRITE`)

```
Transfer(Wallet_A, Wallet_B)
         │
         ▼
Sort IDs deterministically:
[min(UUID_A, UUID_B), max(UUID_A, UUID_B)]
         │
         ▼
Acquire PESSIMISTIC_WRITE lock on First ID:
SELECT * FROM wallets WHERE id = :id1 FOR UPDATE;
         │
         ▼
Acquire PESSIMISTIC_WRITE lock on Second ID:
SELECT * FROM wallets WHERE id = :id2 FOR UPDATE;
         │
         ▼
Execute Balance Verification & Ledger Creation
         │
         ▼
Commit DB Transaction (Releases Locks)
```

By enforcing a strict canonical sorting order on wallet IDs before issuing `FOR UPDATE` queries, **deadlocks between cross-transfers are mathematically impossible**.

---

### 2.3 Idempotency Engine & Network Retry Safety

Clients communicating over HTTP can experience dropped network connections after the server has processed a charge but before the HTTP 200 response is acknowledged.

To guarantee safe retries without double-charging:

1. **Client Header**: Client provides `Idempotency-Key: <unique-uuid>`.
2. **Request Hashing**: A SHA-256 fingerprint of the request URI and payload is computed.
3. **Atomic Execution State**:
   - If the key is new: A lock is acquired with status `PROCESSING`.
   - If the key is already `COMPLETED` and the hash matches: The cached HTTP status and JSON response body are returned immediately without touching the ledger.
   - If the key exists with a different hash: An HTTP 422 Conflict is raised (`Idempotency conflict`).
   - If the key is currently `PROCESSING`: A 409 Conflict is returned to prevent concurrent duplicate execution.

---

### 2.4 Transactional Outbox Pattern

Direct dual-writes (committing to PostgreSQL and then publishing to Kafka) are inherently susceptible to partial failure (e.g. database commits, JVM crashes before Kafka message send).

**FintechLedger eliminates dual-writes with the Transactional Outbox Pattern**:
1. Within the database transaction, an `OutboxEvent` entity containing the serialized domain event payload is saved to the `outbox_events` table.
2. The transaction commits atomically.
3. An asynchronous poller (`OutboxPublisher`) selects pending events in chronological order with batching.
4. Events are published to Kafka with producer idempotence (`enable.idempotence=true`, `acks=all`).
5. On successful broker acknowledgment, the event status is marked as `PUBLISHED`.

---

### 2.5 Distributed Rate Limiting (Token Bucket)

Endpoints are protected by **Bucket4j** token-bucket rate limiters:
- **Payment Transfers**: 30 requests/minute per authenticated user.
- **Authentication**: 15 requests/minute per IP address.
- **General APIs**: 100 requests/minute per user.

When the bucket is exhausted, requests are short-circuited with HTTP 429 Too Many Requests and include a `Retry-After: <seconds>` header.

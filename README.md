# 💳 FintechLedger — Event-Driven Ledger & High-Throughput Wallet Engine

A production-grade, event-driven fintech ledger and wallet backend engineered with enterprise-level financial guarantees:

- **Double-Entry Bookkeeping**: Strict adherence to the accounting invariant $\sum \text{Debits} == \sum \text{Credits}$.
- **ACID Transaction Boundaries**: Absolute data integrity preventing phantom balance creation.
- **Pessimistic Concurrency Control**: Deadlock-free wallet locking for high-volume concurrent transfers.
- **Idempotent Payment API**: Protection against duplicate requests and network retries using unique `Idempotency-Key` tracking.
- **Transactional Outbox Pattern**: Guaranteed at-least-once message dispatch to Apache Kafka, eliminating dual-write anomalies.
- **Spring Security 6 & JWT**: Stateless role-based access control (RBAC).
- **Token-Bucket Rate Limiting**: Distributed rate-limiting powered by Bucket4j.
- **RFC-7807 Problem Details**: Standardized, machine-readable API error responses with distributed trace IDs.

---

## 🛠️ Technology Stack

| Component | Technology |
| :--- | :--- |
| **Language** | Java 17 LTS (Java 21 ready) |
| **Framework** | Spring Boot 3.3.3 |
| **Database** | PostgreSQL 16 |
| **Migrations** | Flyway |
| **ORM** | Spring Data JPA / Hibernate |
| **Message Broker** | Apache Kafka (KRaft mode) |
| **Security** | Spring Security 6 + JJWT |
| **Rate Limiting** | Bucket4j |
| **API Documentation** | OpenAPI 3.0 / Swagger UI (SpringDoc) |
| **Testing** | JUnit 5, Mockito, Testcontainers |
| **Containers** | Docker & Docker Compose |

---

## 🚀 Getting Started

### Prerequisites
- JDK 17+
- Maven 3.9+
- Docker & Docker Compose

### 1. Start Infrastructure
```bash
docker-compose up -d
```
This launches:
- **PostgreSQL**: `localhost:5432` (Database: `fintech_ledger`)
- **Apache Kafka**: `localhost:9092`
- **Kafka UI**: `http://localhost:8085`

### 2. Build & Run Application
```bash
mvn clean spring-boot:run
```

### 3. API Documentation
Once running, explore the interactive Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

---

## 🏗️ Architecture & Core Principles

```
Client (Web / Mobile / Curl)
           │
           ▼
    API Gateway / Security Filter (JWT + Rate Limiting)
           │
           ▼
    Wallet & Payment Service (Pessimistic Locking & Idempotency)
           │
     ┌─────┴─────┐
     ▼           ▼
PostgreSQL     Transactional Outbox
(ACID Ledger)    │
                 ▼
             Apache Kafka
                 │
     ┌───────────┼───────────┐
     ▼           ▼           ▼
Audit Log   Fraud Guard   Notifications
```

---

## 📄 License
Distributed under the Apache 2.0 License.

# 💳 FintechLedger — High-Throughput Financial Ledger & Wallet Engine

[![Java 17](https://img.shields.io/badge/Java-17%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.8.0-red.svg)](https://kafka.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

An enterprise-grade, event-driven fintech ledger and wallet backend engineered for high-concurrency transactional workloads, strict accounting invariants, and zero-loss message delivery.

---

## 🌟 Key Highlights & Guarantees

* **Double-Entry Bookkeeping**: Strict adherence to accounting integrity where every transaction comprises balanced debit and credit entries ($\sum \text{Debits} == \sum \text{Credits}$).
* **ACID Transaction Boundaries**: Atomic execution guarantees balance consistency with strict prevention of phantom credits and overdrafts.
* **Deadlock-Free Pessimistic Concurrency**: Deterministic row-level locking (`PESSIMISTIC_WRITE`) sorted by wallet IDs preventing circular deadlocks under high-throughput concurrent transfers.
* **Idempotent Payment Engine**: SHA-256 payload hashing combined with unique `Idempotency-Key` headers to safely handle network retries and deduplicate requests.
* **Transactional Outbox Pattern**: Guaranteed at-least-once asynchronous message dispatch to Apache Kafka without dual-write inconsistency risks.
* **Spring Security 6 & RBAC**: Stateless JSON Web Token (JWT) authentication supporting granular roles (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_AUDITOR`).
* **Distributed Token-Bucket Rate Limiting**: In-memory and API-level traffic shaping powered by Bucket4j.
* **RFC-7807 Standardized Errors**: Machine-readable error handling across all APIs with contextual trace IDs and field-level validation errors.
* **Full Observability Stack**: Pre-configured Prometheus metrics scraping, custom business metrics (`ledger_transactions_total`, `wallet_transfers_total`), and pre-built Grafana dashboards.

---

## 🏗️ System Architecture

```
                                  ┌───────────────────────────────┐
                                  │      Client Applications      │
                                  │   (Web, Mobile, Third-Party)  │
                                  └───────────────┬───────────────┘
                                                  │ HTTPS / REST
                                                  ▼
                        ┌──────────────────────────────────────────────────┐
                        │              Security & Gateway Layer            │
                        │  - JWT Bearer Authentication (Spring Security 6) │
                        │  - Bucket4j Token-Bucket Rate Limiting Filter    │
                        │  - RFC-7807 Problem Details Error Interceptor    │
                        └─────────────────────────┬────────────────────────┘
                                                  │
                                                  ▼
                        ┌──────────────────────────────────────────────────┐
                        │             FintechLedger Core Engine            │
                        │                                                  │
                        │  ┌────────────────────┐  ┌────────────────────┐  │
                        │  │ Idempotency Filter │  │  Wallet Controller │  │
                        │  │ (SHA-256 Checksum) │  │  & Ledger Service  │  │
                        │  └─────────┬──────────┘  └─────────┬──────────┘  │
                        │            │                       │             │
                        │            ▼                       ▼             │
                        │  ┌────────────────────────────────────────────┐  │
                        │  │ Deadlock-Free Canonical Locking Service    │  │
                        │  │ (min(UUID, UUID) -> max(UUID, UUID))       │  │
                        │  └─────────────────────┬──────────────────────┘  │
                        └────────────────────────┼─────────────────────────┘
                                                 │
                             ┌───────────────────┴───────────────────┐
                             │ Atomic DB Transaction Boundary (ACID) │
                             ▼                                       ▼
        ┌────────────────────────────────────────┐  ┌───────────────────────────────────┐
        │        PostgreSQL Database (16)        │  │       Transactional Outbox        │
        │ - Wallets (Balance & Version)          │  │ (Event: PENDING / PROCESSED)      │
        │ - Ledger Transactions & Entries        │  └─────────────────┬─────────────────┘
        │ - Idempotency Keys                     │                    │
        └────────────────────────────────────────┘                    │ Scheduled Dispatch
                                                                      ▼
                                                        ┌──────────────────────────┐
                                                        │   Apache Kafka Broker    │
                                                        │  - payment-events        │
                                                        │  - wallet-events         │
                                                        └─────────────┬────────────┘
                                                                      │
                                              ┌───────────────────────┼───────────────────────┐
                                              ▼                       ▼                       ▼
                                     ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
                                     │ Audit & History │     │   Fraud Engine  │     │  Notifications  │
                                     │    Consumers    │     │    Consumers    │     │    Consumers    │
                                     └─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

## 🛠️ Technology Stack

| Layer | Component | Version | Rationale |
| :--- | :--- | :--- | :--- |
| **Language** | Java | 17 LTS (Java 21 Ready) | Strong typing, high performance, enterprise support |
| **Framework** | Spring Boot | 3.3.3 | Modern cloud-native Java framework |
| **Database** | PostgreSQL | 16 | ACID compliance, JSONB support, robust row-level locking |
| **Migrations** | Flyway | 10.x | Version-controlled, idempotent schema migrations |
| **ORM & Data** | Spring Data JPA / Hibernate | 6.5.x | Object-relational mapping with Pessimistic Locking support |
| **Messaging** | Apache Kafka | 3.8.0 (KRaft) | High-throughput distributed event streaming |
| **Security** | Spring Security & JJWT | 6.3.x / 0.12.6 | Stateless JWT authentication and role-based authorization |
| **Rate Limiting**| Bucket4j | 8.10.1 | High-performance token-bucket rate limiting |
| **Observability**| Prometheus & Grafana | Latest | Real-time JVM and financial domain metrics visualization |
| **API Docs** | SpringDoc OpenAPI | 2.6.0 | Interactive OpenAPI 3.0 & Swagger UI documentation |
| **Testing** | JUnit 5, Mockito, Testcontainers | Latest | Unit, mock, and containerized integration test suite |
| **Packaging** | Docker & Docker Compose | Multi-stage | Reproducible, lightweight container deployment |

---

## ⚙️ Core Architectural Invariants & Patterns

### 1. Double-Entry Accounting Invariant
Money is never created or destroyed arbitrarily. For every transfer:
$$\sum \text{Debit Entries} == \sum \text{Credit Entries}$$
* A **Debit Entry** increases asset/expense accounts or decreases liability/equity accounts.
* A **Credit Entry** decreases asset/expense accounts or increases liability/equity accounts.

### 2. Deadlock Prevention Strategy
When transferring between Wallet A and Wallet B concurrently:
* Lock order is sorted lexicographically: `lock(min(idA, idB))` followed by `lock(max(idA, idB))`.
* Eliminates circular wait conditions across distributed worker threads.

### 3. Idempotent Execution Lifecycle
```
Incoming Request (Idempotency-Key: X)
  ├── 1. Check if Key X exists in DB
  │     ├── Found & COMPLETED  ──> Return Cached Response (HTTP 200)
  │     ├── Found & PROCESSING ──> Return 409 Conflict (Concurrent in-flight request)
  │     └── Found & Hash Mismatch ──> Return 422 Unprocessable Entity
  └── 2. Key Not Found ──> Insert Key as PROCESSING ──> Execute Transfer ──> Update Key to COMPLETED
```

### 4. Transactional Outbox Pattern
* Events are committed to the `outbox_events` database table inside the **same database transaction** as the financial state change.
* An asynchronous background poller dispatches events to Kafka and marks records as `PROCESSED`.
* Guarantees **at-least-once delivery** with zero risk of dual-write discrepancies.

---

## 📡 REST API Reference

### Authentication (`/api/v1/auth`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Public | Register a new user |
| `POST` | `/api/v1/auth/login` | Public | Authenticate user and receive JWT bearer token |

### Wallets (`/api/v1/wallets`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/wallets` | Authenticated | Create a multi-currency digital wallet (`USD`, `EUR`, `INR`, etc.) |
| `GET` | `/api/v1/wallets/{id}` | Authenticated | Retrieve wallet details and current balance |
| `POST` | `/api/v1/wallets/{id}/deposit` | Authenticated | Deposit funds into a wallet |
| `PUT` | `/api/v1/wallets/{id}/freeze` | `ROLE_ADMIN` | Freeze wallet operations |
| `PUT` | `/api/v1/wallets/{id}/unfreeze` | `ROLE_ADMIN` | Unfreeze wallet operations |

### Payments & Ledger (`/api/v1/payments`, `/api/v1/ledger`)
| Method | Endpoint | Header Required | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/payments/transfer` | `Idempotency-Key` | Execute atomic, double-entry cross-wallet transfer |
| `GET` | `/api/v1/ledger/transactions/{id}` | Authenticated | Query transaction details with balance-verified entries |
| `GET` | `/api/v1/ledger/wallets/{id}/history` | Authenticated | Retrieve paginated ledger audit trail for a wallet |

---

## 🚀 Quick Start (Docker Compose)

### 1. Clone & Run the Full Stack
```bash
git clone https://github.com/abhishek-s12/FintechLedger.git
cd FintechLedger
docker compose up -d
```

### 2. Infrastructure Services Overview
| Service | URL / Port | Credentials / Notes |
| :--- | :--- | :--- |
| **Fintech API** | `http://localhost:8080` | Core Spring Boot application |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` | Interactive API explorer |
| **PostgreSQL** | `localhost:5432` | DB: `fintech_ledger`, User: `fintech_user`, Pass: `fintech_pass` |
| **Apache Kafka** | `localhost:9092` | KRaft Broker |
| **Kafka UI** | `http://localhost:8085` | Topic, consumer group, and message inspector |
| **Prometheus** | `http://localhost:9090` | Time-series metrics engine |
| **Grafana** | `http://localhost:3000` | Dashboards (User: `admin`, Pass: `admin`) |

---

## 🎬 Automated End-to-End Live Demo

We provide automated test scripts that register users, create multi-currency wallets, simulate deposits, execute concurrent transfers, test idempotency deduplication, and assert audit ledger balance invariants.

### Linux / macOS:
```bash
chmod +x scripts/demo-flow.sh
./scripts/demo-flow.sh
```

### Windows (PowerShell):
```powershell
./scripts/demo-flow.ps1
```

---

## 🧪 Testing Suite

Execute the automated test suite across unit, slice, and integration layers:

```bash
# Run all unit and service tests
mvn clean test

# Package JAR file
mvn clean package -DskipTests
```

---

## 📁 Repository Structure

```
FintechLedger/
├── src/main/java/com/abhishek/fintech/
│   ├── auth/          # Authentication & user registration endpoints
│   ├── common/        # RFC-7807 problem details, global exceptions, utilities
│   ├── config/        # OpenAPI, Kafka, JPA, & security configurations
│   ├── idempotency/   # SHA-256 payload hashing & idempotency repository
│   ├── ledger/        # Double-entry ledger entities, services & controllers
│   ├── messaging/     # Kafka producers & consumer event contracts
│   ├── outbox/        # Transactional outbox polling & dispatch engine
│   ├── payment/       # Payment engine with sorted pessimistic row-locking
│   ├── ratelimit/     # Bucket4j token-bucket filter implementation
│   ├── security/      # Spring Security 6 filter chain & JWT token provider
│   ├── user/          # User & Role domain models
│   └── wallet/        # Wallet lifecycle, multi-currency & balance services
├── src/main/resources/
│   ├── db/migration/  # Flyway SQL schema migration scripts (V1 - V6)
│   └── application.yml
├── docs/              # In-depth architectural & API specifications
├── docker/            # Prometheus, Grafana, and Docker configuration files
├── scripts/           # Cross-platform demo and Kafka bootstrap scripts
├── docker-compose.yml
└── pom.xml
```

---

## 📚 Technical Documentation

Deep dive into the architecture and specifications:
* [System Architecture & Concurrency Guarantees](docs/architecture.md)
* [Database Schema & ER Models](docs/database.md)
* [REST API Reference & Curl Examples](docs/api.md)
* [Kafka Event Contracts & Outbox Topology](docs/event-contracts.md)

---

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

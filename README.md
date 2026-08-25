# 💳 FintechLedger — Event-Driven Ledger & High-Throughput Wallet Engine

[![Java 17](https://img.shields.io/badge/Java-17%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.8.0-red.svg)](https://kafka.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

A production-grade, event-driven fintech ledger and wallet backend engineered with enterprise financial guarantees:

- **Double-Entry Bookkeeping**: Strict adherence to the accounting invariant $\sum \text{Debits} == \sum \text{Credits}$.
- **ACID Transaction Boundaries**: Absolute data integrity preventing phantom balance creation or overdrafts.
- **Deadlock-Free Pessimistic Concurrency**: Canonical row-level locking (`PESSIMISTIC_WRITE`) for high-volume cross-wallet transfers.
- **Idempotent Payment API**: Protection against network retries and duplicate transactions using SHA-256 request-hashed `Idempotency-Key` tracking.
- **Transactional Outbox Pattern**: Guaranteed at-least-once message dispatch to Apache Kafka, eliminating dual-write anomalies.
- **Spring Security 6 & JWT**: Stateless role-based access control (RBAC: `ROLE_USER`, `ROLE_ADMIN`, `ROLE_AUDITOR`).
- **Token-Bucket Rate Limiting**: Distributed rate-limiting powered by Bucket4j.
- **RFC-7807 Problem Details**: Standardized, machine-readable API error responses with trace IDs.
- **Full Observability Stack**: Pre-configured Prometheus scraping and rich Grafana financial metrics dashboards.

---

## 🏗️ System Architecture

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
| **Monitoring** | Prometheus & Grafana |
| **API Documentation** | OpenAPI 3.0 / Swagger UI (SpringDoc) |
| **Testing** | JUnit 5, Mockito, Testcontainers |
| **Containers** | Docker & Docker Compose (Multi-stage) |

---

## 🚀 Quick Start (Complete Stack)

### Prerequisites
- Docker & Docker Compose
- JDK 17+ and Maven 3.9+ (for local builds)

### 1. Launch All Services via Docker Compose
```bash
docker compose up -d
```

This spins up the entire production infrastructure:
- **Fintech API**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **Kafka UI**: `http://localhost:8085`
- **Prometheus**: `http://localhost:9090`
- **Grafana**: `http://localhost:3000` (User: `admin` / Password: `admin`)
- **PostgreSQL**: `localhost:5432` (Database: `fintech_ledger`)
- **Apache Kafka**: `localhost:9092`

---

### 2. Run Interactive Live Demo

Execute the cross-platform end-to-end demo script (registers users, creates wallets, deposits funds, executes double-entry transfers, tests idempotency retries, and tests error handling):

**Linux / macOS (Bash):**
```bash
chmod +x scripts/demo-flow.sh
./scripts/demo-flow.sh
```

**Windows (PowerShell):**
```powershell
./scripts/demo-flow.ps1
```

---

## 🧪 Testing

Run the full automated test suite (Unit, Controller, Service, and Integration tests):

```bash
mvn clean test
```

---

## 📚 Technical Documentation

Explore the comprehensive technical specification documents:

- [System Architecture & Guarantees](docs/architecture.md)
- [Database Schema & ER Models](docs/database.md)
- [REST API Reference & Curl Examples](docs/api.md)
- [Event Contracts & Kafka Topology](docs/event-contracts.md)

---

## 📄 License
Distributed under the Apache 2.0 License.

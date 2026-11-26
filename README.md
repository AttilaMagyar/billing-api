# Billing API

RESTful invoicing and financial ledger backend built with **Java**, **Spring Boot**, **Flyway**, and **PostgreSQL**, focusing on transactional integrity, invoice lifecycle rules, and **idempotent payment processing**.

## Key Features
- **Customer Management:** Create and retrieve customers with unique email validation.
- **Invoice Lifecycle:** Create invoices with calculated line items (`BigDecimal` precision), issue `DRAFT` invoices (`ISSUED`), cancel eligible invoices (`CANCELLED`), and filter by status (`DRAFT`, `ISSUED`, `PAID`, `OVERDUE`, `CANCELLED`).
- **Idempotent Payments:** `POST /api/v1/invoices/{id}/payments` requires an `Idempotency-Key` header so retried requests do not double-charge or corrupt paid balances; automatically transitions invoices to `PAID` when settled in full.
- **Database Migrations:** Schema versioned with Flyway (`V1__init_schema.sql`) and validated by JPA (`ddl-auto: validate`).
- **Centralized Error Handling:** Consistent JSON error responses for `400 Bad Request` (validation), `404 Not Found`, and `409 Conflict` (business rule violations).

## Tech Stack
- Java 21+ / Spring Boot (Web, Data JPA, Validation, Flyway, Actuator, Lombok)
- PostgreSQL 17 (via Docker Compose)
- JUnit 5 & Mockito
- GitHub Actions CI

## Running Locally

1. Start PostgreSQL with `docker compose up -d`.
2. Run the application with `./mvnw spring-boot:run` (available at `http://localhost:8080`).
3. Run tests with `./mvnw test`.

## Example Endpoints
- `POST /api/v1/customers` — Create a customer
- `GET /api/v1/customers` — List customers
- `POST /api/v1/invoices` — Create an invoice with items
- `GET /api/v1/invoices?status=ISSUED` — List/filter invoices
- `POST /api/v1/invoices/{id}/issue` — Issue a draft invoice
- `POST /api/v1/invoices/{id}/cancel` — Cancel an unpaid/unissued invoice
- `POST /api/v1/invoices/{id}/payments` — Record a payment (requires `Idempotency-Key` header)

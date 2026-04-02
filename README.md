# Real Estate EMI Payment Tracking System

A production-ready Spring Boot 3.3+ REST API for managing real estate EMI payment deals. Designed for admin-only access, it handles customer management, deal creation with automated EMI schedule generation, FIFO payment application, and dashboard analytics.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Security | Spring Security + JWT (JJWT 0.11.5) |
| Persistence | Spring Data JPA + PostgreSQL 16 |
| Validation | Jakarta Bean Validation |
| Mapping | MapStruct 1.5.5 |
| Documentation | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |
| Containerization | Docker / Docker Compose |

---

## Prerequisites

- Java 21
- Maven 3.9+
- Docker and Docker Compose

---

## How to Run

### 1. Start PostgreSQL with Docker

```bash
docker-compose up -d
```

This starts a PostgreSQL 16 instance at `localhost:5432` with:
- Database: `realestate_emi_db`
- Username: `postgres`
- Password: `postgres`

### 2. Build the Application

```bash
mvn clean package -DskipTests
```

### 3. Run the Application

```bash
java -jar target/realestate-emi-tracker-1.0.0.jar
```

Or run with Maven:

```bash
mvn spring-boot:run
```

The application starts on **port 8090**.

### 4. Environment Variable Overrides

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `JWT_SECRET` | (long default key) | JWT signing secret |

---

## Default Credentials

On first startup, the system seeds a default admin user:

| Field | Value |
|---|---|
| Username | `admin` |
| Password | `admin123` |

**Change these credentials in production.**

---

## Swagger UI

Once the application is running, access the interactive API documentation at:

```
http://localhost:8090/swagger-ui.html
```

OpenAPI JSON spec:
```
http://localhost:8090/api-docs
```

---

## API Overview

### Authentication
| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/login` | Login and receive JWT token |

### Property Types
| Method | Path | Description |
|---|---|---|
| GET | `/api/property-types` | List all property types |
| POST | `/api/property-types` | Create a property type |
| PUT | `/api/property-types/{id}` | Update a property type |
| DELETE | `/api/property-types/{id}` | Delete a property type |

### Customers
| Method | Path | Description |
|---|---|---|
| GET | `/api/customers` | List all customers |
| GET | `/api/customers/{id}` | Get customer by ID |
| POST | `/api/customers` | Create a customer |
| PUT | `/api/customers/{id}` | Update a customer |

### Deals
| Method | Path | Description |
|---|---|---|
| POST | `/api/deals` | Create a deal (auto-generates EMI schedule) |
| GET | `/api/deals` | List all deals with summary |
| GET | `/api/deals/{id}` | Get deal details with schedules and payments |
| PUT | `/api/deals/{id}/status` | Change deal status |

### Payments
| Method | Path | Description |
|---|---|---|
| POST | `/api/deals/{dealId}/payments` | Record a payment (FIFO applied to EMI schedule) |
| GET | `/api/deals/{dealId}/payments` | List payments for a deal |

### Dashboard
| Method | Path | Description |
|---|---|---|
| GET | `/api/dashboard/summary` | Get overview: deal counts, amounts, outstanding |

---

## Business Logic Highlights

- **EMI Calculation**: Uses standard reducing-balance formula when interest rate > 0, simple division otherwise.
- **FIFO Payment Application**: Payments are applied to the oldest pending/partial EMI schedules first.
- **Auto-Completion**: A deal is automatically marked `COMPLETED` when all EMI schedules are `PAID`.
- **Initial Deposit**: Recorded as a separate `INITIAL_DEPOSIT` payment; reduces `totalPayableAfterDeposit`.
- **EMI Tenure Validation**: Only 6, 12, 24, 48, or 60 months are accepted.

---

## Supported Payment Methods

`CASH`, `BANK_TRANSFER`, `UPI`, `CHEQUE`, `OTHER`

> `INITIAL_DEPOSIT` is reserved for system use only and cannot be submitted via the payment API.

---

## Logging

Each request is tagged with a `X-Correlation-ID` header (auto-generated if not provided). Structured logs include correlation ID, user ID, and log level.

Pattern:
```
2024-01-15 10:30:00 [abc-123] [42] INFO  c.r.e.c.DealController - → POST /api/deals [admin]
```

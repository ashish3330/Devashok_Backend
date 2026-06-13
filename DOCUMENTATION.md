# DevAshok Platform — End-to-End Documentation

> Multi-tenant SaaS for real estate construction-linked installment management **plus** a full society/MyGate-style residents experience.
> Backend repo: this directory (`realestate-emi-tracker`). Three frontends live in `~/Documents/`.

Generated: 2026-06-13 — verified against the live boot of all four services on this machine.

---

## 1. System Overview

DevAshok is a **single multi-tenant Spring Boot backend** that powers three separate frontends, each targeting a different user persona:

```
                ┌──────────────────────────────────────────────────────────┐
                │  PostgreSQL 16   (single DB, every row carries org_id)   │
                └──────────────────────────────────────────────────────────┘
                                          ▲
                                          │  JPA / Hibernate
                ┌──────────────────────────────────────────────────────────┐
                │  Spring Boot 3.3.5 Backend  ::  port 8090                │
                │  - JWT auth (admin user/pass  +  resident phone OTP)     │
                │  - REST API  /api/...                                    │
                │  - 2 cron schedulers (overdue interest, bounce charges)  │
                │  - Twilio SMS, OpenPDF, Apache POI (Excel)               │
                └──────────────────────────────────────────────────────────┘
                          ▲                ▲                       ▲
                          │                │                       │
              ┌───────────┴────┐  ┌────────┴───────────┐  ┌────────┴───────────┐
              │ devashok-admin │  │ devashok-resident- │  │ devashok-resident- │
              │   Vite + React │  │  web (Vite+React)  │  │ app (Expo, RN 0.81)│
              │   port 5173    │  │   port 5174        │  │ npm run ios/android│
              │  ADMIN / SUP / │  │ RESIDENT, OTP login │  │ RESIDENT, OTP login│
              │  VIEWER staff  │  │                     │  │                    │
              └────────────────┘  └─────────────────────┘  └────────────────────┘
```

### 1.1 Tenancy model
- One `organizations` row per builder/society (currently seeded: **DEVASHOK** Navi Mumbai, **COUNTY** Pune).
- **Every** business entity has an `organization_id` FK (User, Deal, Customer, Staff, Material, Supplier, Block, Flat, Resident, Notice, Complaint…).
- JWT payload carries `organizationId`. `TenantContext` extracts it from `SecurityContextHolder` and every service appends it to repository queries (`*ByOrg` methods).
- `findById/update/delete` validate org ownership — **mismatch returns 404, not 403** (so cross-org IDs cannot be enumerated).
- Login per-org: `/login/DEVASHOK`, `/login/COUNTY`. The branding header (org name + RERA) is fetched from a public endpoint:
  `GET /api/organizations/public/{code}` (no auth).
- `installment_plan_templates` is the **one** shared table (the 9-phase construction schedule is platform-wide).

### 1.2 Two product surfaces, one backend
The backend is split conceptually into:

| Surface | Purpose | Frontends |
|---|---|---|
| **Real-estate management** | Builder back office: deals, customers, construction phases, payments, staff, attendance, payroll, suppliers, inventory, purchase orders, dashboards | `devashok-admin` only |
| **Society / MyGate** | Resident-facing community ops: blocks, flats, residents, visitors, complaints, notices, maintenance bills, amenities, daily help, documents, emergency SOS, vehicles, family members, notifications | `devashok-admin` (society section) + `devashok-resident-web` + `devashok-resident-app` |

---

## 2. Backend (Spring Boot)

**Path:** `/Users/ashish/Downloads/realestate-emi-tracker`
**Build:** Gradle (`build.gradle`). The README still says Maven — ignore that, **only `gradlew` works**.
**Port:** `8090`

### 2.1 Stack

| Layer | Choice |
|---|---|
| Language | Java 21 (compiles fine on JDK 25 too — verified) |
| Framework | Spring Boot 3.3.5 |
| Persistence | Spring Data JPA + Hibernate 6.5, PostgreSQL 16 |
| Security | Spring Security 6 + JJWT 0.11.5 (HS256) |
| Mapping | MapStruct 1.5.5 (Spring component model) |
| Boilerplate | Lombok 1.18.34 |
| Validation | Jakarta Bean Validation |
| API docs | SpringDoc OpenAPI 2.5.0 → Swagger UI |
| SMS | Twilio 10.1.0 (falls back to console log if not configured) |
| PDF | OpenPDF 1.3.30 |
| Excel | Apache POI 5.2.5 |

### 2.2 Source layout

```
src/main/java/com/realestate/emi/
├── RealestateEmiTrackerApplication.java   # @SpringBootApplication + @EnableScheduling, seeds DEVASHOK+COUNTY orgs + admin/countyadmin users
├── config/                                # SecurityConfig, OpenApiConfig, WebConfig (CORS), JpaAuditingConfig
├── controller/                            # 45 REST controllers (one per resource — see §2.5)
├── dto/{request,response}/                # All request / response DTOs (separated)
├── entity/                                # 47 JPA entities — see §2.4
├── enums/                                 # 27 enums (DealStatus, EmiStatus, PhaseStatus, Role, ResidentType, …)
├── exception/                             # Global @ControllerAdvice + custom exceptions
├── filter/                                # CorrelationIdFilter, JwtAuthFilter, RequestResponseLoggingFilter
├── mapper/                                # MapStruct mappers (entity ↔ DTO)
├── repository/                            # 43 Spring Data repositories (all org-scoped *ByOrg methods)
├── scheduler/                             # OverdueInterestScheduler, BounceChargeScheduler
├── security/                              # JwtService, JwtTokenValidator, CustomPrincipal, CustomUserDetailsService, TenantContext
├── service/                               # 40 services — business logic lives here
├── specification/                         # JPA Specification builders for filtered list endpoints (date ranges, etc.)
└── util/                                  # Helpers (PDF, Excel, formatting)
```

### 2.3 Configuration (`src/main/resources/application.yml`)

| Setting | Default | Env override |
|---|---|---|
| DB URL | `jdbc:postgresql://localhost:5432/realestate_emi_db` | — |
| DB user/pass | `postgres` / *(empty)* | `DB_USERNAME`, `DB_PASSWORD` |
| `ddl-auto` | `update` (Hibernate auto-migrates schema) | — |
| JWT secret | long inline default | `JWT_SECRET` |
| JWT expiration | 24h (`86400000` ms) | — |
| Server port | `8090` | `SERVER_PORT` |
| Twilio creds | placeholder | `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER` |
| Bounce cron | `0 0 0 * * *` (midnight) | `scheduler.bounce.cron` |
| Overdue interest cron | `0 0 8 * * *` (8 am) | `scheduler.overdue-interest.cron` |

A `data.sql` runs after Hibernate creates tables: it seeds the two organizations and the **9 fixed construction phases** (`Registration` → `DPC` → `First Floor Slab` → … → `Possession`, totalling 100%).

### 2.4 Entities (47)

**Real-estate / construction (16):**
`Organization`, `User`, `Customer`, `PropertyType`, `Deal`, `EmiSchedule`, `Payment`, `InstallmentPlanTemplate`, `InstallmentPhase`, `Material`, `Supplier`, `SupplierPayment`, `StockTransaction`, `StockAlert`, `PurchaseOrder`, `PurchaseOrderItem`.

**Staff / payroll (6):**
`Staff`, `StaffRole`, `Attendance`, `Holiday`, `SalaryRecord`, `SalaryAdvance`.

**Society foundation (5):**
`Block`, `Flat`, `Resident`, `ResidentUser`, `ResidentOtp`, `ResidentPhone`.

**Society v1 — MyGate features (10):**
`Visitor`, `Complaint`, `ComplaintCategory`, `Notice`, `MaintenanceBill`, `MaintenancePayment`, `Amenity`, `AmenityBooking`, `DailyHelp`, `DailyHelpAssignment`, `DailyHelpAttendance`, `ResidentDocument`.

**Society v2 (most recent commits):**
`EmergencyAlert`, `Vehicle`, `FamilyMember`, `Notification`, `ResidentNotification`, plus *frequent visitors* (extension of `Visitor`).

**Auditing:** `BaseAuditEntity` provides `createdAt`, `updatedAt`, `createdBy`, `updatedBy` to most entities.

### 2.5 Controllers / API surface (45)

Auth & org
- `AuthController` — `POST /api/auth/login` (staff: username/password)
- `ResidentAuthController` — `/api/resident/auth/{request-otp, verify-otp, refresh}` (resident: phone+OTP)
- `OrganizationController` — incl. `GET /api/organizations/public/{code}` (no auth, for login-page branding)

Real-estate core
- `CustomerController`, `PropertyTypeController`
- `DealController` — create deal (auto-generates 9 construction phases), list, change status
- `InstallmentPhaseController` — phase progression (Token paid → Phase 1 activates → admin activates next when previous PAID)
- `InstallmentPlanTemplateController` — read-only shared phase definitions
- `PaymentController` — FIFO payment application (CASH / BANK_TRANSFER / UPI / CHEQUE / OTHER; `INITIAL_DEPOSIT` system-only)
- `DashboardController` — `/api/dashboard/summary` (deal counts, amounts, outstanding)

Staff & payroll
- `StaffController`, `StaffRoleController` (per-org name uniqueness — same role name allowed across orgs)
- `AttendanceController` — admin marks for everyone incl. supervisors; supervisor marks regular staff; no self-marking
- `HolidayController`
- `SalaryController` — auto-generated from attendance, OT @ 1.5×
- `SalaryAdvanceController`

Inventory & suppliers
- `MaterialController`, `SupplierController` (M:N via `materialIds`)
- `StockTransactionController` — IN / OUT / ADJUSTMENT; rolls into `StockAlert`
- `PurchaseOrderController` — PO lifecycle with line items
- `SupplierPaymentController`
- `InventoryReportController` — Excel/PDF exports

Society — admin side
- `BlockController`, `FlatController`, `ResidentManagementController`
- `VisitorController` (admin/guard view of visitor logs)
- `ComplaintController`, `ComplaintCategoryController`
- `NoticeController`
- `MaintenanceBillController`
- `AmenityController`, `AmenityBookingController`
- `DailyHelpController`
- `EmergencyAlertController`
- `UserController` — staff user CRUD

Society — resident side (require resident JWT)
- `ResidentAuthController` — phone-OTP login
- `ResidentVisitorController` — request entry, frequent visitors, share QR
- `ResidentComplaintController`
- `ResidentNoticeController`
- `ResidentMaintenanceController` — view bills + pay
- `ResidentAmenityController` — browse + book
- `ResidentDailyHelpController` — assign maids/cooks/drivers to flat
- `ResidentDocumentController` (admin uploads) + `ResidentDocumentResidentController` (resident downloads)
- `ResidentEmergencyController` — fire SOS alert
- `ResidentVehicleController`
- `ResidentFamilyMemberController`
- `ResidentNotificationController` — fanned notifications inbox

### 2.6 Security

`SecurityConfig.java` — stateless JWT, BCrypt(12), method-level `@PreAuthorize` enabled.

**Public endpoints (no auth):**
```
/api/auth/login
/api/resident/auth/request-otp
/api/resident/auth/verify-otp
/api/resident/auth/refresh
/api/organizations/public/**
/api-docs/**, /swagger-ui/**, /swagger-ui.html, /v3/api-docs/**
/actuator/health
```

**Roles:**
- `ADMIN` — full access, can't delete own account
- `SUPERVISOR` — inventory + suppliers + attendance only
- `VIEWER` — read-only dashboard / deals / customers
- `RESIDENT` — society-side endpoints scoped to their `flatId`/`residentId` JWT claims

`TenantContext` exposes: `getCurrentOrganizationId`, `getCurrentUserId`, `getCurrentUsername`, `getCurrentResidentId`, `getCurrentFlatId`.

Error envelope is uniform `ApiResponse<T>` (success flag, message, data, error code). Auth failures return JSON `401` with `code=UNAUTHORIZED`; authorization failures `403` with `code=ACCESS_DENIED`.

### 2.7 Schedulers (2)

| Scheduler | Cron | Behaviour |
|---|---|---|
| `OverdueInterestScheduler` | `0 0 8 * * *` (8 am daily) | Finds DUE/PARTIAL phases past their 15-day deadline → marks `OVERDUE`, applies **10% p.a.** interest computed on **total deal outstanding** (not just the phase). Also logs phases due today/tomorrow/within 3 days and triggers `NotificationService.generateNotifications()`. |
| `BounceChargeScheduler` | `0 0 0 * * *` (midnight) | For every overdue, not-yet-bounced EMI: applies a **10% p.a.** bounce charge on **total deal outstanding × daysOverdue/365**. Both schedulers were recently updated to bill on the *whole* deal outstanding, not the single phase — see commit `eaa1cfb`. |

### 2.8 Key business rules

- **Construction-linked installments**: 9 fixed phases, no tenure/EMI. Sequential activation: previous must be `PAID` before next can be activated. Customer has **15 days** after a phase is marked complete to pay. After that → `OVERDUE` + interest.
- **Phase amount** = `(Total Price − Token Amount) × Phase %`, rounded to nearest rupee.
- **FIFO payment application** — payments hit the oldest pending/partial EMI/phase first. Deal auto-marked `COMPLETED` when all schedules are `PAID`.
- **Initial deposit** is recorded as a separate `INITIAL_DEPOSIT` payment that reduces `totalPayableAfterDeposit`.
- **EMI tenure** (legacy reducing-balance path) only accepts 6 / 12 / 24 / 48 / 60 months.
- **Per-org uniqueness** for staff phone/aadhar/PAN and staff-role names — same role/name can exist in multiple orgs.
- **Org isolation** is enforced everywhere — including uniqueness checks; salary budget no longer leaks across orgs.

### 2.9 Cross-cutting filters
- `CorrelationIdFilter` — injects/propagates `X-Correlation-ID` header into MDC.
- `JwtAuthFilter` — parses Bearer token, populates `CustomPrincipal` (userId, organizationId, role, residentId, flatId).
- `RequestResponseLoggingFilter` — structured request/response logs tagged with correlation + user id.

Log pattern:
```
2026-06-13 12:40:23 [abc-123] [42] INFO  c.r.e.c.DealController - → POST /api/deals [admin]
```

---

## 3. Frontends

### 3.1 `devashok-admin` (Vite + React 18 + TypeScript) — port **5173**

**Path:** `/Users/ashish/Documents/devashok-admin`

**Stack:** Vite 6, React 18.3, React Router 7, TanStack Query 5, MUI 7 + `@mui/x-data-grid-pro`, Tailwind 4, Radix UI primitives, Recharts + Chart.js, framer-motion, react-hook-form + yup/zod, jsPDF + jspdf-autotable, axios, sonner toasts, lucide + heroicons + FontAwesome + fluentui icons.

**Pages (split by surface):**

```
src/pages/
├── realestate/
│   ├── LoginPage.tsx          # /login/:orgCode — pulls org branding from public endpoint
│   ├── DashboardPage.tsx      # dark hero, charts, KPIs
│   ├── RegisterPage.tsx       # admin user signup (also creates org)
│   ├── CustomersPage.tsx
│   ├── DealsPage.tsx
│   ├── DealDetailPage.tsx     # construction phases timeline, payments, deal docs
│   ├── PropertyTypesPage.tsx
│   ├── StaffPage.tsx
│   ├── AttendancePage.tsx
│   ├── SalaryPage.tsx
│   ├── SuppliersPage.tsx
│   ├── InventoryPage.tsx
│   ├── UserManagementPage.tsx
│   ├── AnalyticsPage.tsx
│   └── NotFoundPage.tsx
└── society/
    ├── BlocksPage.tsx
    ├── FlatsPage.tsx
    ├── ResidentsPage.tsx
    ├── SocietyVisitorsPage.tsx
    ├── SocietyComplaintsPage.tsx
    ├── SocietyNoticesPage.tsx
    ├── SocietyMaintenancePage.tsx
    ├── SocietyAmenitiesPage.tsx
    ├── SocietyDailyHelpPage.tsx
    ├── SocietyDocumentsPage.tsx
    └── SocietyEmergencyPage.tsx
```

**API services:** `src/services/realestate.ts` and `src/services/society.ts` (axios instances, token-aware interceptors).

**UI conventions enforced in this codebase:**
- Dark hero only on Dashboard + Login. Every other page uses a light gradient header (`bg-gradient-to-r from-white via-slate-50/80 to-white`).
- `CustomSelect` dropdowns use `createPortal` to `document.body` to escape table/modal `overflow:hidden`.
- Sidebar collapse button hidden on mobile; topbar hamburger opens an overlay sidebar.
- Logo has a `variant="light"` mode for white-background pages.
- Footer pinned to viewport bottom via `min-height: 100vh` content wrapper.

### 3.2 `devashok-resident-web` (Vite + React 19 + TypeScript) — port **5174**

**Path:** `/Users/ashish/Documents/devashok-resident-web`

**Stack:** Vite 8, React 19, React Router 6, TanStack Query 5, Tailwind 4, framer-motion, qrcode.react, react-hook-form + yup, sonner.

**Pages:**

```
src/pages/
├── LoginPage.tsx              # phone → OTP
├── HomePage.tsx               # quick actions, today's status
├── VisitorsPage.tsx           # pre-approve, frequent, share QR
├── ComplaintsPage.tsx
├── NoticesPage.tsx
├── MaintenancePage.tsx        # bills + pay
├── AmenitiesPage.tsx          # browse + book
├── DailyHelpPage.tsx          # maids/cooks/drivers assigned to flat
├── DocumentsPage.tsx          # documents shared by admin
├── EmergencyPage.tsx          # SOS button + history
├── VehiclesPage.tsx
├── FamilyPage.tsx
├── ProfilePage.tsx
├── NotificationsPage.tsx
├── MorePage.tsx               # secondary menu
└── NotFoundPage.tsx
```

### 3.3 `devashok-resident-app` (Expo SDK 54, React Native 0.81, expo-router)

**Path:** `/Users/ashish/Documents/devashok-resident-app`
**Run:** `npm run ios` / `npm run android` / `expo start`

**File-based routing (expo-router):**

```
app/
├── (auth)/
│   ├── welcome.tsx
│   ├── phone.tsx
│   └── otp.tsx
├── (tabs)/
│   ├── home.tsx
│   ├── maintenance.tsx
│   ├── visitors.tsx
│   └── more.tsx
├── visitors/new.tsx
├── amenities.tsx
├── complaints.tsx
├── daily-help.tsx
├── documents.tsx
├── notices.tsx
├── profile.tsx
└── index.tsx
```

Secure token storage via `expo-secure-store`. Shares the same backend OTP login flow as the resident web app.

---

## 4. Run book

### 4.1 What's currently running on this machine (verified 12:40 today)

```
postgres   :5432   (local install, db `realestate_emi_db` exists)
java       :8090   (Spring Boot — `Started RealestateEmiTrackerApplication in 6.294s`)
node       :5173   (devashok-admin — VITE v6.4.1)
node       :5174   (devashok-resident-web — VITE v8.0.10)
```

Health check: `curl -X POST http://localhost:8090/api/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123","organizationCode":"DEVASHOK"}'` returns a JWT — confirmed.

### 4.2 Cold-start from scratch

```bash
# 1. Database (use the local install, or Docker)
docker compose up -d postgres          # spins up postgres:16-alpine on :5432
# or use local postgres (already configured here)

# 2. Backend
cd ~/Downloads/realestate-emi-tracker
./gradlew bootJar -x test
DB_USERNAME=postgres DB_PASSWORD=postgres \
  java -jar build/libs/realestate-emi-tracker-1.0.0.jar
# → http://localhost:8090
# → Swagger UI: http://localhost:8090/swagger-ui.html
# → OpenAPI JSON: http://localhost:8090/api-docs

# 3. Admin UI
cd ~/Documents/devashok-admin
npm install                            # first time only
npm run dev                            # → http://localhost:5173/login/DEVASHOK

# 4. Resident web
cd ~/Documents/devashok-resident-web
npm install                            # first time only
npm run dev                            # → http://localhost:5174

# 5. Resident mobile (optional)
cd ~/Documents/devashok-resident-app
npm install                            # first time only
npm run ios                            # or: npm run android | expo start
```

### 4.3 Default credentials

| Role | Username | Password | Org |
|---|---|---|---|
| Admin (DEVASHOK) | `admin` | `admin123` | DEVASHOK |
| Admin (COUNTY) | `countyadmin` | `admin123` | COUNTY |
| Resident | any seeded resident phone | OTP printed to backend logs (Twilio off in dev) | per-flat |

**Change `admin123` and `JWT_SECRET` before any deploy.**

### 4.4 Common operations

```bash
# Tail backend logs
tail -f /tmp/emi-backend.log

# Reset DB (nuclear)
docker compose down -v && docker compose up -d postgres
# or: psql -U postgres -c 'DROP DATABASE realestate_emi_db' && createdb -U postgres realestate_emi_db

# Hit a protected endpoint
TOKEN=$(curl -s -X POST http://localhost:8090/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123","organizationCode":"DEVASHOK"}' \
  | jq -r .data.token)
curl -s http://localhost:8090/api/dashboard/summary -H "Authorization: Bearer $TOKEN" | jq
```

### 4.5 Production deploy (single-VPS Docker Compose)

`docker-compose.prod.yml` defines three services on a single VPS:
- `postgres` (port 5432 bound to `127.0.0.1:5433`)
- `backend` (port 8090 bound to `127.0.0.1:8090`)
- `frontend` (`devashok-admin` built and served by Nginx on `:3000`)

Env file: `.env.prod` (DB creds + `JWT_SECRET` — change before deploy).
One-command deploy: `./deploy.sh`.
URL after deploy: `http://VPS_IP:3000/login/DEVASHOK`.

Resident web + resident app are **not** in `docker-compose.prod.yml` yet (no remote configured, local-only at the moment).

---

## 5. Repos & deployment

| Component | Repo | Branches |
|---|---|---|
| Backend | https://github.com/ashish3330/Devashok_Backend | `main`, `dev` |
| Admin frontend | https://github.com/ashish3330/Devashok_FE | `main`, `dev` |
| Resident web | (no remote yet — local only) | — |
| Resident mobile | (no remote yet — local only) | — |

Push via HTTPS (SSH key not set up on this machine).

---

## 6. Known gotchas (codified from prior incidents)

1. **MapStruct `@AfterMapping` doesn't fire for list mappers** → use `@Mapping(target=..., expression="java(...)")` for computed fields like `balanceDue`, `stockValue`.
2. **`deal.emiSchedules` can be `null`** (not `[]`) for construction-only deals. Every API array → `?? []` in TypeScript.
3. **Field-name parity**: BE DTO and FE TS interface must match exactly. Past breakage: `roleId` vs `staffRoleId`, `phoneNumber` vs `phone`, `bankDetails` vs `bankAccountNumber` on StaffPage.
4. **Per-org uniqueness only** for staff phone/aadhar/PAN/email and staff-role names. Global uniqueness blocked a second org from creating common roles like `MASON`.
5. **Cross-org access returns 404, not 403** (so IDs aren't enumerable).
6. **Admin can't delete themselves.**
7. **The README's "use Maven" line is stale** — only `gradlew` works (no `pom.xml`).
8. **Two cron schedulers bill on `total deal outstanding`**, not just the single overdue phase/EMI — changed in commit `eaa1cfb`.

---

## 7. Where things live (quick reference)

| Want to… | Look at |
|---|---|
| Add a new society feature | new entity + repo + service + controller + DTO pair + mapper, then add page in `devashok-admin/src/pages/society/` and a resident page + service |
| Change interest/bounce % | `OverdueInterestScheduler.INTEREST_RATE_ANNUAL`, `BounceChargeScheduler.BOUNCE_PENALTY_RATE` |
| Change construction phases | `src/main/resources/data.sql` (template) — note phases are shared across orgs |
| Tweak login branding | `OrganizationController#getPublic` + `LoginPage.tsx` (both admin + resident) |
| Add a new role | `enums/Role.java` + `SecurityConfig` matchers + `@PreAuthorize` on controllers |
| Add a new public route | matchers list in `SecurityConfig.securityFilterChain` |
| Postman collection | `RealEstate-EMI-Tracker.postman_collection.json` in repo root |

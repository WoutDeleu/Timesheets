# Timesheets — Local Work Hours Tracking System

## Overview

A local-first timesheet application for a **consultant** to track billable hours across client projects, manage leave balances, and generate configurable reports. The app runs entirely on the local machine — no cloud hosting, no external dependencies at runtime.

**Tech Stack:** Java 21, Spring Boot, Thymeleaf, Bootstrap 5, H2 (file-based), Maven

---

## Core Concepts

### Single User
No authentication required. The app opens and the data is yours. One user, one dataset.

### Projects
- Multiple projects can be created (e.g., client engagements, internal work)
- Each project has a name, optional description, and optional daily hour target
- Projects can be active or archived
- Distinguish between **client projects** (billable) and **internal projects** (non-billable)

### Work Schedule
- **Global default:** 7.6 hours/day (38h/week — Belgian standard full-time)
- **Per-project override:** individual projects can define a different daily target (e.g., 8h, 4h for part-time)
- Work week is Monday–Friday by default

---

## Time Entry

### Daily Form
- Select a date, pick a project, enter hours worked (total hours or start/end time)
- One entry per project per day
- Quick entry for common patterns

### Weekly Grid
- Calendar-style Monday–Friday grid view
- Fill in hours per project per day
- Bulk editing capability
- Visual overview of the week at a glance

---

## Leave & Absence Tracking

### Leave Types
| Type | Description |
|------|-------------|
| **Vacation (Verlof)** | Legal vacation days. 20 days/year, resets January 1 |
| **ADV (Arbeidsduurvermindering)** | Compensatory days earned from working 8h instead of 7.6h. Auto-calculated |
| **Sick Leave (Ziekteverlof)** | Sick days. Tracked for reporting |
| **Belgian National Holidays** | Auto-populated per year (including movable holidays like Easter Monday). Editable — can add/remove company-specific days |
| **Training / Education** | Conferences, certifications, courses |
| **Compensatory Rest** | Rest days taken to offset overtime |
| **Unpaid Leave** | Unpaid days off |
| **Other / Custom** | User-defined leave type with custom label |

### Belgian National Holidays (auto-populated)
- New Year's Day (1 Jan)
- Easter Monday (movable)
- Labour Day (1 May)
- Ascension Day (movable, Easter + 39 days)
- Whit Monday (movable, Easter + 50 days)
- Belgian National Day (21 Jul)
- Assumption of Mary (15 Aug)
- All Saints' Day (1 Nov)
- Armistice Day (11 Nov)
- Christmas Day (25 Dec)

Users can add company-specific closure days (e.g., bridge days, company events).

---

## Saldi & Balances

### Holiday Balance (Verlof Saldo)
- Starts at **20 days** on January 1 each year
- Decremented when vacation days are taken
- Dashboard shows: used / remaining / planned
- Prediction: "At current rate, you will run out by [date]" or "You have X days remaining for the rest of the year"

### ADV Balance (ADV Saldo)
- **Auto-calculated** from actual workdays
- Each day worked at 8h (instead of 7.6h) contributes **0.4h** to the ADV pot
- When the pot accumulates **7.6h**, one ADV day is earned
- Taking an ADV day consumes one earned day
- Sick days and holidays (7.6h days) do **not** contribute to ADV accumulation
- Dashboard shows: earned / used / available

### Overtime Balance (per project)
- When hours worked on a project exceed the project's daily target, the surplus is overtime
- Overtime accumulates as a **running saldo per project**
- Can be "spent" by working fewer hours on that project on another day (compensatory rest)
- Dashboard shows: accumulated overtime per project

---

## Reports & Export

### Monthly Summary
- Daily breakdown per project: date, hours worked, leave type, overtime
- Totals: total hours, overtime, leave days by type
- Running saldi at month end

### Yearly Overview
- Monthly totals aggregated
- Saldi evolution over the year (graph/table)
- Holiday balance trajectory
- ADV accumulation over time

### Configurable Export
- **Format:** CSV and PDF
- **Date range:** custom start/end date
- **Filter by:** specific projects, all projects
- **Data selection:** choose which columns/data to include
- Suitable for sharing with HR or client invoicing

---

## Technical Requirements

### Architecture
- **Backend:** Spring Boot (Java 21) — REST-ish controllers serving Thymeleaf views
- **Frontend:** Thymeleaf templates with Bootstrap 5 for responsive UI
- **Database:** H2 in file mode — persistent across restarts, stored locally
- **Build:** Maven with Maven Wrapper (`./mvnw`)

### Launcher
- Shell script (`start.sh`) that:
  1. Builds the project (if needed)
  2. Starts the Spring Boot application
  3. Auto-opens the default browser to `http://localhost:8080`
- Also runnable via `./mvnw spring-boot:run` for development
- Executable JAR for production: `java -jar timesheets.jar`

### Data Persistence
- H2 database stored in `~/.timesheets/data/` (outside project directory)
- Data survives application restarts
- Schema managed via Flyway migrations

### Developer Experience
- No IDE dependency — fully manageable from terminal/vim
- Clean Maven project structure
- Hot-reload in dev mode (`spring-boot-devtools`)

---

## UI Structure (Pages)

1. **Dashboard** — overview of current saldi (holiday, ADV, overtime per project), quick stats
2. **Time Entry (Daily)** — form to log hours for a specific date
3. **Time Entry (Weekly)** — grid view for the current/selected week
4. **Projects** — CRUD for projects, set daily targets, archive
5. **Leave Management** — book leave days, view leave calendar
6. **Holidays** — view/edit Belgian national holidays and company days
7. **Reports** — monthly/yearly summaries with export options
8. **Settings** — global defaults (daily hours, work week), data management

---

## Data Model (High-Level)

### Project
- id, name, description, dailyHourTarget (optional), billable (boolean), active (boolean)

### TimeEntry
- id, date, projectId, hoursWorked, startTime (optional), endTime (optional), notes (optional)

### LeaveEntry
- id, date, leaveType (enum), hours (default 7.6), notes (optional)

### Holiday
- id, date, name, type (NATIONAL / COMPANY), editable (boolean)

### Settings
- id, key, value (global configuration key-value store)

### SaldoSnapshot (for tracking balance evolution)
- id, date, type (HOLIDAY / ADV / OVERTIME), projectId (nullable), balance

---

## Non-Functional Requirements

- **Performance:** Instant page loads — this is a local app with minimal data
- **Reliability:** Data must never be lost. H2 file-based storage with proper shutdown hooks
- **Usability:** Clean, functional UI. Not flashy — just efficient for daily use
- **Maintainability:** Clean code, clear package structure, Flyway migrations for schema changes
- **Portability:** Runs on macOS (primary), should also work on Linux. Java 21 is the only prerequisite

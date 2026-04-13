# Timesheets

A local-first timesheet app for consultants. Track billable hours across client projects, manage leave and ADV balances, and export reports — all on your own machine, no cloud required.

## Features

- **Time tracking** — log hours per project per day, with work location (office/home)
- **Projects** — multiple projects with individual daily hour targets, billable/non-billable flag
- **Leave management** — vacation, ADV, sick leave, training, and custom types
- **Balances** — auto-calculated ADV saldo, holiday balance, overtime per project
- **Belgian holidays** — auto-populated national holidays (including movable Easter-based days), editable per year
- **Reports** — monthly and yearly summaries, overtime overview
- **Export** — CSV and PDF export with configurable date range and filters
- **Clock sessions** — clock in/out to auto-fill daily time entries

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.x |
| Frontend | Thymeleaf, Bootstrap 5, HTMX |
| Database | H2 (file-based, stored at `~/.timesheets/data/`) |
| Migrations | Flyway |
| Build | Maven Wrapper (`./mvnw`) |

## Getting started

**Prerequisites:** Java 21

```bash
# Clone and run
git clone https://github.com/WoutDeleu/Timesheets.git
cd Timesheets
./start.sh        # builds, starts, and opens the browser
```

Or run manually:

```bash
./mvnw spring-boot:run   # dev mode with hot-reload
```

The app is available at [http://localhost:8080](http://localhost:8080).

## Key business rules

### ADV (Arbeidsduurvermindering)
- Standard workday is 7.6h (38h/week Belgian full-time)
- Working 8h earns 0.4h in the ADV pot
- Every 7.6h accumulated → 1 ADV day earned (i.e., after 19 days at 8h)
- Sick days and leave days do not contribute

### Overtime
- Overtime is per-project: hours beyond the project's daily target
- Accumulates as a running saldo; can be offset by shorter days on the same project

### Holiday balance
- 20 vacation days per year, resets January 1
- Belgian national holidays auto-calculated using the Computus algorithm for Easter

## Database

Data is stored in `~/.timesheets/data/timesheets.mv.db`. Schema is managed via Flyway — migrations run automatically on startup.

The H2 console is available at `/h2-console` in dev mode:
- JDBC URL: `jdbc:h2:file:~/.timesheets/data/timesheets`

## Development

```bash
./mvnw clean install          # build with tests
./mvnw test                   # run tests only
```

Project structure:

```
src/main/java/io/timesheets/
  config/       Spring configuration
  controller/   Thymeleaf controllers (one per page)
  model/        JPA entities
  repository/   Spring Data JPA interfaces
  service/      Business logic (saldo calculations, reports)
  dto/          Java records for data transfer
  util/         Belgian holiday calculator, formatting helpers
```

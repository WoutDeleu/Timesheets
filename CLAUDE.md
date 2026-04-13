# Timesheets

## Project Overview
Local-first timesheet app for an Axxes consultant. Tracks billable hours across client projects, manages leave/ADV/overtime balances, generates exportable reports. Single user, no auth.

See `PROJECT_DESCRIPTION.md` for full requirements.

## Tech Stack
- **Language:** Java 21 (use modern features: records, pattern matching, text blocks, sealed classes where appropriate)
- **Framework:** Spring Boot 3.x
- **Frontend:** Thymeleaf + Bootstrap 5 + HTMX (for dynamic partial updates without full-page reloads)
- **Database:** H2 file-based (stored at `~/.timesheets/data/timesheets`)
- **Migrations:** Flyway
- **Build:** Maven with Maven Wrapper
- **Testing:** JUnit 5, Mockito, Spring Boot Test

## Project Structure
```
src/
  main/
    java/be/axxes/timesheets/
      TimesheetsApplication.java
      config/           # Spring configuration, H2 setup
      controller/       # Thymeleaf controllers (one per page/feature)
      model/            # JPA entities
      repository/       # Spring Data JPA repositories
      service/          # Business logic (saldo calculations, holiday engine, reports)
      dto/              # Data transfer objects (use Java records)
      util/             # Belgian holidays calculator, date utilities
    resources/
      templates/        # Thymeleaf HTML templates
        fragments/      # Reusable template fragments (nav, footer, modals)
        layout/         # Base layout template
      static/
        css/            # Custom CSS (minimal — Bootstrap handles most)
        js/             # Custom JS (minimal — HTMX handles interactions)
      db/migration/     # Flyway SQL migrations (V1__*, V2__*, ...)
      application.yml   # Spring Boot config
  test/
    java/be/axxes/timesheets/
      controller/       # Controller tests
      service/          # Service unit tests
      repository/       # Repository integration tests
```

## Coding Conventions

### Java
- Use **records** for DTOs and value objects
- Use **`var`** for local variables when the type is obvious from context
- Follow standard Java naming: `camelCase` methods/fields, `PascalCase` classes
- Keep controllers thin — business logic goes in services
- Use `@Transactional` at the service layer
- Prefer constructor injection (no `@Autowired` on fields)
- No Lombok — use records and IDE/manual generation instead

### Database
- Flyway migrations are the source of truth for schema
- Migration files: `V{number}__{description}.sql` (double underscore)
- Never modify existing migrations — always create new ones
- Use `snake_case` for table and column names

### Templates
- Use Thymeleaf layout dialect for consistent page structure
- Fragment reuse for navigation, forms, modals
- Keep JavaScript minimal — prefer HTMX for dynamic behavior
- All dates displayed in `dd/MM/yyyy` format (Belgian convention)
- All monetary/hour values use comma as decimal separator in display

### Testing
- Test services thoroughly (saldo calculations are critical)
- Controller tests via `@WebMvcTest`
- Repository tests via `@DataJpaTest`
- Name tests descriptively: `shouldCalculateAdvDaysFromEightHourWorkdays()`

## Key Business Rules (reference for implementation)

### ADV Calculation
- Standard workday = 7.6h. When 8h is worked, 0.4h surplus goes to ADV pot
- 1 ADV day is earned per 7.6h accumulated surplus (i.e., after 19 workdays at 8h)
- Sick days and leave days do NOT contribute to ADV
- Only actual 8h workdays count

### Overtime
- Per-project: hours exceeding the project's daily target are overtime
- Overtime accumulates as a running balance (saldo) per project
- Can be offset by working fewer hours on another day

### Holiday Balance
- 20 vacation days per year, resets January 1
- Prediction: remaining days / remaining workdays in year

### Belgian Holidays
- 10 national holidays per year (3 movable based on Easter)
- Auto-calculated using Computus algorithm for Easter
- Users can add/remove company-specific days

## Commands

### Build & Run
```bash
./mvnw clean install          # Build with tests
./mvnw spring-boot:run        # Run in dev mode (hot-reload)
./start.sh                    # Build + run + open browser
```

### Testing
```bash
./mvnw test                   # Run all tests
./mvnw test -pl :timesheets -Dtest=ClassName  # Run specific test class
```

### Database
- H2 console available at `/h2-console` in dev mode
- JDBC URL: `jdbc:h2:file:~/.timesheets/data/timesheets`

## Parallel Development Workflow

Multiple Claude Code windows can work on different features simultaneously using git worktrees. Each window gets an isolated copy of the repo on its own branch, avoiding conflicts during development.

### Starting a feature
Use `/start-feature <name>` (e.g., `/start-feature feature/add-reporting`). This creates an isolated worktree and branch so your work does not interfere with other windows.

### Finishing a feature
Use `/finish-feature` to merge the current feature branch back into main. This will:
1. Commit any remaining changes
2. Run tests
3. Merge into main (resolving conflicts if needed)
4. Run tests again post-merge
5. Clean up the worktree

### Rules for parallel work
- **Always use worktrees** — never work directly on `main` when implementing features
- **Run tests before merging** — ensure the feature works in isolation first
- **Resolve conflicts carefully** — when merging, understand both sides before resolving
- **One merge at a time** — if two features finish simultaneously, merge them sequentially to keep conflict resolution manageable

## Important Notes
- This is a LOCAL app — no cloud, no external APIs, no internet required at runtime
- Single user — no authentication, no multi-tenancy
- Data persistence is critical — never lose timesheet data
- Belgian locale: dates in dd/MM/yyyy, comma decimal separator in display
- The app should be fully functional from a terminal workflow (vim-friendly)

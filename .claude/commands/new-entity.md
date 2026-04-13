---
description: Scaffold a new JPA entity with repository, service, and controller
allowed-tools: Read, Write, Glob, Grep, Bash
---

Scaffold a complete feature for entity: $ARGUMENTS

1. Read `CLAUDE.md` and `PROJECT_DESCRIPTION.md` for conventions and data model reference
2. Create the following files following project conventions:
   - **Entity** in `src/main/java/io/timesheets/model/{EntityName}.java` — JPA entity with proper annotations
   - **Repository** in `src/main/java/io/timesheets/repository/{EntityName}Repository.java` — Spring Data JPA interface
   - **Service** in `src/main/java/io/timesheets/service/{EntityName}Service.java` — Business logic with constructor injection
   - **Controller** in `src/main/java/io/timesheets/controller/{EntityName}Controller.java` — Thymeleaf controller with CRUD endpoints
   - **DTO** in `src/main/java/io/timesheets/dto/{EntityName}Dto.java` — Java record for data transfer
   - **Flyway migration** — SQL to create the table (use `/new-migration` conventions)
   - **Thymeleaf template** in `src/main/resources/templates/{entity-name}/list.html` — List view
   - **Thymeleaf template** in `src/main/resources/templates/{entity-name}/form.html` — Create/edit form

3. Follow all conventions from CLAUDE.md:
   - Records for DTOs
   - Constructor injection
   - @Transactional on service methods
   - snake_case DB columns
   - Bootstrap 5 templates with layout dialect

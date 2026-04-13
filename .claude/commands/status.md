---
description: Show project status — what's implemented, what's pending
allowed-tools: Read, Glob, Grep, Bash
---

Give a project status overview:

1. Check which entities/models exist in `src/main/java/io/timesheets/model/`
2. Check which controllers exist and what endpoints they serve
3. Check which Thymeleaf templates exist
4. Check which Flyway migrations exist
5. Check test coverage: which services/controllers have tests
6. Compare against `PROJECT_DESCRIPTION.md` requirements
7. Report:
   - Features implemented (with checkmarks)
   - Features partially done
   - Features not started
   - Next recommended feature to build

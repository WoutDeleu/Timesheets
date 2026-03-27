---
description: Review recent changes for code quality, conventions, and bugs
allowed-tools: Bash, Read, Glob, Grep
---

Review the current uncommitted changes (or the last commit if everything is committed):

1. Run `git diff` (or `git diff HEAD~1` if clean) to see what changed
2. Check against CLAUDE.md conventions:
   - Records used for DTOs?
   - Constructor injection (no field @Autowired)?
   - Services have @Transactional?
   - Flyway migrations not modified (only new ones)?
   - snake_case in SQL?
   - Belgian date format (dd/MM/yyyy) in templates?
3. Look for:
   - Missing null checks at system boundaries
   - SQL injection risks
   - Missing validation on user input
   - Broken Thymeleaf expressions
   - Incorrect saldo/ADV calculation logic
4. Report findings as a concise checklist: what's good, what needs fixing

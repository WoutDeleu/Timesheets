---
name: Timesheets Project Context
description: Local timesheet app for Axxes consultant — Spring Boot, Thymeleaf, H2, Belgian labor rules
type: project
---

Building a **local-first timesheet application** from scratch (greenfield).

**Key decisions made:**
- Java 21 + Spring Boot + Thymeleaf + Bootstrap 5 + H2 file-based
- Single user, no authentication
- ADV days auto-calculated from 8h workdays (0.4h surplus per day -> 1 ADV day per 7.6h accumulated)
- Overtime tracked per-project as accumulating saldo
- 20 vacation days/year, Jan 1 reset
- Belgian national holidays auto-populated + editable
- Full leave types: vacation, ADV, sick, training, compensatory rest, unpaid, custom
- Reports: monthly + yearly + configurable CSV/PDF export
- Two time entry views: daily form + weekly grid

**Why:** User needs to track consultant hours across client engagements and manage Belgian labor balances (holiday, ADV, overtime).

**How to apply:** All features should follow Belgian labor conventions. Dates in dd/MM/yyyy. The app must work offline and be manageable from the terminal.

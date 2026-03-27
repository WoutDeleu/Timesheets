---
description: Verify saldo calculation logic against Belgian labor rules
allowed-tools: Read, Grep, Glob
---

Audit all saldo/balance calculation logic in the codebase:

1. Find all service classes related to saldo calculations (ADV, holiday, overtime)
2. Verify ADV calculation:
   - Only 8h workdays contribute 0.4h to the ADV pot
   - 1 ADV day = 7.6h accumulated surplus
   - Sick days and leave days do NOT contribute
3. Verify holiday balance:
   - 20 days/year, resets Jan 1
   - Properly decremented when vacation is taken
4. Verify overtime:
   - Per-project calculation
   - Only hours exceeding project daily target count as overtime
5. Verify Belgian national holidays:
   - All 10 holidays present
   - Easter-based holidays correctly calculated (Computus algorithm)
6. Report any discrepancies or edge cases not handled

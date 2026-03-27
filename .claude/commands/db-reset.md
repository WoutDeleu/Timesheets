---
description: Reset the local H2 database (deletes all data)
---

WARNING: This will delete all timesheet data!

1. Ask the user to confirm they want to reset the database
2. If confirmed:
   - Stop the running application if active
   - Delete the H2 database files at `~/.timesheets/data/`
   - Tell the user the database will be recreated with Flyway migrations on next startup

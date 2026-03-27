---
description: Create a new Flyway migration file with the next version number
allowed-tools: Bash, Glob, Write, Read
---

Create a new Flyway SQL migration file:

1. Check `src/main/resources/db/migration/` for existing migration files
2. Determine the next version number (V1, V2, V3, ...)
3. Ask the user what the migration should do (if not provided as $ARGUMENTS)
4. Create the file as `V{next}__{description}.sql` (double underscore, snake_case description)
5. Write the SQL DDL/DML based on the user's request
6. Follow the existing schema conventions: `snake_case` table/column names, proper constraints and foreign keys

If user provided arguments, use them as the migration description: $ARGUMENTS

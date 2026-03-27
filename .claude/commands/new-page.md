---
description: Create a new Thymeleaf page with controller endpoint
allowed-tools: Read, Write, Glob, Grep
---

Create a new page: $ARGUMENTS

1. Read existing templates in `src/main/resources/templates/` to match the layout pattern
2. Read existing controllers to match conventions
3. Create:
   - **Thymeleaf template** using the base layout, Bootstrap 5, with proper fragment structure
   - **Controller method** in the appropriate controller (or new controller if needed)
4. Add navigation link to the shared nav fragment
5. Use HTMX for any dynamic behavior instead of custom JavaScript

Start working on a new feature in an isolated worktree.

## Instructions

You are about to start work on a new feature. Follow these steps exactly:

1. **Create a worktree** using `EnterWorktree` with the feature name provided by the user (e.g., `feature/add-reporting`). If no name is given, ask for one.

2. **Confirm** to the user:
   - The worktree path
   - The branch name
   - That they are now isolated from main and other parallel Claude windows

3. **Proceed** with the feature implementation as requested.

All work will happen on the isolated branch inside the worktree. Other Claude windows working on different features will not interfere.

Usage: /start-feature <feature-name>

Feature to work on: $ARGUMENTS

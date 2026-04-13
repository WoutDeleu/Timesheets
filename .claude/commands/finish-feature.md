Finish the current feature branch and merge it back into main.

## Instructions

You are finishing work on a feature branch. Follow these steps exactly:

1. **Verify all work is committed** — run `git status` to check for uncommitted changes. If there are any, commit them first.

2. **Run tests** to make sure the feature works:
   ```bash
   ./mvnw test
   ```
   If tests fail, fix them before proceeding.

3. **Switch to main and pull latest**:
   ```bash
   git checkout main
   ```

4. **Merge the feature branch into main**:
   ```bash
   git merge <feature-branch-name>
   ```

5. **If there are merge conflicts**, resolve them:
   - Read each conflicted file
   - Understand both sides of the conflict
   - Resolve by keeping the correct combination of changes
   - Stage the resolved files with `git add`
   - Complete the merge with `git commit`

6. **Run tests again** after the merge to ensure nothing is broken:
   ```bash
   ./mvnw test
   ```

7. **Clean up** — exit the worktree using `ExitWorktree` with action `remove`.

8. **Report** the result to the user:
   - Which branch was merged
   - Whether there were conflicts and how they were resolved
   - Test results after merge

If $ARGUMENTS is provided, use it as additional context. Otherwise, operate on the current branch.

Additional context: $ARGUMENTS

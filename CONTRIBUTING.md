# Contributing to Compiler Group Four

This document describes how all group members contribute to the project through GitHub.

## Workflow

All feature work goes through branches and pull requests. Do not push directly to `main`.

### Step by step

1. **Update your local `main` branch.**

   ```
   git checkout main
   git pull origin main
   ```

2. **Create a branch for your task.**

   ```
   git checkout -b scanner/tokenize-identifiers
   ```

3. **Implement your change.** Keep it focused on one task. Add or update tests for the code you changed.

4. **Review your own changes and run the tests.**

   ```
   mvn test
   git diff
   ```

5. **Commit with a meaningful message.**

   ```
   git add .
   git commit -m "scanner: tokenize identifiers and keywords"
   ```

6. **Push your branch to GitHub.**

   ```
   git push origin scanner/tokenize-identifiers
   ```

7. **Open a pull request** targeting `main` on GitHub.

8. **Another member reviews** the pull request. The reviewer should check correctness, test coverage, readability, and that the build passes.

9. **Address feedback** by pushing additional commits to the same branch.

10. **Merge** the pull request after approval and passing checks. Use "Squash and merge" or "Merge commit" consistently as a group.

11. **Update your local `main`** before starting another task.

    ```
    git checkout main
    git pull origin main
    ```

## Branch Naming

Use the pattern `phase/short-description` or `fix/short-description`:

- `scanner/tokenize-numbers`
- `parser/expression-grammar`
- `semantic/type-checking`
- `codegen/emit-arithmetic`
- `optimizer/constant-folding`
- `fix/scanner-off-by-one`
- `docs/update-readme`

Keep names lowercase with hyphens separating words.

## Commit Messages

Write clear, specific messages. Start with the phase or area, then describe what changed:

- `scanner: handle single-line comments`
- `parser: add precedence for binary operators`
- `tests: add edge cases for string literal scanning`
- `docs: add Phase 1 design notes`
- `fix: correct token position tracking for multi-line input`

Avoid vague messages like "update code", "fix bug", or "work in progress".

## Pull Request Guidelines

- Give the PR a descriptive title matching the commit message style.
- Describe what the change does and why, briefly.
- Keep each PR focused. One task or feature per PR is easier to review.
- Make sure `mvn test` passes before requesting review.
- Respond to review feedback by pushing new commits to the same branch.

## Handling Merge Conflicts

If your branch conflicts with `main`:

```
git checkout main
git pull origin main
git checkout your-branch
git merge main
```

Resolve the conflicts in your editor, then:

```
git add .
git commit -m "merge main into your-branch, resolve conflicts"
git push origin your-branch
```

Alternatively, use rebase if the group prefers:

```
git checkout your-branch
git rebase main
# resolve conflicts, then:
git rebase --continue
git push --force-with-lease origin your-branch
```

Pick one approach (merge or rebase) and use it consistently.

## Keeping Contributions Reviewable

- Work on one logical change at a time. If a feature is large, break it into smaller PRs.
- Avoid mixing unrelated changes (for example, reformatting code in a feature branch).
- Pull from `main` frequently to reduce the size of merges.

## Expectations

Every member is expected to contribute implementation code, tests, documentation, and reviews throughout the project. 
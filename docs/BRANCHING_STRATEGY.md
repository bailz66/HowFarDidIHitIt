# Branching Strategy: GitLab Flow
> GitHub Issue: #18

## Overview

This project uses **GitLab Flow** — a simplified branching model that combines feature branches with environment-based release branches. It provides the structure needed for production releases without the overhead of full Git Flow.

## Branch Diagram

```
feature/shot-tracking ──┐
feature/weather-api ────┤
feature/analytics ──────┤
                        ▼
                     master ──────────────────────────────────►
                        │                    │
                        ▼                    ▼
                  release/1.0.0        release/1.1.0
                     │    │               │
                     ▼    ▼               ▼
                  v1.0.0  v1.0.1       v1.1.0
```

## Branch Types

### `master` (Main Development Branch)
- Always contains the latest **completed** features
- All feature branches merge into `master`
- CI runs on every push and PR
- Must pass all tests and lint checks before merging
- This is the "source of truth" for the project

### `feature/*` (Feature Branches)
- Created from `master` for each new feature or task
- Naming: `feature/<short-description>` (e.g., `feature/shot-tracking`)
- Short-lived — merge back to `master` as soon as the feature is complete
- One developer per feature branch (no shared feature branches)
- Squash-merged to keep `master` history clean

### `bugfix/*` (Bug Fix Branches)
- Created from `master` for non-critical fixes
- Naming: `bugfix/<short-description>` (e.g., `bugfix/gps-calibration-outliers`)
- Same workflow as feature branches

### `release/X.Y.Z` (Release Branches)
- Created from `master` when preparing a new release
- Naming: `release/<semver>` (e.g., `release/1.0.0`)
- Only **bug fixes and release prep** are committed to release branches
- No new features on release branches
- Tagged when ready for production: `v1.0.0`, `v1.0.1`, etc.

### `hotfix/*` (Hotfix Branches)
- Created from the **release branch** that needs the fix
- Naming: `hotfix/<short-description>` (e.g., `hotfix/crash-on-launch`)
- Merged back into the release branch AND `master`
- Used only for critical production bugs

## Workflows

### Feature Development
```bash
# 1. Create feature branch from master
git checkout master
git pull origin master
git checkout -b feature/my-feature

# 2. Develop, commit, push
git add .
git commit -m "Add my feature"
git push -u origin feature/my-feature

# 3. Open PR to master
gh pr create --base master --title "Add my feature"

# 4. After CI passes and review, squash-merge
# 5. Delete feature branch
```

### Creating a Release
```bash
# 1. Create release branch from master
git checkout master
git pull origin master
git checkout -b release/1.0.0

# 2. Bump version in build.gradle.kts
# versionCode = 1, versionName = "1.0.0"

# 3. Commit and push
git commit -am "Prepare release 1.0.0"
git push -u origin release/1.0.0

# 4. After final testing, tag the release
git tag v1.0.0
git push origin v1.0.0
# → Triggers release.yml workflow → GitHub Release + signed AAB
```

### Hotfix Workflow
```bash
# 1. Create hotfix branch from release
git checkout release/1.0.0
git checkout -b hotfix/critical-crash

# 2. Fix, commit, push
git commit -m "Fix critical crash on launch"
git push -u origin hotfix/critical-crash

# 3. Merge into release branch
gh pr create --base release/1.0.0 --title "Hotfix: critical crash"

# 4. Tag new patch release
git checkout release/1.0.0
git pull
git tag v1.0.1
git push origin v1.0.1

# 5. Cherry-pick or merge fix back into master
git checkout master
git cherry-pick <hotfix-commit-sha>
git push origin master
```

## Branch Protection Rules

### `master` Branch
- Require pull request before merging
- Require at least 1 approval (when team grows)
- Require CI status checks to pass:
  - `lint`
  - `unit-test`
  - `build`
- Require branch to be up to date before merging
- No force pushes
- No deletion

### `release/*` Branches
- Require pull request before merging
- Require CI status checks to pass
- No force pushes
- Only hotfix branches may merge into release branches

## Merge Strategies

| Source → Target | Strategy | Reason |
|----------------|----------|--------|
| `feature/*` → `master` | **Squash merge** | Clean linear history |
| `bugfix/*` → `master` | **Squash merge** | Clean linear history |
| `hotfix/*` → `release/*` | **Merge commit** | Preserve fix history |
| `hotfix/*` → `master` | **Cherry-pick** | Apply fix without release commits |

## CI Trigger Rules

| Branch Pattern | `ci.yml` | `release.yml` |
|---------------|----------|---------------|
| `master` push | Yes | No |
| `master` PR | Yes | No |
| `release/**` push | Yes | No |
| `release/**` PR | Yes | No |
| `v*` tag | No | Yes |
| `feature/*` PR to master | Yes | No |

## Related Documentation
- [Deployment & CI/CD](./DEPLOYMENT.md) — workflow YAML and release process
- [Code Quality](./CODE_QUALITY.md) — commit message and PR conventions

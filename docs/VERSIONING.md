# Versioning Strategy

## Semantic Versioning

SmackTrack follows [Semantic Versioning](https://semver.org/) (SemVer):

```
MAJOR.MINOR.PATCH
```

| Component | When to bump | Example |
|-----------|-------------|---------|
| **MAJOR** | Breaking changes to data format, account migration required | 1.0.0 → 2.0.0 |
| **MINOR** | New features, non-breaking additions | 1.0.0 → 1.1.0 |
| **PATCH** | Bug fixes, performance improvements | 1.0.0 → 1.0.1 |

## Android Version Fields

### `versionName` (user-facing)

The SemVer string displayed to users (e.g., `"1.0.0"`). Shown in Settings and on the Play Store.

### `versionCode` (Play Store)

A monotonically increasing integer required by the Play Store. Each upload must have a higher `versionCode` than the previous one.

**Formula:**

```
versionCode = MAJOR * 10000 + MINOR * 100 + PATCH
```

| Version | versionCode |
|---------|-------------|
| 1.0.0   | 10000       |
| 1.0.1   | 10001       |
| 1.1.0   | 10100       |
| 1.2.3   | 10203       |
| 2.0.0   | 20000       |

This formula supports up to 99 minor versions and 99 patches per minor version, which is more than sufficient.

## Where Versions Are Defined

All version values live in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 10000  // MAJOR * 10000 + MINOR * 100 + PATCH
    versionName = "1.0.0"
}
```

## Release Checklist

1. Decide on the new version number based on the changes (major/minor/patch)
2. Update `versionName` in `app/build.gradle.kts`
3. Update `versionCode` using the formula above
4. Commit: `git commit -m "Bump version to X.Y.Z"`
5. Tag: `git tag vX.Y.Z`
6. Push: `git push origin master --tags`
7. The `release.yml` workflow triggers on the tag and builds the release

## Current Version

| Field | Value |
|-------|-------|
| `versionName` | `1.0.0` |
| `versionCode` | `10000` |

## Related Documentation

- [Deployment](DEPLOYMENT.md) — CI/CD and release pipeline
- [Branching Strategy](BRANCHING_STRATEGY.md) — Git workflow

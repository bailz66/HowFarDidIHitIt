# Deployment & Play Store
> GitHub Issues: #10, #11, #12, #13

## Overview
CI/CD pipeline using GitHub Actions. Automated builds, tests, and release artifact generation. Manual Play Store upload for v1 with a path to automated deployment.

## CI/CD Pipeline (GitHub Actions)

### Workflow: `ci.yml` — Runs on Every Push & PR
```yaml
name: CI
on:
  push:
    branches: [master, main]
  pull_request:
    branches: [master, main]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '21' }
      - run: ./gradlew lint detekt ktlintCheck

  unit-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '21' }
      - run: ./gradlew testDebugUnitTest
      - uses: actions/upload-artifact@v4
        with:
          name: unit-test-reports
          path: app/build/reports/tests/

  instrumented-test:
    runs-on: ubuntu-latest
    needs: unit-test
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '21' }
      - uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 33
          script: ./gradlew connectedDebugAndroidTest
      - uses: actions/upload-artifact@v4
        with:
          name: instrumented-test-reports
          path: app/build/reports/androidTests/

  build:
    runs-on: ubuntu-latest
    needs: [lint, unit-test]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '21' }
      - run: ./gradlew assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

### Workflow: `release.yml` — Runs on Git Tags
```yaml
name: Release
on:
  push:
    tags: ['v*']

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '21' }
      - run: ./gradlew testDebugUnitTest
      - name: Build signed release bundle
        run: ./gradlew bundleRelease
        env:
          KEYSTORE_FILE: ${{ secrets.KEYSTORE_FILE }}
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
      - uses: actions/upload-artifact@v4
        with:
          name: release-aab
          path: app/build/outputs/bundle/release/app-release.aab
      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          files: app/build/outputs/bundle/release/app-release.aab
```

## App Signing

### Debug Signing
- Auto-generated debug keystore (default Android behavior)
- Used for local development and CI debug builds

### Release Signing
- Generate a release keystore (do this once, keep it safe):
```bash
keytool -genkey -v -keystore howfardidihitit-release.keystore \
  -alias howfardidihitit -keyalg RSA -keysize 2048 -validity 10000
```
- **NEVER commit the keystore to git**
- Store keystore and passwords as GitHub Secrets:
  - `KEYSTORE_FILE` (base64-encoded keystore)
  - `KEYSTORE_PASSWORD`
  - `KEY_ALIAS`
  - `KEY_PASSWORD`

### Gradle Signing Config
```kotlin
android {
    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("KEYSTORE_FILE")
            if (keystoreFile != null) {
                storeFile = file(Base64.decode(keystoreFile))
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(...)
        }
    }
}
```

## Play Store Deployment

### Prerequisites
1. **Google Play Developer account** ($25 one-time fee)
2. **App signing key** enrolled in Play App Signing (Google manages the upload key)
3. **Store listing** prepared (see below)

### Store Listing

| Field | Value |
|-------|-------|
| App name | How Far Did I Hit It |
| Short description | Simple golf GPS — measure how far you hit the ball |
| Full description | See below |
| Category | Sports |
| Content rating | Everyone |
| Pricing | Free |
| Screenshots | Phone (min 2), Tablet (optional) |
| Feature graphic | 1024x500 banner |
| App icon | 512x512 high-res icon |

**Full description draft:**
```
How Far Did I Hit It is the simplest way to measure your golf shot distances.

No subscriptions. No ads. No account needed. Just tap, walk, and tap.

HOW IT WORKS:
1. Select your club (Driver through Lob Wedge)
2. Tap "Mark Start" at your ball
3. Walk or drive to where it landed
4. Tap "Mark End" — see your distance instantly

FEATURES:
• GPS-calibrated distance measurement (yards & meters)
• Live distance tracking as you walk
• Automatic weather recording (temperature, conditions, wind)
• Shot history and per-club analytics
• Filter stats by club, date, weather, and temperature
• Works offline — no internet required for shot tracking

PRIVACY:
Your data stays on your device. No accounts, no cloud sync, no tracking.
```

### Release Process (v1 — Manual)
1. Bump `versionCode` and `versionName` in `build.gradle.kts`
2. Create a git tag: `git tag v1.0.0`
3. Push tag: `git push origin v1.0.0`
4. CI builds signed AAB and creates GitHub Release
5. Download AAB from GitHub Release
6. Upload to Play Console → Production track
7. Fill out release notes
8. Submit for review

### Release Process (Future — Automated)
Use `r0adkll/upload-google-play` GitHub Action or Fastlane to automate Play Store upload:
```yaml
- uses: r0adkll/upload-google-play@v1
  with:
    serviceAccountJsonPlainText: ${{ secrets.PLAY_SERVICE_ACCOUNT }}
    packageName: com.example.howfardidihitit
    releaseFiles: app/build/outputs/bundle/release/app-release.aab
    track: production
```

### Version Strategy
- `versionCode`: integer, increments with every release (1, 2, 3, ...)
- `versionName`: semver (1.0.0, 1.1.0, 2.0.0)
- Major: breaking changes or major feature additions
- Minor: new features
- Patch: bug fixes

## Pre-Release Checklist
- [ ] All tests passing in CI
- [ ] Lint clean (zero warnings)
- [ ] Manual testing on physical device
- [ ] Version code and name bumped
- [ ] Release notes written
- [ ] Screenshots updated (if UI changed)
- [ ] ProGuard/R8 rules verified (no runtime crashes in release build)
- [ ] App size checked (target: < 10MB)
- [ ] Privacy policy URL set (Play Store requires this)

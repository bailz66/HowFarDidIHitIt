# SmackTrack — Play Store Publication Guide

## Pre-Flight Checklist

Everything below has been completed unless marked with `[ ]`.

### Code & Build
- [x] All 462 unit tests pass
- [x] Lint passes with zero errors
- [x] Release APK builds and signs successfully
- [x] Release APK verified on emulator (no crashes)
- [x] ProGuard/R8 keeps all critical classes (serialization, enums, services, Firebase)
- [x] Firebase App Check initialized with try-catch (handles emulators)
- [x] `android:allowBackup="false"` in manifest
- [x] `cleartextTrafficPermitted="false"` enforced
- [x] No `removeFirst()` or other API 35+ calls (minSdk is 33)

### Security
- [x] Release keystore created and backed up
- [x] Release SHA-1 registered in Firebase Console
- [x] Old compromised keystore deleted and SHA-1 removed
- [x] `google-services.json` NOT in git history
- [x] `.claude/settings.local.json` NOT in git (was removed — contained passwords)
- [x] `*.jks` in `.gitignore`
- [x] No secrets, passwords, or API keys in committed code

### CI/CD
- [x] GitHub Secrets configured: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `GOOGLE_SERVICES_JSON`
- [x] `release.yml` decodes base64 keystore to file before build
- [x] `ci.yml` and `release.yml` decode `google-services.json` from secrets
- [x] Release workflow triggers on `v*` tags

### Firebase
- [x] Firestore production rules deployed
- [x] Schema validation on all document writes
- [x] User data isolated by UID
- [x] Firebase App Check (Play Integrity) enabled

### Privacy & Compliance
- [x] Privacy policy live: https://bailz66.github.io/HowFarDidIHitIt/privacy-policy.html
- [x] Data deletion page live: https://bailz66.github.io/HowFarDidIHitIt/data-deletion.html
- [x] In-app analytics opt-out (Settings > Privacy)
- [x] In-app account deletion (Settings > Cloud Sync > Delete Account)
- [x] Privacy policy covers: location, analytics, crash reporting, App Check, data retention

---

## Step 1: Create Play Console Developer Account

If you don't have one yet:
1. Go to https://play.google.com/console
2. Pay the one-time $25 registration fee
3. Complete identity verification (takes 1-3 days)
4. Set up a payments profile if you plan to accept donations via Play

---

## Step 2: Create the App in Play Console

1. Click **Create app**
2. App name: **SmackTrack**
3. Default language: **English (United States)**
4. App or game: **App**
5. Free or paid: **Free**
6. Accept declarations

---

## Step 3: Complete App Content (required before review)

### 3a. Privacy Policy
- URL: `https://bailz66.github.io/HowFarDidIHitIt/privacy-policy.html`

### 3b. App Access
- Select: **All functionality is available without special access**

### 3c. Ads
- Select: **No, my app does not contain ads**

### 3d. Content Rating
Complete the IARC questionnaire:
- Violence: **No**
- Sexual content: **No**
- Language: **No**
- Controlled substances: **No**
- Gambling: **No**
- User-generated content: **No**
- Expected rating: **Everyone**

### 3e. Target Audience
- Target age group: **18 and over** (simplest — avoids Families Policy requirements)
- Is this app designed for children? **No**

### 3f. News App
- Select: **No**

### 3g. Data Safety
Fill out the form:

| Question | Answer |
|----------|--------|
| Does your app collect or share data? | Yes |
| Is all collected data encrypted in transit? | Yes |
| Do you provide a way to delete data? | Yes |
| Data deletion URL | `https://bailz66.github.io/HowFarDidIHitIt/data-deletion.html` |

**Data types collected:**

| Data type | Collected | Shared | Purpose | Optional |
|-----------|-----------|--------|---------|----------|
| Approximate location | Yes | Yes (Open-Meteo for weather) | App functionality | No |
| Email address | Yes | No | Account management | Yes |
| App interactions | Yes | No | Analytics | Yes (opt-out in Settings) |
| Crash logs | Yes | No | App stability | No |
| Device/OS info | Yes | No | App stability | No |

### 3h. Government Apps
- Select: **No**

### 3i. Financial Features
- Select: **No**

### 3j. Health Features
- Select: **No**

---

## Step 4: Set Up Store Listing

### 4a. Main Store Listing

**App name** (30 chars max):
```
SmackTrack
```

**Short description** (80 chars max):
```
Tap where you hit. Walk to where it landed. Know your distance.
```

**Full description** (4000 chars max):
```
SmackTrack uses GPS to measure how far you hit a golf ball. No range finder needed — just your phone.

HOW IT WORKS
1. Smack — tap the button where you hit your ball
2. Track — walk to where it landed and tap again
3. Done — see your distance with real-time wind and weather data

FEATURES
• GPS distance tracking in yards or meters
• Adaptive GPS calibration for accurate readings
• Real-time wind and weather conditions
• Wind-adjusted carry distance (physics-based model)
• Per-club statistics with averages, trends, and sparklines
• Session-grouped shot history
• 60 achievements across 12 categories
• Cloud sync with Google Sign-in (optional)
• Share shot cards with friends
• Low/Mid/High trajectory settings for wind calculations
• Foreground GPS service keeps tracking while your screen is locked
• Privacy-first: GPS coordinates are never stored

DESIGNED FOR GOLFERS
Whether you're at the range, playing a casual round, or dialing in your club distances, SmackTrack gives you the data you need without the clutter. No subscriptions, no ads, no complicated setup.

ANALYTICS THAT MATTER
See your average, longest, and shortest for every club. Spot trends with sparklines and scatter plots. Filter by time period. Toggle between raw and wind-adjusted distances to understand your true carry.

ACHIEVEMENTS
Earn 60 achievements across 12 categories — from your first shot to hitting 300-yard bombs. Track your progress in the achievement gallery.

CLOUD SYNC (OPTIONAL)
Sign in with Google to sync your shots, settings, and achievements across devices. Everything works offline too — no account required.

PRIVACY MATTERS
• GPS coordinates are used only for distance calculation and are never stored
• In-app analytics opt-out toggle
• Full account and data deletion from Settings
• No ads, no tracking, no data selling

No ads. No subscriptions. No clutter. Just distance.
```

### 4b. Graphics (you need to create these)

| Asset | Size | Notes |
|-------|------|-------|
| App icon | 512 × 512 px | High-res version of adaptive icon |
| Feature graphic | 1024 × 500 px | Banner image for store listing |
| Phone screenshots | Min 2, max 8 | 16:9 or 9:16, min 320px |
| Tablet screenshots | Optional | 16:9 or 9:16 |

**Screenshot suggestions** (take from emulator):
1. Home screen with SMACK button
2. Walking screen with live distance counter
3. Result screen with wind arrow and weather
4. Stats/Analytics tab with club stats
5. History tab with session-grouped shots
6. Achievement gallery
7. Settings with cloud sync
8. GPS calibration screen (the radar animation)

### 4c. Categorization
- App category: **Sports**
- Tags: Golf, Distance, GPS, Tracking

---

## Step 5: Upload the Release

### 5a. App Signing
1. Go to **Release > Setup > App signing**
2. Choose **Let Google manage and protect your app signing key** (recommended)
3. Upload your release keystore or use Google's managed key

### 5b. Create a Release
1. Go to **Release > Production**
2. Click **Create new release**
3. Upload: `app/build/outputs/bundle/release/app-release.aab`
4. Release name: `1.0.0`
5. Release notes:
```
Initial release of SmackTrack — GPS golf distance tracker.

• Measure shot distances with GPS
• Real-time wind and weather data
• Per-club analytics and trends
• 60 achievements to earn
• Optional cloud sync with Google Sign-in
• Share shot cards with friends
```

### 5c. Review and Roll Out
1. Click **Review release**
2. Fix any warnings (there should be none)
3. Click **Start rollout to Production**

---

## Step 6: Post-Submission

### Automated Release (alternative to manual upload)
Instead of manual upload, you can use the GitHub release workflow:

```bash
git tag v1.0.0
git push origin v1.0.0
```

This triggers `.github/workflows/release.yml` which:
1. Runs unit tests
2. Builds signed AAB + APK
3. Creates a GitHub Release with artifacts
4. Download the AAB from the GitHub Release and upload to Play Console

### Monitor
- **Review time**: Usually 1-3 days for first submission
- **Crashlytics**: Monitor Firebase Console for crash reports after launch
- **Analytics**: Check Firebase Analytics for usage patterns
- **Play Console**: Check for policy warnings or rejection feedback

### If Rejected
1. Read the rejection reason carefully
2. Common reasons and fixes:
   - **Metadata policy**: Adjust title/description/screenshots
   - **Foreground service**: Provide video showing the feature in use
   - **Permissions**: Add more detailed justification
   - **Privacy policy**: Ensure URL is accessible and comprehensive
3. Fix the issue
4. Resubmit (subsequent reviews are usually faster)

---

## Step 7: Post-Launch

### Version Updates
1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`
2. Commit and push to master
3. Tag: `git tag v1.1.0 && git push origin v1.1.0`
4. Upload new AAB to Play Console

### Respond to Users
- Monitor Play Console reviews
- Respond to feedback within 48 hours
- Track feature requests for v1.1

---

## Play Console Declaration Text (copy-paste ready)

### Foreground Service Justification
> SmackTrack uses a foreground location service to keep GPS active while the user walks from their shot start position to where their ball landed. The screen may lock during this walk (30-300 yards). Without the foreground service, GPS would stop and the distance measurement would fail. The service shows a persistent notification ("Tracking your shot..."), runs only during active shot tracking, and stops automatically when the shot completes or after 15 minutes. No background location permission is used.

### Location Permission Justification
> SmackTrack uses precise GPS location to calculate the distance between two points on a golf course — where the user hit the ball and where it landed. This is the core functionality of the app. Location coordinates are used transiently for distance calculation and are not stored. Approximate coordinates (rounded to ~1km) are sent to the Open-Meteo weather API to fetch current wind and temperature conditions for wind-adjusted distance calculations.

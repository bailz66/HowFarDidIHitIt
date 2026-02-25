# Project: SmackTrack - Golf Shot Distance

Android golf GPS distance tracking app built with Jetpack Compose + Material 3.

## Environment

- **Android Studio**: `F:\android-studio\`
- **JAVA_HOME**: `F:/android-studio/jbr` (must be set before every Gradle command)
- **Android SDK**: `C:\Users\User\AppData\Local\Android\Sdk\`
- **Shell**: Git Bash on Windows (use Unix paths with `/c/` prefix)

## Building

```bash
export JAVA_HOME="F:/android-studio/jbr"
cd /c/Users/User/AndroidStudioProjects/HowfardidIhitit2
./gradlew assembleDebug
```

## Running the Emulator

The working AVD is `Medium_Phone_API_36.1_2` (API 36, x86_64). The other AVD (`Medium_Phone_API_36.1`) is broken — missing system image.

### Start the emulator (run in background):

```bash
/c/Users/User/AppData/Local/Android/Sdk/emulator/emulator.exe -avd Medium_Phone_API_36.1_2 -no-snapshot-load &
```

### Wait for it to fully boot:

```bash
/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe wait-for-device && \
/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe shell "while [[ -z \$(getprop sys.boot_completed) ]]; do sleep 2; done" && \
echo "Emulator ready"
```

### Install and launch the app:

```bash
/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -S -n com.smacktrack.golf/.MainActivity
```

### Restart app without reinstalling:

```bash
/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -S -n com.smacktrack.golf/.MainActivity
```

### Check if emulator is running:

```bash
/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe devices
```

## Building Release

```bash
export JAVA_HOME="F:/android-studio/jbr"
./gradlew assembleRelease
```

## Running Tests

```bash
export JAVA_HOME="F:/android-studio/jbr"
./gradlew testDebugUnitTest
```

## Features

- **Smack page** — centered Smack button, last 3 recent shots (swipe left to delete), donate tile
- **Walking page** — live distance counter, club selection chips, Track button with extended GPS calibration
- **History page** — full shot history with swipe-to-delete
- **Result page** — distance card with weather, wind arrow, wind-adjusted carry
- **Settings** — distance/wind/temperature units, trajectory, club toggles
- **GPS calibration** — accuracy-weighted multi-sample calibration; fresh GPS warmup on each new shot; start 3.5s, end 4.5s
- **Delete shots** — swipe left on any shot (recent or history) to delete

## Firebase / Firestore

- **Project**: `smashtrack-a2c99` (Firebase Console)
- **Plan**: Spark (free tier) — no billing required
- **Firestore**: test mode (rules expire after 30 days — add security rules before publishing)
- **Auth**: Google Sign-in via Credential Manager API
- **`app/google-services.json`**: downloaded from Firebase Console — do NOT commit to public repos

### SHA-1 Fingerprints (registered in Firebase Console → Project settings → Your apps)

- **Debug**: `F5:20:03:5C:D2:FF:F7:A7:92:CB:06:C9:C6:69:B4:A6:BB:03:F6:C4` (from `~/.android/debug.keystore`, valid until 2056)
- **Release**: TODO — add release keystore SHA-1 before publishing to Play Store

To get a SHA-1:
```bash
"F:/android-studio/jbr/bin/keytool" -list -v -keystore /c/Users/User/.android/debug.keystore -alias androiddebugkey -storepass android
```

## Key Dependencies

- `play-services-location` — GPS via FusedLocationProviderClient
- `firebase-firestore-ktx` / `firebase-auth-ktx` — Firestore + Firebase Auth (via BOM 33.7.0)
- `credentials-play-services-auth` / `googleid` — Credential Manager Google Sign-in
- `org.json:json` — test-only dependency for WeatherService JSON parsing tests

## GitHub

- **Repo**: https://github.com/bailz66/HowFarDidIHitIt
- **CLI auth**: `gh` is installed at `/c/Users/User/gh/bin/gh.exe` and authenticated with workflow scope

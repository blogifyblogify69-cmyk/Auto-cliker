# Auto Clicker Test

A user-controlled Android automation/testing controller for a selected installed test application.

## Exact automation workflow

1. User selects an installed app.
2. User launches that app from this controller.
3. User explicitly enables the Accessibility Service in Android Settings.
4. The floating A icon is available over the selected app.
5. User opens the menu and taps ACTIVE.
6. The service watches only the selected package.
7. When an accessible countdown value is exactly 15, it clicks Target B.
8. It waits exactly 22 seconds.
9. It clicks Target A.
10. The cycle ends and will not retrigger until the 15 value has disappeared and is detected again.

The trigger is exact: 15 triggers; 115, 150, and 15.0 do not.

## Target clicking reliability

The service tries Target A/B in this order:

1. Exact accessibility text/content description.
2. `target_a` / `target_b` accessibility description.
3. A user-calibrated screen coordinate.

If the target app is a custom Canvas/OpenGL/game surface and does not expose accessible controls, use:

- CALIBRATE TARGET A
- CALIBRATE TARGET B

Tap the exact target location once. The coordinates are saved locally and used with Android accessibility gesture dispatch.

## Important countdown requirement

For the most reliable trigger, the target app should expose the countdown as an accessibility text node. If the countdown is only pixels inside a Canvas/OpenGL surface, the service cannot read the number directly from the accessibility tree. This build deliberately does not use screen recording or OCR.

For an app you control, expose the countdown with an accessibility-visible TextView/content description and expose Target A/Target B with stable text/content descriptions.

## User control

The controller provides:

- Installed-app selection
- Launch selected app
- Accessibility disclosure before enabling
- ACTIVE
- STOP
- STOP ALL ACTIVE
- Target A calibration
- Target B calibration
- Accessibility Settings

Automation stays off until the user activates it.

## Accessibility disclosure

This app uses Android AccessibilityService to inspect the selected test app's visible accessibility tree and perform the explicitly configured click sequence. It does not record the screen, upload accessibility data, or make decisions outside the fixed user-defined rule.

If distributed through Google Play, complete the applicable AccessibilityService declaration and disclosure/consent requirements. Do not falsely declare this as an accessibility tool unless its primary purpose actually qualifies as disability assistance.

## Build types

### Debug

Pushes to `main` run the debug validation job:

```text
gradle assembleDebug
```

The resulting debug APK is uploaded as the `AutoClicker-debug-apk` Actions artifact.

### Signed release

The manual `workflow_dispatch` release job creates:

- `AutoClicker-v3.1.apk` — signed APK for direct Android installation.
- `AutoClicker-v3.1.aab` — signed Android App Bundle for Play Console.
- `SHA256.txt` — SHA-256 checksums.
- `BUILD_INFO.txt` — build metadata.

Android release builds must be signed with a release key. The private keystore is intentionally not stored in this repository.

## GitHub Actions release signing setup

Add these **repository Actions secrets** before running the manual release workflow:

```text
RELEASE_KEYSTORE_BASE64
RELEASE_KEYSTORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

### Create a release keystore

Run this on your own computer and keep the resulting keystore and passwords safe:

```bash
keytool -genkeypair -v \
  -keystore autoclicker-release.jks \
  -alias autoclicker \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Convert the keystore to Base64 for the `RELEASE_KEYSTORE_BASE64` secret:

Linux/macOS:

```bash
base64 -w 0 autoclicker-release.jks
```

On systems whose `base64` does not support `-w`, use:

```bash
base64 autoclicker-release.jks
```

Then add the four values under:

```text
GitHub repository
→ Settings
→ Secrets and variables
→ Actions
→ New repository secret
```

Never commit the keystore, passwords, or decoded keystore into the repository.

## Run a signed release

Open:

```text
GitHub
→ Actions
→ Android Build and Release
→ Run workflow
```

Enter a release tag such as:

```text
v3.1.0
```

The workflow restores the private keystore only on the GitHub runner, builds the signed APK/AAB, verifies the APK signature, calculates SHA-256 checksums, uploads the artifacts, and creates a GitHub Release.

## Important installation note

A signed release APK is the correct Android release format, but no build can guarantee that Android/Play Protect will never warn about an app. This project uses AccessibilityService for its disclosed testing/automation function, so Android security systems may apply additional checks.

Do not disable or bypass Android security protections to install the application. If installation is blocked, use the exact warning/error shown by Android to diagnose the issue.

## Security

- No release keystore is stored in Git.
- Signing passwords are supplied through GitHub Actions secrets.
- The workflow grants `contents: write` only because it creates the GitHub Release.
- The automation remains user-controlled and limited to the selected package.

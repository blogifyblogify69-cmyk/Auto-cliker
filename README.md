# Auto Clicker Test

Android automation/testing utility for apps you own or are authorized to test.

## Exact automation rule

**When the selected app exposes the countdown text as exactly `15`, click Target B.**

Then:
1. Click Target B once.
2. Wait **22 seconds**.
3. Click Target B again.
4. Resume monitoring.
5. Do not trigger repeatedly while the countdown remains `15`.

The service only reacts to the selected app package.

## Detection method

The main automation path uses Android **AccessibilityService** rather than continuous screenshots/OCR.

- Countdown is read from the accessibility tree.
- The comparison is exact: `text == 15`.
- `115`, `150`, `15.0`, etc. do not match.
- Target B can be exposed as accessibility text/content description: `Target B`, `target_b`, or `B`.
- Coordinate calibration is available as a fallback when Target B is not exposed as an accessibility node.

## Recommended setup in your own test app

Expose the countdown and button to accessibility:

- Countdown: text/value = `15`, with a stable accessibility description such as `countdown`.
- Target button: content description = `target_b`.

This avoids screen capture and OCR and is more reliable across screen sizes.

## User flow

1. Install the Auto Clicker Test APK.
2. Enable its Accessibility Service in Android Settings.
3. Select your test app.
4. Set trigger to `15`.
5. Set delay to `22` seconds.
6. Launch the selected app.
7. Tap **START AUTOMATION**.
8. The floating **A** button provides Activate, Stop and Stop All Active controls.

## Permissions

The current design does not request MediaProjection screen capture or the `SYSTEM_ALERT_WINDOW` permission. Android's Accessibility Service permission must still be explicitly enabled by the user.

Use this only with your own app or where you have authorization to automate the target app.

## Build

Open the project in Android Studio or run:

`./gradlew assembleDebug`

The GitHub Actions workflow also builds the debug APK.
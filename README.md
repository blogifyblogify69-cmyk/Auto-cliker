# Auto Clicker Test — Virtual Test APK

This repository contains a **self-contained Android automation test environment**.

The current build does **not** inspect another app, capture the screen, draw overlays, use AccessibilityService, or request special Android permissions.

## Current automation flow

The APK contains its own virtual countdown and its own Target A / Target B controls.

1. Open **Auto Clicker Test**.
2. Tap **ACTIVATE AUTOMATION**.
3. The virtual countdown runs from 30 down to 0 and repeats.
4. When the countdown is **exactly 15**, the controller clicks **Target B**.
5. The controller waits **22 seconds**.
6. The controller clicks **Target A**.
7. It returns to waiting for the next 15.
8. A repeated observation of the same visible 15 cannot trigger another cycle. The value must become something other than 15 and later become 15 again.
9. **STOP AUTOMATION** cancels the active cycle, including a pending Target A click.
10. **RESET COUNTDOWN** resets the virtual countdown to 30 and resets the automation state.

This is intentionally a deterministic test environment. It is not a cross-app clicker.

## Project structure

- `MainActivity.java` — launcher entry point; opens the virtual test activity.
- `VirtualTestActivity.java` — owns the virtual countdown, Target A/B buttons, and user controls.
- `AutomationController.java` — deterministic state machine for 15 → B → 22 seconds → A.
- `AutomationState.java` — controller states.
- `AutomationControllerTest.java` — JVM tests for repeated-15 protection, 22-second scheduling, Target A completion, STOP cancellation, and failed-click recovery.
- `AndroidManifest.xml` — contains only the two activities; no permissions or services.
- `android.yml` — builds, lints, signs, and inspects the debug APK.

## Permissions

The current manifest intentionally declares **no Android permissions**.

There is no:

- AccessibilityService
- `BIND_ACCESSIBILITY_SERVICE`
- `SYSTEM_ALERT_WINDOW`
- screen capture
- foreground service
- external-app inspection
- special permission setup

The CI build also checks the generated APK for stale accessibility/overlay classes and restricted permissions.

## Build configuration

- compileSdk: 35
- targetSdk: 35
- minSdk: 26
- applicationId: `com.autoclicker.virtualtest`
- versionName: `5.2`
- Android Gradle Plugin: 8.7.3
- Gradle: 8.9
- Java: 17

AGP 8.7 supports API 35 with Gradle 8.9 and JDK 17.

The project intentionally produces a debug APK, so no personal release keystore or signing password is required.

## Android 15 / API 35 UI handling

Because the app targets SDK 35, Android 15 enforces edge-to-edge behavior. The virtual activity applies system-bar insets to keep its controls tappable and visible.

## CI verification

Every push to `main` and every manual workflow run performs:

1. Gradle/JDK/SDK checks.
2. JVM unit tests.
3. Android lint.
4. Clean debug APK build.
5. APK signing verification.
6. APK package/version/launcher verification.
7. Permission inspection.
8. Stale accessibility/overlay class inspection.
9. APK artifact upload.

Artifact name:

`AutoClicker-Portable-Virtual-v5.2`

## Installation

After a successful GitHub Actions build, download the APK artifact and install it on an Android device.

For ADB installation:

~~~text
adb install app-debug.apk
~~~

If an older build with a different package ID is installed, it can coexist with this virtual-test package because the current package ID is `com.autoclicker.virtualtest`.

## Important limitation

This version deliberately does **not** automate another installed application. Its targets and countdown are internal controls owned by this APK. A cross-app implementation would require Android-supported mechanisms such as UI Automator or an appropriate accessibility/test framework and is outside this virtual build.

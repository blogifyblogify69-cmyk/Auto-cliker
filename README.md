# Auto Clicker Test — Virtual QA Host

This repository contains a self-contained Android QA test host for the two-APK Floatinger prototype.

## Two-APK test architecture

- Floatinger (com.floatinger.demo) provides the user settings and floating controller.
- Auto-cliker (com.autoclicker.virtualtest) provides the built-in Virtual QA Test App.

The Floatinger controller can send ACTIVE / STOP commands to this test host. The test host owns the countdown and the test buttons.

This build does not inspect, clone, or automatically click another installed application.

## Test flow

1. Open Floatinger and configure Target A, trigger value, and delay.
2. Grant Floatinger the Android Display over other apps permission.
3. Show the floating controller.
4. Open this Virtual QA Test App.
5. Tap ACTIVE on the Floatinger bubble.
6. The test countdown runs from 30 down to 0 and repeats.
7. When the countdown reaches the configured trigger (default 15), the test host clicks its own Target B.
8. It waits the configured delay (default 22 seconds).
9. It clicks its own Target A twice.
10. The same visible trigger value cannot retrigger until the countdown changes away from it.
11. STOP cancels the pending delay.

## Why this is a test host

The target controls belong to this APK. This lets the two-APK communication and state machine be tested without controlling a third-party application.

Android accessibility APIs can retrieve accessible window content and perform gestures when the user explicitly enables an accessibility service, but that capability is not included in this QA build.

## Build

compileSdk 35, targetSdk 35, minSdk 26, applicationId com.autoclicker.virtualtest, versionName 5.4-qa, Java 17, Gradle 8.9.

The debug build requires no personal release keystore.

## Security note

The test receiver is intentionally exported so the separate Floatinger APK can communicate with this QA host. The host is a dedicated test package and should not be reused as a receiver for sensitive operations.

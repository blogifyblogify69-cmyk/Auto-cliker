# Auto Clicker Test

A safe Android self-test APK for validating a simple timed click workflow inside its own UI.

## Main logic

1. The countdown runs normally.
2. When the countdown is exactly **15**, the app automatically clicks **Target B**.
3. It waits **22 seconds**.
4. It automatically clicks **Target A**.
5. The cycle finishes and does not repeatedly trigger while the countdown remains 15.

Exact matching is intentional:

- 15 -> triggers
- 115 -> does not trigger
- 150 -> does not trigger
- 15.0 -> does not trigger

## Safe installation design

This version does not use:

- AccessibilityService
- SYSTEM_ALERT_WINDOW
- MediaProjection/screen capture
- OCR
- external-app automation
- special Android permissions

It is a normal APK that installs and runs as a regular application.

## How to test

1. Install the APK normally.
2. Open **Auto Clicker Test**.
3. Leave the starting countdown at **30**.
4. Tap **START TEST**.
5. Watch the countdown reach **15**.
6. At exactly **15**, Target B is clicked automatically.
7. After **22 seconds**, Target A is clicked automatically.
8. Check the event log.

You can turn **AUTOMATION** off to verify that automatic actions stop.

## Important

This build intentionally tests the logic inside this APK. It does not control arbitrary third-party applications. That keeps installation simple and avoids sensitive Android automation permissions.

## Build

Run:

`gradle assembleDebug`

The GitHub Actions workflow uploads the resulting debug APK as **AutoClicker-debug-apk**.

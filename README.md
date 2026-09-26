# Auto Clicker Test

A user-controlled Android automation/testing controller for a selected installed test application.

## Exact workflow
1. User selects an installed app.
2. User launches that app from this controller.
3. User explicitly enables the Accessibility Service in Android Settings.
4. The floating A icon is available over the selected app.
5. User opens the menu and taps ACTIVE.
6. The service watches only the selected package.
7. When an accessible countdown value is exactly 15, it clicks Target B.
8. It waits 22 seconds.
9. It clicks Target A.
10. The cycle ends.

The trigger is exact: 15 triggers; 115, 150, and 15.0 do not.

## Target clicking reliability
The service tries Target A/B in this order:
1. Exact accessibility text/content description.
2. target_a / target_b accessibility description.
3. A user-calibrated screen coordinate.

If the target app is a custom Canvas/OpenGL/game surface and does not expose accessible controls, use the floating menu:
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

## Build
Run: gradle assembleDebug
GitHub Actions uploads the debug APK as AutoClicker-debug-apk.
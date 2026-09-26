# Auto Clicker Test

A user-controlled Android automation/testing controller for a selected installed test application.

## Fixed automation logic

The APK now has a simple fixed workflow. No trigger value, timing value, keystore value, password, or signing key is required from the user.

1. Select an installed test app.
2. Enable the controller's Accessibility Service in Android Settings.
3. Launch the selected app.
4. Use the floating **A** button.
5. Choose **CALIBRATE TARGET A** and tap Target A.
6. Choose **CALIBRATE TARGET B** and tap Target B.
7. Choose **ACTIVE**.
8. The controller watches only the selected package.
9. When the accessible countdown is exactly **15**, it clicks **Target B**.
10. It waits exactly **22 seconds**.
11. It clicks **Target A**.
12. The cycle finishes.
13. It will not trigger again while the same 15 remains visible. It must disappear and be detected again before another cycle.

The trigger is fixed to exactly 15. The delay is fixed to exactly 22 seconds.

## Target clicking

For each target, the service first tries accessible controls named Target A / Target B or target_a / target_b.

If the selected test app does not expose those controls through Accessibility, use the floating calibration buttons. The controller stores the screen coordinates locally and uses Android accessibility gesture dispatch.

## Countdown detection

The current build uses the Accessibility UI tree to find an exact text/content-description value of 15.

If the countdown is drawn only as pixels by a custom Canvas/OpenGL surface and is not exposed to Accessibility, this build cannot read the number from the screen. In that case the target app should expose the countdown as an accessibility-visible text/content description, or a separate test-only visual/OCR implementation would be needed.

## Controls

Main controller:

- Select installed app
- Refresh apps
- Launch selected app
- Enable Accessibility
- Activate automation
- Stop automation

Floating controller:

- ACTIVE / STOP
- STOP ALL ACTIVE
- CALIBRATE TARGET A
- CALIBRATE TARGET B
- Accessibility Settings
- CLOSE

Automation remains off until the user explicitly activates it.

## Build and installation

This repository intentionally uses a debug-only APK workflow for testing.

There are no:

- RELEASE_KEYSTORE_BASE64
- RELEASE_KEYSTORE_PASSWORD
- RELEASE_KEY_ALIAS
- RELEASE_KEY_PASSWORD
- release keystore files
- release signing setup

Every push to main, and every manual workflow run, builds:

app/build/outputs/apk/debug/app-debug.apk

GitHub Actions uploads it as the AutoClicker-debug-v3.2 artifact.

Android debug builds use the standard debug signing setup, so no personal signing key is required for this test build.

For local installation with Android tools:

~~~text
adb install app-debug.apk
~~~

## Android permissions and security

The controller uses Android AccessibilityService because cross-app UI inspection and gesture dispatch require an Android-supported accessibility mechanism.

The user must explicitly enable the service in Android Settings.

Do not disable or bypass Android security protections to install the APK.

If the selected app does not expose its countdown through Accessibility, the fixed automation cannot reliably detect 15 with this build.

## Testing sequence

After installing:

1. Open Auto Clicker Test.
2. Tap ENABLE ACCESSIBILITY.
3. Enable Auto Clicker Test in Android Accessibility Settings.
4. Return to the controller.
5. Select the test app.
6. Tap LAUNCH SELECTED APP.
7. Open the floating A button.
8. Calibrate Target A.
9. Calibrate Target B.
10. Tap ACTIVE.
11. Test the countdown: 15 -> B -> 22 seconds -> A.
12. Tap STOP to cancel automation.

This repository build is intended for testing an app you control or are authorized to test.

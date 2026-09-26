# Auto Clicker Test

Android accessibility + screen OCR testing utility for apps you own or are authorized to test.

## Workflow

1. Select an installed app.
2. Enable Android Accessibility Service.
3. Launch the selected app.
4. Calibrate the countdown area and Target B.
5. Start automation and approve Android screen-capture permission.
6. When OCR detects the configured countdown (default **15**), Target B is clicked once.
7. After **22 seconds**, Target B is clicked again, then monitoring resumes.
8. The floating **A** button opens Activate, Stop, and Stop All Active controls.

## Important
Android requires explicit user permission for Accessibility Service and MediaProjection screen capture. Use this only for your own apps or authorized testing.

## Build
Open the project in Android Studio or run `./gradlew assembleDebug`.

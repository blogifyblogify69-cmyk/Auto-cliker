package com.autoclicker.test;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class TestAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Intentionally passive in this QA build. The user must enable this service
        // manually in Android Settings before any accessibility-based automation.
    }

    @Override
    public void onInterrupt() {
        // No active gesture is running in the passive QA build.
    }
}

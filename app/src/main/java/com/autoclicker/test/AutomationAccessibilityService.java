package com.autoclicker.test;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.WindowManager;

import java.util.Locale;

public class AutomationAccessibilityService extends AccessibilityService
        implements AutomationController.Host, OverlayController.Host {

    public static final String PREFS = "automation_prefs";
    public static final String KEY_PACKAGE = "selected_package";
    public static final String KEY_ACTIVE = "automation_active";
    public static final String KEY_A_X = "target_a_x";
    public static final String KEY_A_Y = "target_a_y";
    public static final String KEY_B_X = "target_b_x";
    public static final String KEY_B_Y = "target_b_y";

    private static final int MAX_ATTEMPTS = 4;
    private static final long RETRY_DELAY_MS = 300L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private TargetManager targets;
    private CountdownDetector countdownDetector;
    private GestureExecutor gestureExecutor;
    private AutomationController controller;
    private OverlayController overlay;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        targets = new TargetManager(prefs);
        countdownDetector = new CountdownDetector();
        gestureExecutor = new GestureExecutor(this, handler);
        controller = new AutomationController(this);

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlay = new OverlayController(this, wm, this, targets);
        overlay.showBubble();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isActive() || prefs == null || controller == null) return;

        String selectedPackage = prefs.getString(KEY_PACKAGE, "");
        String eventPackage = event.getPackageName() == null
                ? "" : event.getPackageName().toString();

        if (selectedPackage.isEmpty() || !selectedPackage.equals(eventPackage)) return;
        if (!isUsefulEvent(event.getEventType())) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        boolean found15 = countdownDetector.containsExact15(root);
        root.recycle();

        controller.observe(found15);
    }

    private boolean isUsefulEvent(int type) {
        return type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                || type == AccessibilityEvent.TYPE_VIEW_SCROLLED
                || type == AccessibilityEvent.TYPE_VIEW_CLICKED;
    }

    @Override
    public boolean isActive() {
        return prefs != null && prefs.getBoolean(KEY_ACTIVE, false);
    }

    @Override
    public void activate() {
        if (prefs == null || controller == null) return;
        prefs.edit().putBoolean(KEY_ACTIVE, true).apply();
        controller.reset();
        status("Automation ACTIVE. Waiting for countdown 15.");
    }

    @Override
    public void stop() {
        if (prefs != null) prefs.edit().putBoolean(KEY_ACTIVE, false).apply();
        if (controller != null) controller.stop();
        status("Automation STOPPED.");
    }

    @Override
    public void clickTarget(String target, Runnable success, Runnable failure) {
        clickTargetWithRetry(target, 0, success, failure);
    }

    private void clickTargetWithRetry(String target, int attempt,
                                      Runnable success, Runnable failure) {
        if (!isActive()) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            AccessibilityNodeInfo node = findTargetNode(root, target);
            root.recycle();

            if (node != null && clickNode(node)) {
                success.run();
                return;
            }
        }

        Point point = targets.get(target);
        gestureExecutor.tap(point, new GestureExecutor.Callback() {
            @Override
            public void onSuccess() {
                success.run();
            }

            @Override
            public void onFailure() {
                if (attempt + 1 < MAX_ATTEMPTS && isActive()) {
                    handler.postDelayed(
                            () -> clickTargetWithRetry(target, attempt + 1, success, failure),
                            RETRY_DELAY_MS);
                } else {
                    failure.run();
                }
            }
        });
    }

    private AccessibilityNodeInfo findTargetNode(
            AccessibilityNodeInfo root, String target) {
        String wantedText = target.equals("A") ? "target a" : "target b";
        String wantedId = target.equals("A") ? "target_a" : "target_b";
        return findNode(root, wantedText, wantedId);
    }

    private AccessibilityNodeInfo findNode(
            AccessibilityNodeInfo node, String wantedText, String wantedId) {
        if (node == null) return null;

        String text = node.getText() == null ? "" : node.getText().toString();
        String description = node.getContentDescription() == null
                ? "" : node.getContentDescription().toString();
        String viewId = node.getViewIdResourceName() == null
                ? "" : node.getViewIdResourceName();

        if (normalize(text).equals(wantedText)
                || normalize(description).equals(wantedText)
                || normalize(viewId).equals(wantedId)) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findNode(child, wantedText, wantedId);
            if (result != null) {
                if (child != result) child.recycle();
                return result;
            }
            if (child != null) child.recycle();
        }
        return null;
    }

    private boolean clickNode(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        try {
            while (current != null) {
                if (current.isClickable()
                        && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true;
                }

                AccessibilityNodeInfo parent = current.getParent();
                if (current != node) current.recycle();
                current = parent;
            }
            return false;
        } finally {
            if (current != null && current != node) current.recycle();
            try { node.recycle(); } catch (Exception ignored) {}
        }
    }

    private String normalize(String value) {
        return value == null
                ? "" : value.trim().replaceAll("\\s+", "").toLowerCase(Locale.US);
    }

    @Override
    public void status(String message) {
        if (overlay != null) {
            // Status text is deliberately kept out of the target app UI.
        }
    }

    @Override
    public void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void calibrate(String target) {
        // Calibration is owned by OverlayController.
    }

    @Override
    public void onInterrupt() {
        stop();
    }

    @Override
    public void onDestroy() {
        if (controller != null) controller.stop();
        handler.removeCallbacksAndMessages(null);
        if (overlay != null) overlay.removeAll();
        super.onDestroy();
    }
}

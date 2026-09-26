package com.autoclicker.test;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class AutomationAccessibilityService extends AccessibilityService
        implements AutomationEngine.Listener {

    public static final String PREFS = "automation_prefs";
    public static final String KEY_PACKAGE = "selected_package";
    public static final String KEY_ACTIVE = "automation_active";
    public static final String KEY_A_X = "target_a_x";
    public static final String KEY_A_Y = "target_a_y";
    public static final String KEY_B_X = "target_b_x";
    public static final String KEY_B_Y = "target_b_y";

    private static final String FIXED_TRIGGER = "15";
    private static final long SECOND_DELAY_MS = 22_000L;
    private static final long RETRY_DELAY_MS = 300L;
    private static final int MAX_ATTEMPTS = 4;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private WindowManager windowManager;
    private View bubble;
    private View menu;
    private View calibration;
    private String calibrationTarget;

    private AutomationEngine engine;
    private boolean waitingForTargetAClick;
    private Runnable targetARunnable;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        engine = new AutomationEngine(this);
        showBubble();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isAutomationActive() || prefs == null || engine == null) return;

        String selectedPackage = prefs.getString(KEY_PACKAGE, "");
        String eventPackage = event.getPackageName() == null
                ? "" : event.getPackageName().toString();

        if (selectedPackage.isEmpty() || !selectedPackage.equals(eventPackage)) return;
        if (!isUsefulEvent(event.getEventType())) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        boolean found15 = containsExactText(root, FIXED_TRIGGER);
        root.recycle();

        // Feed only the exact countdown observation to the state machine.
        // Non-15 observations release the latch so a later 15 starts a new cycle.
        engine.observeCountdown(found15 ? FIXED_TRIGGER : "");
    }

    private boolean isUsefulEvent(int type) {
        return type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                || type == AccessibilityEvent.TYPE_VIEW_SCROLLED
                || type == AccessibilityEvent.TYPE_VIEW_CLICKED;
    }

    @Override
    public void onTargetBRequired() {
        if (!isAutomationActive() || waitingForTargetAClick) return;
        clickTarget("B", 0);
    }

    @Override
    public void onTargetARequired() {
        // The engine enters WAITING_FOR_A when B is requested. This callback is
        // intentionally unused; A is scheduled only after a successful B click.
    }

    @Override
    public void onCycleCompleted() {
        waitingForTargetAClick = false;
        targetARunnable = null;
        message("Target A clicked. Cycle complete. Waiting for the next 15.");
    }

    private boolean isAutomationActive() {
        return prefs != null && prefs.getBoolean(KEY_ACTIVE, false);
    }

    private void clickTarget(String target, int attempt) {
        if (!isAutomationActive()) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo node = null;

        if (root != null) {
            node = findClickableTarget(root, target);
            if (node != null) {
                boolean clicked = clickNodeAndRelease(node);
                root.recycle();

                if (clicked) {
                    handleSuccessfulClick(target);
                    return;
                }
            } else {
                root.recycle();
            }
        }

        Point point = configuredPoint(target);
        if (point.x >= 0 && point.y >= 0) {
            dispatchCoordinateTap(point.x, point.y, target, attempt);
            return;
        }

        retryOrFail(target, attempt);
    }

    private AccessibilityNodeInfo findClickableTarget(
            AccessibilityNodeInfo root, String target) {
        String title = target.equals("A") ? "target a" : "target b";
        String id = target.equals("A") ? "target_a" : "target_b";

        return findNode(root, title, id);
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

    private boolean containsExactText(AccessibilityNodeInfo node, String wanted) {
        if (node == null) return false;

        CharSequence text = node.getText();
        if (text != null && normalize(text.toString()).equals(normalize(wanted))) {
            return true;
        }

        CharSequence description = node.getContentDescription();
        if (description != null
                && normalize(description.toString()).equals(normalize(wanted))) {
            return true;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            boolean found = containsExactText(child, wanted);
            if (child != null) child.recycle();
            if (found) return true;
        }
        return false;
    }

    private boolean clickNodeAndRelease(AccessibilityNodeInfo node) {
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
            try {
                node.recycle();
            } catch (Exception ignored) {
            }
        }
    }

    private void handleSuccessfulClick(String target) {
        if ("B".equals(target)) {
            engine.onTargetBClicked();
            scheduleTargetA();
        } else {
            waitingForTargetAClick = false;
            engine.onTargetAClicked();
        }
    }

    private void scheduleTargetA() {
        waitingForTargetAClick = true;
        if (targetARunnable != null) handler.removeCallbacks(targetARunnable);

        message("Target B clicked. Waiting exactly 22 seconds for Target A.");

        targetARunnable = () -> {
            targetARunnable = null;
            if (isAutomationActive() && waitingForTargetAClick) {
                engine.requestTargetA();
            }
        };

        handler.postDelayed(targetARunnable, SECOND_DELAY_MS);
    }

    private void dispatchCoordinateTap(
            int x, int y, String target, int attempt) {
        if (!isAutomationActive()) return;

        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 70))
                .build();

        dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription description) {
                handleSuccessfulClick(target);
            }

            @Override
            public void onCancelled(GestureDescription description) {
                retryOrFail(target, attempt);
            }
        }, handler);
    }

    private void retryOrFail(String target, int attempt) {
        if (attempt + 1 < MAX_ATTEMPTS && isAutomationActive()) {
            handler.postDelayed(
                    () -> clickTarget(target, attempt + 1), RETRY_DELAY_MS);
            return;
        }

        if ("A".equals(target)) {
            waitingForTargetAClick = false;
            engine.onTargetAFailed();
            message("Target A click failed. Automation is waiting for the next 15.");
        } else {
            engine.onTargetBFailed();
            message("Target B click failed. Calibrate Target B and try again.");
        }
    }

    private Point configuredPoint(String target) {
        String xKey = target.equals("A") ? KEY_A_X : KEY_B_X;
        String yKey = target.equals("A") ? KEY_A_Y : KEY_B_Y;
        return new Point(
                prefs.getInt(xKey, -1),
                prefs.getInt(yKey, -1));
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().replaceAll("\\s+", "").toLowerCase(Locale.US);
    }

    private void showBubble() {
        if (bubble != null || windowManager == null) return;

        TextView view = new TextView(this);
        view.setText("A");
        view.setTextSize(22);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(Color.rgb(30, 100, 220));
        view.setOnClickListener(v -> toggleMenu());
        bubble = view;

        WindowManager.LayoutParams params = overlayParams(62, 62);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 18;
        params.y = 180;

        try {
            windowManager.addView(bubble, params);
        } catch (Exception e) {
            bubble = null;
        }
    }

    private void toggleMenu() {
        if (menu != null) {
            removeMenu();
            return;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(10, 10, 10, 10);
        box.setBackgroundColor(Color.WHITE);

        Button active = button(isAutomationActive() ? "STOP" : "ACTIVE");
        active.setOnClickListener(v -> {
            if (isAutomationActive()) {
                stopAutomation();
            } else {
                prefs.edit().putBoolean(KEY_ACTIVE, true).apply();
                engine.reset();
                waitingForTargetAClick = false;
                message("Automation ACTIVE. Waiting for countdown 15.");
            }
            removeMenu();
        });
        box.addView(active);

        Button stopAll = button("STOP ALL ACTIVE");
        stopAll.setOnClickListener(v -> stopAutomation());
        box.addView(stopAll);

        Button calibrateA = button("CALIBRATE TARGET A");
        calibrateA.setOnClickListener(v -> beginCalibration("A"));
        box.addView(calibrateA);

        Button calibrateB = button("CALIBRATE TARGET B");
        calibrateB.setOnClickListener(v -> beginCalibration("B"));
        box.addView(calibrateB);

        Button settings = button("ACCESSIBILITY SETTINGS");
        settings.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) {
            }
            removeMenu();
        });
        box.addView(settings);

        Button close = button("CLOSE");
        close.setOnClickListener(v -> removeMenu());
        box.addView(close);

        menu = box;
        WindowManager.LayoutParams params =
                overlayParams(340, WindowManager.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 18;
        params.y = 250;

        try {
            windowManager.addView(menu, params);
        } catch (Exception e) {
            menu = null;
        }
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        return button;
    }

    private WindowManager.LayoutParams overlayParams(int width, int height) {
        return new WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
    }

    private void beginCalibration(String target) {
        removeMenu();
        calibrationTarget = target;

        TextView view = new TextView(this);
        view.setText(
                "CALIBRATE TARGET " + target
                        + "\\nTap the exact target location once");
        view.setTextSize(22);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(0x88000000);

        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int x = Math.round(event.getRawX());
                int y = Math.round(event.getRawY());

                if ("A".equals(calibrationTarget)) {
                    prefs.edit().putInt(KEY_A_X, x).putInt(KEY_A_Y, y).apply();
                } else {
                    prefs.edit().putInt(KEY_B_X, x).putInt(KEY_B_Y, y).apply();
                }

                removeCalibration();
                message("Target " + target + " coordinate saved.");
                return true;
            }
            return true;
        });

        calibration = view;
        WindowManager.LayoutParams params = overlayParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);

        try {
            windowManager.addView(calibration, params);
        } catch (Exception e) {
            calibration = null;
        }
    }

    private void removeMenu() {
        if (menu != null) {
            try {
                windowManager.removeView(menu);
            } catch (Exception ignored) {
            }
            menu = null;
        }
    }

    private void removeCalibration() {
        if (calibration != null) {
            try {
                windowManager.removeView(calibration);
            } catch (Exception ignored) {
            }
            calibration = null;
        }
    }

    private void stopAutomation() {
        if (prefs != null) {
            prefs.edit().putBoolean(KEY_ACTIVE, false).apply();
        }

        waitingForTargetAClick = false;
        if (targetARunnable != null) {
            handler.removeCallbacks(targetARunnable);
            targetARunnable = null;
        }
        if (engine != null) engine.reset();

        removeMenu();
        message("Automation STOPPED.");
    }

    private void message(String text) {
        if (windowManager == null) return;

        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(16);
        view.setGravity(Gravity.CENTER);
        view.setPadding(20, 14, 20, 14);
        view.setBackgroundColor(0xDD222222);

        WindowManager.LayoutParams params = overlayParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = 120;

        try {
            windowManager.addView(view, params);
            handler.postDelayed(() -> {
                try {
                    windowManager.removeView(view);
                } catch (Exception ignored) {
                }
            }, 2200);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onInterrupt() {
        stopAutomation();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        waitingForTargetAClick = false;
        removeAllOverlays();
        super.onDestroy();
    }

    private void removeAllOverlays() {
        removeMenu();
        removeCalibration();

        if (bubble != null) {
            try {
                windowManager.removeView(bubble);
            } catch (Exception ignored) {
            }
            bubble = null;
        }
    }
}

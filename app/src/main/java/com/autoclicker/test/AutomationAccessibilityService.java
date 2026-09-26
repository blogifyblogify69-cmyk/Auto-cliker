package com.autoclicker.test;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class AutomationAccessibilityService extends AccessibilityService {
    public static final String PREFS = "automation_prefs";
    public static final String KEY_PACKAGE = "selected_package";
    public static final String KEY_ACTIVE = "automation_active";
    public static final String KEY_A_X = "target_a_x";
    public static final String KEY_A_Y = "target_a_y";
    public static final String KEY_B_X = "target_b_x";
    public static final String KEY_B_Y = "target_b_y";
    public static final String KEY_TARGET_A_TEXT = "target_a_text";
    public static final String KEY_TARGET_B_TEXT = "target_b_text";
    public static final String KEY_TRIGGER = "trigger_text";

    private static final long SECOND_DELAY_MS = 22_000L;
    private static final long RETRY_DELAY_MS = 350L;
    private static final int MAX_ATTEMPTS = 3;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private WindowManager wm;
    private View bubble;
    private View menu;
    private View calibration;
    private boolean waitingForA;
    private boolean triggerConsumed;
    private String calibrationTarget;

    private final Runnable secondClick = () -> {
        if (waitingForA && isActive()) clickTarget("A", 0);
    };

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        showBubble();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isActive() || prefs == null) return;
        String selected = prefs.getString(KEY_PACKAGE, "");
        String pkg = event.getPackageName() == null ? "" : event.getPackageName().toString();
        if (selected.isEmpty() || !selected.equals(pkg)) return;

        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && type != AccessibilityEvent.TYPE_VIEW_SCROLLED) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        String trigger = normalize(prefs.getString(KEY_TRIGGER, "15"));
        AccessibilityNodeInfo triggerNode = findExact(root, trigger, true);

        if (triggerNode != null && !waitingForA && !triggerConsumed) {
            triggerConsumed = true;
            clickTarget("B", 0);
        } else if (triggerNode == null && !waitingForA) {
            triggerConsumed = false;
        }

        root.recycle();
    }

    @Override public void onInterrupt() {
        stopAutomation();
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        removeAllOverlays();
        super.onDestroy();
    }

    private boolean isActive() {
        return prefs != null && prefs.getBoolean(KEY_ACTIVE, false);
    }

    private void clickTarget(String target, int attempt) {
        if (!isActive()) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo node = null;

        if (root != null) {
            String configured = target.equals("A")
                    ? prefs.getString(KEY_TARGET_A_TEXT, "Target A")
                    : prefs.getString(KEY_TARGET_B_TEXT, "Target B");

            node = findExact(root, normalize(configured), true);
            if (node == null) {
                node = findExact(root, target.equals("A") ? "target_a" : "target_b", true);
            }
        }

        if (node != null) {
            boolean ok = clickNode(node);
            node.recycle();
            root.recycle();
            if (ok) {
                afterClick(target);
                return;
            }
        } else if (root != null) {
            root.recycle();
        }

        Point p = configuredPoint(target);
        if (p.x >= 0 && p.y >= 0) {
            tap(p.x, p.y, target);
            return;
        }

        if (attempt + 1 < MAX_ATTEMPTS) {
            handler.postDelayed(() -> clickTarget(target, attempt + 1), RETRY_DELAY_MS);
        } else {
            waitingForA = false;
            message("Target " + target + " not found. Use CALIBRATE TARGET " + target + ".");
        }
    }

    private boolean clickNode(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        while (current != null) {
            if (current.isClickable()
                    && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            AccessibilityNodeInfo parent = current.getParent();
            if (current != node) current.recycle();
            current = parent;
        }
        return false;
    }

    private void afterClick(String target) {
        if ("B".equals(target)) {
            waitingForA = true;
            handler.removeCallbacks(secondClick);
            handler.postDelayed(secondClick, SECOND_DELAY_MS);
            message("Target B clicked. Target A will be clicked after 22 seconds.");
        } else {
            waitingForA = false;
            message("Target A clicked. Automation cycle complete.");
        }
    }

    private void tap(int x, int y, String target) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 80))
                .build();

        dispatchGesture(gesture, new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription g) {
                afterClick(target);
            }
            @Override public void onCancelled(GestureDescription g) {
                if ("A".equals(target)) waitingForA = false;
                else if (isActive()) handler.postDelayed(() -> clickTarget(target, 1), RETRY_DELAY_MS);
            }
        }, handler);
    }

    private Point configuredPoint(String target) {
        String xKey = target.equals("A") ? KEY_A_X : KEY_B_X;
        String yKey = target.equals("A") ? KEY_A_Y : KEY_B_Y;
        return new Point(prefs.getInt(xKey, -1), prefs.getInt(yKey, -1));
    }

    private AccessibilityNodeInfo findExact(AccessibilityNodeInfo node, String wanted, boolean description) {
        if (node == null) return null;

        CharSequence text = node.getText();
        if (text != null && normalize(text.toString()).equals(wanted)) return node;

        if (description) {
            CharSequence d = node.getContentDescription();
            if (d != null && normalize(d.toString()).equals(wanted)) return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findExact(child, wanted, description);
            if (result != null) return result;
            if (child != null) child.recycle();
        }
        return null;
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", "").toLowerCase(Locale.US);
    }

    private void showBubble() {
        if (bubble != null || wm == null) return;

        TextView v = new TextView(this);
        v.setText("A");
        v.setTextSize(22);
        v.setTextColor(Color.WHITE);
        v.setGravity(Gravity.CENTER);
        v.setBackgroundColor(Color.rgb(30, 100, 220));
        v.setOnClickListener(x -> toggleMenu());
        bubble = v;

        WindowManager.LayoutParams lp = overlayParams(62, 62);
        lp.gravity = Gravity.TOP | Gravity.END;
        lp.x = 18;
        lp.y = 180;

        try { wm.addView(bubble, lp); }
        catch (Exception e) { bubble = null; }
    }

    private void toggleMenu() {
        if (menu != null) { removeMenu(); return; }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(10, 10, 10, 10);
        box.setBackgroundColor(Color.WHITE);

        Button active = button(isActive() ? "STOP" : "ACTIVE");
        active.setOnClickListener(v -> {
            boolean next = !isActive();
            prefs.edit().putBoolean(KEY_ACTIVE, next).apply();
            waitingForA = false;
            triggerConsumed = false;
            if (!next) handler.removeCallbacks(secondClick);
            removeMenu();
            message(next ? "Automation ACTIVE." : "Automation STOPPED.");
        });
        box.addView(active);

        Button stopAll = button("STOP ALL ACTIVE");
        stopAll.setOnClickListener(v -> stopAutomation());
        box.addView(stopAll);

        Button a = button("CALIBRATE TARGET A");
        a.setOnClickListener(v -> beginCalibration("A"));
        box.addView(a);

        Button b = button("CALIBRATE TARGET B");
        b.setOnClickListener(v -> beginCalibration("B"));
        box.addView(b);

        Button settings = button("ACCESSIBILITY SETTINGS");
        settings.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) {}
            removeMenu();
        });
        box.addView(settings);

        Button close = button("CLOSE");
        close.setOnClickListener(v -> removeMenu());
        box.addView(close);

        menu = box;
        WindowManager.LayoutParams lp = overlayParams(340, WindowManager.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.END;
        lp.x = 18;
        lp.y = 250;

        try { wm.addView(menu, lp); }
        catch (Exception e) { menu = null; }
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        return b;
    }

    private WindowManager.LayoutParams overlayParams(int width, int height) {
        return new WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
    }

    private void beginCalibration(String target) {
        removeMenu();
        calibrationTarget = target;

        TextView v = new TextView(this);
        v.setText("CALIBRATE TARGET " + target + "\nTap the exact target location once");
        v.setTextSize(22);
        v.setTextColor(Color.WHITE);
        v.setGravity(Gravity.CENTER);
        v.setBackgroundColor(0x88000000);
        v.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int x = Math.round(event.getRawX());
                int y = Math.round(event.getRawY());

                if ("A".equals(calibrationTarget)) {
                    prefs.edit().putInt(KEY_A_X, x).putInt(KEY_A_Y, y).apply();
                } else {
                    prefs.edit().putInt(KEY_B_X, x).putInt(KEY_B_Y, y).apply();
                }

                removeCalibration();
                message("Target " + target + " coordinate saved: " + x + ", " + y);
                return true;
            }
            return true;
        });

        calibration = v;
        WindowManager.LayoutParams lp = overlayParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);

        try { wm.addView(calibration, lp); }
        catch (Exception e) { calibration = null; }
    }

    private void removeMenu() {
        if (menu != null) {
            try { wm.removeView(menu); } catch (Exception ignored) {}
            menu = null;
        }
    }

    private void removeCalibration() {
        if (calibration != null) {
            try { wm.removeView(calibration); } catch (Exception ignored) {}
            calibration = null;
        }
    }

    private void stopAutomation() {
        prefs.edit().putBoolean(KEY_ACTIVE, false).apply();
        waitingForA = false;
        triggerConsumed = false;
        handler.removeCallbacks(secondClick);
        removeMenu();
        message("Automation STOPPED.");
    }

    private void message(String text) {
        if (wm == null) return;

        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(16);
        v.setGravity(Gravity.CENTER);
        v.setPadding(20, 14, 20, 14);
        v.setBackgroundColor(0xDD222222);

        WindowManager.LayoutParams lp = overlayParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.y = 120;

        try {
            wm.addView(v, lp);
            handler.postDelayed(() -> {
                try { wm.removeView(v); } catch (Exception ignored) {}
            }, 2200);
        } catch (Exception ignored) {}
    }

    private void removeAllOverlays() {
        removeMenu();
        removeCalibration();
        if (bubble != null) {
            try { wm.removeView(bubble); } catch (Exception ignored) {}
            bubble = null;
        }
    }
}

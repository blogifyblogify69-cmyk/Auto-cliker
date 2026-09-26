package com.autoclicker.test;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class AutomationAccessibilityService extends AccessibilityService {
    public static AutomationAccessibilityService instance;

    private static final String DEFAULT_TRIGGER = "15";
    private static final long DEFAULT_DELAY_MS = 22_000L;

    private WindowManager wm;
    private View bubble;
    private View calibration;

    private boolean active = false;
    private boolean waitingForSecondClick = false;
    private long delayMs = DEFAULT_DELAY_MS;

    private String selectedPackage = "";
    private String triggerText = DEFAULT_TRIGGER;

    private float targetX = 500;
    private float targetY = 500;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable secondClick = () -> {
        if (!active) return;
        clickTarget();
        waitingForSecondClick = false;
    };

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        loadConfiguration();
        showBubble();
    }

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {
        if (!active || waitingForSecondClick) return;

        String packageName = event.getPackageName() == null
                ? "" : event.getPackageName().toString();

        if (!selectedPackage.isEmpty() && !selectedPackage.equals(packageName)) {
            return;
        }

        // Only react when the currently exposed countdown text is EXACTLY "15"
        // (or the configured trigger). We intentionally do not use contains().
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        AccessibilityNodeInfo countdown = findExactText(root, triggerText);
        if (countdown == null) return;

        // Exact match found. Click Target B, then wait 22 seconds before
        // clicking Target B a second time.
        waitingForSecondClick = true;

        if (!clickAccessibleTarget(root)) {
            // Fallback for a target that is not exposed as an accessibility node.
            click(targetX, targetY);
        }

        handler.removeCallbacks(secondClick);
        handler.postDelayed(secondClick, delayMs);
        root.recycle();
    }

    @Override
    public void onInterrupt() {
        // Android interrupted the accessibility service; leave automation stopped.
        active = false;
        waitingForSecondClick = false;
        handler.removeCallbacks(secondClick);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(secondClick);
        remove(bubble);
        remove(calibration);
        instance = null;
        super.onDestroy();
    }

    public void configure(String packageName, String trigger, long delaySeconds) {
        selectedPackage = packageName == null ? "" : packageName;
        triggerText = normalizeNumber(trigger);
        if (triggerText.isEmpty()) triggerText = DEFAULT_TRIGGER;
        delayMs = Math.max(0, delaySeconds) * 1000L;

        getSharedPreferences("automation", MODE_PRIVATE)
                .edit()
                .putString("package", selectedPackage)
                .putString("trigger", triggerText)
                .putLong("delay_ms", delayMs)
                .apply();
    }

    public void activate() {
        loadConfiguration();
        active = true;
        waitingForSecondClick = false;
        Toast.makeText(this, "Automation ACTIVE: countdown == " + triggerText, Toast.LENGTH_SHORT).show();
    }

    public void pause() {
        active = false;
        waitingForSecondClick = false;
        handler.removeCallbacks(secondClick);
        Toast.makeText(this, "Automation stopped", Toast.LENGTH_SHORT).show();
    }

    public boolean isActive() {
        return active;
    }

    private void loadConfiguration() {
        android.content.SharedPreferences p = getSharedPreferences("automation", MODE_PRIVATE);
        selectedPackage = p.getString("package", "");
        triggerText = p.getString("trigger", DEFAULT_TRIGGER);
        delayMs = p.getLong("delay_ms", DEFAULT_DELAY_MS);
        if (triggerText == null || triggerText.isEmpty()) triggerText = DEFAULT_TRIGGER;
    }

    private String normalizeNumber(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", "");
    }

    /**
     * Exact text matching: a node must have text == "15".
     * We do not use contains("15"), so 150/115/etc. cannot trigger it.
     */
    private AccessibilityNodeInfo findExactText(AccessibilityNodeInfo node, String wanted) {
        if (node == null) return null;

        CharSequence text = node.getText();
        if (text != null && normalizeNumber(text.toString()).equals(wanted)) {
            return AccessibilityNodeInfo.obtain(node);
        }

        CharSequence description = node.getContentDescription();
        if (description != null && normalizeNumber(description.toString()).equals(wanted)) {
            return AccessibilityNodeInfo.obtain(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = findExactText(child, wanted);
            if (child != null) child.recycle();
            if (found != null) return found;
        }
        return null;
    }

    private boolean clickAccessibleTarget(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo target = findTargetNode(root);
        if (target == null) return false;

        boolean clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (!clicked) {
            AccessibilityNodeInfo parent = target.getParent();
            if (parent != null) {
                clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                parent.recycle();
            }
        }
        target.recycle();
        return clicked;
    }

    private AccessibilityNodeInfo findTargetNode(AccessibilityNodeInfo node) {
        if (node == null) return null;

        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();

        if ((text != null && isTargetName(text.toString()))
                || (desc != null && isTargetName(desc.toString()))) {
            return AccessibilityNodeInfo.obtain(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = findTargetNode(child);
            if (child != null) child.recycle();
            if (found != null) return found;
        }
        return null;
    }

    private boolean isTargetName(String value) {
        String s = value.trim().toLowerCase(Locale.ROOT);
        return s.equals("target b")
                || s.equals("target_b")
                || s.equals("b");
    }

    private void showBubble() {
        if (bubble != null) return;

        TextView b = new TextView(this);
        b.setText("A");
        b.setTextColor(Color.WHITE);
        b.setTextSize(18);
        b.setGravity(Gravity.CENTER);

        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.rgb(21, 101, 192));
        g.setShape(GradientDrawable.OVAL);
        b.setBackground(g);

        WindowManager.LayoutParams p = overlayParams();
        p.width = 64;
        p.height = 64;
        p.x = 20;
        p.y = 140;

        b.setOnClickListener(v -> showMenu());
        bubble = b;
        wm.addView(b, p);
    }

    private void showMenu() {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(18, 12, 18, 12);
        box.setBackgroundColor(Color.WHITE);

        TextView status = new TextView(this);
        status.setText("AUTO TEST\nStatus: " + (active ? "ACTIVE" : "STOPPED")
                + "\nTrigger: countdown == " + triggerText
                + "\nDelay: " + (delayMs / 1000L) + " seconds");
        status.setTextSize(16);
        box.addView(status);

        Button activate = new Button(this);
        activate.setText("Activate");
        activate.setOnClickListener(v -> {
            this.activate();
            remove(box);
        });
        box.addView(activate);

        Button stop = new Button(this);
        stop.setText("Stop");
        stop.setOnClickListener(v -> {
            pause();
            remove(box);
        });
        box.addView(stop);

        Button stopAll = new Button(this);
        stopAll.setText("Stop All Active");
        stopAll.setOnClickListener(v -> {
            pause();
            remove(box);
        });
        box.addView(stopAll);

        Button close = new Button(this);
        close.setText("Close");
        close.setOnClickListener(v -> remove(box));
        box.addView(close);

        WindowManager.LayoutParams p = overlayParams();
        p.width = 620;
        p.height = WindowManager.LayoutParams.WRAP_CONTENT;
        p.x = 20;
        p.y = 220;
        wm.addView(box, p);
    }

    public void startTargetCalibration() {
        remove(calibration);

        TextView v = calibrationView("Tap the center of Target B\nTap once to save");
        calibration = v;

        WindowManager.LayoutParams p = overlayParams();
        p.width = -1;
        p.height = -1;

        v.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                targetX = event.getRawX();
                targetY = event.getRawY();
                remove(calibration);
                Toast.makeText(this,
                        "Target B saved: " + (int) targetX + "," + (int) targetY,
                        Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        wm.addView(v, p);
    }

    private TextView calibrationView(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(22);
        v.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        v.setPadding(20, 100, 20, 20);
        v.setBackgroundColor(0x55000000);
        return v;
    }

    private WindowManager.LayoutParams overlayParams() {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                android.graphics.PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        return p;
    }

    public void clickTarget() {
        click(targetX, targetY);
    }

    private void click(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 80);
        dispatchGesture(
                new GestureDescription.Builder().addStroke(stroke).build(),
                null,
                null);
    }

    private void remove(View v) {
        if (v != null && wm != null) {
            try {
                wm.removeView(v);
            } catch (Exception ignored) {
            }
        }
    }
}

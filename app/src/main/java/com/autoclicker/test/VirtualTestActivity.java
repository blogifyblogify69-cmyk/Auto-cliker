package com.autoclicker.test;

import android.app.Activity;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.provider.Settings;
import android.content.ComponentName;
import android.view.accessibility.AccessibilityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Build;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class VirtualTestActivity extends Activity implements AutomationController.Host {
    private static final String ACTION_ACTIVE = "com.floatinger.demo.ACTION_ACTIVE";
    private static final String ACTION_STOP = "com.floatinger.demo.ACTION_STOP";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView countdownView;
    private TextView stateView;
    private TextView configView;
    private Button targetA;
    private Button targetB;

    private AutomationController controller;
    private boolean active;
    private int countdown = 30;
    private boolean countdownRunning;
    private String targetAValue = "1.50";
    private int triggerValue = 15;
    private int delaySeconds = 22;

    private final BroadcastReceiver controllerReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            String a = intent.getStringExtra("target_a");
            if (a != null && !a.trim().isEmpty()) targetAValue = a.trim();

            triggerValue = clamp(intent.getIntExtra("trigger", 15), 1, 999);
            delaySeconds = clamp(intent.getIntExtra("delay", 22), 0, 3600);
            controller.configure(triggerValue, delaySeconds);

            if (ACTION_ACTIVE.equals(intent.getAction())) {
                activate();
            } else if (ACTION_STOP.equals(intent.getAction())) {
                stop();
            }
            updateConfig();
        }
    };

    private final Runnable countdownTick = new Runnable() {
        @Override public void run() {
            if (!countdownRunning) return;

            countdownView.setText(String.valueOf(countdown));

            if (active) {
                controller.observe(countdown == triggerValue);
            }

            countdown--;
            if (countdown < 0) countdown = 30;
            handler.postDelayed(this, 1000L);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        controller = new AutomationController(this);
        buildUi();
        registerControllerReceiver();
        startVirtualCountdown();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerControllerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ACTIVE);
        filter.addAction(ACTION_STOP);

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(controllerReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(controllerReceiver, filter);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(28, 28, 28, 28);
        root.setBackgroundColor(Color.rgb(245, 245, 245));
        root.setOnApplyWindowInsetsListener((View v, WindowInsets insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars =
                        insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(28, 28 + top, 28, 28 + bottom);
            return insets;
        });

        TextView title = new TextView(this);
        title.setText("VIRTUAL QA TEST APP");
        title.setTextSize(22);
        title.setTextColor(Color.rgb(21, 101, 192));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        configView = new TextView(this);
        configView.setTextSize(15);
        configView.setPadding(8, 18, 8, 18);
        root.addView(configView);

        countdownView = new TextView(this);
        countdownView.setText("30");
        countdownView.setTextSize(64);
        countdownView.setGravity(Gravity.CENTER);
        countdownView.setTextColor(Color.BLACK);
        root.addView(countdownView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 150));

        stateView = new TextView(this);
        stateView.setText("AUTOMATION STOPPED");
        stateView.setTextSize(18);
        stateView.setGravity(Gravity.CENTER);
        root.addView(stateView);

        targetA = new Button(this);
        targetA.setText("TARGET A");
        targetA.setTextSize(20);
        targetA.setOnClickListener(v -> status("Target A clicked (QA test)."));
        root.addView(targetA,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 85));

        targetB = new Button(this);
        targetB.setText("TARGET B");
        targetB.setTextSize(20);
        targetB.setOnClickListener(v -> status("Target B clicked (QA test)."));
        root.addView(targetB,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 85));

        Button accessibility = new Button(this);
        accessibility.setText("ACCESSIBILITY: CHECK / ENABLE");
        accessibility.setOnClickListener(v -> openAccessibilitySettings());
        root.addView(accessibility);

        Button activate = new Button(this);
        activate.setText("ACTIVATE LOCALLY");
        activate.setOnClickListener(v -> activate());
        root.addView(activate);

        Button stop = new Button(this);
        stop.setText("STOP LOCALLY");
        stop.setOnClickListener(v -> stop());
        root.addView(stop);

        Button reset = new Button(this);
        reset.setText("RESET COUNTDOWN");
        reset.setOnClickListener(v -> {
            countdown = 30;
            controller.reset();
            status("Countdown reset. Waiting for " + triggerValue + ".");
        });
        root.addView(reset);

        setContentView(root);
        root.requestApplyInsets();
        updateConfig();
        updateAccessibilityButton(accessibility);
    }

    private void updateAccessibilityButton(Button button) {
        button.setText(isAccessibilityServiceEnabled()
                ? "ACCESSIBILITY: ENABLED"
                : "ACCESSIBILITY: OFF — TAP TO ENABLE");
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager manager =
                (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (manager == null) return false;

        String expected = new ComponentName(this, TestAccessibilityService.class).flattenToString();
        for (AccessibilityServiceInfo info :
                manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (info.getResolveInfo() == null || info.getResolveInfo().serviceInfo == null) continue;
            ComponentName component = new ComponentName(
                    info.getResolveInfo().serviceInfo.packageName,
                    info.getResolveInfo().serviceInfo.name);
            if (expected.equals(component.flattenToString())) return true;
        }
        return false;
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception ignored) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void updateConfig() {
        if (configView == null) return;
        configView.setText(
                "Floatinger connection: TEST MODE\n" +
                "Target A value: " + targetAValue + "\n" +
                "Trigger: " + triggerValue + "\n" +
                "Delay after B: " + delaySeconds + " seconds");
        if (targetA != null) targetA.setText("TARGET A (" + targetAValue + ")");
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void startVirtualCountdown() {
        if (!countdownRunning) {
            countdownRunning = true;
            handler.post(countdownTick);
        }
    }

    @Override public boolean isActive() {
        return active;
    }

    public void activate() {
        active = true;
        controller.reset();
        status("ACTIVE — waiting for exactly " + triggerValue + ".");
    }

    public void stop() {
        active = false;
        controller.stop();
        status("AUTOMATION STOPPED.");
    }

    @Override public void clickTarget(String target, Runnable success, Runnable failure) {
        if (!active) {
            failure.run();
            return;
        }

        if ("B".equals(target)) {
            targetB.performClick();
            success.run();
        } else if ("A".equals(target)) {
            targetA.performClick();
            success.run();
        } else {
            failure.run();
        }
    }

    @Override public void status(String message) {
        if (stateView != null) stateView.setText(message);
    }

    @Override protected void onDestroy() {
        try { unregisterReceiver(controllerReceiver); } catch (Exception ignored) {}
        countdownRunning = false;
        handler.removeCallbacksAndMessages(null);
        controller.stop();
        super.onDestroy();
    }
}

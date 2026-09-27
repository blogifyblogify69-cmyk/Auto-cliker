package com.autoclicker.test;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class VirtualTestActivity extends Activity implements AutomationController.Host {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView countdownView;
    private TextView stateView;
    private Button targetA;
    private Button targetB;

    private AutomationController controller;
    private boolean active;
    private int countdown = 30;
    private boolean countdownRunning;

    private final Runnable countdownTick = new Runnable() {
        @Override public void run() {
            if (!countdownRunning) return;

            countdownView.setText(String.valueOf(countdown));

            if (active) {
                controller.observe(countdown == 15);
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
        startVirtualCountdown();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(28, 28, 28, 28);
        root.setOnApplyWindowInsetsListener((View v, WindowInsets insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
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
        title.setText("AUTO CLICKER — VIRTUAL TEST");
        title.setTextSize(22);
        title.setTextColor(Color.rgb(21, 101, 192));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("Self-contained test UI. No screen inspection, overlay, "
                + "AccessibilityService, or special permission is used.\n\n"
                + "Rule: exactly 15 → Target B → wait exactly 22 seconds → Target A.");
        info.setTextSize(15);
        info.setPadding(8, 20, 8, 20);
        root.addView(info);

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
        targetA.setOnClickListener(v -> status("Target A clicked by automation."));
        root.addView(targetA,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 85));

        targetB = new Button(this);
        targetB.setText("TARGET B");
        targetB.setTextSize(20);
        targetB.setOnClickListener(v -> status("Target B clicked by automation."));
        root.addView(targetB,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 85));

        Button activate = new Button(this);
        activate.setText("ACTIVATE AUTOMATION");
        activate.setOnClickListener(v -> activate());
        root.addView(activate);

        Button stop = new Button(this);
        stop.setText("STOP AUTOMATION");
        stop.setOnClickListener(v -> stop());
        root.addView(stop);

        Button reset = new Button(this);
        reset.setText("RESET COUNTDOWN");
        reset.setOnClickListener(v -> {
            countdown = 30;
            controller.reset();
            status("Countdown reset. Waiting for exactly 15.");
        });
        root.addView(reset);

        setContentView(root);
        root.requestApplyInsets();
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
        status("ACTIVE — waiting for exactly 15.");
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
        countdownRunning = false;
        handler.removeCallbacksAndMessages(null);
        controller.stop();
        super.onDestroy();
    }
}

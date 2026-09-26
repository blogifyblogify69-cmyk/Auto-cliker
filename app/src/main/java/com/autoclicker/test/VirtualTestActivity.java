package com.autoclicker.test;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

            // The virtual environment exposes the state directly. No screen
            // inspection or AccessibilityService is used.
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

        TextView title = new TextView(this);
        title.setText("AUTO CLICKER — VIRTUAL TEST ENVIRONMENT");
        title.setTextSize(22);
        title.setTextColor(Color.rgb(21, 101, 192));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText(
                "Self-contained test environment\n\n"
                + "The countdown, Target A and Target B are controls owned by this APK.\n"
                + "The automation does not inspect another app, capture the screen, use overlays, "
                + "or request AccessibilityService access.\n\n"
                + "Rule: when countdown is exactly 15 → click B → wait exactly 22 seconds → click A."
        );
        info.setTextSize(15);
        info.setPadding(8, 24, 8, 24);
        root.addView(info);

        countdownView = new TextView(this);
        countdownView.setText("30");
        countdownView.setTextSize(64);
        countdownView.setGravity(Gravity.CENTER);
        countdownView.setTextColor(Color.BLACK);
        root.addView(countdownView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 180));

        stateView = new TextView(this);
        stateView.setText("AUTOMATION STOPPED");
        stateView.setTextSize(18);
        stateView.setGravity(Gravity.CENTER);
        root.addView(stateView);

        targetA = new Button(this);
        targetA.setText("TARGET A");
        targetA.setTextSize(20);
        targetA.setOnClickListener(v ->
                Toast.makeText(this, "Target A clicked", Toast.LENGTH_SHORT).show());
        root.addView(targetA,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 90));

        targetB = new Button(this);
        targetB.setText("TARGET B");
        targetB.setTextSize(20);
        targetB.setOnClickListener(v ->
                Toast.makeText(this, "Target B clicked", Toast.LENGTH_SHORT).show());
        root.addView(targetB,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 90));

        Button activate = new Button(this);
        activate.setText("ACTIVATE AUTOMATION");
        activate.setOnClickListener(v -> activate());
        root.addView(activate);

        Button stop = new Button(this);
        stop.setText("STOP AUTOMATION");
        stop.setOnClickListener(v -> stop());
        root.addView(stop);

        Button reset = new Button(this);
        reset.setText("RESET VIRTUAL COUNTDOWN");
        reset.setOnClickListener(v -> {
            countdown = 30;
            countdownView.setText("30");
            controller.reset();
            status("Countdown reset. Waiting for 15.");
        });
        root.addView(reset);

        setContentView(root);
    }

    private void startVirtualCountdown() {
        if (countdownRunning) return;
        countdownRunning = true;
        handler.post(countdownTick);
    }

    @Override public boolean isActive() {
        return active;
    }

    @Override public void activate() {
        active = true;
        controller.reset();
        status("AUTOMATION ACTIVE — waiting for exactly 15.");
    }

    @Override public void stop() {
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
            return;
        }

        if ("A".equals(target)) {
            targetA.performClick();
            success.run();
            return;
        }

        failure.run();
    }

    @Override public void status(String message) {
        statusView.setText(message);
    }

    @Override protected void onDestroy() {
        countdownRunning = false;
        handler.removeCallbacksAndMessages(null);
        controller.stop();
        super.onDestroy();
    }
}

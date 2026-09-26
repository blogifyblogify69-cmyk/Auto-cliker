package com.autoclicker.test;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int TRIGGER_COUNTDOWN = 15;
    private static final long SECOND_ACTION_DELAY_MS = 22_000L;
    private static final int DEFAULT_START_COUNTDOWN = 30;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView countdownView;
    private TextView statusView;
    private TextView logView;
    private Button targetA;
    private Button targetB;
    private EditText startValue;

    private int countdown = DEFAULT_START_COUNTDOWN;
    private boolean testRunning;
    private boolean automationEnabled = true;
    private boolean waitingForSecondAction;
    private boolean triggeredAt15;

    private final Runnable countdownTick = new Runnable() {
        @Override public void run() {
            if (!testRunning) return;

            updateCountdown();

            if (countdown > 0) {
                countdown--;
                handler.postDelayed(this, 1000L);
            } else {
                testRunning = false;
                updateStatus("Countdown finished. Test is idle.");
                appendLog("Countdown finished.");
            }
        }
    };

    private final Runnable secondAction = () -> {
        if (!waitingForSecondAction) return;

        targetA.performClick();
        waitingForSecondAction = false;
        updateStatus("Automation complete: Target A clicked after 22 seconds.");
        appendLog("22 seconds elapsed -> Target A clicked automatically.");
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        scroll.addView(root);

        TextView title = text("Auto Clicker Test", 28);
        title.setTextColor(Color.rgb(21, 101, 192));
        root.addView(title);

        root.addView(text(
                "Safe self-test mode\n\n"
                + "This APK tests its own UI. It does not use AccessibilityService, "
                + "screen capture, overlay permission, or external-app control.", 15));

        countdownView = text("", 64);
        countdownView.setGravity(Gravity.CENTER);
        root.addView(countdownView, new LinearLayout.LayoutParams(-1, 150));

        statusView = text("Ready. Automation is ON.", 16);
        root.addView(statusView);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        targetA = new Button(this);
        targetA.setText("Target A");
        targetA.setContentDescription("Target A");
        targetA.setOnClickListener(v -> {
            appendLog("Target A clicked.");
            updateStatus("Target A clicked.");
        });

        targetB = new Button(this);
        targetB.setText("Target B");
        targetB.setContentDescription("Target B");
        targetB.setOnClickListener(v -> {
            appendLog("Target B clicked.");
            updateStatus("Target B clicked.");
        });

        row.addView(targetA, weightParams());
        row.addView(targetB, weightParams());
        root.addView(row);

        startValue = new EditText(this);
        startValue.setInputType(2);
        startValue.setText(String.valueOf(DEFAULT_START_COUNTDOWN));
        startValue.setHint("Starting countdown");
        root.addView(startValue);

        Button start = new Button(this);
        start.setText("START TEST");
        start.setOnClickListener(v -> startTest());
        root.addView(start);

        Button toggle = new Button(this);
        toggle.setText("AUTOMATION: ON");
        toggle.setOnClickListener(v -> {
            automationEnabled = !automationEnabled;
            toggle.setText(automationEnabled ? "AUTOMATION: ON" : "AUTOMATION: OFF");
            updateStatus(automationEnabled ? "Automation enabled." : "Automation disabled.");
            appendLog(automationEnabled ? "Automation enabled." : "Automation disabled.");
        });
        root.addView(toggle);

        Button reset = new Button(this);
        reset.setText("RESET");
        reset.setOnClickListener(v -> resetTest());
        root.addView(reset);

        root.addView(text(
                "MAIN LOGIC\n\n"
                + "Countdown == 15\n"
                + "-> Automatically click Target B\n"
                + "-> Wait exactly 22 seconds\n"
                + "-> Automatically click Target A\n\n"
                + "Only the exact numeric value 15 triggers the workflow.", 17));

        logView = text("EVENT LOG\n", 14);
        root.addView(logView);

        setContentView(scroll);
        updateCountdown();
    }

    private void startTest() {
        int start;
        try {
            start = Integer.parseInt(startValue.getText().toString().trim());
        } catch (Exception e) {
            Toast.makeText(this, "Enter a valid starting number.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (start < 0 || start > 999) {
            Toast.makeText(this, "Starting number must be 0-999.", Toast.LENGTH_SHORT).show();
            return;
        }

        handler.removeCallbacksAndMessages(null);
        countdown = start;
        testRunning = true;
        waitingForSecondAction = false;
        triggeredAt15 = false;

        appendLog("Test started at " + countdown + ".");
        updateStatus("Monitoring countdown. Waiting for exactly 15.");
        handler.post(countdownTick);
    }

    private void updateCountdown() {
        countdownView.setText(String.valueOf(countdown));

        if (automationEnabled
                && testRunning
                && countdown == TRIGGER_COUNTDOWN
                && !triggeredAt15
                && !waitingForSecondAction) {

            triggeredAt15 = true;
            waitingForSecondAction = true;

            targetB.performClick();
            appendLog("Countdown == 15 -> Target B clicked automatically.");
            updateStatus("Target B clicked. Waiting 22 seconds for Target A.");

            handler.postDelayed(secondAction, SECOND_ACTION_DELAY_MS);
        }

        // Prevent repeated triggers while the same 15 value is displayed.
        if (countdown != TRIGGER_COUNTDOWN && !waitingForSecondAction) {
            triggeredAt15 = false;
        }
    }

    private void resetTest() {
        handler.removeCallbacksAndMessages(null);
        testRunning = false;
        waitingForSecondAction = false;
        triggeredAt15 = false;
        countdown = DEFAULT_START_COUNTDOWN;
        updateCountdown();
        updateStatus("Reset. Ready for another test.");
        appendLog("Test reset.");
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setPadding(18, 14, 18, 14);
        return t;
    }

    private LinearLayout.LayoutParams weightParams() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private void updateStatus(String value) {
        if (statusView != null) statusView.setText(value);
    }

    private void appendLog(String value) {
        if (logView == null) return;
        logView.setText(logView.getText().toString() + value + "\n");
    }
}

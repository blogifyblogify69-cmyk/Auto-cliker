package com.autoclicker.test;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private static final String SERVICE_NAME =
            "com.autoclicker.test/.AutomationAccessibilityService";

    private SharedPreferences prefs;
    private Spinner appSpinner;
    private EditText triggerInput;
    private EditText targetAText;
    private EditText targetBText;
    private TextView status;

    private final List<ResolveInfo> launchableApps = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(AutomationAccessibilityService.PREFS, MODE_PRIVATE);
        buildUi();
    }

    @Override protected void onResume() {
        super.onResume();
        if (status != null) updateStatus();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        TextView title = text("Auto Clicker Test", 28);
        title.setTextColor(Color.rgb(21, 101, 192));
        root.addView(title);

        TextView disclosure = text(
                "IMPORTANT AUTOMATION DISCLOSURE\n\n"
                + "This app uses Android AccessibilityService only after you explicitly enable it. "
                + "It reads the selected test app's visible UI to find the exact countdown value "
                + "and the configured Target A/Target B controls. It can perform the fixed rule "
                + ""countdown = 15 -> click B -> wait 22 seconds -> click A". "
                + "It does not record the screen, capture screenshots, upload UI data, or make decisions "
                + "outside this fixed rule.\n\n"
                + "Only enable the service when you understand and want this automation.", 14);
        disclosure.setTextColor(Color.DKGRAY);
        root.addView(disclosure);

        Button enable = new Button(this);
        enable.setText("ENABLE ACCESSIBILITY");
        enable.setOnClickListener(v -> {
            Toast.makeText(this,
                    "Android Settings will open. Enable Auto Clicker Test, then return here.",
                    Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open Accessibility Settings.", Toast.LENGTH_LONG).show();
            }
        });
        root.addView(enable);

        status = text("", 15);
        root.addView(status);

        root.addView(text("1. SELECT INSTALLED APP", 18));
        appSpinner = new Spinner(this);
        root.addView(appSpinner);

        Button refresh = new Button(this);
        refresh.setText("REFRESH INSTALLED APPS");
        refresh.setOnClickListener(v -> loadApps());
        root.addView(refresh);

        Button launch = new Button(this);
        launch.setText("LAUNCH SELECTED APP");
        launch.setOnClickListener(v -> launchSelectedApp());
        root.addView(launch);

        root.addView(text("2. AUTOMATION RULE", 18));

        triggerInput = input("15");
        root.addView(labeled("Countdown exact value", triggerInput));

        targetAText = input("Target A");
        root.addView(labeled("Target A text/content description", targetAText));

        targetBText = input("Target B");
        root.addView(labeled("Target B text/content description", targetBText));

        Button save = new Button(this);
        save.setText("SAVE AUTOMATION SETTINGS");
        save.setOnClickListener(v -> saveSettings());
        root.addView(save);

        TextView rule = text(
                "FIXED WORKFLOW\n\n"
                + "Countdown == 15\n"
                + "↓\n"
                + "Click Target B\n"
                + "↓\n"
                + "Wait 22 seconds\n"
                + "↓\n"
                + "Click Target A\n\n"
                + "Target controls are searched by exact text/content description first. "
                + "If the target app does not expose accessible controls, use the floating menu's "
                + "CALIBRATE TARGET A / CALIBRATE TARGET B options.", 16);
        root.addView(rule);

        Button active = new Button(this);
        active.setText("ACTIVATE AUTOMATION");
        active.setOnClickListener(v -> {
            saveSettings();
            prefs.edit().putBoolean(AutomationAccessibilityService.KEY_ACTIVE, true).apply();
            Toast.makeText(this, "Automation ACTIVE. Launch the selected app.", Toast.LENGTH_SHORT).show();
        });
        root.addView(active);

        Button stop = new Button(this);
        stop.setText("STOP AUTOMATION");
        stop.setOnClickListener(v -> {
            prefs.edit().putBoolean(AutomationAccessibilityService.KEY_ACTIVE, false).apply();
            Toast.makeText(this, "Automation stopped.", Toast.LENGTH_SHORT).show();
        });
        root.addView(stop);

        setContentView(root);
        loadApps();
        loadSettingsIntoUi();
        updateStatus();
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        launchableApps.clear();
        launchableApps.addAll(pm.queryIntentActivities(intent, 0));
        Collections.sort(launchableApps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString());
            }
        });

        List<String> labels = new ArrayList<>();
        for (ResolveInfo info : launchableApps) {
            labels.add(info.loadLabel(pm) + "\n" + info.activityInfo.packageName);
        }
        if (labels.isEmpty()) labels.add("No launchable apps found");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appSpinner.setAdapter(adapter);

        String selected = prefs.getString(AutomationAccessibilityService.KEY_PACKAGE, "");
        if (!selected.isEmpty()) {
            for (int i = 0; i < launchableApps.size(); i++) {
                if (launchableApps.get(i).activityInfo.packageName.equals(selected)) {
                    appSpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private void launchSelectedApp() {
        if (launchableApps.isEmpty()) {
            Toast.makeText(this, "No launchable app selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        ResolveInfo info = launchableApps.get(appSpinner.getSelectedItemPosition());
        String pkg = info.activityInfo.packageName;
        prefs.edit().putString(AutomationAccessibilityService.KEY_PACKAGE, pkg).apply();

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launchIntent == null) {
            Toast.makeText(this, "Could not launch selected app.", Toast.LENGTH_LONG).show();
            return;
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launchIntent);
        Toast.makeText(this, "Selected app launched.", Toast.LENGTH_SHORT).show();
    }

    private void saveSettings() {
        String trigger = triggerInput.getText().toString().trim();
        if (trigger.isEmpty()) trigger = "15";

        prefs.edit()
                .putString(AutomationAccessibilityService.KEY_TRIGGER, trigger)
                .putString(AutomationAccessibilityService.KEY_TARGET_A_TEXT,
                        targetAText.getText().toString().trim())
                .putString(AutomationAccessibilityService.KEY_TARGET_B_TEXT,
                        targetBText.getText().toString().trim())
                .apply();

        if (!launchableApps.isEmpty() && appSpinner.getSelectedItemPosition() >= 0) {
            ResolveInfo info = launchableApps.get(appSpinner.getSelectedItemPosition());
            prefs.edit().putString(
                    AutomationAccessibilityService.KEY_PACKAGE,
                    info.activityInfo.packageName).apply();
        }

        Toast.makeText(this, "Automation settings saved.", Toast.LENGTH_SHORT).show();
    }

    private void loadSettingsIntoUi() {
        triggerInput.setText(prefs.getString(AutomationAccessibilityService.KEY_TRIGGER, "15"));
        targetAText.setText(prefs.getString(
                AutomationAccessibilityService.KEY_TARGET_A_TEXT, "Target A"));
        targetBText.setText(prefs.getString(
                AutomationAccessibilityService.KEY_TARGET_B_TEXT, "Target B"));
    }

    private void updateStatus() {
        boolean enabled = isAccessibilityEnabled();
        boolean active = prefs.getBoolean(AutomationAccessibilityService.KEY_ACTIVE, false);
        status.setText("Accessibility: " + (enabled ? "ENABLED" : "NOT ENABLED")
                + "\nAutomation: " + (active ? "ACTIVE" : "STOPPED"));
    }

    private boolean isAccessibilityEnabled() {
        String enabledServices = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabledServices != null && enabledServices.toLowerCase()
                .contains(SERVICE_NAME.toLowerCase());
    }

    private EditText input(String value) {
        EditText e = new EditText(this);
        e.setText(value);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        return e;
    }

    private LinearLayout labeled(String label, EditText input) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView l = text(label, 14);
        box.addView(l);
        box.addView(input);
        return box;
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setPadding(12, 12, 12, 12);
        return t;
    }
}

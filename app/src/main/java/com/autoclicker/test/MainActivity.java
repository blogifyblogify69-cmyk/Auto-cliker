package com.autoclicker.test;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
                "AUTOMATION DISCLOSURE\n\n"
                + "This app uses Android AccessibilityService only after you explicitly enable it. "
                + "The automation is fixed and user-controlled:\n\n"
                + "Countdown exactly 15 -> click Target B -> wait 22 seconds -> click Target A.\n\n"
                + "The countdown trigger and 22-second delay are built in. You do not need to enter "
                + "any trigger, timing, keystore, password, or signing key. "
                + "Target A and Target B are selected using the floating calibration controls.\n\n"
                + "This test build does not record or upload screen data.", 14);
        disclosure.setTextColor(Color.DKGRAY);
        root.addView(disclosure);

        Button enable = new Button(this);
        enable.setText("ENABLE ACCESSIBILITY");
        enable.setOnClickListener(v -> {
            Toast.makeText(this,
                    "Enable Auto Clicker Test in Android Accessibility Settings, then return.",
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

        root.addView(text("SELECT INSTALLED TEST APP", 18));
        appSpinner = new Spinner(this);
        root.addView(appSpinner);

        Button refresh = new Button(this);
        refresh.setText("REFRESH APPS");
        refresh.setOnClickListener(v -> loadApps());
        root.addView(refresh);

        Button launch = new Button(this);
        launch.setText("LAUNCH SELECTED APP");
        launch.setOnClickListener(v -> launchSelectedApp());
        root.addView(launch);

        TextView rule = text(
                "FIXED AUTOMATION\n\n"
                + "1. Detect countdown = 15\n"
                + "2. Click Target B\n"
                + "3. Wait exactly 22 seconds\n"
                + "4. Click Target A\n"
                + "5. Wait until 15 disappears before another cycle\n\n"
                + "After launching the selected app, use the floating A button. "
                + "Open it and choose CALIBRATE TARGET A / CALIBRATE TARGET B to mark "
                + "the two screen positions. Then choose ACTIVE.", 16);
        root.addView(rule);

        Button active = new Button(this);
        active.setText("ACTIVATE AUTOMATION");
        active.setOnClickListener(v -> {
            saveSelectedPackage();
            prefs.edit().putBoolean(AutomationAccessibilityService.KEY_ACTIVE, true).apply();
            Toast.makeText(this, "Automation ACTIVE.", Toast.LENGTH_SHORT).show();
            updateStatus();
        });
        root.addView(active);

        Button stop = new Button(this);
        stop.setText("STOP AUTOMATION");
        stop.setOnClickListener(v -> {
            prefs.edit().putBoolean(AutomationAccessibilityService.KEY_ACTIVE, false).apply();
            Toast.makeText(this, "Automation STOPPED.", Toast.LENGTH_SHORT).show();
            updateStatus();
        });
        root.addView(stop);

        setContentView(root);
        loadApps();
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

    private void saveSelectedPackage() {
        if (!launchableApps.isEmpty() && appSpinner.getSelectedItemPosition() >= 0) {
            ResolveInfo info = launchableApps.get(appSpinner.getSelectedItemPosition());
            prefs.edit().putString(
                    AutomationAccessibilityService.KEY_PACKAGE,
                    info.activityInfo.packageName).apply();
        }
    }

    private void launchSelectedApp() {
        if (launchableApps.isEmpty()) {
            Toast.makeText(this, "No launchable app selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        saveSelectedPackage();
        String pkg = prefs.getString(AutomationAccessibilityService.KEY_PACKAGE, "");
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launchIntent == null) {
            Toast.makeText(this, "Could not launch selected app.", Toast.LENGTH_LONG).show();
            return;
        }

        startActivity(launchIntent);
        Toast.makeText(this, "Selected app launched.", Toast.LENGTH_SHORT).show();
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

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setPadding(12, 12, 12, 12);
        return t;
    }
}

package com.autoclicker.test;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private String selectedPackage;
    private final ArrayList<String> packages = new ArrayList<>();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setPadding(24, 18, 24, 18);
        return t;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        TextView title = text("Auto Clicker Test", 26);
        title.setTextColor(Color.rgb(21, 101, 192));
        root.addView(title);

        root.addView(text(
                "Installation uses no normal runtime permission. After installation, Android will ask you to explicitly enable the Accessibility Service in Settings. " +
                "The service is used only for your selected test app.", 15));

        TextView rule = text(
                "AUTOMATION RULE\n\n" +
                "Countdown == 15\n" +
                "        ↓\n" +
                "Click Target B\n" +
                "        ↓\n" +
                "Wait 22 seconds\n" +
                "        ↓\n" +
                "Click Target B again", 17);
        rule.setTextColor(Color.DKGRAY);
        root.addView(rule);

        Button access = new Button(this);
        access.setText("1. Enable Accessibility Service");
        access.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(access);

        root.addView(text("2. Select your test app", 19));

        Spinner spinner = new Spinner(this);
        loadApps(spinner);
        root.addView(spinner);

        EditText trigger = new EditText(this);
        trigger.setInputType(2);
        trigger.setHint("3. Countdown trigger (exact text)");
        trigger.setText("15");
        root.addView(trigger);

        EditText delay = new EditText(this);
        delay.setInputType(2);
        delay.setHint("4. Second click delay (seconds)");
        delay.setText("22");
        root.addView(delay);

        Button launch = new Button(this);
        launch.setText("5. Launch Selected App");
        root.addView(launch);

        Button target = new Button(this);
        target.setText("Calibrate Target B (fallback)");
        target.setOnClickListener(v -> {
            if (AutomationAccessibilityService.instance != null) {
                AutomationAccessibilityService.instance.startTargetCalibration();
            } else {
                toast("Enable Accessibility Service first.");
            }
        });
        root.addView(target);

        Button start = new Button(this);
        start.setText("6. START AUTOMATION");
        start.setOnClickListener(v -> {
            if (AutomationAccessibilityService.instance == null) {
                toast("Enable Accessibility Service first.");
                return;
            }

            if (selectedPackage == null || selectedPackage.isEmpty()) {
                toast("Select an app first.");
                return;
            }

            int delaySeconds;
            try {
                delaySeconds = Integer.parseInt(delay.getText().toString().trim());
            } catch (Exception e) {
                toast("Invalid delay.");
                return;
            }

            String triggerValue = trigger.getText().toString().trim();
            if (!triggerValue.matches("\\d+")) {
                toast("Countdown must be a number, e.g. 15.");
                return;
            }

            AutomationAccessibilityService.instance.configure(
                    selectedPackage, triggerValue, delaySeconds);
            AutomationAccessibilityService.instance.activate();
        });
        root.addView(start);

        Button stop = new Button(this);
        stop.setText("STOP ALL ACTIVE");
        stop.setOnClickListener(v -> {
            if (AutomationAccessibilityService.instance != null) {
                AutomationAccessibilityService.instance.pause();
            }
        });
        root.addView(stop);

        root.addView(text(
                "Exact trigger: 15 → Target B click → wait 22 seconds → Target B click. " +
                "The service ignores other apps.", 14));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}

            @Override public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < packages.size()) {
                    selectedPackage = packages.get(position);
                }
            }
        });

        launch.setOnClickListener(v -> {
            if (selectedPackage == null) {
                toast("Select an app first.");
                return;
            }

            Intent intent = getPackageManager()
                    .getLaunchIntentForPackage(selectedPackage);

            if (intent != null) {
                startActivity(intent);
            } else {
                toast("Selected app cannot be launched.");
            }
        });

        setContentView(root);
    }

    private void loadApps(Spinner spinner) {
        List<ApplicationInfo> apps =
                getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);

        ArrayList<String> labels = new ArrayList<>();

        for (ApplicationInfo app : apps) {
            if (getPackageManager().getLaunchIntentForPackage(app.packageName) != null) {
                packages.add(app.packageName);
                labels.add(app.loadLabel(getPackageManager()) + "\n" + app.packageName);
            }
        }

        spinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                labels));

        if (!packages.isEmpty()) {
            selectedPackage = packages.get(0);
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

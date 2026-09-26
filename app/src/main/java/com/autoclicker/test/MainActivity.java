package com.autoclicker.test;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(28, 28, 28, 28);

        TextView title = new TextView(this);
        title.setText("AUTO CLICKER TEST");
        title.setTextSize(30);
        title.setTextColor(Color.rgb(21, 101, 192));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView disclosure = text(
                "PORTABLE VIRTUAL TEST MODE\n\n"
                + "This build contains its own virtual test environment. "
                + "The countdown and Target A/Target B are controls owned by this APK, "
                + "so the automation can operate directly on test state without reading "
                + "another app's screen.\n\n"
                + "No AccessibilityService is required. No screen capture is used. "
                + "No floating overlay is used. No special screen-inspection permission is requested.\n\n"
                + "Fixed rule:\n"
                + "15 exactly → click Target B → wait exactly 22 seconds → click Target A.\n\n"
                + "The cycle will not retrigger while 15 remains visible; 15 must disappear "
                + "and appear again before the next cycle."
        );
        root.addView(disclosure);

        Button open = new Button(this);
        open.setText("OPEN VIRTUAL TEST ENVIRONMENT");
        open.setTextSize(18);
        open.setOnClickListener(v ->
                startActivity(new Intent(this, VirtualTestActivity.class)));
        root.addView(open);

        TextView limits = text(
                "WHY THIS MODE IS PORTABLE\n\n"
                + "The normal Android sandbox does not allow an ordinary APK to silently "
                + "inspect and control another installed app. For cross-app testing, Android "
                + "provides test frameworks such as UI Automator, normally run as instrumentation "
                + "tests on a device/emulator.\n\n"
                + "This portable APK instead keeps the target UI inside its own process, making "
                + "the automation deterministic and permission-free."
        );
        root.addView(limits);

        setContentView(root);
    }

    private TextView text(String value) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(16);
        t.setPadding(8, 18, 8, 18);
        return t;
    }
}

package com.autoclicker.test;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class OverlayController {
    public interface Host {
        boolean isActive();
        void activate();
        void stop();
        void calibrate(String target);
        void openAccessibilitySettings();
    }

    private final WindowManager windowManager;
    private final Host host;
    private final TargetManager targets;
    private final android.content.SharedPreferences prefs;

    private View bubble;
    private View menu;
    private View calibration;
    private String calibrationTarget;

    public OverlayController(WindowManager windowManager,
                             Host host,
                             TargetManager targets,
                             android.content.SharedPreferences prefs) {
        this.windowManager = windowManager;
        this.host = host;
        this.targets = targets;
        this.prefs = prefs;
    }

    public void showBubble() {
        if (bubble != null) return;

        TextView view = new TextView((android.content.Context) host);
        view.setText("A");
        view.setTextSize(22);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(Color.rgb(30, 100, 220));
        view.setOnClickListener(v -> toggleMenu());
        bubble = view;

        WindowManager.LayoutParams params = params(62, 62);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 18;
        params.y = 180;
        add(bubble, params);
    }

    private void toggleMenu() {
        if (menu != null) {
            removeMenu();
            return;
        }

        LinearLayout box = new LinearLayout((android.content.Context) host);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(10, 10, 10, 10);
        box.setBackgroundColor(Color.WHITE);

        Button active = button(host.isActive() ? "STOP" : "ACTIVE");
        active.setOnClickListener(v -> {
            if (host.isActive()) host.stop(); else host.activate();
            removeMenu();
        });
        box.addView(active);

        Button stopAll = button("STOP ALL ACTIVE");
        stopAll.setOnClickListener(v -> host.stop());
        box.addView(stopAll);

        Button a = button("CALIBRATE TARGET A");
        a.setOnClickListener(v -> beginCalibration("A"));
        box.addView(a);

        Button b = button("CALIBRATE TARGET B");
        b.setOnClickListener(v -> beginCalibration("B"));
        box.addView(b);

        Button settings = button("ACCESSIBILITY SETTINGS");
        settings.setOnClickListener(v -> {
            host.openAccessibilitySettings();
            removeMenu();
        });
        box.addView(settings);

        Button close = button("CLOSE");
        close.setOnClickListener(v -> removeMenu());
        box.addView(close);

        menu = box;
        WindowManager.LayoutParams p = params(340, WindowManager.LayoutParams.WRAP_CONTENT);
        p.gravity = Gravity.TOP | Gravity.END;
        p.x = 18;
        p.y = 250;
        add(menu, p);
    }

    private Button button(String label) {
        Button b = new Button((android.content.Context) host);
        b.setText(label);
        return b;
    }

    private void beginCalibration(String target) {
        removeMenu();
        calibrationTarget = target;

        TextView view = new TextView((android.content.Context) host);
        view.setText("CALIBRATE TARGET " + target + "\nTap the exact target location once");
        view.setTextSize(22);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(0x88000000);
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                targets.save(calibrationTarget,
                        Math.round(event.getRawX()), Math.round(event.getRawY()));
                removeCalibration();
                return true;
            }
            return true;
        });

        calibration = view;
        add(calibration, params(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT));
    }

    private WindowManager.LayoutParams params(int width, int height) {
        return new WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
    }

    private void add(View view, WindowManager.LayoutParams params) {
        try {
            windowManager.addView(view, params);
        } catch (Exception ignored) {
        }
    }

    private void removeMenu() {
        if (menu != null) {
            try { windowManager.removeView(menu); } catch (Exception ignored) {}
            menu = null;
        }
    }

    private void removeCalibration() {
        if (calibration != null) {
            try { windowManager.removeView(calibration); } catch (Exception ignored) {}
            calibration = null;
        }
    }

    public void removeAll() {
        removeMenu();
        removeCalibration();
        if (bubble != null) {
            try { windowManager.removeView(bubble); } catch (Exception ignored) {}
            bubble = null;
        }
    }
}

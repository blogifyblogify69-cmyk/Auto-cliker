package com.autoclicker.test;

import android.content.SharedPreferences;
import android.graphics.Point;

public final class TargetManager {
    private final SharedPreferences prefs;

    public TargetManager(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public void save(String target, int x, int y) {
        if ("A".equals(target)) {
            prefs.edit().putInt(AutomationAccessibilityService.KEY_A_X, x)
                    .putInt(AutomationAccessibilityService.KEY_A_Y, y).apply();
        } else {
            prefs.edit().putInt(AutomationAccessibilityService.KEY_B_X, x)
                    .putInt(AutomationAccessibilityService.KEY_B_Y, y).apply();
        }
    }

    public Point get(String target) {
        boolean a = "A".equals(target);
        int x = prefs.getInt(a ? AutomationAccessibilityService.KEY_A_X
                : AutomationAccessibilityService.KEY_B_X, -1);
        int y = prefs.getInt(a ? AutomationAccessibilityService.KEY_A_Y
                : AutomationAccessibilityService.KEY_B_Y, -1);
        return new Point(x, y);
    }

    public boolean isConfigured(String target) {
        Point p = get(target);
        return p.x >= 0 && p.y >= 0;
    }
}

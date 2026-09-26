package com.autoclicker.test;

import android.view.accessibility.AccessibilityNodeInfo;

public final class CountdownDetector {
    public boolean containsExact15(AccessibilityNodeInfo root) {
        return containsExact(root, "15");
    }

    private boolean containsExact(AccessibilityNodeInfo node, String wanted) {
        if (node == null) return false;

        CharSequence text = node.getText();
        if (text != null && normalize(text.toString()).equals(wanted)) return true;

        CharSequence description = node.getContentDescription();
        if (description != null && normalize(description.toString()).equals(wanted)) return true;

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            boolean found = containsExact(child, wanted);
            if (child != null) child.recycle();
            if (found) return true;
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "");
    }
}

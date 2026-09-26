package com.autoclicker.test;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Handler;

public final class GestureExecutor {
    public interface Callback {
        void onSuccess();
        void onFailure();
    }

    private final AccessibilityService service;
    private final Handler handler;

    public GestureExecutor(AccessibilityService service, Handler handler) {
        this.service = service;
        this.handler = handler;
    }

    public boolean tap(Point point, Callback callback) {
        if (point == null || point.x < 0 || point.y < 0) {
            callback.onFailure();
            return false;
        }

        Path path = new Path();
        path.moveTo(point.x, point.y);

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 70))
                .build();

        boolean dispatched = service.dispatchGesture(
                gesture,
                new AccessibilityService.GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription description) {
                        callback.onSuccess();
                    }

                    @Override
                    public void onCancelled(GestureDescription description) {
                        callback.onFailure();
                    }
                },
                handler);

        if (!dispatched) callback.onFailure();
        return dispatched;
    }
}

package com.autoclicker.test;

import android.os.Handler;
import android.os.Looper;

public final class AutomationController {
    public interface Host {
        boolean isActive();
        void clickTarget(String target, Runnable success, Runnable failure);
        void status(String message);
    }

    private static final long DELAY_MS = 22_000L;

    private final Host host;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AutomationState state = AutomationState.WAITING_FOR_15;
    private boolean fifteenVisible;
    private Runnable delayedA;

    public AutomationController(Host host) {
        this.host = host;
    }

    public void reset() {
        cancelDelay();
        state = AutomationState.WAITING_FOR_15;
        fifteenVisible = false;
    }

    public AutomationState getState() {
        return state;
    }

    public void observe(boolean is15) {
        if (!host.isActive()) return;

        if (!is15) {
            fifteenVisible = false;
            return;
        }

        if (fifteenVisible || state != AutomationState.WAITING_FOR_15) return;

        fifteenVisible = true;
        state = AutomationState.CLICKING_B;
        host.status("15 detected. Clicking Target B.");

        host.clickTarget("B", () -> {
            if (state != AutomationState.CLICKING_B || !host.isActive()) return;
            state = AutomationState.WAITING_22_SECONDS;
            host.status("Target B clicked. Waiting exactly 22 seconds.");
            scheduleA();
        }, () -> {
            state = AutomationState.WAITING_FOR_15;
            fifteenVisible = false;
            host.status("Target B click failed. Waiting for the next 15.");
        });
    }

    private void scheduleA() {
        cancelDelay();
        delayedA = () -> {
            delayedA = null;
            if (!host.isActive() || state != AutomationState.WAITING_22_SECONDS) return;

            state = AutomationState.CLICKING_A;
            host.status("22 seconds elapsed. Clicking Target A.");

            host.clickTarget("A", () -> {
                if (state != AutomationState.CLICKING_A) return;
                state = AutomationState.WAITING_FOR_15;
                host.status("Target A clicked. Waiting for a new 15.");
            }, () -> {
                state = AutomationState.WAITING_FOR_15;
                fifteenVisible = false;
                host.status("Target A click failed. Waiting for the next 15.");
            });
        };
        handler.postDelayed(delayedA, DELAY_MS);
    }

    private void cancelDelay() {
        if (delayedA != null) {
            handler.removeCallbacks(delayedA);
            delayedA = null;
        }
    }

    public void stop() {
        cancelDelay();
        state = AutomationState.WAITING_FOR_15;
        fifteenVisible = false;
    }
}

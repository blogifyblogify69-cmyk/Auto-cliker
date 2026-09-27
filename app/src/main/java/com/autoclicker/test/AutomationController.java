package com.autoclicker.test;

import android.os.Handler;
import android.os.Looper;

public final class AutomationController {
    public interface Host {
        boolean isActive();
        void clickTarget(String target, Runnable success, Runnable failure);
        void status(String message);
    }

    interface Scheduler {
        void postDelayed(Runnable task, long delayMs);
        void remove(Runnable task);
    }

    private static final long DELAY_MS = 22_000L;
    private static final int TARGET_A_CLICKS = 2;

    private final Host host;
    private final Scheduler scheduler;
    private AutomationState state = AutomationState.WAITING_FOR_15;
    private boolean fifteenVisible;
    private Runnable delayedA;
    private int completedAClicks;

    public AutomationController(Host host) {
        this(host, new HandlerScheduler(new Handler(Looper.getMainLooper())));
    }

    AutomationController(Host host, Scheduler scheduler) {
        this.host = host;
        this.scheduler = scheduler;
    }

    public void reset() {
        cancelDelay();
        state = AutomationState.WAITING_FOR_15;
        fifteenVisible = false;
        completedAClicks = 0;
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
            host.status("Target B click failed. Waiting for 15 to disappear.");
        });
    }

    private void scheduleA() {
        cancelDelay();
        delayedA = () -> {
            delayedA = null;
            if (!host.isActive() || state != AutomationState.WAITING_22_SECONDS) return;

            state = AutomationState.CLICKING_A;
            completedAClicks = 0;
            host.status("22 seconds elapsed. Clicking Target A twice.");
            clickNextA();
        };
        scheduler.postDelayed(delayedA, DELAY_MS);
    }

    private void clickNextA() {
        if (!host.isActive() || state != AutomationState.CLICKING_A) return;

        final int clickNumber = completedAClicks + 1;
        host.clickTarget("A", () -> {
            if (state != AutomationState.CLICKING_A || !host.isActive()) return;

            completedAClicks++;
            if (completedAClicks < TARGET_A_CLICKS) {
                host.status("Target A click " + clickNumber + " complete. Performing click 2.");
                clickNextA();
            } else {
                state = AutomationState.WAITING_FOR_15;
                host.status("Target A clicked twice. Waiting for 15 to disappear.");
            }
        }, () -> {
            state = AutomationState.WAITING_FOR_15;
            host.status("Target A click " + clickNumber + " failed. Waiting for 15 to disappear.");
        });
    }

    private void cancelDelay() {
        if (delayedA != null) {
            scheduler.remove(delayedA);
            delayedA = null;
        }
    }

    public void stop() {
        cancelDelay();
        state = AutomationState.WAITING_FOR_15;
        fifteenVisible = false;
        completedAClicks = 0;
    }

    private static final class HandlerScheduler implements Scheduler {
        private final Handler handler;

        HandlerScheduler(Handler handler) {
            this.handler = handler;
        }

        @Override public void postDelayed(Runnable task, long delayMs) {
            handler.postDelayed(task, delayMs);
        }

        @Override public void remove(Runnable task) {
            handler.removeCallbacks(task);
        }
    }
}

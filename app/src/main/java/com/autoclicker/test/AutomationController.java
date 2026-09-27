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

    private static final int TARGET_A_CLICKS = 2;

    private final Host host;
    private final Scheduler scheduler;
    private AutomationState state = AutomationState.WAITING_FOR_15;
    private boolean triggerVisible;
    private Runnable delayedA;
    private int completedAClicks;
    private int triggerValue = 15;
    private long delayMs = 22_000L;

    public AutomationController(Host host) {
        this(host, new HandlerScheduler(new Handler(Looper.getMainLooper())));
    }

    AutomationController(Host host, Scheduler scheduler) {
        this.host = host;
        this.scheduler = scheduler;
    }

    public void configure(int trigger, int delaySeconds) {
        triggerValue = Math.max(1, Math.min(999, trigger));
        delayMs = Math.max(0L, Math.min(3_600_000L, delaySeconds * 1000L));
        reset();
    }

    public void reset() {
        cancelDelay();
        state = AutomationState.WAITING_FOR_15;
        triggerVisible = false;
        completedAClicks = 0;
    }

    public AutomationState getState() {
        return state;
    }

    public void observe(boolean isTrigger) {
        if (!host.isActive()) return;

        if (!isTrigger) {
            triggerVisible = false;
            return;
        }

        if (triggerVisible || state != AutomationState.WAITING_FOR_15) return;

        triggerVisible = true;
        state = AutomationState.CLICKING_B;
        host.status(triggerValue + " detected. Clicking Target B.");

        host.clickTarget("B", () -> {
            if (state != AutomationState.CLICKING_B || !host.isActive()) return;
            state = AutomationState.WAITING_22_SECONDS;
            host.status("Target B clicked. Waiting " + (delayMs / 1000L) + " seconds.");
            scheduleA();
        }, () -> {
            state = AutomationState.WAITING_FOR_15;
            host.status("Target B test click failed. Waiting for trigger to disappear.");
        });
    }

    private void scheduleA() {
        cancelDelay();
        delayedA = () -> {
            delayedA = null;
            if (!host.isActive() || state != AutomationState.WAITING_22_SECONDS) return;

            state = AutomationState.CLICKING_A;
            completedAClicks = 0;
            host.status("Delay elapsed. Clicking Target A twice.");
            clickNextA();
        };
        scheduler.postDelayed(delayedA, delayMs);
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
                host.status("Target A clicked twice. Waiting for trigger to disappear.");
            }
        }, () -> {
            state = AutomationState.WAITING_FOR_15;
            host.status("Target A test click failed. Waiting for trigger to disappear.");
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
        triggerVisible = false;
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

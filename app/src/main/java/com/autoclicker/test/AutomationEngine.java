package com.autoclicker.test;

/**
 * Deterministic state machine for:
 * exact countdown "15" -> Target B -> wait 22 seconds -> Target A.
 */
public final class AutomationEngine {
    public enum State {
        WAITING_FOR_15,
        CLICKING_B,
        WAITING_22_SECONDS,
        CLICKING_A
    }

    public interface Listener {
        void onTargetBRequired();
        void onTargetARequired();
        void onCycleCompleted();
    }

    private final Listener listener;
    private State state = State.WAITING_FOR_15;
    private boolean countdown15Visible;

    public AutomationEngine(Listener listener) {
        this.listener = listener;
    }

    public void reset() {
        state = State.WAITING_FOR_15;
        countdown15Visible = false;
    }

    public State getState() {
        return state;
    }

    public void observeCountdown(String value) {
        boolean is15 = "15".equals(value);

        if (!is15) {
            countdown15Visible = false;
            return;
        }

        if (countdown15Visible || state != State.WAITING_FOR_15) {
            return;
        }

        countdown15Visible = true;
        state = State.CLICKING_B;
        listener.onTargetBRequired();
    }

    public void onTargetBClicked() {
        if (state == State.CLICKING_B) {
            state = State.WAITING_22_SECONDS;
        }
    }

    public void requestTargetA() {
        if (state == State.WAITING_22_SECONDS) {
            state = State.CLICKING_A;
            listener.onTargetARequired();
        }
    }

    public void onTargetAClicked() {
        if (state != State.CLICKING_A) return;
        state = State.WAITING_FOR_15;
        listener.onCycleCompleted();
    }

    public void onTargetBFailed() {
        if (state == State.CLICKING_B) {
            state = State.WAITING_FOR_15;
            countdown15Visible = false;
        }
    }

    public void onTargetAFailed() {
        if (state == State.CLICKING_A) {
            state = State.WAITING_FOR_15;
            countdown15Visible = false;
        }
    }
}

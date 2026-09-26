package com.autoclicker.test;

/**
 * Small, UI-independent state machine for the fixed automation rule:
 * exact countdown "15" -> Target B -> wait 22 seconds -> Target A.
 *
 * Android AccessibilityService owns the actual screen inspection and taps.
 * This class owns only cycle state, so timing/trigger behavior is deterministic
 * and easy to test without an Android device.
 */
public final class AutomationEngine {
    public enum State {
        WAITING_FOR_15,
        WAITING_FOR_A
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

    public boolean isWaitingForA() {
        return state == State.WAITING_FOR_A;
    }

    /**
     * Feed the current countdown observation.
     * A new cycle can start only after 15 has disappeared and later appears again.
     */
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
        state = State.WAITING_FOR_A;
        listener.onTargetBRequired();
    }

    /** Call only after Target B has actually been dispatched successfully. */
    public void onTargetBClicked() {
        if (state != State.WAITING_FOR_A) return;
        // The state is already latched so repeated "15" accessibility events
        // cannot start another B click.
    }

    /** Call after Target A has actually been dispatched successfully. */
    public void onTargetAClicked() {
        if (state != State.WAITING_FOR_A) return;
        state = State.WAITING_FOR_15;
        listener.onCycleCompleted();
    }
}

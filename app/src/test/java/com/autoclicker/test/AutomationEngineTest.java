package com.autoclicker.test;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AutomationEngineTest {
    @Test
    public void fifteenStartsBOnlyOnceUntilItDisappears() {
        final int[] b = {0};
        final int[] a = {0};

        AutomationEngine engine = new AutomationEngine(new AutomationEngine.Listener() {
            @Override public void onTargetBRequired() { b[0]++; }
            @Override public void onTargetARequired() { a[0]++; }
            @Override public void onCycleCompleted() {}
        });

        engine.observeCountdown("15");
        engine.observeCountdown("15");
        engine.observeCountdown("15");

        assertEquals(1, b[0]);
        assertEquals(0, a[0]);
        assertEquals(1, engine.isWaitingForA() ? 1 : 0);

        engine.observeCountdown("14");
        engine.observeCountdown("15");

        assertEquals(2, b[0]);
        assertEquals(0, a[0]);
    }

    @Test
    public void completedCycleWaitsForNextFifteen() {
        final int[] b = {0};

        AutomationEngine engine = new AutomationEngine(new AutomationEngine.Listener() {
            @Override public void onTargetBRequired() { b[0]++; }
            @Override public void onTargetARequired() {}
            @Override public void onCycleCompleted() {}
        });

        engine.observeCountdown("15");
        assertEquals(1, b[0]);

        engine.onTargetAClicked();
        assertEquals(AutomationEngine.State.WAITING_FOR_15, engine.getState());

        engine.observeCountdown("15");
        assertEquals(1, b[0]);

        engine.observeCountdown("14");
        engine.observeCountdown("15");
        assertEquals(2, b[0]);
    }
}

package com.autoclicker.test;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AutomationEngineTest {
    @Test
    public void fifteenStartsBOnlyOnceUntilItDisappears() {
        final int[] b = {0};
        AutomationEngine engine = new AutomationEngine(new AutomationEngine.Listener() {
            public void onTargetBRequired() { b[0]++; }
            public void onTargetARequired() {}
            public void onCycleCompleted() {}
        });

        engine.observeCountdown("15");
        engine.observeCountdown("15");
        assertEquals(1, b[0]);
        assertEquals(AutomationEngine.State.CLICKING_B, engine.getState());

        engine.onTargetBClicked();
        assertEquals(AutomationEngine.State.WAITING_22_SECONDS, engine.getState());
        engine.observeCountdown("15");
        assertEquals(1, b[0]);
    }

    @Test
    public void targetAIsRequestedAfterBAndDelayState() {
        final int[] a = {0};
        AutomationEngine engine = new AutomationEngine(new AutomationEngine.Listener() {
            public void onTargetBRequired() {}
            public void onTargetARequired() { a[0]++; }
            public void onCycleCompleted() {}
        });

        engine.observeCountdown("15");
        engine.onTargetBClicked();
        engine.requestTargetA();

        assertEquals(1, a[0]);
        assertEquals(AutomationEngine.State.CLICKING_A, engine.getState());

        engine.onTargetAClicked();
        assertEquals(AutomationEngine.State.WAITING_FOR_15, engine.getState());
    }

    @Test
    public void failedBCanStartAgainAfterNew15() {
        final int[] b = {0};
        AutomationEngine engine = new AutomationEngine(new AutomationEngine.Listener() {
            public void onTargetBRequired() { b[0]++; }
            public void onTargetARequired() {}
            public void onCycleCompleted() {}
        });

        engine.observeCountdown("15");
        engine.onTargetBFailed();
        engine.observeCountdown("14");
        engine.observeCountdown("15");

        assertEquals(2, b[0]);
    }
}

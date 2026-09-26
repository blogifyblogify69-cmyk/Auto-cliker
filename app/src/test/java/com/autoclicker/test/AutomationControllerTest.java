package com.autoclicker.test;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AutomationControllerTest {
    @Test
    public void repeated15DoesNotTriggerBAgain() {
        FakeHost host = new FakeHost();
        AutomationController controller = new AutomationController(host);

        controller.observe(true);
        controller.observe(true);

        assertEquals(1, host.bClicks);
        assertEquals(AutomationState.CLICKING_B, controller.getState());
    }

    @Test
    public void successfulBEntersTwentyTwoSecondWait() {
        FakeHost host = new FakeHost();
        AutomationController controller = new AutomationController(host);

        controller.observe(true);
        host.bSuccess.run();

        assertEquals(AutomationState.WAITING_22_SECONDS, controller.getState());
    }

    @Test
    public void failedBAllowsNew15AfterItDisappears() {
        FakeHost host = new FakeHost();
        AutomationController controller = new AutomationController(host);

        controller.observe(true);
        host.bFailure.run();
        controller.observe(false);
        controller.observe(true);

        assertEquals(2, host.bClicks);
    }

    private static final class FakeHost implements AutomationController.Host {
        int bClicks;
        Runnable bSuccess = () -> {};
        Runnable bFailure = () -> {};

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void clickTarget(String target, Runnable success, Runnable failure) {
            if ("B".equals(target)) {
                bClicks++;
                bSuccess = success;
                bFailure = failure;
            }
        }

        @Override
        public void status(String message) {
        }
    }
}

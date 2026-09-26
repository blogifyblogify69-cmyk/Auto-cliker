package com.autoclicker.test;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AutomationControllerTest {
    @Test
    public void repeated15DoesNotTriggerBAgain() {
        FakeHost host = new FakeHost();
        FakeScheduler scheduler = new FakeScheduler();
        AutomationController controller = new AutomationController(host, scheduler);

        controller.observe(true);
        controller.observe(true);

        assertEquals(1, host.bClicks);
        assertEquals(AutomationState.CLICKING_B, controller.getState());
        assertEquals(0, scheduler.pendingCount());
    }

    @Test
    public void successfulBEntersTwentyTwoSecondWaitAndSchedulesA() {
        FakeHost host = new FakeHost();
        FakeScheduler scheduler = new FakeScheduler();
        AutomationController controller = new AutomationController(host, scheduler);

        controller.observe(true);
        host.completeBSuccess();

        assertEquals(AutomationState.WAITING_22_SECONDS, controller.getState());
        assertEquals(1, scheduler.pendingCount());
        assertEquals(22_000L, scheduler.lastDelayMs);
    }

    @Test
    public void twentyTwoSecondsLaterClicksAAndWaitsForNew15() {
        FakeHost host = new FakeHost();
        FakeScheduler scheduler = new FakeScheduler();
        AutomationController controller = new AutomationController(host, scheduler);

        controller.observe(true);
        host.completeBSuccess();
        scheduler.runPending();

        assertEquals(1, host.bClicks);
        assertEquals(1, host.aClicks);
        assertEquals(AutomationState.CLICKING_A, controller.getState());

        host.completeASuccess();

        assertEquals(AutomationState.WAITING_FOR_15, controller.getState());
        assertEquals(0, scheduler.pendingCount());

        controller.observe(true);
        assertEquals(1, host.bClicks);

        controller.observe(false);
        controller.observe(true);
        assertEquals(2, host.bClicks);
    }

    @Test
    public void stopCancelsPendingA() {
        FakeHost host = new FakeHost();
        FakeScheduler scheduler = new FakeScheduler();
        AutomationController controller = new AutomationController(host, scheduler);

        controller.observe(true);
        host.completeBSuccess();
        assertEquals(1, scheduler.pendingCount());

        controller.stop();
        assertEquals(0, scheduler.pendingCount());
        assertEquals(AutomationState.WAITING_FOR_15, controller.getState());

        scheduler.runPending();
        assertEquals(0, host.aClicks);
    }

    @Test
    public void failedBAllowsNew15AfterItDisappears() {
        FakeHost host = new FakeHost();
        FakeScheduler scheduler = new FakeScheduler();
        AutomationController controller = new AutomationController(host, scheduler);

        controller.observe(true);
        host.completeBFailure();

        assertEquals(AutomationState.WAITING_FOR_15, controller.getState());

        controller.observe(true);
        assertEquals(1, host.bClicks);

        controller.observe(false);
        controller.observe(true);
        assertEquals(2, host.bClicks);
    }

    private static final class FakeHost implements AutomationController.Host {
        int bClicks;
        int aClicks;
        private Runnable bSuccess = () -> {};
        private Runnable bFailure = () -> {};
        private Runnable aSuccess = () -> {};
        private Runnable aFailure = () -> {};

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
            } else if ("A".equals(target)) {
                aClicks++;
                aSuccess = success;
                aFailure = failure;
            } else {
                failure.run();
            }
        }

        void completeBSuccess() {
            bSuccess.run();
        }

        void completeBFailure() {
            bFailure.run();
        }

        void completeASuccess() {
            aSuccess.run();
        }

        @SuppressWarnings("unused")
        void completeAFailure() {
            aFailure.run();
        }
    }

    private static final class FakeScheduler implements AutomationController.Scheduler {
        Runnable pending;
        long lastDelayMs;

        @Override
        public void postDelayed(Runnable task, long delayMs) {
            pending = task;
            lastDelayMs = delayMs;
        }

        @Override
        public void remove(Runnable task) {
            if (pending == task) pending = null;
        }

        void runPending() {
            Runnable task = pending;
            pending = null;
            if (task != null) task.run();
        }

        int pendingCount() {
            return pending == null ? 0 : 1;
        }
    }
}

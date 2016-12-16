package com.venaglia.roger.ui;

import com.venaglia.roger.buttons.Button;
import com.venaglia.roger.buttons.TestButtonLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Created by ed on 10/13/16.
 */
public class ButtonProcessorTest {

    private Button buttonA = TestButtonLoader.getSimpleButton("test-A", "A");
    private Button buttonB = TestButtonLoader.getSimpleButton("test-B", "B");
    private Button buttonC = TestButtonLoader.getSimpleButton("test-C", "C");
    private TestIntervals intervals;
    private ButtonProcessor buttonProcessor;
    private EventCounters eventCounters;
    private Supplier<Set<Button>> buttonsDown;
    private Supplier<Integer> queueSize;
    private Runnable runOnExit;

    @Before
    public void setUp() throws Exception {
        if (runOnExit != null) {
            runOnExit.run();
            runOnExit = null;
        }
        intervals = new TestIntervals();
        buttonProcessor = new ButtonProcessor(intervals,
                                              (b) -> buttonsDown = b,
                                              (q) -> queueSize = q,
                                              (r) -> runOnExit = r);
        TestButtonListener listener = new TestButtonListener();
        eventCounters = listener.getEventCounter();
        buttonProcessor.addButtonListener(listener);
        buttonProcessor.addButtonNoiseListener(listener);
        eventCounters.clear();
    }

    @After
    public void tearDown() throws Exception {
        if (runOnExit != null) {
            runOnExit.run();
            runOnExit = null;
        }
        buttonProcessor = null;
    }

    @Test
    public void testShortClicksAreIgnored() {
        assertEquals(0, queueSize.get().intValue());
        assertOnlyButtonsDown();
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(0, 0, 0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0);
        buttonProcessor.handleButtonDown(buttonA);
        assertEquals(1, queueSize.get().intValue());
        assertOnlyButtonsDown(buttonA);
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        buttonProcessor.handleButtonUp(buttonA);
        assertEquals(0, queueSize.get().intValue());
        assertOnlyButtonsDown();
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 1, 0, 0, 0).assertCounts(1, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
    }

    @Test
    public void testShortClicks() {
        assertEquals(0, queueSize.get().intValue());
        assertOnlyButtonsDown();
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(0, 0, 0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0);
        buttonProcessor.handleButtonDown(buttonA);
        long downAt = System.currentTimeMillis();
        assertEquals(1, queueSize.get().intValue());
        assertOnlyButtonsDown(buttonA);
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        sleep(Math.max(2, downAt - System.currentTimeMillis() + intervals.click_a()));
        buttonProcessor.handleButtonUp(buttonA);
        long upAt = System.currentTimeMillis();
        assertEquals(1, queueSize.get().intValue());
        assertOnlyButtonsDown();
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 1, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        sleep(Math.max(2, upAt - System.currentTimeMillis() + intervals.eventDelay() + 10));
        assertEquals(0, queueSize.get().intValue());
        assertOnlyButtonsDown();
        eventCounters.getFor(buttonA).assertCounts(1, 1, 1, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
    }

    @Test
    public void testAmbiguousClicksAreIgnored() {
        assertEquals(0, queueSize.get().intValue());
        assertOnlyButtonsDown();
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(0, 0, 0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0);
        buttonProcessor.handleButtonDown(buttonA);
        long downAt = System.currentTimeMillis();
        assertEquals(1, queueSize.get().intValue());
        assertOnlyButtonsDown(buttonA);
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        sleep(Math.max(2, downAt - System.currentTimeMillis() + intervals.click_b()));
        buttonProcessor.handleButtonUp(buttonA);
        assertEquals(0, queueSize.get().intValue());
        assertOnlyButtonsDown();
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 1, 0, 0, 0).assertCounts(0, 1, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
    }

    @Test
    public void testLongClicks() {
        assertEquals(0, queueSize.get().intValue());
        assertOnlyButtonsDown();
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(0, 0, 0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0);
        buttonProcessor.handleButtonDown(buttonA);
        long downAt = System.currentTimeMillis();
        assertEquals(1, queueSize.get().intValue());
        assertOnlyButtonsDown(buttonA);
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        sleep(Math.max(2, downAt - System.currentTimeMillis() + intervals.longPress_a()));
        buttonProcessor.handleButtonUp(buttonA);
        long upAt = System.currentTimeMillis();
        assertEquals(1, queueSize.get().intValue());
        assertOnlyButtonsDown();
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 1, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        sleep(Math.max(2, upAt - System.currentTimeMillis() + intervals.eventDelay() + 10));
        assertEquals(0, queueSize.get().intValue());
        assertOnlyButtonsDown();
        eventCounters.getFor(buttonA).assertCounts(1, 1, 0, 1, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
    }


    @Test
    public void testExtendedLongClicksAreIgnored() {
        assertEquals(0, queueSize.get().intValue());
        assertOnlyButtonsDown();
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(0, 0, 0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0);
        buttonProcessor.handleButtonDown(buttonA);
        long downAt = System.currentTimeMillis();
        assertEquals(1, queueSize.get().intValue());
        assertOnlyButtonsDown(buttonA);
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        sleep(Math.max(2, downAt - System.currentTimeMillis() + intervals.longPress_b() + intervals.longPressRepeat() * 2 + 50));
        buttonProcessor.handleButtonUp(buttonA);
        assertEquals(0, queueSize.get().intValue());
        assertOnlyButtonsDown();
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 1, 0, 0, 3).assertCounts(0, 0, 1);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
    }

    @Test
    public void testIntersectingShortClicksAreIgnored() {
        assertEquals(0, queueSize.get().intValue());
        assertOnlyButtonsDown();
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(0, 0, 0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0);
        buttonProcessor.handleButtonDown(buttonA);
        assertEquals(1, queueSize.get().intValue());
        assertOnlyButtonsDown(buttonA);
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        buttonProcessor.handleButtonDown(buttonB);
        assertEquals(2, queueSize.get().intValue());
        assertOnlyButtonsDown(buttonA, buttonB);
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(1, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(0, 0, 0, 0, 0).assertCounts(0, 0, 0);
        buttonProcessor.handleButtonDown(buttonC);
        assertEquals(3, queueSize.get().intValue());
        assertOnlyButtonsDown(buttonA, buttonB, buttonC);
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(1, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(1, 0, 0, 0, 0).assertCounts(0, 0, 0);
        buttonProcessor.handleButtonUp(buttonA);
        assertEquals(2, queueSize.get().intValue());
        assertOnlyButtonsDown(buttonB, buttonC);
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 1, 0, 0, 0).assertCounts(1, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(1, 0, 0, 0, 0).assertCounts(0, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(1, 0, 0, 0, 0).assertCounts(0, 0, 0);
        buttonProcessor.handleButtonUp(buttonB);
        assertEquals(1, queueSize.get().intValue());
        assertOnlyButtonsDown(buttonC);
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 1, 0, 0, 0).assertCounts(1, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(1, 1, 0, 0, 0).assertCounts(1, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(1, 0, 0, 0, 0).assertCounts(0, 0, 0);
        buttonProcessor.handleButtonUp(buttonC);
        assertEquals(0, queueSize.get().intValue());
        assertOnlyButtonsDown();
        sleep(2);
        eventCounters.getFor(buttonA).assertCounts(1, 1, 0, 0, 0).assertCounts(1, 0, 0);
        eventCounters.getFor(buttonB).assertCounts(1, 1, 0, 0, 0).assertCounts(1, 0, 0);
        eventCounters.getFor(buttonC).assertCounts(1, 1, 0, 0, 0).assertCounts(1, 0, 0);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // don't care
        }
    }

    private void assertButtonsDown(Button... buttons) {
        Set<Button> buttonSet = buttonsDown.get();
        assertTrue(buttonSet.containsAll(Arrays.asList(buttons)));
    }

    private void assertOnlyButtonsDown(Button... buttons) {
        Set<Button> buttonSet = buttonsDown.get();
        assertEquals(buttons.length, buttonSet.size());
        assertTrue(buttonSet.containsAll(Arrays.asList(buttons)));
    }

    private static class EventCounter {

        int down, up, click, longPress, continuedLongPress;
        int tooShort, ambiguous, tooLong;

        void clear() {
            down = up = click = longPress = continuedLongPress = 0;
            tooShort = ambiguous = tooLong = 0;
        }

        EventCounter assertCounts(int down, int up, int click, int longPress, int extendedLongPress) {
            assert this.down == down;
            assert this.up == up;
            assert this.click == click;
            assert this.longPress == longPress;
            assert this.continuedLongPress == extendedLongPress;
            return this;
        }

        EventCounter assertCounts(int tooShort, int ambiguous, int tooLong) {
            assert this.tooShort == tooShort;
            assert this.ambiguous == ambiguous;
            assert this.tooLong == tooLong;
            return this;
        }
    }

    private static class TestButtonListener implements ButtonListener, ButtonNoiseListener {

        private final HashMap<Button,EventCounter> countByButton = new HashMap<>();

        private EventCounters getEventCounter() {
            return new EventCounters() {
                @Override
                public EventCounter getFor(Button button) {
                    return getCounter(button);
                }

                @Override
                public void clear() {
                    for (EventCounter counter : countByButton.values()) {
                        counter.clear();
                    }
                }
            };
        }

        private EventCounter getCounter(Button button) {
            EventCounter counters = countByButton.get(button);
            if (counters == null) {
                counters = new EventCounter();
                countByButton.put(button, counters);
            }
            return counters;
        }

        @Override
        public void handleClick(Button button) {
            getCounter(button).click++;
        }

        @Override
        public void handleLongPress(Button button) {
            getCounter(button).longPress++;
        }

        @Override
        public void handleContinuedLongPress(Button button, long ms, int count) {
            getCounter(button).continuedLongPress++;
        }

        @Override
        public void handleButtonDown(Button button) {
            getCounter(button).down++;
        }

        @Override
        public void handleButtonUp(Button button) {
            getCounter(button).up++;
        }

        @Override
        public void handleTooShort(Button button) {
            getCounter(button).tooShort++;
        }

        @Override
        public void handleAmbiguous(Button button) {
            getCounter(button).ambiguous++;
        }

        @Override
        public void handleTooLong(Button button) {
            getCounter(button).tooLong++;
        }
    }

    interface EventCounters {
        EventCounter getFor(Button button);
        void clear();
    }

    private static class TestIntervals extends Intervals {

        @Override
        public long eventDelay() {
            return 200L;
        }

        @Override
        public long hardButtonDelay() {
            return 200L;
        }

        @Override
        public long click_a() {
            return 200L;
        }

        @Override
        public long click_b() {
            return 300L;
        }

        @Override
        public long longPress_a() {
            return 350L;
        }

        @Override
        public long longPress_b() {
            return 500L;
        }

        @Override
        public long longPressRepeat() {
            return 250;
        }
    }
}

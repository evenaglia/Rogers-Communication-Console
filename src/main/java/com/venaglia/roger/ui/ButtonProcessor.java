/*
 * Copyright 2016 - 2017 Ed Venaglia
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.venaglia.roger.ui;

import com.google.inject.Singleton;
import com.venaglia.roger.buttons.Button;
import com.venaglia.roger.buttons.SimpleButtonListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by ed on 10/11/16.
 */
@Singleton
public class ButtonProcessor implements SimpleButtonListener {

    private final List<ButtonListener> buttonListeners = new ArrayList<>(4);
    private final List<ButtonNoiseListener> buttonNoiseListeners = new ArrayList<>(4);
    private final Map<Button,Trigger> buttonsDown = new HashMap<>();
    private final Lock handleLock = new ReentrantLock(true);
    private final Lock upNextLock = new ReentrantLock(true);
    private final TreeMap<Long,Wrapper> upNext;
    private final Thread buttonProcessorThread;
    private final ThreadLocal<Wrapper> activeWrapper = new ThreadLocal<>();
    private final Intervals intervals;
    private final Trigger nullTrigger = new Trigger(0L, Button.NIL, null);

    private Trigger lastButtonUpTrigger = nullTrigger;
    private boolean alive = true;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    public ButtonProcessor() {
        this(new Intervals(), null, null, null);
    }

    ButtonProcessor(Intervals intervals,
                    Consumer<Supplier<Set<Button>>> getButtonsDown,
                    Consumer<Supplier<Integer>> getQueueSize,
                    Consumer<Runnable> runAtEndOfTest) {
        assert intervals != null;
        this.intervals = intervals;
        buttonProcessorThread = new Thread(this::mainLoop, "Button Processor");
        uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (uncaughtExceptionHandler == null) {
            uncaughtExceptionHandler = (t, e) -> e.printStackTrace();
        }
        buttonProcessorThread.setDaemon(true);
        upNext = new TreeMap<>();
        if (getButtonsDown != null) {
            getButtonsDown.accept(this::buttonsDown);
        }
        if (getQueueSize != null) {
            getQueueSize.accept(upNext::size);
        }
        if (runAtEndOfTest != null) {
            runAtEndOfTest.accept(this::die);
        }
    }

    @Override
    public void handleButtonDown(Button button) {
        long now = System.currentTimeMillis();
        handleLock.lock();
        try {
            long age = now - lastButtonUpTrigger.eventTime;
            if (age <= intervals.eventDelay() && lastButtonUpTrigger.button.equals(button) && lastButtonUpTrigger.abort != null) {
                // A high-level event was scheduled, but the button was pressed
                // shortly after it was released, so no high-level event will
                // be fired.
                lastButtonUpTrigger.abort.abort();
            }
            Abort abort = schedule(button + ".continuedLongPress", button, new Runnable() {
                int count = 1;
                @Override
                public void run() {
                    ButtonProcessor.this.fire(getContinuedLongPressConsumer(button, now, count));
                    count++;
                    reschedule(intervals.longPressRepeat(), TimeUnit.MILLISECONDS);
                }
            }, intervals.longPress_b(), TimeUnit.MILLISECONDS);
            Trigger trigger = new Trigger(now, button, abort);
            buttonsDown.put(button, trigger);
            fire(l -> l.handleButtonDown(button));
        } finally {
            handleLock.unlock();
        }
    }

    @Override
    public void handleButtonUp(Button button) {
        long now = System.currentTimeMillis();
        handleLock.lock();
        try {
            fire(l -> l.handleButtonUp(button));
            Trigger trigger = buttonsDown.get(button);
            if (trigger == null && buttonsDown.size() > 0) {
                trigger = buttonsDown.get(buttonsDown.keySet().iterator().next());
            }
            if (trigger != null && trigger.abort != null) {
                trigger.abort.abort();
            }
            lastButtonUpTrigger = new Trigger(now, button, null);
            if (trigger != null) {
                buttonsDown.remove(button);
                long age = now - trigger.eventTime;
                if (age < intervals.click_a()) {
                    // too short, no high-level event will be fired
                    fireNoise(l -> l.handleTooShort(button));
                } else if (age < intervals.click_b()) {
                    lastButtonUpTrigger = new Trigger(now, button, schedule(button + ".delayClick", button, getClickDispatcher(button), intervals.eventDelay(), TimeUnit.MILLISECONDS));
                } else if (age < intervals.longPress_a()) {
                    // ambiguous, no high-level event will be fired
                    fireNoise(l -> l.handleAmbiguous(button));
                } else if (age < intervals.longPress_b()) {
                    lastButtonUpTrigger = new Trigger(now, button, schedule(button + ".longPress", button, getLongPressDispatcher(button), intervals.eventDelay(), TimeUnit.MILLISECONDS));
                } else {
                    // too long, no high-level event will be fired
                    fireNoise(l -> l.handleTooLong(button));
                }
            }
        } finally {
            handleLock.unlock();
        }
    }

    public Set<Button> getButtonsDown() {
        handleLock.lock();
        try {
            return Collections.unmodifiableSet(new HashSet<>(buttonsDown.keySet()));
        } finally {
            handleLock.unlock();
        }
    }

    private Consumer<ButtonListener> getContinuedLongPressConsumer(Button button, long now, int count) {
        return l -> l.handleContinuedLongPress(button, System.currentTimeMillis() - now, count);
    }

    private Runnable getClickDispatcher(Button button) {
        return () -> {
            lastButtonUpTrigger = nullTrigger;
            fire(l -> l.handleClick(button));
        };
    }

    private Runnable getLongPressDispatcher(Button button) {
        return () -> {
            lastButtonUpTrigger = nullTrigger;
            fire(l -> l.handleLongPress(button));
        };
    }

    private void fire(Consumer<? super ButtonListener> relay) {
        buttonListeners.forEach(relay);
    }

    private void fireNoise(Consumer<? super ButtonNoiseListener> relay) {
        buttonNoiseListeners.forEach(relay);
    }

    @SuppressWarnings("InfiniteLoopStatement")
    private synchronized void mainLoop() {
        if (Thread.currentThread() != buttonProcessorThread) {
            throw new IllegalStateException("mainLoop() cannot be run from any thread other than " + buttonProcessorThread);
        }
        while (alive) {
            long now = System.currentTimeMillis();
            Map.Entry<Long,Wrapper> next = getNext();
            if (next != null && now >= next.getKey()) {
                safeRun(next, next.getValue());
            } else {
                long wait = (next != null ? next.getKey() : Long.MAX_VALUE) - now;
                try {
                    wait(wait);
                } catch (InterruptedException e) {
                    // time to wake up
                }
            }
        }
    }

    private void safeRun(Map.Entry<Long,Wrapper> next, Wrapper wrapper) {
        unschedule(next.getKey(), wrapper);
        Wrapper save = activeWrapper.get();
        activeWrapper.set(wrapper);
        try {
            wrapper.canReschedule = true;
            wrapper.runnable.run();
        } catch (Throwable t) {
            uncaughtExceptionHandler.uncaughtException(buttonProcessorThread, t);
        } finally {
            wrapper.canReschedule = false;
            activeWrapper.set(save);
        }
    }

    private void die() {
        alive = false;
    }

    private Set<Button> buttonsDown() {
        handleLock.lock();
        try {
            return new HashSet<>(buttonsDown.keySet());
        } finally {
            handleLock.unlock();
        }
    }

    private boolean unschedule(Long key, Wrapper expecting) {
        upNextLock.lock();
        try {
            if (upNext.get(key) == expecting) {
                upNext.remove(key);
                return true;
            }
            return false;
        } finally {
            upNextLock.unlock();
        }
    }

    private void reschedule(long after, TimeUnit afterUnit) {
        Wrapper wrapper = activeWrapper.get();
        if (wrapper == null) {
            throw new IllegalStateException("reschedule() can only be called inside the run() method when it executes on schedule");
        }
        assert after > 0 : "Wait period must be a positive value to indicate execution in the future: " + after + " " + afterUnit;
        long when = System.currentTimeMillis() + afterUnit.toMillis(after);
        assert when >= 10 : "Wait period must be at least 10ms in the future: " + when + "ms";
        boolean interrupt;
        upNextLock.lock();
        try {
            while (upNext.containsKey(when)) when++;
            Long key = when;
            upNext.put(key, wrapper);
            wrapper.reschedule(key);
            interrupt = upNext.firstKey() == when;
        } finally {
            upNextLock.unlock();
        }
        if (buttonProcessorThread.getState() == Thread.State.NEW) {
            buttonProcessorThread.start();
        } else if (interrupt) {
            buttonProcessorThread.interrupt();
        }
    }

    private Abort schedule(String name, Button button, final Runnable runThis, long after, TimeUnit afterUnit) {
        assert runThis != null;
        assert after > 0 : "Wait period must be a positive value to indicate execution in the future: " + after + " " + afterUnit;
        Wrapper wrapper;
        long when = System.currentTimeMillis() + afterUnit.toMillis(after);
        assert when >= 10 : "Wait period must be at least 10ms in the future: " + when + "ms";
        boolean interrupt;
        upNextLock.lock();
        try {
            while (upNext.containsKey(when)) when++;
            Long key = when;
            wrapper = new Wrapper(name, runThis, button, key, true);
            upNext.put(key, wrapper);
            interrupt = upNext.firstKey() == when;
        } finally {
            upNextLock.unlock();
        }
        if (buttonProcessorThread.getState() == Thread.State.NEW) {
            buttonProcessorThread.start();
        } else if (interrupt) {
            buttonProcessorThread.interrupt();
        }
        return wrapper.abort;
    }

    private void runASAP(String name, Button button, Runnable... runThese) {
        int l = runThese.length;
        upNextLock.lock();
        try {
            long before = (upNext.isEmpty() ? System.currentTimeMillis() : upNext.firstKey()) - l;
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < l; i++) {
                Long key = before++;
                Wrapper wrapper = new Wrapper(String.format("%s[%d]", name, i), runThese[i], button, key, false);
                upNext.put(key, wrapper);
            }
        } finally {
            upNextLock.unlock();
        }
        if (buttonProcessorThread.getState() == Thread.State.NEW) {
            buttonProcessorThread.start();
        } else {
            buttonProcessorThread.interrupt();
        }
    }

    private Map.Entry<Long,Wrapper> getNext() {
        upNextLock.lock();
        try {
            return upNext.isEmpty() ? null : upNext.firstEntry();
        } finally {
            upNextLock.unlock();
        }
    }

    public void addButtonListener(ButtonListener buttonListener) {
        assert buttonListener != null;
        buttonListeners.add(buttonListener);
    }

    public void removeButtonListener(ButtonListener buttonListener) {
        assert buttonListener != null;
        buttonListeners.remove(buttonListener);
    }

    public void addButtonNoiseListener(ButtonNoiseListener buttonNoiseListener) {
        assert buttonNoiseListener != null;
        buttonNoiseListeners.add(buttonNoiseListener);
    }

    public void removeButtonNoiseListener(ButtonNoiseListener buttonNoiseListener) {
        assert buttonNoiseListener != null;
        buttonNoiseListeners.remove(buttonNoiseListener);
    }

    String dumpQueue() {
        long now = System.currentTimeMillis();
        upNextLock.lock();
        try {
            StringBuilder buffer = new StringBuilder();
            buffer.append("Button Processor Queue:");
            if (!upNext.isEmpty()) {
                for (Map.Entry<Long,Wrapper> entry : upNext.entrySet()) {
                    double delay = entry.getKey() - now;
                    String unit = "ms";
                    String format = "%5.f";
                    if (delay <= 0) {
                        delay = 0;
                    } else if (delay < 400) {
                        unit = "s";
                        format = "%5.2f";
                        delay /= 1000;
                    } else if (delay < 900) {
                        unit = "s";
                        format = "%5.1f";
                        delay /= 1000;
                    } else {
                        unit = "s";
                        format = "%5f";
                        delay = Math.round(delay / 1000.0);
                    }
                    buffer.append("\n\t");
                    buffer.append(String.format(format, delay)).append(unit);
                    buffer.append(" -> ").append(entry.getValue());
                }
            } else {
                buffer.append("\n\t(empty)");
            }
            return buffer.toString();
        } finally {
            upNextLock.unlock();
        }
    }

    private class Wrapper {
        final String name;
        final Runnable runnable;
        final Button button;
        Abort abort;
        Long key;
        boolean canReschedule = false;

        public Wrapper(String name, Runnable runnable, Button button, Long key, boolean canAbort) {
            assert name != null;
            assert runnable != null;
            assert button != null;
            assert key != null;
            this.name = name;
            this.runnable = runnable;
            this.button = button;
            this.abort = canAbort ? new Abort(this, button, key) : null;
            this.key = key;
        }

        void reschedule(Long key) {
            if (!canReschedule) {
                throw new IllegalStateException("The current task cannot be rescheduled more than once");
            }
            canReschedule = false;
            this.key = key;
            if (this.abort != null) {
                this.abort = new Abort(this, button, key);
            }
        }

        @Override
        public String toString() {
            return "Wrapper[\"" + name + "\"]";
        }
    }

    private class Abort {

        final Wrapper wrapper;
        final Button button;
        final long startTime;

        public Abort(Wrapper wrapper, Button button, long startTime) {
            assert wrapper != null;
            assert button != null;
            this.wrapper = wrapper;
            this.button = button;
            this.startTime = startTime;
        }

        void abort() {
            unschedule(wrapper.key, wrapper);
        }

        Button getButton() {
            return button;
        }

        long getStartTime() {
            return startTime;
        }
    }

    private static class Trigger {
        final long eventTime;
        final Button button;

        Abort abort;

        public Trigger(long eventTime, Button button, Abort abort) {
            this.eventTime = eventTime;
            this.button = button;
            this.abort = abort;
        }

        @Override
        public String toString() {
            return String.format("Button[%1$s].trigger@%2$tl:%2$tM:%2$tS.%2$tL", button, eventTime);
        }
    }

}

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

package com.venaglia.roger.console;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.venaglia.roger.autocomplete.AutoCompleter;
import com.venaglia.roger.buttons.Button;
import com.venaglia.roger.buttons.ButtonSet;
import com.venaglia.roger.buttons.ButtonSetLoader;
import com.venaglia.roger.buttons.SimpleButtonListener;
import com.venaglia.roger.menu.MenuTree;
import com.venaglia.roger.ui.ButtonProcessor;
import com.venaglia.roger.ui.Intervals;
import com.venaglia.roger.ui.UI;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ed on 9/4/16.
 */
@Singleton
public class Updater {

    private enum State {
        NEW, PAUSED, ADVANCE_RIGHT, ADVANCE_LEFT
    }

    private final Lock lock = new ReentrantLock();
    private final SimpleButtonListener buttonListener;
    private final ScheduledExecutorService executorService;

    @Inject
    public Updater(ScheduledExecutorService executorService) {
        this.executorService = executorService;
        scheduledFuture = executorService.scheduleAtFixedRate(this::init, 100, 100, TimeUnit.MILLISECONDS);
        buttonListener = new SimpleButtonListener() {
            @Override
            public void handleButtonDown(Button button) {
                if (state == State.NEW) {
                    return;
                }
                State newState;
                for (String _do : button.getAction().getDo()) {
                    switch (_do) {
                        case "nav:@scroll-left":
                            newState = State.ADVANCE_LEFT;
                            break;
                        case "nav:@scroll-right":
                            newState = State.ADVANCE_RIGHT;
                            break;
                        default:
                            newState = State.PAUSED;
                            break;
                    }
                    setAdvanceDirection(newState);
                }
            }

            @Override
            public void handleButtonUp(Button button) {
                if (state == State.NEW) {
                    return;
                }
                Set<Button> buttonsDown = buttonProcessor.getButtonsDown();
                if (buttonsDown.size() == 1) {
                    handleButtonDown(buttonsDown.iterator().next());
                } else {
                    setAdvanceDirection(State.PAUSED);
                }
            }
        };
    }

    private State state = State.NEW;
    private State nextState = State.NEW;
    private long nextStateAt = Long.MAX_VALUE;
    private ScheduledFuture<?> scheduledFuture;
    private int index;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private WrappingSubList<Button> subList = new WrappingSubList<>();
    private Button nil;

    private void init() {
        if (noneAreNull(autoCompleter, buttonProcessor, buttonSetLoader, menuTree, ui)) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
            ButtonSet buttonSet = buttonSetLoader.get();
            nil = buttonSet.get("[nil]");
            ui.setHardButtons(buttonSet.get(MenuTree.HARD_BUTTONS.LEFT),
                              buttonSet.get(MenuTree.HARD_BUTTONS.RIGHT),
                              buttonSet.get(MenuTree.HARD_BUTTONS.TOP_LEFT),
                              buttonSet.get(MenuTree.HARD_BUTTONS.TOP_CENTER),
                              buttonSet.get(MenuTree.HARD_BUTTONS.TOP_RIGHT));
            menuTree.addChangeListener((a, b) -> navToMenu(menuTree));
            menuTree.navigate(MenuTree.TOP);
            navToMenu(menuTree);
        }
    }

    private void tick() {
        assert state != State.NEW;
        long now = System.currentTimeMillis();
        lock.lock();
        try {
            if (now >= nextStateAt) {
                setState(nextState);
            }
            List<Button> currentButtons = menuTree.getCurrentButtons();
            int buttonCount = currentButtons.size();
            switch (state) {
                case ADVANCE_RIGHT:
                    index = (index + 1) % buttonCount;
                    subList.update(index, 4, currentButtons, nil);
                    ui.showTheseButtons(subList.get(0), subList.get(1), subList.get(2), subList.get(3), nil, nil, nil);
                    break;
                case ADVANCE_LEFT:
                    index = (index + buttonCount - 1) % buttonCount;
                    subList.update(index, 4, currentButtons, nil);
                    ui.showTheseButtons(subList.get(0), subList.get(1), subList.get(2), subList.get(3), nil, nil, nil);
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    private void navToMenu(MenuTree menuTree) {
        lock.lock();
        try {
            List<Button> currentButtons = menuTree.getCurrentButtons();
            subList.update(0, 4, currentButtons, nil);
            ui.showTheseButtons(subList.get(0), subList.get(1), subList.get(2), subList.get(3), nil, nil, nil);
            state = State.PAUSED;
            index = 0;
        } finally {
            lock.unlock();
        }
    }

    private void setState(State state) {
        setState(state, state, Long.MAX_VALUE);
    }

    private void setState(State state, State nextState, long when) {
        this.state = state;
        this.nextState = nextState;
        this.nextStateAt = when < Integer.MAX_VALUE ? System.currentTimeMillis() + when : when;
    }

    private boolean noneAreNull(Object... objects) {
        for (Object object : objects) {
            if (object == null) return false;
        }
        return true;
    }

    public SimpleButtonListener getButtonListener() {
        return buttonListener;
    }

    private void setAdvanceDirection(State advanceDirection) {
        assert advanceDirection != null && advanceDirection != State.NEW;
        lock.lock();
        try {
            if (state == State.NEW || state == advanceDirection) {
                if (nextState != state && scheduledFuture != null) {
                    // happens when we release a hard-button early
                    scheduledFuture.cancel(false);
                    scheduledFuture = null;
                }
                return;
            }
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
                scheduledFuture = null;
            }
            switch (state) {
                case PAUSED:
                    switch (advanceDirection) {
                        case ADVANCE_RIGHT:
                        case ADVANCE_LEFT:
                            long delay = intervals.hardButtonDelay();
                            scheduledFuture = executorService.scheduleWithFixedDelay(this::tick,
                                                                                     delay,
                                                                                     intervals.eventDelay(),
                                                                                     TimeUnit.MILLISECONDS);
                            setState(state, advanceDirection, System.currentTimeMillis() + delay);
                            break;
                    }
                    break;
                case ADVANCE_RIGHT:
                case ADVANCE_LEFT:
                    setState(advanceDirection);
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    @Inject
    private AutoCompleter autoCompleter;

    @Inject
    private ButtonProcessor buttonProcessor;

    @Inject
    private ButtonSetLoader buttonSetLoader;

    @Inject
    private Intervals intervals;

    @Inject
    private MenuTree menuTree;

    @Inject
    private UI ui;
}

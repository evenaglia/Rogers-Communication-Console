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

package com.venaglia.roger.console.server.impl;

import com.venaglia.roger.ui.Command;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ed on 1/13/17.
 */
public class DelegatedCommand implements Command, Future<String> {

    private final String command;
    private final List<String> args;
    private final Pattern responsePattern;

    private State state = State.PENDING;
    private String response;
    private Exception failure;

    private enum State {
        PENDING, SUCCEEDED, FAILED, CANCELLED
    }

    public DelegatedCommand(String command, List<String> args, Pattern responsePattern) {
        assert command != null;
        assert args != null;
        assert responsePattern != null;
        this.command = command;
        this.args = args;
        this.responsePattern = responsePattern;
    }

    @Override
    public String getCommand() {
        return command;
    }

    @Override
    public String[] getArgs() {
        return args.toArray(new String[args.size()]);
    }

    @Override
    public Pattern expectedResponsePattern() {
        return responsePattern;
    }

    @Override
    public synchronized void handleResponse(Matcher matcher, Consumer<Command> queue) {
        if (state == State.PENDING) {
            response = matcher.group();
            state = State.SUCCEEDED;
            notifyAll();
        }
    }

    @Override
    public synchronized void handleError(IOException ioe, Consumer<Command> queue) throws IOException {
        if (state == State.PENDING) {
            failure = ioe;
            state = State.FAILED;
            notifyAll();
        }
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (mayInterruptIfRunning && state == State.PENDING) {
            state = State.CANCELLED;
            notifyAll();
        }
        return state == State.CANCELLED;
    }

    @Override
    public synchronized boolean isCancelled() {
        return state == State.CANCELLED;
    }

    @Override
    public synchronized boolean isDone() {
        return state != State.PENDING;
    }

    @Override
    public synchronized String get() throws InterruptedException, ExecutionException {
        do {
            switch (state) {
                case PENDING:
                    wait();
                case SUCCEEDED:
                    return response;
                case FAILED:
                    throw new ExecutionException(failure);
                case CANCELLED:
                    throw new CancellationException();
            }
        } while (state == State.PENDING);
        throw new RuntimeException("Wait loop failed");
    }

    @Override
    public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long until = System.currentTimeMillis() + unit.toMillis(timeout);
        do {
            switch (state) {
                case PENDING:
                    long delay = until - System.currentTimeMillis();
                    if (delay < 0) {
                        throw new TimeoutException();
                    }
                    wait(Math.max(delay, 1));
                case SUCCEEDED:
                    return response;
                case FAILED:
                    throw new ExecutionException(failure);
                case CANCELLED:
                    throw new CancellationException();
            }
        } while (state == State.PENDING);
        throw new RuntimeException("Wait loop failed");
    }
}

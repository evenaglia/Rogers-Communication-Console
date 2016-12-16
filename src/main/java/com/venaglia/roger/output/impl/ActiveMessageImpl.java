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

package com.venaglia.roger.output.impl;

import com.venaglia.roger.output.ActiveMessage;
import com.venaglia.roger.output.ActiveMessageListener;
import com.venaglia.roger.output.ArchivedMessage;
import com.venaglia.roger.output.ElementType;
import com.venaglia.roger.output.OutputElement;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by ed on 9/30/16.
 */
public class ActiveMessageImpl extends MessageImpl implements ActiveMessage {

    static final OutputElement<Void> CURSOR = OutputElement.special(ElementType.CURSOR, "");

    private static final int MAX_UNDO = 64;
    private final ActiveMessageDelegate messageDelegator = new ActiveMessageDelegate();

    private ArchivedMessage archivedMessage;
    private List<ReplayEvent> eventBuffer = new LinkedList<>();
    private int eventBufferIndex = 0;

    public ActiveMessageImpl() {
    }

    public ActiveMessageImpl(ArchivedMessage from) {
        for (int i = 0, l = from.getSize(); i < l; i++) {
            OutputElement<?> element = from.getElement(i);
            eventBuffer.add(new MyEvent(element));
            elements.add(element);
        }
    }

    @Override
    public int getSize() {
        return elements.size() + 1;
    }

    @Override
    public OutputElement<?> getElement(int index) {
        if (index == elements.size()) {
            return CURSOR;
        }
        return super.getElement(index);
    }

    @Override
    public void append(OutputElement<?> element) {
        ensureNotArchived("append");
        messageDelegator.beforeAppend(element);
        ReplayEvent replayEvent = new MyEvent(element);
        if (eventBufferIndex < 0) {
            eventBuffer.subList(eventBuffer.size() + eventBufferIndex, eventBuffer.size()).clear();
        }
        eventBuffer.add(replayEvent);
        eventBufferIndex = -1;
        replayEvent.redo();
        if (eventBuffer.size() > MAX_UNDO) {
            eventBuffer.subList(0, eventBuffer.size() - MAX_UNDO).clear();
        }
        messageDelegator.afterAppend(element);
        messageDelegator.changed();
    }

    @Override
    public boolean canUndo() {
        return archivedMessage != null  && prevEvent() != null;
    }

    @Override
    public void undo() {
        ensureNotArchived("undo");
        ReplayEvent event = prevEvent();
        if (event == null) {
            throw new IllegalStateException("Can't undo, buffer size exceeded");
        }
        messageDelegator.beforeUndo();
        event.undo();
        messageDelegator.afterUndo();
        messageDelegator.changed();
    }

    @Override
    public boolean canRedo() {
        return archivedMessage != null && nextEvent() != null;
    }

    @Override
    public void redo() {
        ensureNotArchived("redo");
        ReplayEvent event = nextEvent();
        if (event == null) {
            throw new IllegalStateException("Can't redo, no remaining changes to restore");
        }
        messageDelegator.beforeRedo();
        event.undo();
        messageDelegator.afterRedo();
        messageDelegator.changed();
    }

    private ReplayEvent prevEvent() {
        int size = eventBuffer.size();
        int index = size + eventBufferIndex - 1;
        return index >= 0 && index < size ? eventBuffer.get(index) : null;
    }

    private ReplayEvent nextEvent() {
        int size = eventBuffer.size();
        int index = size + eventBufferIndex;
        return index >= 0 && index < size ? eventBuffer.get(index) : null;
    }

    @Override
    public ArchivedMessage archive() {
        if (archivedMessage != null) {
            messageDelegator.beforeArchive();
            archivedMessage = new AchivedMessageImpl(elements);
            messageDelegator.afterArchive(archivedMessage);
            eventBuffer.clear(); // discard this, we do not need it any more
        }
        return archivedMessage;
    }

    private void ensureNotArchived(String methodName) {
        if (archivedMessage != null) {
            throw new IllegalStateException("Cannot call " + methodName + "() after the message has been archived.");
        }
    }

    @Override
    public void addListener(ActiveMessageListener listener) {
        messageDelegator.add(listener);
    }

    @Override
    public void removeListener(ActiveMessageListener listener) {
        messageDelegator.remove(listener);
    }

    private class MyEvent implements ReplayEvent {

        private final OutputElement<?> element;

        private MyEvent(OutputElement<?> element) {
            assert element != null;
            this.element = element;
        }

        @Override
        public void undo() {
            if (prevEvent() != this) {
                throw new IllegalStateException("Can't undo an event out of order");
            }
            elements.remove(elements.size() - 1);
            eventBufferIndex--;
        }

        @Override
        public void redo() {
            if (nextEvent() != this) {
                throw new IllegalStateException("Can't redo an event out of order");
            }
            elements.add(element);
            eventBufferIndex++;
        }
    }
}

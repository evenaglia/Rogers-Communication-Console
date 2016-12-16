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

import com.venaglia.roger.output.ActiveMessageListener;
import com.venaglia.roger.output.ArchivedMessage;
import com.venaglia.roger.output.OutputElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ed on 10/6/16.
 */
public class ActiveMessageDelegate implements ActiveMessageListener {

    private List<ActiveMessageListener> listeners = new ArrayList<>(4);


    @Override
    public void beforeAppend(OutputElement<?> element) {
        for (ActiveMessageListener listener : listeners) {
            listener.beforeAppend(element);
        }
    }

    @Override
    public void afterAppend(OutputElement<?> element) {
        for (ActiveMessageListener listener : listeners) {
            listener.afterAppend(element);
        }
    }

    @Override
    public void beforeUndo() {
        for (ActiveMessageListener listener : listeners) {
            listener.beforeUndo();
        }
    }

    @Override
    public void afterUndo() {
        for (ActiveMessageListener listener : listeners) {
            listener.afterUndo();
        }
    }

    @Override
    public void beforeRedo() {
        for (ActiveMessageListener listener : listeners) {
            listener.beforeRedo();
        }
    }

    @Override
    public void afterRedo() {
        for (ActiveMessageListener listener : listeners) {
            listener.afterRedo();
        }
    }

    @Override
    public void beforeArchive() {
        for (ActiveMessageListener listener : listeners) {
            listener.beforeArchive();
        }
    }

    @Override
    public void afterArchive(ArchivedMessage message) {
        for (ActiveMessageListener listener : listeners) {
            listener.afterArchive(message);
        }
    }

    @Override
    public void changed() {
        for (ActiveMessageListener listener : listeners) {
            listener.changed();
        }
    }

    public void add(ActiveMessageListener listener) {
        assert listener != null;
        listeners.add(listener);
    }

    public void remove(ActiveMessageListener listener) {
        assert listener != null;
        listeners.remove(listener);
    }
}

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

package com.venaglia.roger;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.venaglia.roger.autocomplete.AutoCompleter;
import com.venaglia.roger.autocomplete.Reducer;
import com.venaglia.roger.autocomplete.impl.DummyAutoCompleter;
import com.venaglia.roger.autocomplete.reducers.HardConsonantReducer;
import com.venaglia.roger.autocomplete.reducers.SoftConsonantReducer;
import com.venaglia.roger.buttons.ButtonSet;
import com.venaglia.roger.buttons.ButtonSetLoader;
import com.venaglia.roger.emoji.EmojiSet;
import com.venaglia.roger.emoji.EmojiSetLoader;
import com.venaglia.roger.menu.MenuTree;
import com.venaglia.roger.menu.MenuTreeLoader;
import com.venaglia.roger.ui.FontLoader;
import com.venaglia.roger.ui.UI;
import com.venaglia.roger.ui.impl.UIImpl;

import java.awt.Font;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ed on 8/28/16.
 */
public class RogerModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Font.class).toProvider(FontLoader.class);
        bind(AutoCompleter.class).to(DummyAutoCompleter.class);
        bind(ButtonSet.class).toProvider(ButtonSetLoader.class);
        bind(EmojiSet.class).toProvider(EmojiSetLoader.class);
        bind(MenuTree.class).toProvider(MenuTreeLoader.class);
        bind(UI.class).to(UIImpl.class);
        bind(ScheduledExecutorService.class).toInstance(new ScheduledThreadPoolExecutor(4, new ThreadFactory() {

            private final NavigableSet<Integer> unused = new TreeSet<>();
            private final AtomicInteger seq = new AtomicInteger(1);
            private final ThreadGroup tg = new ThreadGroup("Executor");

            @Override
            public Thread newThread(Runnable r) {
                Integer s = unused.pollFirst();
                int seq = s == null ? this.seq.getAndIncrement() : s;
                Thread thread = new Thread(tg, r, String.format("Worker-%d", seq)) {
                    @Override
                    public void run() {
                        try {
                            super.run();
                        } finally {
                            unused.add(seq);
                        }
                    }
                };
                thread.setDaemon(true);
                return thread;
            }
        }));
    }

    @Provides
    @Named("UIScale")
    float getScale() {
        return 1.0f;
    }
}

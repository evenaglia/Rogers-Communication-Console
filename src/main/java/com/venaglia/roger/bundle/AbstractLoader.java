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

package com.venaglia.roger.bundle;

import com.google.inject.Inject;
import com.google.inject.Provider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Created by ed on 8/28/16.
 */
public abstract class AbstractLoader<T> implements Provider<T>, Supplier<T> {

    private final AtomicBoolean loaded = new AtomicBoolean();

    private Lock lock = new ReentrantLock();
    private T value;

    @Inject
    protected Bundle bundle;

    @Override
    public T get() {
        if (!loaded.get()) {
            lock.lock();
            try {
                if (!loaded.get()) {
                    value = load();
                }
            } finally {
                loaded.set(true);
                lock.unlock();
            }
        } else if (lock != null) {
            lock = null;
        }
        return value;
    }

    protected abstract T load();

    protected String readString(Reader reader) {
        StringBuilder buffer = new StringBuilder(512);
        char[] b = new char[512];
        try {
            for (int i = reader.read(b); i > 0; i = reader.read(b)) {
                buffer.append(b, 0, i);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return buffer.toString();
    }

    protected String[] readLines(Reader reader) {
        return readString(reader).split("\n");
    }

    protected InputStream getStream(String source) {
        return bundle.get(source).orElse(null);
//        try {
//            return new FileInputStream("src/main/bundle/" + source);
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e.getMessage(), e);
//        }
    }

    protected BufferedImage bufferImage(String source) {
        if (source == null) {
            return null;
        }
        try {
            InputStream stream = getStream(source);
            return stream == null ? null : ImageIO.read(stream);
        } catch (IOException e) {
            return null;
        }
    }

}

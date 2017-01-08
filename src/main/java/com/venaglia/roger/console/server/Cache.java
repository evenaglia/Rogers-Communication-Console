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

package com.venaglia.roger.console.server;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by ed on 1/7/17.
 */
public class Cache<K,V> {

    private final Map<K,V> store;
    private final int limit;

    public Cache(int limit) {
        assert limit > 0 && limit <= 1 << 20;
        this.limit = limit;
        store = new LinkedHashMap<>(limit * 2, 0.5f);
    }

    public V get(K key) {
        return store.get(key);
    }

    public void put(K key, V value) {
        store.put(key, value);
        if (store.size() > limit) {
            Iterator<Map.Entry<K,V>> iterator = store.entrySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    public void invalidate(K key) {
       store.remove(key);
    }

    public void invalidateAll() {
        store.clear();
    }
}

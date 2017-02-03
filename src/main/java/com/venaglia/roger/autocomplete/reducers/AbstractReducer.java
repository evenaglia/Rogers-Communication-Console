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

package com.venaglia.roger.autocomplete.reducers;

import com.venaglia.roger.autocomplete.Reducer;

import java.text.Normalizer;
import java.util.NavigableSet;
import java.util.TreeMap;

/**
 * Created by ed on 1/31/17.
 */
abstract class AbstractReducer implements Reducer {

    private final TreeMap<String,String> reduction = new TreeMap<>();

    private NavigableSet<String> entries;

    protected final void map(String identity) {
        map(identity, identity);
    }

    protected final void map(String from, String to) {
        assert entries == null;
        reduction.put(normalize(from), normalize(to));
    }

    private String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).toLowerCase().replaceAll("[^a-z]+", " ");
    }

    @Override
    public String reduce(String word) {
        if (entries == null) {
            entries = reduction.navigableKeySet();
        }
        word = normalize(word);
        int l = word.length();
        StringBuilder out = new StringBuilder(l + l >> 2 + 2);
        int i = 0;
        while (i < l) {
            String substring = word.substring(i);
            String key = entries.floor(substring);
            if (key != null && substring.startsWith(key)) {
                out.append(reduction.get(key));
                i += key.length();
            } else {
                out.append(word.charAt(i));
                i++;
            }
        }
        return out.toString().trim();
    }
}

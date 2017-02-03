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

package com.venaglia.roger.autocomplete.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;

/**
 * Created by ed on 2/2/17.
 */
class DictionaryTuple {

    private final String name;
    private final NavigableSet<String> words;
    private final NavigableMap<String,String[]> autocomplete;
    private final NavigableMap<String,String[]> phrases;
    private final Set<Locale> languages;

    DictionaryTuple(String name,
                    Iterable<Locale> languages,
                    NavigableSet<String> words,
                    NavigableMap<String, String[]> autocomplete,
                    NavigableMap<String, String[]> phrases) {
        assert name != null;
        assert languages != null;
        assert words != null;
        assert autocomplete != null;
        assert phrases != null;
        assert name.length() > 0;
        assert languages.iterator().hasNext();
        assert !words.isEmpty();
        assert !autocomplete.isEmpty();
//        assert !phrases.isEmpty();
        this.name = name;
        Set<Locale> l = new LinkedHashSet<>();
        for (Locale locale : languages) {
            l.add(locale);
        }
        this.languages = Collections.unmodifiableSet(l);
        this.words = Collections.unmodifiableNavigableSet(words);
        this.autocomplete = Collections.unmodifiableNavigableMap(autocomplete);
        this.phrases = Collections.unmodifiableNavigableMap(phrases);
    }

    public String getName() {
        return name;
    }

    public Set<Locale> getLanguages() {
        return languages;
    }

    public NavigableSet<String> getWords() {
        return words;
    }

    public NavigableMap<String, String[]> getAutocomplete() {
        return autocomplete;
    }

    public NavigableMap<String, String[]> getPhrases() {
        return phrases;
    }
}

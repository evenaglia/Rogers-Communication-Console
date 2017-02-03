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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.venaglia.roger.autocomplete.AutoCompleter;
import com.venaglia.roger.autocomplete.CompletablePart;
import com.venaglia.roger.autocomplete.Reducer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by ed on 2/1/17.
 */
@Singleton
public class WordAutoCompleter implements AutoCompleter {

    private final DictionaryTuple dictionary;

    @Inject
    public WordAutoCompleter(DictionaryLoader loader) {
        dictionary = loader.get();
    }

    @Override
    public Set<CompletablePart> suggest(List<CompletablePart> lastFewParts, int limit) {
        return null;
    }

    @Override
    public Set<String> suggestWord(String stringSoFar) {
        Set<String> results = new LinkedHashSet<>();
        NavigableMap<String,String[]> allWords = dictionary.getAutocomplete();
        for (String string : iterateOn(Reducer.IDENTITY.reduce(stringSoFar))) {
            for (Reducer reducer : Reducer.ALL) {
                String token = reducer.reduce(string);
                SortedMap<String,String[]> match = allWords.subMap(token, token + "\uFFFF");
                for (String[] words : match.values()) {
                    Collections.addAll(results, words);
                }
            }
        }
        return results;
    }

    private Iterable<String> iterateOn(String tokenStringSoFar) {
        int from = tokenStringSoFar.length();
        for (int i = 0; i < 4 && from >= 0; i++) {
            // limit matching to 4 "words"
            from = tokenStringSoFar.lastIndexOf(' ', from);
        }
        final String block = from >= 0 ? tokenStringSoFar.substring(from + 1) : tokenStringSoFar;
        return () -> new Iterator<String>() {

            int i = 0;

            @Override
            public boolean hasNext() {
                return i >= 0;
            }

            @Override
            public String next() {
                if (i < 0) {
                    throw new NoSuchElementException();
                }
                try {
                    return block.substring(i);
                } finally {
                    i = block.indexOf(' ', i);
                }
            }
        };
    }

    public static void main(String[] args) throws IOException {
        // training
        Collection<Reducer> reducers = Reducer.ALL;
        Set<String> words = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try (BufferedReader in = new BufferedReader(new FileReader(args[0]))) {
            for (String word = in.readLine(); word != null; word = in.readLine()) {
                words.add(word);
            }
        }
        TreeMap<String,String[]> wordsByReducedForm = new TreeMap<>();
        for (Reducer reducer : reducers) {
            for (String word : words) {
                String form = reducer.reduce(word);
                String[] options = wordsByReducedForm.get(form);
                if (options == null) {
                    options = new String[]{ word };
                    wordsByReducedForm.put(form, options);
                } else if (!options[options.length - 1].equals(word)) {
                    String[] tmp = new String[options.length + 1];
                    System.arraycopy(options, 0, tmp, 0, options.length);
                    tmp[options.length] = word;
                    wordsByReducedForm.put(form, tmp);
                }
            }
        }
        System.out.printf("Derived %d forms for %d words\n", wordsByReducedForm.size(), words.size());
        try (PrintWriter out = new PrintWriter(new FileWriter(args[1]))) {
            for (Map.Entry<String,String[]> entry : wordsByReducedForm.entrySet()) {
                out.print(entry.getKey());
                out.print(';');
                out.println(String.join(",", entry.getValue()));
            }
        }
    }
}

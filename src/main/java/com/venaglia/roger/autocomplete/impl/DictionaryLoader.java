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

import com.venaglia.roger.bundle.AbstractLoader;
import com.venaglia.roger.bundle.Bundle;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ed on 2/2/17.
 */
public class DictionaryLoader extends AbstractLoader<DictionaryTuple> {

    private final String source = "dictionary.txt";

    @Override
    protected DictionaryTuple load() {
        Reader in = new InputStreamReader(getStream(source), StandardCharsets.UTF_8);
        List<String> names = new ArrayList<>();
        NavigableSet<String> words = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<Locale> languages = new LinkedHashSet<>();
        NavigableMap<String,String[]> autocomplete = new TreeMap<>();
        NavigableMap<String,String[]> phrases = new TreeMap<>();
        parseDictionaryFile((dt) -> {
            names.add(dt.getName());
            languages.addAll(dt.getLanguages());
            words.addAll(dt.getWords());
            merge(dt.getAutocomplete(), autocomplete);
            // todo: phrases
        }, readString(in));
        String name = String.format("Composite(%s)", String.join(", ", names));
        return new DictionaryTuple(name, languages, words, autocomplete, phrases);
    }

    private void merge(Map<String,String[]> from, Map<String,String[]> accum) {
        for (Map.Entry<String,String[]> entry : from.entrySet()) {
            String key = entry.getKey();
            String[] left = accum.get(key);
            if (left != null) {
                int m = left.length;
                String[] right = entry.getValue();
                String[] composite = new String[m + right.length];
                System.arraycopy(left, 0, composite, 0, m);
                System.arraycopy(right, 0, composite, m, right.length);
                accum.put(key, composite);
            } else {
                accum.put(key, entry.getValue());
            }
        }
    }

    private void parseDictionaryFile(Consumer<DictionaryTuple> dictionaries, String s) {
        Pattern matchNameValue = Pattern.compile("^\\s+([a-zA-Z0-9_-]+)\\s*:\\s*(.*)$");
        String[] lines = s.split("\n");
        Map<String,String> buffer = new HashMap<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trim = line.trim();
            if (trim.length() == 0 || trim.charAt(0) == '#') continue;
            if (Character.isWhitespace(line.charAt(0))) {
                Matcher matcher = matchNameValue.matcher(line);
                if (matcher.find()) {
                    String key = matcher.group(1);
                    switch (key) {
                        case "iso639":
                        case "words":
                        case "phrases":
                            buffer.put(key, matcher.group(2));
                            break;
                        default:
                            System.err.printf("Unknown key in %s [line %d]: '%s'\n", source, i + 1, key);
                    }
                } else {
                    System.err.printf("Syntax error in %s [line %d]: '%s'\n", source, i + 1, line);
                }
            } else {
                if (buffer.size() > 0) {
                    DictionaryTuple dt = parseDictionary(buffer);
                    if (dt != null) {
                        dictionaries.accept(dt);
                    }
                }
                buffer = new HashMap<>();
                buffer.put("name", trim);
            }
        }
        if (buffer.size() > 0) {
            DictionaryTuple dt = parseDictionary(buffer);
            if (dt != null) {
                dictionaries.accept(dt);
            }
        }
    }

    private DictionaryTuple parseDictionary(Map<String,String> props) {
        Locale locale = new Locale(props.get("iso639"));
        NavigableMap<String,String[]> autocomplete = parseWords(read(props.get("words")));
        if (autocomplete.isEmpty()) {
            return null;
        }
        NavigableSet<String> words = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String[] strings : autocomplete.values()) {
            Collections.addAll(words, strings);
        }
//        String[] phrases = read(props.get("phrases")); // todo: phrases
        return new DictionaryTuple(props.get("name"), Collections.singleton(locale), words, autocomplete, new TreeMap<>());
    }

    private String[] read(String resource) {
        InputStream stream = resource != null && resource.length() > 0 ? getStream(resource) : null;
        return stream != null ? readLines(new InputStreamReader(stream, StandardCharsets.UTF_8)) : new String[0];
    }

    private NavigableMap<String,String[]> parseWords(String[] lines) {
        TreeMap<String, String[]> result = new TreeMap<>();
        for (String line : lines) {
            if (line.startsWith("#") || line.length() == 0) {
                continue;
            }
            String[] parts = line.split(";", 2);
            if (parts.length == 2) {
                result.put(parts[0], parts[1].split(","));
            }
        }
        return result;
    }

    public static void main(String[] args) {
        DictionaryLoader loader = new DictionaryLoader();
        loader.bundle = new Bundle();
        DictionaryTuple dt = loader.get();
        System.out.printf("Loaded %d dictionaries: %s\n", dt.getLanguages().size(), dt.getLanguages());
        System.out.printf("    containing %d words (%d forms for autocomplete)\n", dt.getWords().size(), dt.getAutocomplete().size());
        System.out.printf("    containing %d phrases\n", dt.getPhrases().size());
    }
}

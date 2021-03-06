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

import com.venaglia.roger.autocomplete.AutoCompleter;
import com.venaglia.roger.autocomplete.CompletablePart;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by ed on 9/5/16.
 */
public class DummyAutoCompleter implements AutoCompleter {

    @Override
    public Set<CompletablePart> suggest(List<CompletablePart> lastFewParts, int limit) {
        return Collections.emptySet();
    }

    @Override
    public Set<String> suggestWord(String stringSoFar) {
        return Collections.emptySet();
    }
}

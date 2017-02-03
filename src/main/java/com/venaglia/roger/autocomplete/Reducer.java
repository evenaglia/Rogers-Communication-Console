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

package com.venaglia.roger.autocomplete;

import com.venaglia.roger.autocomplete.reducers.HardConsonantReducer;
import com.venaglia.roger.autocomplete.reducers.IdentityReducer;
import com.venaglia.roger.autocomplete.reducers.SoftConsonantReducer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Transformation function that reduces words into ones which give better
 * equality comparison results.
 */
public interface Reducer {

    Reducer IDENTITY = new IdentityReducer();

    Collection<Reducer> ALL = Collections.unmodifiableCollection(
            Arrays.<Reducer>asList(IDENTITY,
                                   new HardConsonantReducer(),
                                   new SoftConsonantReducer()
    ));

    String reduce(String word);
}

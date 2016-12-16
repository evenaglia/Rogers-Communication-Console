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

package com.venaglia.roger.output;

/**
 * Created by ed on 10/4/16.
 */
public interface ActiveMessageListener {

    default void beforeAppend(OutputElement<?> element) {}

    default void afterAppend(OutputElement<?> element) {}

    default void beforeUndo() {}

    default void afterUndo() {}

    default void beforeRedo() {}

    default void afterRedo() {}

    default void beforeArchive() {}

    default void afterArchive(ArchivedMessage message) {}

    default void changed() {}
}

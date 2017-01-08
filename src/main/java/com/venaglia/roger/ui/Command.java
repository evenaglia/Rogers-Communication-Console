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

package com.venaglia.roger.ui;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ed on 1/3/17.
 */
public interface Command {

    Pattern MATCH_OK_RESPONSE = Pattern.compile("ok");

    String getCommand();
    String[] getArgs();
    default Pattern expectedResponsePattern() { return MATCH_OK_RESPONSE; }
    default void handleResponse(Matcher matcher, Consumer<Command> queue) {}
    default void handleError(IOException ioe, Consumer<Command> queue) throws IOException { throw ioe; }
}

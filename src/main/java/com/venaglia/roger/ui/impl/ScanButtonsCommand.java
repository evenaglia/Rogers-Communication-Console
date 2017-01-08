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

package com.venaglia.roger.ui.impl;

import com.venaglia.roger.ui.Command;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ed on 1/4/17.
 */
public class ScanButtonsCommand implements Command {

    private static Pattern MATCH_DOWN_RESPONSE = Pattern.compile("^down ([-x]{12})");
    private static Map<String,ScanCode> SCAN_CODE_LOOKUP;

    static {
        Map<String,ScanCode> lookup = new HashMap<>(32);
        lookup.put("------------", ScanCode.NO_BUTTONS_DOWN);
        lookup.put("x-----------", ScanCode.BUTTON_A1_DOWN);
        lookup.put("-x----------", ScanCode.BUTTON_A2_DOWN);
        lookup.put("--x---------", ScanCode.BUTTON_A3_DOWN);
        lookup.put("---x--------", ScanCode.BUTTON_A4_DOWN);
        lookup.put("----x-------", ScanCode.BUTTON_B1_DOWN);
        lookup.put("-----x------", ScanCode.BUTTON_B2_DOWN);
        lookup.put("------x-----", ScanCode.BUTTON_B3_DOWN);
        lookup.put("-------x----", ScanCode.BUTTON_B4_DOWN);
        lookup.put("--------x---", ScanCode.BUTTON_C1_DOWN);
        lookup.put("---------x--", ScanCode.BUTTON_C2_DOWN);
        lookup.put("----------X-", ScanCode.BUTTON_C3_DOWN);
        lookup.put("-----------x", ScanCode.BUTTON_C4_DOWN);
        SCAN_CODE_LOOKUP = Collections.unmodifiableMap(lookup);
    }

    private final Consumer<ScanCode> handler;

    public ScanButtonsCommand(Consumer<ScanCode> handler) {
        this.handler = handler;
    }

    @Override
    public String getCommand() {
        return "scan";
    }

    @Override
    public String[] getArgs() {
        return new String[]{};
    }

    @Override
    public Pattern expectedResponsePattern() {
        return MATCH_DOWN_RESPONSE;
    }

    @Override
    public void handleResponse(Matcher matcher, Consumer<Command> queue) {
        ScanCode scanCode = SCAN_CODE_LOOKUP.get(matcher.group(1));
        handler.accept(scanCode == null ? ScanCode.MULTIPLE_BUTTONS_DOWN : scanCode);
    }
}

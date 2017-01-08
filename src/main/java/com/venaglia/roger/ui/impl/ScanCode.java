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

/**
 * Created by ed on 1/4/17.
 */
public enum ScanCode {
    NO_BUTTONS_DOWN,
    BUTTON_A1_DOWN,
    BUTTON_A2_DOWN,
    BUTTON_A3_DOWN,
    BUTTON_A4_DOWN,
    BUTTON_B1_DOWN,
    BUTTON_B2_DOWN,
    BUTTON_B3_DOWN,
    BUTTON_B4_DOWN,
    BUTTON_C1_DOWN,
    BUTTON_C2_DOWN,
    BUTTON_C3_DOWN,
    BUTTON_C4_DOWN,
    MULTIPLE_BUTTONS_DOWN;

    public static ScanCode byCode(String code) {
        switch (code) {
            case "a1": return BUTTON_A1_DOWN;
            case "a2": return BUTTON_A2_DOWN;
            case "a3": return BUTTON_A3_DOWN;
            case "a4": return BUTTON_A4_DOWN;
            case "b1": return BUTTON_B1_DOWN;
            case "b2": return BUTTON_B2_DOWN;
            case "b3": return BUTTON_B3_DOWN;
            case "b4": return BUTTON_B4_DOWN;
            case "c1": return BUTTON_C1_DOWN;
            case "c2": return BUTTON_C2_DOWN;
            case "c3": return BUTTON_C3_DOWN;
            case "c4": return BUTTON_C4_DOWN;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}

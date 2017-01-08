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
 * Created by ed on 1/3/17.
 */
public enum DisplayNumber {
    DISPLAY7(0b00000001),
    DISPLAY6(0b00000010),
    DISPLAY5(0b00000100),
    DISPLAY4(0b00001000),
    DISPLAY3(0b00010000),
    DISPLAY2(0b00100000),
    DISPLAY1(0b01000000),
    DISPLAY0(0b10000000),
    ALL(0b11111111);

    private final String selector;

    DisplayNumber(int selector) {
        this.selector = String.format("0x%02x", selector);
    }

    public String getSelector() {
        return selector;
    }
}

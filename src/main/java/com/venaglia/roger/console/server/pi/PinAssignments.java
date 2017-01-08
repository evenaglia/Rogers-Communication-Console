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

package com.venaglia.roger.console.server.pi;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

/**
 * Created by ed on 12/7/16.
 */
public interface PinAssignments {

    interface Buttons {
        Pin COLUMN0 = RaspiPin.GPIO_26;
        Pin COLUMN1 = RaspiPin.GPIO_27;
        Pin COLUMN2 = RaspiPin.GPIO_28;
        Pin ROW1    = RaspiPin.GPIO_21;
        Pin ROW2    = RaspiPin.GPIO_22;
        Pin ROW3    = RaspiPin.GPIO_23;
        Pin ROW4    = RaspiPin.GPIO_24;
    }

    interface Displays {
        Pin CS0       = RaspiPin.GPIO_10;
        Pin CS1       = RaspiPin.GPIO_11;
        Pin MOSI      = RaspiPin.GPIO_12;
        Pin MISO      = RaspiPin.GPIO_13;
        Pin CLK       = RaspiPin.GPIO_14;
        Pin RESET     = RaspiPin.GPIO_07;
        Pin BACKLIGHT = RaspiPin.GPIO_01;
    }

}

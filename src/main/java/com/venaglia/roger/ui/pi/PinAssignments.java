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

package com.venaglia.roger.ui.pi;

import com.pi4j.io.gpio.PiFacePin;
import com.pi4j.io.gpio.Pin;

/**
 * Created by ed on 12/7/16.
 */
public interface PinAssignments {

    interface Buttons {
        Pin COLUMN0 = PiFacePin.GPIO_12;
        Pin COLUMN1 = PiFacePin.GPIO_16;
        Pin COLUMN2 = PiFacePin.GPIO_20;
        Pin ROW1    = PiFacePin.GPIO_05;
        Pin ROW2    = PiFacePin.GPIO_06;
        Pin ROW3    = PiFacePin.GPIO_13;
        Pin ROW4    = PiFacePin.GPIO_19;
    }

    interface Displays {
        Pin CS0       = PiFacePin.GPIO_08;
        Pin CS1       = PiFacePin.GPIO_07;
        Pin MOSI      = PiFacePin.GPIO_10;
        Pin MISO      = PiFacePin.GPIO_09;
        Pin CLK       = PiFacePin.GPIO_11;
        Pin BACKLIGHT = PiFacePin.GPIO_18;
    }

}

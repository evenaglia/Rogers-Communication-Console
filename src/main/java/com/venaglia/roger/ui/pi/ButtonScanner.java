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

import static com.pi4j.io.gpio.PinMode.DIGITAL_INPUT;
import static com.pi4j.io.gpio.PinMode.DIGITAL_OUTPUT;
import static com.pi4j.io.gpio.PinPullResistance.PULL_UP;
import static com.venaglia.roger.ui.pi.PinAssignments.Buttons.*;

import com.google.inject.Singleton;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.PinState;

/**
 * Created by ed on 12/7/16.
 */
@Singleton
public class ButtonScanner {

    public enum ScanCode {
        NO_BUTTONS_DOWN,
        BUTTON_00_DOWN,
        BUTTON_01_DOWN,
        BUTTON_02_DOWN,
        BUTTON_03_DOWN,
        BUTTON_10_DOWN,
        BUTTON_11_DOWN,
        BUTTON_12_DOWN,
        BUTTON_13_DOWN,
        BUTTON_20_DOWN,
        BUTTON_21_DOWN,
        BUTTON_22_DOWN,
        BUTTON_23_DOWN,
        MULTIPLE_BUTTONS_DOWN
    }

    private final GpioPinDigitalMultipurpose columnPins[];
    private final GpioPinDigitalInput rowPins[];

    public ButtonScanner(GpioController gpioController) {
        columnPins = new GpioPinDigitalMultipurpose[]{
                gpioController.provisionDigitalMultipurposePin(COLUMN0, "col0", DIGITAL_INPUT, PULL_UP),
                gpioController.provisionDigitalMultipurposePin(COLUMN1, "col1", DIGITAL_INPUT, PULL_UP),
                gpioController.provisionDigitalMultipurposePin(COLUMN2, "col2", DIGITAL_INPUT, PULL_UP)
        };
        columnPins[0].setState(PinState.LOW);
        columnPins[1].setState(PinState.LOW);
        columnPins[2].setState(PinState.LOW);
        rowPins = new GpioPinDigitalInput[]{
                gpioController.provisionDigitalInputPin(ROW0, "row0", PULL_UP),
                gpioController.provisionDigitalInputPin(ROW1, "row1", PULL_UP),
                gpioController.provisionDigitalInputPin(ROW2, "row2", PULL_UP),
                gpioController.provisionDigitalInputPin(ROW3, "row3", PULL_UP)
        };
    }

    public ScanCode scan() {
        byte bitsA = 0, bitsB = 0, bitsC = 0;
        columnPins[1].setMode(DIGITAL_INPUT);
        columnPins[2].setMode(DIGITAL_INPUT);
        columnPins[0].setMode(DIGITAL_OUTPUT);
        if (rowPins[0].getState() == PinState.LOW) bitsA |= 0b0001;
        if (rowPins[1].getState() == PinState.LOW) bitsA |= 0b0010;
        if (rowPins[2].getState() == PinState.LOW) bitsA |= 0b0100;
        if (rowPins[3].getState() == PinState.LOW) bitsA |= 0b1000;
        columnPins[0].setMode(DIGITAL_INPUT);
        columnPins[1].setMode(DIGITAL_OUTPUT);
        if (rowPins[0].getState() == PinState.LOW) bitsB |= 0b0001;
        if (rowPins[1].getState() == PinState.LOW) bitsB |= 0b0010;
        if (rowPins[2].getState() == PinState.LOW) bitsB |= 0b0100;
        if (rowPins[3].getState() == PinState.LOW) bitsB |= 0b1000;
        columnPins[1].setMode(DIGITAL_INPUT);
        columnPins[2].setMode(DIGITAL_OUTPUT);
        if (rowPins[0].getState() == PinState.LOW) bitsC |= 0b0001;
        if (rowPins[1].getState() == PinState.LOW) bitsC |= 0b0010;
        if (rowPins[2].getState() == PinState.LOW) bitsC |= 0b0100;
        if (rowPins[3].getState() == PinState.LOW) bitsC |= 0b1000;
        columnPins[2].setMode(DIGITAL_INPUT);
        byte bitSum = (byte)(bitsA + bitsB + bitsC + (bitsA == 0 ? 0b0010000 : 0) + (bitsB == 0 ? 0b0100000 : 0) + (bitsC == 0 ? 0b1000000 : 0));
        switch (bitSum) {
            case 0b1110000: return ScanCode.NO_BUTTONS_DOWN;
            case 0b1100001: return ScanCode.BUTTON_00_DOWN;
            case 0b1010001: return ScanCode.BUTTON_10_DOWN;
            case 0b0110001: return ScanCode.BUTTON_20_DOWN;
            case 0b1100010: return ScanCode.BUTTON_01_DOWN;
            case 0b1010010: return ScanCode.BUTTON_11_DOWN;
            case 0b0110010: return ScanCode.BUTTON_21_DOWN;
            case 0b1100100: return ScanCode.BUTTON_02_DOWN;
            case 0b1010100: return ScanCode.BUTTON_12_DOWN;
            case 0b0110100: return ScanCode.BUTTON_22_DOWN;
            case 0b1101000: return ScanCode.BUTTON_03_DOWN;
            case 0b1011000: return ScanCode.BUTTON_13_DOWN;
            case 0b0111000: return ScanCode.BUTTON_23_DOWN;
            default: return ScanCode.MULTIPLE_BUTTONS_DOWN;
        }
    }
}

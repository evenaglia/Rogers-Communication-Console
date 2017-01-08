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

import static com.pi4j.io.gpio.PinMode.DIGITAL_INPUT;
import static com.pi4j.io.gpio.PinMode.DIGITAL_OUTPUT;
import static com.pi4j.io.gpio.PinPullResistance.PULL_UP;
import static com.venaglia.roger.console.server.pi.PinAssignments.Buttons.*;
import static java.lang.System.currentTimeMillis;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.impl.GpioControllerImpl;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import com.pi4j.wiringpi.Gpio;
import com.venaglia.roger.console.server.ConServer;
import com.venaglia.roger.console.server.Con;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Created by ed on 1/4/17.
 */
public class ConPi extends ConServer {

    private static final byte   SOFT_RESET_COMMAND = 0x01;
    private static final byte   SET_DISPLAY_OFF_COMMAND = 0x28;
    private static final byte   SET_DISPLAY_ROTATION_COMMAND = 0x36;
    private static final byte[] SET_DISPLAY_ROTATION_DATA = { 0b01100000 };
    private static final byte   SET_DISPLAY_CLEAR_COMMAND = 0x2C;
    private static final byte[] SET_DISPLAY_CLEAR_DATA = new byte[160 * 128 * 3];
    private static final byte   SET_DISPLAY_ON_COMMAND = 0x29;
    private static final byte   SLEEP_COMMAND = 0x10;
    private static final byte   WAKE_COMMAND = 0x11;
    private static final byte   SET_COLUMN_0_COMMAND = 0x2A;
    private static final byte[] SET_COLUMN_0_DATA = { 0, 0, 0, (byte)159 };
    private static final byte   SET_ROW_0_COMMAND = 0x2B;
    private static final byte[] SET_ROW_0_DATA = { 0, 0, 0, 127 };
    private static final byte MEMORY_WRITE_COMMAND = 0x2C;

    private final GpioPinDigitalOutput reset;
    private final GpioPinPwmOutput brightness;
    private final GpioPinDigitalMultipurpose columnPins[];
    private final GpioPinDigitalInput rowPins[];
    private final SpiDevice displaySelector;
    private final SpiDevice displayBus;

    public ConPi() throws IOException {
        super();
        displaySelector = SpiFactory.getInstance(SpiChannel.CS0, SpiDevice.DEFAULT_SPI_SPEED);
        displayBus = SpiFactory.getInstance(SpiChannel.CS1, SpiDevice.DEFAULT_SPI_SPEED);
        displaySelector.write((byte)0);
        displayBus.write((byte)0, (byte)0);

        Con con = getCon();
        for (String id : "0x01,0x02,0x04,0x08,0x10,0x20,0x40,0x80".split(",")) {
            byte[] colorBars = getColorBars();
            byte who = (byte)Integer.parseInt(id.substring(2, 16));
            con.wake(who);
            con.updateImage(who, colorBars);
        }
        con.brightness(1000);

        GpioController gpioController = new GpioControllerImpl(GpioFactory.getDefaultProvider());
        reset = gpioController.provisionDigitalOutputPin(PinAssignments.Displays.RESET, PinState.HIGH);
        reset.setShutdownOptions(false, PinState.LOW);
        brightness = gpioController.provisionPwmOutputPin(PinAssignments.Displays.BACKLIGHT);
        columnPins = new GpioPinDigitalMultipurpose[]{
                gpioController.provisionDigitalMultipurposePin(COLUMN0, "col0", DIGITAL_INPUT, PULL_UP),
                gpioController.provisionDigitalMultipurposePin(COLUMN1, "col1", DIGITAL_INPUT, PULL_UP),
                gpioController.provisionDigitalMultipurposePin(COLUMN2, "col2", DIGITAL_INPUT, PULL_UP)
        };
        rowPins = new GpioPinDigitalInput[]{
                gpioController.provisionDigitalInputPin(ROW1, "row0", PULL_UP),
                gpioController.provisionDigitalInputPin(ROW2, "row1", PULL_UP),
                gpioController.provisionDigitalInputPin(ROW3, "row2", PULL_UP),
                gpioController.provisionDigitalInputPin(ROW4, "row3", PULL_UP)
        };
        Gpio.pwmSetMode(Gpio.PWM_MODE_MS);
        Gpio.pwmSetRange(PWM_RANGE);
        Gpio.pwmSetClock(100);
        brightness.setPwm(32);
    }

    @Override
    protected Con getCon() {
        return new Con() {
            @Override
            public void brightness(int value) {
                brightness.setPwm(value);
            }

            @Override
            public void wake(byte selectorByte) throws IOException {
                if (selectorByte == 0) {
                    return; // no-op
                }
                sendCommandAndData(selectorByte, WAKE_COMMAND);
                sleepUntil(currentTimeMillis() + 250L);
            }

            @Override
            public void sleep(byte selectorByte) throws IOException {
                if (selectorByte == 0) {
                    return; // no-op
                }
                sendCommandAndData(selectorByte, SLEEP_COMMAND);
                sleepUntil(currentTimeMillis() + 250L);
            }

            public void initializeConLcd(byte selectorByte) throws IOException {
                if (selectorByte == 0) {
                    return; // no-op
                }
                sendCommandAndData(selectorByte, WAKE_COMMAND);
                sleepUntil(currentTimeMillis() + 250L);
                sendCommandAndData(selectorByte, SET_DISPLAY_OFF_COMMAND);
                sendCommandAndData(selectorByte, SET_DISPLAY_CLEAR_COMMAND, SET_DISPLAY_CLEAR_DATA);
                sendCommandAndData(selectorByte, SET_DISPLAY_ROTATION_COMMAND, SET_DISPLAY_ROTATION_DATA);
                sendCommandAndData(selectorByte, SET_DISPLAY_ON_COMMAND);
            }

            @Override
            public void softReset(byte selectorByte) throws IOException {
                if (selectorByte == 0) {
                    return; // no-op
                }
                sendCommandAndData(selectorByte, SOFT_RESET_COMMAND);
                sleepUntil(currentTimeMillis() + 250L);
                initializeConLcd(selectorByte);
            }

            @Override
            public void hardReset() throws IOException {
                reset.pulse(300, PinState.LOW, false);
                sleepUntil(currentTimeMillis() + 500L);
                initializeConLcd((byte)0xFF);
            }

            @Override
            public void updateImage(byte selectorByte, byte[] data) throws IOException {
                if (selectorByte == 0) {
                    return; // no-op
                }
                sendCommandAndData(selectorByte, SET_COLUMN_0_COMMAND, SET_COLUMN_0_DATA);
                sendCommandAndData(selectorByte, SET_ROW_0_COMMAND, SET_ROW_0_DATA);
                sendCommandAndData(selectorByte, MEMORY_WRITE_COMMAND, data);
            }

            @Override
            public void readButtons(Consumer<Boolean> buttonStateConsumer) {
                columnPins[1].setMode(DIGITAL_INPUT);
                columnPins[2].setMode(DIGITAL_INPUT);
                columnPins[0].setMode(DIGITAL_OUTPUT);
                columnPins[0].setState(PinState.LOW);
                buttonStateConsumer.accept(rowPins[0].getState() == PinState.LOW);
                buttonStateConsumer.accept(rowPins[1].getState() == PinState.LOW);
                buttonStateConsumer.accept(rowPins[2].getState() == PinState.LOW);
                buttonStateConsumer.accept(rowPins[3].getState() == PinState.LOW);
                columnPins[0].setMode(DIGITAL_INPUT);
                columnPins[1].setMode(DIGITAL_OUTPUT);
                columnPins[1].setState(PinState.LOW);
                buttonStateConsumer.accept(rowPins[0].getState() == PinState.LOW);
                buttonStateConsumer.accept(rowPins[1].getState() == PinState.LOW);
                buttonStateConsumer.accept(rowPins[2].getState() == PinState.LOW);
                buttonStateConsumer.accept(rowPins[3].getState() == PinState.LOW);
                columnPins[1].setMode(DIGITAL_INPUT);
                columnPins[2].setMode(DIGITAL_OUTPUT);
                columnPins[2].setState(PinState.LOW);
                buttonStateConsumer.accept(rowPins[0].getState() == PinState.LOW);
                buttonStateConsumer.accept(rowPins[1].getState() == PinState.LOW);
                buttonStateConsumer.accept(rowPins[2].getState() == PinState.LOW);
                buttonStateConsumer.accept(rowPins[3].getState() == PinState.LOW);
                columnPins[2].setMode(DIGITAL_INPUT);
            }

            private final byte[] selectorBuffer = { 0 };
            private final byte[] commandBuffer = { 0 };

            private void sendCommandAndData(byte to, int command, byte... data) throws IOException {
                selectorBuffer[0] = to;
                commandBuffer[0] = (byte)command;
                displaySelector.write(selectorBuffer);
                displayBus.write(commandBuffer);
                for (int i = 0, l = data.length; i < l; i += 2000) {
                    // Break in to 2KiB chunks to avoid buffer overruns in the native code.
                    displayBus.write(data, i, Math.min(2000, l - i));
                }
            }
        };
    }

    // Static methods

    private static byte[] buildCommand(int command, int... data) {
        byte[] buf = new byte[data.length + 1];
        int i = 0;
        buf[i++] = (byte)command;
        for (int v : data) {
            buf[i++] = (byte)v;
        }
        return buf;
    }
}

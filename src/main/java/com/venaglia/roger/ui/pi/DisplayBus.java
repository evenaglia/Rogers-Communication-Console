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

import static java.lang.System.currentTimeMillis;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import com.pi4j.wiringpi.SoftPwm;
import com.venaglia.roger.RogerModule;
import com.venaglia.roger.buttons.ButtonFace;
import com.venaglia.roger.buttons.ButtonSetLoader;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by ed on 12/9/16.
 */
@Singleton
public class DisplayBus {

    public enum DisplayNumber {
        DISPLAY7(0b00000001),
        DISPLAY6(0b00000010),
        DISPLAY5(0b00000100),
        DISPLAY4(0b00001000),
        DISPLAY3(0b00010000),
        DISPLAY2(0b00100000),
        DISPLAY1(0b01000000),
        DISPLAY0(0b10000000);

        private final byte[] selector;

        DisplayNumber(int selector) {
            this.selector = new byte[]{ (byte)(selector | 0x80)};
        }
    }

    private final SpiDevice displayBus;
    private final SpiDevice displaySelector;
    private final BlockingQueue<Command> queue;

    @SuppressWarnings("UnusedParameters")
    @Inject
    public DisplayBus(GpioController gpioController) throws IOException {
        displayBus = SpiFactory.getInstance(SpiChannel.CS0, 8000000);
        displaySelector = SpiFactory.getInstance(SpiChannel.CS1, 8000000);
        SoftPwm.softPwmCreate(PinAssignments.Displays.BACKLIGHT.getAddress(), 0, 64);
        queue = new ArrayBlockingQueue<>(256, true);
        Thread spiWriterThread = new Thread(this::writeLoop, "Display Bus Writer");
        spiWriterThread.setDaemon(true);
        spiWriterThread.start();
        reset();
    }

    public void reset() {
        for (DisplayNumber displayNumber : DisplayNumber.values()) {
            sendCommand(displayNumber, new byte[]{ 0x01 }); // reset
        }
        for (DisplayNumber displayNumber : DisplayNumber.values()) {
            sendCommand(displayNumber, new byte[]{ 0x02 }); // nop - adds some extra time after the reset
        }
        for (byte[] bytes : ButtonFace.getInitCommands()) {
            for (DisplayNumber displayNumber : DisplayNumber.values()) {
                sendCommand(displayNumber, bytes);
            }
        }
    }

    public void sendCommand(DisplayNumber displayNumber, byte[]... commands) {
        if (!queue.offer(new Command(displayNumber, commands))) {
            throw new RuntimeException("Unable to enqueue command(s): Queue is full");
        }
    }

    public void setBacklight(float value) {
        if (!queue.offer(new Command(value))) {
            throw new RuntimeException("Unable to enqueue command(s): Queue is full");
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    private void writeLoop() {
        long[] waitUntil = new long[DisplayNumber.values().length];
        Command command = null;
        while (true) {
            try {
                command = command == null ? queue.take() : command;
                if (command.displayNumber == null && command.data == null) {
                    SoftPwm.softPwmWrite(PinAssignments.Displays.BACKLIGHT.getAddress(), command.ledValue);
                }
                if (displaySelector != null) {
                    displaySelector.write(command.displayNumber.selector);
                }
                long until = waitUntil[command.displayNumber.ordinal()];
                for (long now = currentTimeMillis(); now > until; now = currentTimeMillis()) {
                    Thread.sleep(now - until);
                }
                double sendTime = 0.0;
                for (byte[] data : command.data) {
                    sendTime += data.length / 1000.0;
                    if (command.displayNumber == DisplayNumber.DISPLAY0) {
                        displayBus.write(data);
                    }
                }
                waitUntil[command.displayNumber.ordinal()] = currentTimeMillis() + 125L + Math.round(sendTime) ;
                command = null;
            } catch (InterruptedException e) {
                // don't care
            } catch (IOException e) {
                command = null;
                e.printStackTrace();
            }
        }
    }

    private static class Command {
        final DisplayNumber displayNumber;
        final byte[][] data;
        final int ledValue;

        private Command(DisplayNumber displayNumber, byte[][] data) {
            assert displayNumber != null;
            assert data != null && data.length > 0;
            this.displayNumber = displayNumber;
            this.data = data;
            this.ledValue = -1;
        }

        private Command(float led) {
            this.displayNumber = null;
            this.data = null;
            this.ledValue = Math.round(Math.max(0.0f, Math.min(1.0f, led)) * 64);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        try {
            Injector injector = Guice.createInjector(new RogerModule());
            ButtonSetLoader buttonSetLoader = injector.getInstance(ButtonSetLoader.class);
            ButtonFace face = buttonSetLoader.get().get("Feel").getButtonFace();
            DisplayBus displayBus = injector.getInstance(DisplayBus.class);
            displayBus.sendCommand(DisplayNumber.DISPLAY0, face.getButtonUpdateCommands());
            Thread.sleep(5000L);
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

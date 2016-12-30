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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import com.pi4j.io.spi.SpiMode;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.SoftPwm;
import com.venaglia.roger.buttons.ButtonFace;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
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
        DISPLAY0(0b10000000),
        ALL     (0b11111111);

        private final byte[] selector;

        DisplayNumber(int selector) {
            this.selector = new byte[]{ (byte)(selector)};
        }
    }

    private final SpiDevice displayBus;
    private final SpiDevice displaySelector;
    private final BlockingQueue<Command> queue;

    @SuppressWarnings("UnusedParameters")
    @Inject
    public DisplayBus(GpioController gpioController) throws IOException {
        if (Gpio.wiringPiSetup() != 0) {
            throw new IOException("GPIO setup was unsuccessful!");
        }
        displayBus = SpiFactory.getInstance(SpiChannel.CS0, SpiDevice.DEFAULT_SPI_SPEED, SpiMode.MODE_0);
        displaySelector = SpiFactory.getInstance(SpiChannel.CS1, SpiDevice.DEFAULT_SPI_SPEED, SpiMode.MODE_0);
        SoftPwm.softPwmCreate(PinAssignments.Displays.BACKLIGHT.getAddress(), 8, 64);
        Gpio.pinMode(PinAssignments.Displays.RESET.getAddress(), Gpio.OUTPUT);
        Gpio.digitalWrite(PinAssignments.Displays.RESET.getAddress(), Gpio.LOW);
        queue = new ArrayBlockingQueue<>(256, true);
        Thread spiWriterThread = new Thread(this::writeLoop, "Display Bus Writer");
        spiWriterThread.setDaemon(true);
        spiWriterThread.start();
        reset(true);
    }

    public void reset(boolean hard) {
        if (hard) {
            queueCommand(new Command());
        } else {
            queueCommand(new Command(DisplayNumber.ALL, new byte[][]{ new byte[]{ 0x01 } }, 150L));
        }
        for (byte[] bytes : ButtonFace.getInitCommands()) {
            queueCommand(new Command(DisplayNumber.ALL, new byte[][]{ bytes }));
        }
    }

    public void sendCommand(DisplayNumber displayNumber, byte[]... commands) {
        queueCommand(new Command(displayNumber, commands));
    }

    public void sleep(DisplayNumber displayNumber, boolean sleep) {
        byte[] data = new byte[]{ (byte)(sleep ? 0x10 : 0x11) };
        queueCommand(new Command(displayNumber, new byte[][]{ data }, 125));
    }

    public void setBacklight(float value) {
        queueCommand(new Command(value));
    }

    private void queueCommand(Command c) {
        if (!queue.offer(c)) {
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
                long until = waitUntil[command.displayNumber.ordinal()];
                sleepUntil(until);
                until = 0;
                if (command.reset500ms) {
                    displaySelector.write(command.displayNumber.selector);
                    Gpio.digitalWrite(PinAssignments.Displays.RESET.getAddress(), Gpio.LOW);
                    sleepUntil(currentTimeMillis() + 300L);
                    Gpio.digitalWrite(PinAssignments.Displays.RESET.getAddress(), Gpio.HIGH);
                    sleepUntil(currentTimeMillis() + 200L);
                }
                if (command.ledValue >= 0 && command.ledValue <= 64) {
                    SoftPwm.softPwmWrite(PinAssignments.Displays.BACKLIGHT.getAddress(), command.ledValue);
                    until = currentTimeMillis() + 125L;
                }
                if (command.data != null) {
                    displaySelector.write(command.displayNumber.selector);
                    for (byte[] data : command.data) {
                        for (int i = 0, r = data.length; r > 0; i += 2000, r -= 2000) {
                            displayBus.write(data, i, Math.min(r, 2000));
                        }
                    }
                    until = currentTimeMillis() + command.extraWaitTime;
                }
                waitUntil[command.displayNumber.ordinal()] = until;
                command = null;
            } catch (InterruptedException e) {
                // don't care
            } catch (IOException e) {
                command = null;
                e.printStackTrace();
            }
        }
    }

    private void sleepUntil(long until) throws InterruptedException {
        for (long now = currentTimeMillis(); now > until; now = currentTimeMillis()) {
            Thread.sleep(now - until);
        }
    }

    private static class Command {
        final DisplayNumber displayNumber;
        final byte[][] data;
        final long extraWaitTime;
        final int ledValue;
        final boolean reset500ms;

        private Command(DisplayNumber displayNumber, byte[][] data) {
            this(displayNumber, data, 0L);
        }

        private Command(DisplayNumber displayNumber, byte[][] data, long extraWaitTime) {
            assert displayNumber != null;
            assert data != null && data.length > 0;
            this.displayNumber = displayNumber;
            this.data = data;
            this.extraWaitTime = Math.max(0, Math.min(2500, extraWaitTime));
            this.ledValue = -1;
            this.reset500ms = false;
        }

        private Command(float ledValue) {
            this.displayNumber = null;
            this.data = null;
            this.extraWaitTime = 0L;
            this.ledValue = Math.round(Math.max(0.0f, Math.min(1.0f, ledValue)) * 64);
            this.reset500ms = false;
        }

        private Command() {
            this.displayNumber = null;
            this.data = null;
            this.extraWaitTime = 0L;
            this.ledValue = -1;
            this.reset500ms = true;
        }
    }

    private static byte[][] getColorBars() throws IOException {
        @SuppressWarnings("SpellCheckingInspection")
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAKAAAACACAMAAAC7vZIpAAABO1BMVEUCAgIKAAIE" +
                                                "BAQFBQUGBgYRCB0PChAQDAsPCxkPCxwRDBAKDwkKDhcPDgkLDhUPDgoODg4LDw4R" +
                                                "DgcODhAKDxINDw4NEAcKEBAPDw8IEgoMEQ1JAQIfDhRPAgoSExcMFhUUFBQTFRQM" +
                                                "GAwhEwADA/8DBP4WFhYBBf4AHyUbGwBoAR8AIx5GBX0fIAAAJy0WG6pfAORgAOdl" +
                                                "AOFhAedkAeVnAPlQE4oAOgALKaEKK5ILLpYOLpH/AQnxBgL9AwQuQkv4Af//AP//" +
                                                "Af/8A/35BP/6BP32BvvzB//2B/jkFNzaGNnfFuB3SLyAQ9+AROJdo/hdpflTxUut" +
                                                "qvsD/AAB+vwF+v0C+/8C/PoC/PsA/fsA//QQ/9ns6u///A39/gP9/wT/6+r/6+3/" +
                                                "8Pz49/X89vj/9vb/+fn7+v/7/fph/+z1AAAAAWJLR0QAiAUdSAAAAAlwSFlzAAAL" +
                                                "EwAACxMBAJqcGAAAAAd0SU1FB+AMHQgbAW0xedUAAAAmaVRYdENvbW1lbnQAAAAA" +
                                                "AENyZWF0ZWQgd2l0aCBHSU1QIG9uIGEgTWFjleRfWwAAAUZJREFUeNrt0tVOA0EA" +
                                                "heHBHZbF3aW4w+LuDsVbHPr+T0CyW7INnAmEpFz9//XJzJfJmEdd7F4Vvz5X3Z7J" +
                                                "TtZUGwuLqvkunQEIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQI" +
                                                "ECBAgAABAgQIECBAgAABAgQIECDA/wQO6wYjqo6DQ9X+tGzpWHXU16RqONWZHl1l" +
                                                "taplRT7Kbo2sN6q6Ka9TFd7pjOVlSx1V86oE7six0y//w2W+HFdZ/hpAgAABphvY" +
                                                "qStxXbfi2ynty+t+W5vbqe0Vh5WF66EL1VWOBj7ozJxuxvO855evPb0m/AbGJlKb" +
                                                "HAkrCO9szFPlZmQHJVe1rX5t9Tozay/xZmtqdNxWkfNTmck+gd1BljVAgAABAgQI" +
                                                "ECBAgAABAgQIECBAgH8CvqcHaAJf1u+AH2W5R6gIl+R/AAAAAElFTkSuQmCC");
        BufferedImage bufferedImage;
        bufferedImage = ImageIO.read(new ByteArrayInputStream(png));

        int i = 0;
        byte[] buf = new byte[61441];
        buf[i++] = (byte)0x2C; // RAMWR
        for (int argb : bufferedImage.getRGB(0, 0, 160, 128, null, 0, 160)) {
            buf[i++] = (byte)((argb >> 16) & 0xFF);
            buf[i++] = (byte)((argb >> 8) & 0xFF);
            buf[i++] = (byte)(argb & 0xFF);
        }
        return new byte[][]{
                new byte[]{0x2A, 0, 0, 0, (byte)159},
                new byte[]{0x2B, 0, 0, 0, 127},
                buf
        };
    }

    public static void main(String[] args) throws InterruptedException {
        try {
            DisplayBus displayBus = new DisplayBus(GpioFactory.getInstance());
            displayBus.sendCommand(DisplayNumber.ALL, ButtonFace.getInitCommands());
            Thread.sleep(1000L);
            displayBus.sendCommand(DisplayNumber.DISPLAY0, getColorBars());
            Thread.sleep(5000L);
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

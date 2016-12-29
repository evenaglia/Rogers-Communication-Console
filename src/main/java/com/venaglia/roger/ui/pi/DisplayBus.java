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
import com.pi4j.wiringpi.SoftPwm;
import com.venaglia.roger.buttons.ButtonFace;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
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
        SoftPwm.softPwmCreate(PinAssignments.Displays.BACKLIGHT.getAddress(), 8, 64);
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
                if (command.displayNumber == null || command.data == null) {
                    if (command.ledValue >= 0 && command.ledValue <= 64) {
                        SoftPwm.softPwmWrite(PinAssignments.Displays.BACKLIGHT.getAddress(), command.ledValue);
                    }
                    command = null;
                    continue;
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

    private static byte[][] getColorBars() {
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
        try {
            bufferedImage = ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Graphics2D g = (Graphics2D)bufferedImage.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 160, 128);

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
            displayBus.sendCommand(DisplayNumber.DISPLAY0, ButtonFace.getInitCommands());
            displayBus.sendCommand(DisplayNumber.DISPLAY0, getColorBars());
            Thread.sleep(5000L);
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

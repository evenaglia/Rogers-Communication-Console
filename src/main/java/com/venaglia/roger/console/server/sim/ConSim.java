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

package com.venaglia.roger.console.server.sim;

import static java.lang.System.currentTimeMillis;

import com.venaglia.roger.console.server.ConServer;
import com.venaglia.roger.console.server.Con;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by ed on 8/28/16.
 */
public class ConSim extends ConServer {

    private static final long DATA_RATE = 1000000;

    private final SimulatedButtons simulatedButtons;
    private final Executor executor;

    public ConSim(SimulatedButtons simulatedButtons) throws IOException {
        this.simulatedButtons = simulatedButtons;
        createButtonFrame(simulatedButtons);
        this.executor = new ThreadPoolExecutor(1,
                                               1,
                                               1,
                                               TimeUnit.DAYS,
                                               new ArrayBlockingQueue<>(32, true),
                                               runnable -> {
                                                   Thread thread = new Thread(runnable, "Image Updater");
                                                   thread.setDaemon(true);
                                                   return thread;
                                               });
    }

    @SuppressWarnings("Duplicates")
    private void createButtonFrame(SimulatedButtons simulatedButtons) {
        JFrame frame = new JFrame();
        frame.setTitle("Console Simulator");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setBackground(Color.BLACK);
        frame.add(simulatedButtons);
        frame.setSize(simulatedButtons.getSize());
        frame.getContentPane().setCursor(Cursor.getDefaultCursor());
        frame.setVisible(true);
    }

    @Override
    protected Con getCon() {
        return new Con() {

            private final float pwmRange = PWM_RANGE;
            private final byte[][] last = new byte[8][];
            private final byte[] buffer = new byte[160 * 128 * 3];

            {
                for (int i = 0; i < 8; i++) {
                    last[i] = new byte[160 * 128 * 3];
                }
            }

            private int brightnessValue = 0;
            private float brightness = 0.0f;
            private float brightnessScale = 0.0f;
            private float brightnessBase = 0.0f;
            private boolean[] sleeping = new boolean[8];

            @Override
            public void hardReset() throws IOException {
                long until = currentTimeMillis() + 1260L;
                blankImage((byte)0xFF);
                until += timeFor(0, 0, 3, 3, 6, 1, 3, 1, 2, 1, 0, 1, 1, 4, 4, 16, 16, 0, 0, 160 * 128 * 3);
                sleepUntil(until);
            }

            @Override
            public void softReset(byte selectorByte) throws IOException {
                if (selectorByte == 0) {
                    return; // no-op
                }
                long until = currentTimeMillis() + 760L;
                blankImage(selectorByte);
                until += timeFor(0, 0, 3, 3, 6, 1, 3, 1, 2, 1, 0, 1, 1, 4, 4, 16, 16, 0, 0, 160 * 128 * 3);
                sleepUntil(until);
            }

            @Override
            public void brightness(int value) throws IOException {
                if (brightnessValue != value) {
                    brightnessValue = Math.min(PWM_RANGE, Math.max(0, value));
                    brightness = value / pwmRange;
                    brightnessBase = brightness * brightness * 0.75f;
                    brightnessScale = Math.min(1.0f - brightnessBase, brightness * 2.25f + 0.075f);
                    refreshImage((byte)0xFF);
                }
            }

            @Override
            public void sleep(byte selectorByte) throws IOException {
                if (selectorByte == 0) {
                    return; // no-op
                }
                long until = currentTimeMillis() + 250L;
                byte updates = 0;
                for (int i = 0, m = 1; i < 8; i++, m <<= 1) {
                    if ((selectorByte & m) != 0 && !sleeping[i]) {
                        updates |= m;
                        sleeping[i] = true;
                    }
                }
                if (updates != 0) {
                    blankImage(updates);
                }
                until += timeFor(0);
                sleepUntil(until);
            }

            @Override
            public void wake(byte selectorByte) throws IOException {
                if (selectorByte == 0) {
                    return; // no-op
                }
                long until = currentTimeMillis() + 250L;
                byte updates = 0;
                for (int i = 0, m = 1; i < 8; i++, m <<= 1) {
                    if ((selectorByte & m) != 0 && sleeping[i]) {
                        updates |= m;
                        sleeping[i] = false;
                    }
                }
                if (updates != 0) {
                    blankImage(updates);
                }
                until += timeFor(0);
                sleepUntil(until);
            }

            @Override
            public void updateImage(byte selectorByte, byte[] data) throws IOException {
                assert data.length == buffer.length;
                if (selectorByte == 0) {
                    return; // no-op
                }
                long until = currentTimeMillis() + 250L;
                byte[] d = data.clone();
                executor.execute(() -> {
                    float scale = this.brightnessScale;
                    float base = this.brightnessBase;
                    byte update = 0;
                    for (int i = 0, m = 1; i < 8; i++, m <<= 1) {
                        if ((selectorByte & m) != 0 && !sleeping[i]) {
                            System.arraycopy(d, 0, last[i], 0, d.length);
                            update |= m;
                        }
                    }
                    for (int j = 0, l = d.length; j < l; j++) {
                        buffer[j] = (byte)((int)((d[j] & 0xFF) * scale + base * 0xFF) & 0xFF);
                    }
                    simulatedButtons.setImageBytesRgb(update, buffer);
                });
                until += timeFor(4, 4, 160 * 128 * 3); // rol/col select + image data transfer
                sleepUntil(until);
            }

            private long timeFor(int... bytes) {
                double seconds = 0;
                for (int c : bytes) {
                    long totalBytes = 1 + 1 + c; // selector byte + command byte + data bytes
                    seconds = (totalBytes * 9 / DATA_RATE) + 0.00025; // add 250µS for delay between selector and lcd data
                }
                return Math.round(Math.ceil(seconds * 1000.0)); // always round up to the next ms
            }

            @Override
            public void readButtons(Consumer<Boolean> resultBuilder) {
                for (Boolean state : simulatedButtons.getButtonStates()) {
                    resultBuilder.accept(state);
                }
            }

            @Override
            public void sendRaw(byte selectorByte, byte command, byte... bytes) throws IOException {
                StringBuilder buffer = new StringBuilder(11 + 5 * bytes.length);
                buffer.append("[CS0] 0x").append(toHex((byte)(~selectorByte & 0xFF))).append("\n");
                buffer.append("[CS1] 0x").append(toHex(command));
                for (byte b : bytes) {
                    buffer.append(" 0x").append(toHex(b));
                }
                System.out.println(buffer);
                sleepUntil(currentTimeMillis() + timeFor(bytes.length));
            }

            private char[] toHex(byte selectorByte) {
                return new char[]{
                        "0123456789abcdef".charAt(selectorByte >> 4 & 0x0F),
                        "0123456789abcdef".charAt(selectorByte & 0x0F)
                };
            }

            private void refreshImage(byte selectorByte) {
                if (selectorByte == 0) {
                    return; // no-op
                }
                executor.execute(() -> {
                    float scale = this.brightnessScale;
                    float base = this.brightnessBase;
                    for (int i = 0, m = 1; i < 8; i++, m <<= 1) {
                        if ((selectorByte & m) != 0 && !sleeping[i]) {
                            byte[] data = last[i];
                            for (int j = 0, l = data.length; j < l; j++) {
                                buffer[j] = (byte)((int)((data[j] & 0xFF) * scale + base * 0xFF) & 0xFF);
                            }
                            simulatedButtons.setImageBytesRgb((byte)(m & 0xFF), buffer);
                        }
                    }
                });
            }

            private void blankImage(byte selectorByte) {
                if (selectorByte == 0) {
                    return; // no-op
                }
                executor.execute(() -> {
                    byte base = (byte)((int)(brightnessBase * 0xFF) & 0xFF);
                    Arrays.fill(buffer, base);
                    for (int i = 0, m = 1; i < 8; i++, m <<= 1) {
                        if ((selectorByte & m) != 0) {
                            simulatedButtons.setImageBytesRgb((byte)(m & 0xFF), buffer);
                        }
                    }
                });
            }
        };
    }
}

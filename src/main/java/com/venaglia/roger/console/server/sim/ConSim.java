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

    private static final long DATA_RATE = 8000000;

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
            private final BufferSpectrum[] last = new BufferSpectrum[8];
            private final BufferSpectrum buffer = new BufferSpectrum();

            {
                for (int i = 0; i < 8; i++) {
                    last[i] = new BufferSpectrum();
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
                    brightnessBase = brightness * brightness * 0.25f;
                    brightnessScale = Math.min(1.0f - brightnessBase, brightness * 1.5f + 0.075f);
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
                assert data.length == 160 * 128 * 3 || data.length == 160 * 128 * 2 || data.length == 160 * 128 * 3 / 2;
                if (selectorByte == 0) {
                    return; // no-op
                }
                long until = currentTimeMillis() + 250L;
                byte[] d = data.clone();
                executor.execute(() -> {
                    float scale = this.brightnessScale;
                    float base = this.brightnessBase;
                    byte update = 0;
                    BufferSpectrum spectrum = null;
                    for (int i = 0, m = 1; i < 8; i++, m <<= 1) {
                        if ((selectorByte & m) != 0 && !sleeping[i]) {
                            System.arraycopy(d, 0, last[i].select(data.length), 0, d.length);
                            spectrum = last[i];
                            update |= m;
                        }
                    }
                    if (spectrum != null) {
                        simulatedButtons.setImageBytesRgb(update, spectrum.scale(scale, base, buffer));
                    }
                });
                until += timeFor(4, 4, buffer.current().length); // rol/col select + image data transfer
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
                            byte[] data = last[i].scale(scale, base, buffer);
                            simulatedButtons.setImageBytesRgb((byte)(m & 0xFF), data);
                        }
                    }
                });
            }

            private void blankImage(byte selectorByte) {
                if (selectorByte == 0) {
                    return; // no-op
                }
                byte[] b = buffer.current();
                executor.execute(() -> {
                    Arrays.fill(b, buffer.black(brightnessBase));
                    for (int i = 0, m = 1; i < 8; i++, m <<= 1) {
                        if ((selectorByte & m) != 0) {
                            simulatedButtons.setImageBytesRgb((byte)(m & 0xFF), b);
                        }
                    }
                });
            }
        };
    }

    private static class BufferSpectrum {
        private final byte[] _888 = new byte[160 * 128 * 3];
        private final byte[] _565 = new byte[160 * 128 * 2];
        private final byte[] _444 = new byte[160 * 128 * 3 / 2];

        private byte[] active = _888;

        byte[] select(int dataLength) {
            switch (dataLength) {
                case 160 * 128 * 3:     active = _888; break;
                case 160 * 128 * 2:     active = _565; break;
                case 160 * 128 * 3 / 2: active = _444; break;
            }
            return active;
        }

        byte[] current() {
            return active;
        }

        byte[] scale(float scale, float base, BufferSpectrum buffer) {
            byte[] b = buffer.select(active.length);
            switch (active.length) {
                case 160 * 128 * 3: // 8-8-8
                    for (int j = 0, l = active.length; j < l; j++) {
                        b[j] = (byte)((int)((active[j] & 0xFF) * scale + base * 0xFF) & 0xFF);
                    }
                    break;
                case 160 * 128 * 2: // 5-6-5
                    for (int j = 0, l = active.length; j < l; j+=2) {
                        int rgb = (active[j] & 0xFF) << 8 | (active[j+1] & 0xFF);
                        rgb = (int)((rgb & 0xF800) * scale + base * 0xF800) & 0xF800 |
                              (int)((rgb & 0x07E0) * scale + base * 0x07E0) & 0x07E0 |
                              (int)((rgb & 0x001F) * scale + base * 0x001F) & 0x001F;
                        b[j]   = (byte)(rgb >> 8 & 0xFF);
                        b[j+1] = (byte)(rgb & 0xFF);
                    }
                    break;
                case 160 * 128 * 3 / 2: // 4-4-4
                    for (int j = 0, l = active.length; j < l; j+=3) {
                        int rgb = (active[j] & 0xFF) << 16 | (active[j+1] & 0xFF) << 8 | (active[j+2] & 0xFF);
                        rgb = (int)((rgb & 0xF00000) * scale + base * 0xF00000) & 0xF00000 |
                              (int)((rgb & 0x0F0000) * scale + base * 0x0F0000) & 0x0F0000 |
                              (int)((rgb & 0x00F000) * scale + base * 0x00F000) & 0x00F000 |
                              (int)((rgb & 0x000F00) * scale + base * 0x000F00) & 0x000F00 |
                              (int)((rgb & 0x0000F0) * scale + base * 0x0000F0) & 0x0000F0 |
                              (int)((rgb & 0x00000F) * scale + base * 0x00000F) & 0x00000F;
                        b[j]   = (byte)((rgb) >> 16 & 0xFF);
                        b[j+1]   = (byte)((rgb) >> 8 & 0xFF);
                        b[j+2] = (byte)((rgb) & 0xFF);
                    }
                    break;
            }
            return b;
        }

        byte black(float base) {
            int value = Math.min(0xFF, Math.round(base * 0xFF)) & 0xFF;
            int b;
            switch (active.length) {
                case 160 * 128 * 3:
                    return (byte)value;
                case 160 * 128 * 2:
                    b = (value << 8 & 0xF800) | (value << 3 & 0x07E0) | (value >> 3 & 0x001F);
                    return (byte)b;
                case 160 * 128 * 3 / 2:
                    b = (value << 4 & 0xF00) | (value & 0x0F0) | (value >> 4 & 0x00F);
                    b |= b << 12;
                    return (byte)b;
            }
            return 0;
        }
    }
}

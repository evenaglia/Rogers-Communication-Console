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

package com.venaglia.roger.buttons;

import com.venaglia.Environment;
import com.venaglia.roger.bundle.Bundle;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by ed on 12/7/16.
 */
public class ButtonFace {

    public static final ButtonFace NIL = new ButtonFace();

    private static final int IMAGE_COMMAND_BYTES = 128 * 160 * 3 + 1;
    private static final byte[] RESET = buildCommand(0x01);
    private static final byte[] SET_DISPLAY_OFF = buildCommand(0x28);
    private static final byte[] SET_DISPLAY_ROTATION = buildCommand(0x36, 0b01100000);
    private static final byte[] SET_DISPLAY_CLEAR = buildCommand(0x2C, new int[160*128]);
    private static final byte[] SET_DISPLAY_ON = buildCommand(0x29);
    private static final byte[] SET_COLUMN_0_COMMAND = buildCommand(0x2A, 0, 0, 0, 159);
    private static final byte[] SET_ROW_0_COMMAND = buildCommand(0x2B, 0, 0, 0, 127);

    private static byte[] buildCommand(int command, int... data) {
        byte[] buf = new byte[data.length + 1];
        int i = 0;
        buf[i++] = (byte)command;
        for (int v : data) {
            buf[i++] = (byte)v;
        }
        return buf;
    }

    private final String filename;
    private final String label;
    private final Bundle bundle;
    private final Font font;

    private BufferedImage bufferedImage;
    private byte[][] buttonFaceData;

    private ButtonFace() {
        this.filename = null;
        this.label = "";
        this.bundle = null;
        this.font = null;
    }

    ButtonFace(String filename, String label, Bundle bundle, Font font) {
        assert filename == null || bundle != null;
        assert label != null && label.length() == 0 || font != null;
        this.filename = filename;
        this.label = label == null ? "" : label;
        this.bundle = bundle;
        this.font = font;
    }

    public String getFilename() {
        return filename;
    }

    public String getLabel() {
        return label;
    }

    public BufferedImage getBufferedImage() {
        if (Environment.CURRENT_ENVIRONMENT != Environment.DEVELOPMENT) {
            throw new IllegalStateException("Buffered images are not permitted outside of development");
        }
        if (bufferedImage == null) {
            this.bufferedImage = bufferImageImpl();
        }
        return bufferedImage;
    }

    public byte[][] getButtonUpdateCommands() {
        if (buttonFaceData == null) {
            byte[] buf = new byte[IMAGE_COMMAND_BYTES];
            int i = 0;
            buf[i++] = (byte)0x2C; // RAMWR
            for (int argb : bufferImageImpl().getRGB(0, 0, 160, 128, null, 0, 160)) {
                buf[i++] = (byte)((argb >> 16) & 0xFF);
                buf[i++] = (byte)((argb >> 8) & 0xFF);
                buf[i++] = (byte)(argb & 0xFF);
            }
            buttonFaceData = new byte[][]{ SET_COLUMN_0_COMMAND, SET_ROW_0_COMMAND, buf };
        }
        return buttonFaceData;
    }

    public InputStream readOriginalFile() throws IOException {
        return filename == null ? null : bundle.get(filename).orElse(null);
    }

    protected BufferedImage bufferImageImpl() {
        try {
            InputStream stream = readOriginalFile();
            BufferedImage bufferedImage;
            if (stream == null) {
                bufferedImage = new BufferedImage(160, 128, BufferedImage.TYPE_INT_ARGB);
            } else {
                bufferedImage = ImageIO.read(stream);
            }
            Graphics2D g = (Graphics2D)bufferedImage.getGraphics();
            if (label.length() > 0) {
                g.setFont(font);
                g.setColor(Color.WHITE);
                Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(label, g);
                g.drawString(label, 80.0f - (float)stringBounds.getCenterX(), 64.0f - (float)stringBounds.getHeight());
            }
            g.setComposite(AlphaComposite.DstOver);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, 160, 128);
            return bufferedImage;
        } catch (IOException e) {
            return null;
        }
    }

    public static byte[][] getInitCommands() {
        return new byte[][]{
                SET_DISPLAY_OFF,
                SET_DISPLAY_ROTATION,
                SET_COLUMN_0_COMMAND,
                SET_ROW_0_COMMAND,
                SET_DISPLAY_CLEAR,
                SET_DISPLAY_ON
        };
    }
}

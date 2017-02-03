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

package com.venaglia.roger.ui.impl;

import com.google.inject.Singleton;
import com.venaglia.roger.ui.ImageSerializer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/**
 * Created by ed on 1/26/17.
 */
@Singleton
public class ImageSerializer565 implements ImageSerializer {

    private final int[] lutR;
    private final int[] lutG;
    private final int[] lutB;

    public ImageSerializer565() {
        int[] lut85 = LUTils.genLUT(8, 5);
        int[] lut86 = LUTils.genLUT(8, 6);
        lutR = LUTils.shiftLUT(11, lut85);
        lutG = LUTils.shiftLUT(5, lut86);
        lutB = lut85;
        assert LUTils.orLUT(lutR) == 0xF800;
        assert LUTils.orLUT(lutG) == 0x07E0;
        assert LUTils.orLUT(lutB) == 0x001F;
    }

    @Override
    public byte[] serialize(BufferedImage bufferedImage) {
        byte[] buf = new byte[160 * 128 * 2]; // 40960 bytes ~= 41ms @ 8Mbps [21 blocks]
        int i = 0;
        int[] data = bufferedImage.getRGB(0, 0, 160, 128, null, 0, 160);
        for (int i1 = 0; i1 < data.length; i1++) {
            int argb = data[i1];
            int rgb = lutR[argb >> 16 & 0xFF] | lutG[argb >> 8 & 0xFF] | lutB[argb & 0xFF];
            buf[i++] = (byte)(rgb >> 8 & 0xFF);
            buf[i++] = (byte)(rgb & 0xFF);
        }
        return buf;
    }

    public static void main(String[] args) {
        ImageSerializer565 imageSerializer565 = new ImageSerializer565();
        int r0 = 0x33;
        int g0 = 0x99;
        int b0 = 0xFF;
        int r1 = 0x06;
        int g1 = 0x26;
        int b1 = 0x1F;
        int rgb0 = r0 << 16 | g0 << 8 | b0;
        int rgb1 = r1 << 11 | g1 << 5 | b1;
        BufferedImage image = new BufferedImage(160, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();
        graphics.setColor(new Color(r0, g0, b0));
        graphics.fillRect(0, 0, 160, 128);
        graphics.dispose();
        byte[] out = imageSerializer565.serialize(image);
        ByteBuffer buf = ByteBuffer.wrap(out);
        assert buf.limit() == 160 * 128 * 2;
        while (buf.hasRemaining()) {
            int value = (buf.get() & 0xFF) << 8 | buf.get() & 0xFF;
            assert value == rgb1;
        }
    }
}

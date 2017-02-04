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

import java.awt.image.BufferedImage;

/**
 * Created by ed on 1/26/17.
 */
@Singleton
public class ImageSerializer444 implements ImageSerializer {

    private final int[] lutR;
    private final int[] lutG;
    private final int[] lutB;

    public ImageSerializer444() {
        int[] lut84 = LUTils.genLUT(8, 4);
        lutR = lut84;
        lutG = LUTils.shiftLUT(4, lut84);
        lutB = LUTils.shiftLUT(8, lut84);
        assert LUTils.orLUT(lutR) == 0x00F;
        assert LUTils.orLUT(lutG) == 0x0F0;
        assert LUTils.orLUT(lutB) == 0xF00;
    }

    @Override
    public byte[] serialize(BufferedImage bufferedImage) {
        byte[] buf = new byte[160 * 128 * 3 / 2]; // 30720 bytes ~= 31ms @ 8Mbps [16 blocks]
        int i = 0;
        int rgb = 0;
        boolean even = true;
        for (int argb : bufferedImage.getRGB(0, 0, 160, 128, null, 0, 160)) {
            if (even) {
                rgb = (lutR[argb >> 16 & 0xFF] | lutG[argb >> 8 & 0xFF] | lutB[argb & 0xFF]) << 12;
                buf[i++] = (byte)(rgb >> 16 & 0xFF);
            } else {
                rgb |= lutR[argb >> 16 & 0xFF] | lutG[argb >> 8 & 0xFF] | lutB[argb & 0xFF];
                buf[i++] = (byte)(rgb >> 8 & 0xFF);
                buf[i++] = (byte)(rgb & 0xFF);
            }
            even = !even;
        }
        return buf;
    }

}

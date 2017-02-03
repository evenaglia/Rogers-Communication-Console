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

import com.venaglia.roger.ui.ImageSerializer;

import java.awt.image.BufferedImage;

/**
 * Created by ed on 1/26/17.
 */
public class ImageSerializer888 implements ImageSerializer {

    @Override
    public byte[] serialize(BufferedImage bufferedImage) {
        byte[] buf = new byte[128 * 160 * 3]; // 61440 ~= 62ms @ 8Mbps [31 blocks]
        int i = 0;
        for (int argb : bufferedImage.getRGB(0, 0, 160, 128, null, 0, 160)) {
            buf[i++] = (byte)((argb >> 16) & 0xFF);
            buf[i++] = (byte)((argb >> 8) & 0xFF);
            buf[i++] = (byte)(argb & 0xFF);
        }
        return buf;
    }
}

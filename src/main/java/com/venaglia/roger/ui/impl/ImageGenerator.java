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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.venaglia.roger.ui.FontLoader;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Created by ed on 2/2/17.
 */
@Singleton
public class ImageGenerator {

    @Inject
    private FontLoader fontLoader;

    public enum TextSize {
        SMALL(18),
        MEDIUM(27),
        LARGE(36);

        final float size;

        TextSize(float size) {
            this.size = size;
        }
    }

    private static final int[] MASK_GRAY = {0xFFFFFF,0xFFFFFF,0xFFFFFF,0xFFFFFF,0xFFFFFF,0xFFFFFF,0xFFFFFF,0xFFFFFF,0xFFFFFF,9};
    private static final int[] MASK_HRGB = {0xFF0000,0x00FF00,0x0000FF,0xFF0000,0x00FF00,0x0000FF,0xFF0000,0x00FF00,0x0000FF,3};
    private static final int[] MASK_HBGR = {0x0000FF,0x00FF00,0xFF0000,0x0000FF,0x00FF00,0xFF0000,0x0000FF,0x00FF00,0xFF0000,3};
    private static final int[] MASK_VRGB = {0xFF0000,0xFF0000,0xFF0000,0x00FF00,0x00FF00,0x00FF00,0x0000FF,0x0000FF,0x0000FF,3};
    private static final int[] MASK_VBGR = {0x0000FF,0x0000FF,0x0000FF,0x00FF00,0x00FF00,0x00FF00,0xFF0000,0xFF0000,0xFF0000,3};

    public BufferedImage renderString(String text, Color color, TextSize textSize) {
        BufferedImage image = new BufferedImage(160 * 3, 16 * 3, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D)image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setFont(fontLoader.get().deriveFont(textSize.size * 3.0f));


        // todo: handle text wrapping
        Rectangle2D bounds = g.getFontMetrics().getStringBounds(text, g);
        float w = (float)bounds.getWidth();
        g.setColor(color);
        g.drawString(text, (160*3-w)/2.0f, 15 * 3);
        g.dispose();
        BufferedImage result = new BufferedImage(160, 16, BufferedImage.TYPE_4BYTE_ABGR);
        int[] buffer = new int[9];
        int[] mask = MASK_VBGR;
        float pels = mask[9];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 160; x++) {
                image.getRGB(x * 3, y * 3, 3, 3, buffer, 0, 3);
                int a___ = 0, _r__ = 0, __g_ = 0, ___b = 0;
                for (int i = 0; i < 9; i++) {
                    int argb = buffer[i] & mask[i];
                    a___ += argb >> 24 & 0xFF;
                    _r__ += argb >> 16 & 0xFF;
                    __g_ += argb >>  8 & 0xFF;
                    ___b += argb & 0xFF;
                }
                a___ = Math.round(a___ / 9.0f) & 0xFF;
                _r__ = Math.round(_r__ / pels) & 0xFF;
                __g_ = Math.round(__g_ / pels) & 0xFF;
                ___b = Math.round(___b / pels) & 0xFF;
                result.setRGB(x, y, a___ << 24 | _r__ << 16 | __g_ << 8 | ___b);
            }
        }
        return result;

    }
}

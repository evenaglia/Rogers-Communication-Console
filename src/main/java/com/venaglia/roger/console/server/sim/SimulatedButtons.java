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

import javax.swing.JComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by ed on 8/27/16.
 */
public class SimulatedButtons extends JComponent {

    private final VirtualButton[] buttons;
    private final List<Supplier<Boolean>> buttonStateSuppliers = new ArrayList<>(12);
    private final Iterable<Boolean> buttonStateIterator;

    public SimulatedButtons(float scale) {
        VirtualButton vb1 = new VirtualButton(scale);
        VirtualButton vb2 = new VirtualButton(scale);
        VirtualButton vb3 = new VirtualButton(scale);
        VirtualButton vb4 = new VirtualButton(scale);
        VirtualButton vb5 = new VirtualButton(scale);
        VirtualButton vb6 = new VirtualButton(scale);
        VirtualButton vb7 = new VirtualButton(scale);
        VirtualButton hb1 = new VirtualHardButton.Left(scale);
        VirtualButton hb2 = new VirtualHardButton.Right(scale);
        VirtualButton hb3 = new VirtualHardButton.Top(scale);
        VirtualButton hb4 = new VirtualHardButton.Top(scale);
        VirtualButton hb5 = new VirtualHardButton.Top(scale);
        this.buttons = new VirtualButton[]{ vb1, vb2, vb3, vb4, vb5, vb6, vb7 };
        setBackground(Color.BLACK);
        float topSpacing = 72.0f * scale;
        float hSpacing = 240.0f * scale;
        float vSpacing = 160.0f * scale;
        float hbOffset = 36.0f * scale;
        setSize(Math.round(hSpacing * 4.0f), Math.round(vSpacing * 2.15f + topSpacing));
        Object[] buf = new Object[12];
        buf[ 1] = addVirtualButton(vb1, hSpacing * -2.0f, topSpacing);
        buf[ 5] = addVirtualButton(vb2, hSpacing * -1.0f, topSpacing);
        buf[ 6] = addVirtualButton(vb3, hSpacing * 0.0f, topSpacing);
        buf[ 9] = addVirtualButton(vb4, hSpacing * 1.0f, topSpacing);
        buf[ 3] = addVirtualButton(vb5, hSpacing * -1.5f, vSpacing + topSpacing);
        buf[ 7] = addVirtualButton(vb6, hSpacing * -0.5f, vSpacing + topSpacing);
        buf[11] = addVirtualButton(vb7, hSpacing * 0.5f, vSpacing + topSpacing);
        buf[ 2] = addVirtualButton(hb1, hSpacing * -2.0f, vSpacing + topSpacing);
        buf[10] = addVirtualButton(hb2, hSpacing * 2.0f - hbOffset - hbOffset, vSpacing + topSpacing);
        buf[ 0] = addVirtualButton(hb3, hSpacing * -2.0f, 0.0f);
        buf[ 4] = addVirtualButton(hb4, hSpacing * -0.6666667f, 0.0f);
        buf[ 8] = addVirtualButton(hb5, hSpacing * 0.6666667f, 0.0f);
        for (Object o : buf) {
            //noinspection unchecked
            buttonStateSuppliers.add((Supplier<Boolean>)o);
        }
        buttonStateIterator = () -> {

            final Iterator<Supplier<Boolean>> i = buttonStateSuppliers.iterator();

            return new Iterator<Boolean>() {
                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public Boolean next() {
                    return i.next().get();
                }
            };
        };
        setImageBytesRgb((byte)0xFF, new byte[160 * 128 * 3]);
    }

    private Supplier<Boolean> addVirtualButton(VirtualButton virtualButton, float x, float y) {
        virtualButton.setLocation(Math.round(x + getWidth() * 0.5f), Math.round(y));
        add(virtualButton);
        return virtualButton.getButtonStateSupplier();
    }

    public Iterable<Boolean> getButtonStates() {
        return buttonStateIterator;
    }

    public void setImageBytesRgb(byte selectorByte, byte[] imageDateRgb) {
        for (int i = 0, m = 1; i < buttons.length; i++, m <<= 1) {
            if ((selectorByte & m) != 0) {
                buttons[i].setImageBytesRgb(imageDateRgb);
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paint(g);
    }
}

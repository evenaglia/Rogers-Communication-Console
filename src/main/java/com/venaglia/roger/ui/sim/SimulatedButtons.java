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

package com.venaglia.roger.ui.sim;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.venaglia.roger.ui.ButtonListener;
import com.venaglia.roger.ui.ButtonProcessor;

import javax.swing.*;
import java.awt.*;

/**
 * Created by ed on 8/27/16.
 */
@Singleton
public class SimulatedButtons extends JComponent {

    public static final int LEFT_HARD_BUTTON = 64;
    public static final int RIGHT_HARD_BUTTON = 65;
    public static final int TOP_LEFT_HARD_BUTTON = 66;
    public static final int TOP_CENTER_HARD_BUTTON = 67;
    public static final int TOP_RIGHT_HARD_BUTTON = 68;

    private final VirtualButton[] topRow;
    private final VirtualButton[] bottomRow;
    private final VirtualHardButton[] hardButtons;
    private final ButtonProcessor buttonProcessor = new ButtonProcessor();

    @Inject
    public SimulatedButtons(@Named("UIScale") float scale,
                            VirtualButton vb1,
                            VirtualButton vb2,
                            VirtualButton vb3,
                            VirtualButton vb4,
                            VirtualButton vb5,
                            VirtualButton vb6,
                            VirtualButton vb7,
                            VirtualHardButton.Left  hb1,
                            VirtualHardButton.Right hb2,
                            VirtualHardButton.Top   hb3,
                            VirtualHardButton.Top   hb4,
                            VirtualHardButton.Top   hb5) {
        this.topRow = new VirtualButton[]{ vb1, vb2, vb3, vb4 };
        this.bottomRow = new VirtualButton[]{ vb5, vb6, vb7 };
        this.hardButtons = new VirtualHardButton[]{ hb1, hb2, hb3, hb4, hb5 };
        setBackground(Color.BLACK);
        float topSpacing = 72.0f * scale;
        float hSpacing = 240.0f * scale;
        float vSpacing = 160.0f * scale;
        float hbOffset = 36.0f * scale;
        setSize(Math.round(hSpacing * 4.0f), Math.round(vSpacing * 2.2f + topSpacing));
        addVirtualButton(vb1, hSpacing * -2.0f, topSpacing);
        addVirtualButton(vb2, hSpacing * -1.0f, topSpacing);
        addVirtualButton(vb3, hSpacing * 0.0f, topSpacing);
        addVirtualButton(vb4, hSpacing * 1.0f, topSpacing);
        addVirtualButton(vb5, hSpacing * -1.5f, vSpacing + topSpacing);
        addVirtualButton(vb6, hSpacing * -0.5f, vSpacing + topSpacing);
        addVirtualButton(vb7, hSpacing * 0.5f, vSpacing + topSpacing);
        addVirtualButton(hb1, hSpacing * -2.0f, vSpacing + topSpacing);
        addVirtualButton(hb2, hSpacing * 2.0f - hbOffset - hbOffset, vSpacing + topSpacing);
        addVirtualButton(hb3, hSpacing * -2.0f, 0.0f);
        addVirtualButton(hb4, hSpacing * -0.6666667f, 0.0f);
        addVirtualButton(hb5, hSpacing * 0.6666667f, 0.0f);
    }

    private void addVirtualButton(VirtualButton virtualButton, float x, float y) {
        virtualButton.setLocation(Math.round(x + getWidth() * 0.5f), Math.round(y));
        virtualButton.setButtonListener(buttonProcessor);
        add(virtualButton);
    }

    public void setButton(int position, com.venaglia.roger.buttons.Button btn) {
        switch (position) {
            case 1:
                topRow[0].setButton(btn);
                break;
            case 2:
                topRow[1].setButton(btn);
                break;
            case 3:
                topRow[2].setButton(btn);
                break;
            case 4:
                topRow[3].setButton(btn);
                break;
            case 5:
                bottomRow[0].setButton(btn);
                break;
            case 6:
                bottomRow[1].setButton(btn);
                break;
            case 7:
                bottomRow[2].setButton(btn);
                break;
            case LEFT_HARD_BUTTON:
                hardButtons[0].setButton(btn);
                break;
            case RIGHT_HARD_BUTTON:
                hardButtons[1].setButton(btn);
                break;
            case TOP_LEFT_HARD_BUTTON:
                hardButtons[2].setButton(btn);
                break;
            case TOP_CENTER_HARD_BUTTON:
                hardButtons[3].setButton(btn);
                break;
            case TOP_RIGHT_HARD_BUTTON:
                hardButtons[4].setButton(btn);
                break;
            default:
                throw new IllegalArgumentException("Bad position: " + position);
        }
    }

    public void addButtonListener(ButtonListener buttonListener) {
        buttonProcessor.addButtonListener(buttonListener);
    }

    public void removeButtonListener(ButtonListener buttonListener) {
        buttonProcessor.removeButtonListener(buttonListener);
    }

}

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
import com.venaglia.roger.buttons.Button;
import com.venaglia.roger.buttons.SimpleButtonListener;
import com.venaglia.roger.output.OutputWindow;
import com.venaglia.roger.ui.ButtonListener;
import com.venaglia.roger.ui.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by ed on 8/28/16.
 */
@Singleton
public class SimUI implements UI {

    private final SimulatedButtons simulatedButtons;
    private final OutputWindow outputWindow;

    @Inject
    public SimUI(SimulatedButtons simulatedButtons, OutputWindow outputWindow) {
        this.simulatedButtons = simulatedButtons;
        this.outputWindow = outputWindow;
        createButtonFrame(simulatedButtons);
        createOutputFrame((JPanel)outputWindow);
    }

    private void createOutputFrame(JPanel outputWindow) {
        JFrame frame = new JFrame();
        frame.setTitle("Output");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setBackground(Color.BLACK);
        frame.add(outputWindow);
        frame.setSize(outputWindow.getSize());
        frame.getContentPane().setCursor(Cursor.getDefaultCursor());
        frame.setVisible(true);
    }

    private void createButtonFrame(SimulatedButtons simulatedButtons) {
        JFrame frame = new JFrame();
        frame.setTitle("Keyboard");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setBackground(Color.BLACK);
        frame.add(simulatedButtons);
        frame.setSize(simulatedButtons.getSize());
        frame.getContentPane().setCursor(Cursor.getDefaultCursor());
        frame.setVisible(true);
        simulatedButtons.addButtonListener(new ButtonListener() {

            @Override
            public void handleButtonDown(Button button) {
                System.out.println("Down " + button);
            }

            @Override
            public void handleButtonUp(Button button) {
                System.out.println("Up " + button);
            }

            @Override
            public void handleClick(Button button) {
                System.out.println("Clicked " + button);
            }

            @Override
            public void handleLongPress(Button button) {
                System.out.println("Long press " + button);
            }

            @Override
            public void handleContinuedLongPress(Button button, long ms, int count) {
                System.out.println("Continued long press " + button + " (n=" + count + ")");
            }
        });
    }

    @Override
    public void showTheseButtons(Button t1, Button t2, Button t3, Button t4, Button b1, Button b2, Button b3) {
        simulatedButtons.setButton(1, t1);
        simulatedButtons.setButton(2, t2);
        simulatedButtons.setButton(3, t3);
        simulatedButtons.setButton(4, t4);
        simulatedButtons.setButton(5, b1);
        simulatedButtons.setButton(6, b2);
        simulatedButtons.setButton(7, b3);
        simulatedButtons.repaint();
    }

    @Override
    public void setHardButtons(Button left, Button right, Button topLeft, Button topCenter, Button topRight) {
        simulatedButtons.setButton(SimulatedButtons.LEFT_HARD_BUTTON, left);
        simulatedButtons.setButton(SimulatedButtons.RIGHT_HARD_BUTTON, right);
        simulatedButtons.setButton(SimulatedButtons.TOP_LEFT_HARD_BUTTON, topLeft);
        simulatedButtons.setButton(SimulatedButtons.TOP_CENTER_HARD_BUTTON, topCenter);
        simulatedButtons.setButton(SimulatedButtons.TOP_RIGHT_HARD_BUTTON, topRight);
    }

    @Override
    public void addListener(SimpleButtonListener listener) {
        addListener(new ButtonListener() {
            @Override
            public void handleButtonDown(Button button) {
                listener.handleButtonDown(button);
            }

            @Override
            public void handleButtonUp(Button button) {
                listener.handleButtonUp(button);
            }
        });
    }

    @Override
    public void addListener(ButtonListener listener) {
        simulatedButtons.addButtonListener(listener);
    }

    @Override
    public void removeListener(ButtonListener listener) {
        simulatedButtons.removeButtonListener(listener);
    }

    @Override
    public OutputWindow getOutputWindow() {
        return outputWindow;
    }
}

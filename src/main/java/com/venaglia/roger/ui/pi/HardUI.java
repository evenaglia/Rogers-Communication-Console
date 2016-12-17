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

package com.venaglia.roger.ui.pi;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.venaglia.roger.buttons.Button;
import com.venaglia.roger.buttons.SimpleButtonListener;
import com.venaglia.roger.output.OutputWindow;
import com.venaglia.roger.ui.ButtonListener;
import com.venaglia.roger.ui.ButtonProcessor;
import com.venaglia.roger.ui.UI;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Cursor;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by ed on 10/20/16.
 */
@Singleton
public class HardUI implements UI {

    private final Button[] buttons = new Button[7];
    private final Button[] hardButtons = new Button[5];
    private final OutputWindow outputWindow;
    private final ButtonScanner buttonScanner;
    private final ButtonProcessor buttonProcessor;
    private final DisplayBus displayBus;

    private ButtonScanner.ScanCode scanCode = ButtonScanner.ScanCode.NO_BUTTONS_DOWN;
    private Button down = null;

    @Inject
    public HardUI(OutputWindow outputWindow,
                  ButtonScanner buttonScanner,
                  ScheduledExecutorService executor,
                  ButtonProcessor buttonProcessor,
                  DisplayBus displayBus) {
        this.outputWindow = outputWindow;
        this.buttonScanner = buttonScanner;
        this.buttonProcessor = buttonProcessor;
        this.displayBus = displayBus;
        executor.scheduleAtFixedRate(this::pollButtons, 250, 50, TimeUnit.MILLISECONDS);
        Arrays.fill(buttons, Button.NIL);
        Arrays.fill(hardButtons, Button.NIL);
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

    @Override
    public void showTheseButtons(Button t1, Button t2, Button t3, Button t4, Button b1, Button b2, Button b3) {
        setButtonImpl(t1, buttons, 0);
        setButtonImpl(t2, buttons, 1);
        setButtonImpl(t3, buttons, 2);
        setButtonImpl(t4, buttons, 3);
        setButtonImpl(b1, buttons, 4);
        setButtonImpl(b2, buttons, 5);
        setButtonImpl(b3, buttons, 6);
    }

    private void setButtonImpl(Button b, Button[] dest, int idx) {
        assert b != null;
        if (!dest[idx].getId().equals(b.getId())) {
            dest[idx] = b;
            displayBus.sendCommand(DisplayBus.DisplayNumber.values()[idx], b.getButtonFace().getButtonUpdateCommands());
        }
    }

    @Override
    public void setHardButtons(Button left, Button right, Button topLeft, Button topCenter, Button topRight) {
        hardButtons[0] = left;
        hardButtons[1] = right;
        hardButtons[2] = topLeft;
        hardButtons[3] = topCenter;
        hardButtons[4] = topRight;
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
        buttonProcessor.addButtonListener(listener);
    }

    @Override
    public void removeListener(ButtonListener listener) {
        buttonProcessor.removeButtonListener(listener);
    }

    @Override
    public OutputWindow getOutputWindow() {
        return outputWindow;
    }

    private void pollButtons() {
        ButtonScanner.ScanCode scanCode = buttonScanner.scan();
        if (this.scanCode == scanCode) return;
        if (down != null) {
            buttonProcessor.handleButtonUp(down);
            down = null;
        }
        down = forScanCode(scanCode);
        if (down != null) {
            buttonProcessor.handleButtonDown(down);
            down = null;
        }
    }

    private Button forScanCode(ButtonScanner.ScanCode scanCode) {
        switch (scanCode) {
            case BUTTON_00_DOWN: return null; // todo
            case BUTTON_01_DOWN: return null; // todo
            case BUTTON_02_DOWN: return null; // todo
            case BUTTON_03_DOWN: return null; // todo
            case BUTTON_10_DOWN: return null; // todo
            case BUTTON_11_DOWN: return null; // todo
            case BUTTON_12_DOWN: return null; // todo
            case BUTTON_13_DOWN: return null; // todo
            case BUTTON_20_DOWN: return null; // todo
            case BUTTON_21_DOWN: return null; // todo
            case BUTTON_22_DOWN: return null; // todo
            case BUTTON_23_DOWN: return null; // todo
            default: return null;
        }
    }
}

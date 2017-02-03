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

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.common.cache.CacheBuilder;
import com.venaglia.roger.buttons.Button;
import com.venaglia.roger.buttons.SimpleButtonListener;
import com.venaglia.roger.output.OutputWindow;
import com.venaglia.roger.ui.ButtonListener;
import com.venaglia.roger.ui.ButtonProcessor;
import com.venaglia.roger.ui.ImageSerializer;
import com.venaglia.roger.ui.UI;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Cursor;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by ed on 10/20/16.
 */
@Singleton
public class UIImpl implements UI {

    private final Button[] buttons = new Button[7];
    private final Button[] hardButtons = new Button[5];
    private final OutputWindow outputWindow;
    private final ButtonProcessor buttonProcessor;
    private final ConClient conClient;
    private final LoadingCache<Button,DisplayUpdateCommand> commandCache;
    private final ScanButtonsCommand readButtons;

    private ScanCode scanCode = ScanCode.NO_BUTTONS_DOWN;
    private Button down = null;

    @Inject
    public UIImpl(OutputWindow outputWindow,
                  ScheduledExecutorService executor,
                  ButtonProcessor buttonProcessor,
                  ConClient conClient,
                  ImageSerializer imageSerializer) {
        this.outputWindow = outputWindow;
        this.buttonProcessor = buttonProcessor;
        this.conClient = conClient;
        this.commandCache = CacheBuilder.newBuilder().initialCapacity(128).build(new CacheLoader<Button, DisplayUpdateCommand>() {
            @Override
            public DisplayUpdateCommand load(Button button) throws Exception {
                return new DisplayUpdateCommand(button.getButtonFace().getImageDataRGB(imageSerializer));
            }
        });
        this.readButtons = new ScanButtonsCommand(this::processScanCode);
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
            DisplayUpdateCommand command = null;
            try {
                command = commandCache.get(b);
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            command.setDisplayNumber(DisplayNumber.values()[idx]);
            conClient.sendCommand(command);
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
        conClient.sendCommand(readButtons);
    }

    private void processScanCode(ScanCode scanCode) {
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

    private Button forScanCode(ScanCode scanCode) {
        switch (scanCode) {
            case BUTTON_A1_DOWN: return hardButtons[1];
            case BUTTON_A2_DOWN: return buttons[0];
            case BUTTON_A3_DOWN: return hardButtons[0];
            case BUTTON_A4_DOWN: return buttons[4];
            case BUTTON_B1_DOWN: return hardButtons[2];
            case BUTTON_B2_DOWN: return buttons[1];
            case BUTTON_B3_DOWN: return buttons[2];
            case BUTTON_B4_DOWN: return buttons[5];
            case BUTTON_C1_DOWN: return hardButtons[3];
            case BUTTON_C2_DOWN: return buttons[3];
            case BUTTON_C3_DOWN: return hardButtons[4];
            case BUTTON_C4_DOWN: return buttons[6];
            default: return null;
        }
    }
}

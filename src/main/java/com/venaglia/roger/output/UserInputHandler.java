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

package com.venaglia.roger.output;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.venaglia.roger.buttons.Button;
import com.venaglia.roger.buttons.ButtonAction;
import com.venaglia.roger.menu.NavigationHandler;
import com.venaglia.roger.ui.ButtonListener;

import java.awt.image.BufferedImage;

/**
 * Created by ed on 10/4/16.
 */
@Singleton
public class UserInputHandler implements ButtonListener {

    public static final String CURSOR_PREFIX = "cursor:";

    @Override
    public void handleClick(Button button) {
        if (outputWindow == null) {
            return;
        }
        ButtonAction action = button.getAction();
        if (action != null) {
            if (handleDo(action.getDo())) {
                return; // "do" supersedes any textual input, even if we don't process it
            }
            BufferedImage emoji = action.getEmoji();
            String text = action.getText();
            char ch = action.getChar();
            if (emoji != null) {
                append(OutputElement.build(emoji, text));
            } else if (text != null && text.length() > 0) {
                append(OutputElement.build(text));
            } else if (ch != '\0') {
                append(OutputElement.build(ch));
            }
        }
    }

    @Override
    public void handleLongPress(Button button) {
        if (outputWindow == null) {
            return;
        }
        ButtonAction action = button.getLongPressAction();
        if (action != null) {
            handleDo(action.getDo());
        }
    }

    private boolean handleDo(Iterable<String> doIterable) {
        boolean done = false;
        for (String _do : doIterable) {
            done = true;
            if (_do.startsWith(CURSOR_PREFIX)) {
                switch (_do.substring(CURSOR_PREFIX.length())) {
                    case "@eol":
                        append(Message.EOL);
                        break;
                    case "@complete":
                        outputWindow.getActiveMessage().archive();
                        break;
                }
            }
        }
        return done;
    }

    private void append(OutputElement<?> element) {
        outputWindow.getActiveMessage().append(element);
    }

    @Inject
    private NavigationHandler navigationHandler;

    @Inject
    private OutputWindow outputWindow;
}

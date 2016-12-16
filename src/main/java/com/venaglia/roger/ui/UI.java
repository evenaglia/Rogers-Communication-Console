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

package com.venaglia.roger.ui;

import com.venaglia.roger.buttons.Button;
import com.venaglia.roger.buttons.SimpleButtonListener;
import com.venaglia.roger.output.OutputWindow;

/**
 * Created by ed on 8/27/16.
 */
public interface UI {

    void showTheseButtons(Button t1, Button t2, Button t3, Button t4, Button b1, Button b2, Button b3);

    void setHardButtons(Button left, Button right, Button topLeft, Button topCenter, Button topRight);

    void addListener(SimpleButtonListener listener);

    void addListener(ButtonListener listener);

    void removeListener(ButtonListener listener);

    OutputWindow getOutputWindow();
}

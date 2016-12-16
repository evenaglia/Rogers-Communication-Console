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

package com.venaglia.roger.menu;

import com.venaglia.roger.buttons.Button;

import java.util.List;

/**
 * Created by ed on 8/27/16.
 */
public interface MenuTree {

    String PARENT = "@parent";
    String TOP = "@top";

    List<Button> getCurrentButtons();

    boolean canNavigateUp();

    boolean navigate(String toButtonId);

    void addChangeListener(MenuChangeListener listener);

    void removeChangeListener(MenuChangeListener listener);

    final class HARD_BUTTONS {
        private HARD_BUTTONS() {}
        public static final String LEFT       = "hard-button-left";
        public static final String RIGHT      = "hard-button-right";
        public static final String TOP_LEFT   = "hard-button-top-left";
        public static final String TOP_CENTER = "hard-button-top-center";
        public static final String TOP_RIGHT  = "hard-button-top-right";
    }
}

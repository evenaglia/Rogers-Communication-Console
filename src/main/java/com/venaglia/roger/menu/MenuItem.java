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
import com.venaglia.roger.buttons.ButtonSet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ed on 8/27/16.
 */
class MenuItem {

    private final MenuItem parent;
    private final String buttonId;
    private final MenuItem[] children;

    MenuItem(MenuItem parent, String buttonId, MenuItem[] children) {
        this.parent = parent;
        this.buttonId = buttonId;
        this.children = children;
    }

    public MenuItem getParent() {
        return parent;
    }

    public String getButtonId() {
        return buttonId;
    }

    public List<MenuItem> getChildren() {
        return children == null || children.length == 0 ? null : Arrays.asList(children);
    }

    public List<Button> getButtons(ButtonSet lookup) {
        return children == null ? null : Arrays.asList(children)
                                               .stream()
                                               .map(m -> {
                                                   Button button = lookup.get(m.buttonId);
                                                   if (button != null && m.getChildren() != null) {
                                                       lookup.overrideDo(m.getButtonId(), "nav:" + m.getButtonId());
                                                   }
                                                   return button;
                                               })
                                               .collect(Collectors.toList());
    }
}

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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.venaglia.roger.buttons.Button;
import com.venaglia.roger.buttons.ButtonSet;
import com.venaglia.roger.bundle.AbstractLoader;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ed on 8/27/16.
 */
@Singleton
public class MenuTreeLoader extends AbstractLoader<MenuTree> {

    private static final String source = "menu.txt";
    private static final Pattern MATCH_LINE = Pattern.compile("^(\\s*)(\"[^\"]*\"|'[^']*'|[\\w.-]+)");

    private final Provider<ButtonSet> buttonSet;

    @Inject
    public MenuTreeLoader(Provider<ButtonSet> buttonSet) {
        this.buttonSet = buttonSet;
    }

    protected MenuTree load() {
        String[] lines = readLines(new InputStreamReader(getStream(source), StandardCharsets.UTF_8));
        MenuItem top = parseMenuItem(MenuTree.TOP, lines, new AtomicInteger());
        return new MenuTree() {

            private final List<MenuChangeListener> listeners = new ArrayList<>(4);

            MenuItem current = top;

            @Override
            public List<Button> getCurrentButtons() {
                return current.getButtons(buttonSet.get());
            }

            @Override
            public boolean canNavigateUp() {
                return current != top;
            }

            @Override
            public boolean navigate(String toButtonId) {
                assert toButtonId != null;
                for (MenuItem menuItem : current.getChildren()) {
                    if (toButtonId.equals(menuItem.getButtonId())) {
                        if (menuItem.getChildren() != null) {
                            return go(menuItem);
                        }
                    }
                }
                switch (toButtonId) {
                    case TOP:
                        return go(top);
                    case PARENT:
                        return current.getParent() != null && go(current.getParent());
                }
                MenuItem found = find(top, toButtonId);
                return found != null && go(found);
            }

            @Override
            public void addChangeListener(MenuChangeListener listener) {
                assert listener != null;
                listeners.add(listener);
            }

            @Override
            public void removeChangeListener(MenuChangeListener listener) {
                assert listener != null;
                listeners.remove(listener);
            }

            private MenuItem find(MenuItem from, String path) {
                int lookAfter = path.startsWith("\"")
                                ? path.indexOf("\"", 1)
                                : path.startsWith("'")
                                  ? path.indexOf("'", 1)
                                  : 0;
                int dot = path.indexOf("->", lookAfter);
                if (dot < 0) {
                    for (MenuItem menuItem : from.getChildren()) {
                        if (parseId(path.trim()).equals(menuItem.getButtonId())) {
                            return menuItem;
                        }
                    }
                } else {
                    for (MenuItem menuItem : from.getChildren()) {
                        if (parseId(path.substring(0, dot).trim()).equals(menuItem.getButtonId())) {
                            return find(menuItem, path.substring(dot + 1));
                        }
                    }
                }
                return null;
            }

            private boolean go(MenuItem menuItem) {
                if (current != menuItem) {
                    String from = current.getButtonId();
                    current = menuItem;
                    for (MenuChangeListener listener : listeners) {
                        listener.menuChanged(from, current.getButtonId());
                    }
                    return true;
                }
                return false;
            }

            @Override
            public String toString() {
                return toStringImpl("", top, new StringBuilder(512)).toString();
            }

            private StringBuilder toStringImpl(String indent, MenuItem menuItem, StringBuilder buffer) {
                for (MenuItem item : menuItem.getChildren()) {
                    if (buffer.length() > 0) {
                        buffer.append('\n');
                    }
                    buffer.append(indent).append(escape(item.getButtonId()));
                    if (item.getChildren() != null) {
                        String childIndent = indent + "    ";
                        toStringImpl(childIndent, item, buffer);
                    }
                }
                return buffer;
            }

            private String escape(String buttonId) {
                if (buttonId.matches("[\\w.-]+")) {
                    return buttonId;
                } else if (!buttonId.contains("\"")) {
                    return "\"" + buttonId + "\"";
                } else if (!buttonId.contains("'")) {
                    return "'" + buttonId + "'";
                } else {
                    throw new IllegalArgumentException("Cannot escape the passed button id: " + buttonId);
                }
            }
        };
    }

    private MenuItem parseMenuItem(final String id, String[] lines, AtomicInteger nextLine) {
        String line;
        String indent = null;
        List<MenuItem> children = null;
        while (nextLine.get() < lines.length) {
            line = lines[nextLine.getAndIncrement()];
            if (line.startsWith("#") || line.length() == 0) {
                continue;
            }
            Matcher matcher = MATCH_LINE.matcher(line);
            if (matcher.find()) {
                if (indent == null) {
                    indent = matcher.group(1);
                    if (children == null) {
                        children = new ArrayList<>();
                    }
                    children.add(new MenuItem(null, parseId(matcher.group(2)), null));
                } else {
                    String newIndent = matcher.group(1);
                    if (newIndent.equals(indent)) {
                        children.add(new MenuItem(null, parseId(matcher.group(2)), null));
                    } else if (newIndent.startsWith(indent)) {
                        MenuItem lastChild = children.remove(children.size() - 1);
                        assert lastChild.getChildren() == null : "Indent on line " + nextLine.get() + " doesn't properly follow indent from the line above";
                        nextLine.decrementAndGet();
                        children.add(parseMenuItem(lastChild.getButtonId(), lines, nextLine));
                    } else {
                        nextLine.decrementAndGet();
                        break;
                    }
                }
            }
        }
        if (children == null) {
            return new MenuItem(null, id, null);
        } else {
            MenuItem[] myChildren = children.toArray(new MenuItem[children.size()]);
            MenuItem menuItem = new MenuItem(null, id, myChildren);
            for (int i = 0; i < myChildren.length; i++) {
                MenuItem myChild = myChildren[i];
                List<MenuItem> myGrandchildren = myChild.getChildren();
                if (myGrandchildren == null) {
                    myChildren[i] = new MenuItem(menuItem, myChild.getButtonId(), null);
                } else {
                    myChildren[i] = new MenuItem(menuItem,
                                                 myChild.getButtonId(),
                                                 myGrandchildren.toArray(new MenuItem[myGrandchildren.size()]));
                }
            }
            return menuItem;
        }
    }

    private String parseId(String group) {
        if (group.startsWith("\"") && group.endsWith("\"") ||
            group.startsWith("'") && group.endsWith("'")) {
            return group.substring(0, group.length() - 1);
        }
        return group;
    }
}

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

package com.venaglia.roger.buttons;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.venaglia.roger.bundle.AbstractLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ed on 7/6/16.
 */
@Singleton
public class ButtonSetLoader extends AbstractLoader<ButtonSet> {

    private static final String source = "buttons.txt";

    @Inject
    private Font font;

    protected ButtonSet load() {
        Map<String,Button> buttonMap = new HashMap<>();
        Reader reader = new InputStreamReader(getStream(source));
        String s = readString(reader);
        parseButtonFile(buttonMap, s);
        return new ButtonSet() {

            @Override
            public void overrideDo(String id, String overrideDo) {
                Button btn = buttonMap.get(id);
                if (btn == null) {
                    throw new NoSuchElementException("id");
                }
                ((ButtonImpl)btn).setOverrideDo(overrideDo);
            }

            @Override
            public Button get(String id) {
                return buttonMap.get(id);
            }

            @Override
            public Button[] getMany(String... ids) {
                Button[] buttons = new Button[ids.length];
                for (int i = 0; i < ids.length; i++) {
                    buttons[i] = buttonMap.get(ids[i]);
                }
                return buttons;
            }
        };
    }

    private void parseJSON(Map<String, Button> buttonMap, String s) {
        s = s.replaceAll("(?m)^\\s*//.*$", "");
        JSONObject obj = new JSONObject(s);
        JSONArray array = obj.getJSONArray("buttons");
        for (int i = 0, l = array.length(); i < l; i++) {
            Button button = parseButton(array.getJSONObject(i));
            buttonMap.put(button.getId(), button);
        }
    }

    private void parseButtonFile(Map<String,Button> buttonMap, String s) {
        Pattern matchNameValue = Pattern.compile("^\\s+([a-zA-Z0-9_-]+)\\s*:\\s*(.*)$");
        String[] lines = s.split("\n");
        JSONObject buffer = new JSONObject();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trim = line.trim();
            if (trim.length() == 0 || trim.charAt(0) == '#') continue;
            if (Character.isWhitespace(line.charAt(0))) {
                Matcher matcher = matchNameValue.matcher(line);
                if (matcher.find()) {
                    String key = matcher.group(1);
                    switch (key) {
                        case "do":
                        case "alt":
                        case "image":
                        case "label":
                        case "char":
                        case "text":
                            buffer.put(key, matcher.group(2));
                            break;
                        default:
                            System.err.printf("Unknown key in %s [line %d]: '%s'\n", source, i + 1, key);
                    }
                } else {
                    System.err.printf("Syntax error in %s [line %d]: '%s'\n", source, i + 1, line);
                }
            } else {
                if (buffer.length() > 0) {
                    Button button = parseButton(buffer);
                    buttonMap.put(button.getId(), button);
                }
                buffer = new JSONObject().put("id", trim);
            }
        }
    }

    private Button parseButton(JSONObject json) {
        String chString = json.optString("char");
        Character ch = chString != null && chString.length() > 0 ? chString.charAt(0) : null;
        String id = json.getString("id");
        String filename = json.optString("image", null);
        String label = json.optString("label");
        String text = json.optString("text");
        BufferedImage emoji = bufferImage(json.optString("emoji", null));
        String _do = json.optString("do", null);
        String alt = json.optString("alt", null);
        return createButton(id, filename, label, text, ch, emoji, _do, alt);
    }

    void setFont(Font font) {
        this.font = font;
    }

    // separate method to expose constructor for testing
    Button createButton(String id,
                        String filename,
                        String label,
                        String text,
                        Character ch,
                        BufferedImage emoji,
                        String _do,
                        String alt) {
        ButtonFace face = new ButtonFace(filename, label, bundle, font);
        return new ButtonImpl(id, face, text, ch == null ? '\0' : ch, emoji, _do, alt);
    }
}

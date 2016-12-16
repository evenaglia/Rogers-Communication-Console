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
import com.venaglia.roger.bundle.Bundle;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Created by ed on 7/6/16.
 */
class ButtonImpl implements Button {

    private final String id;
    private final ButtonFace face;
    private final ButtonAction action;
    private final ButtonAction longPressAction;

    private String overrideDo;
    private String overrideLongPressDo;

    @Inject
    private Bundle bundle;

    public ButtonImpl(String id,
                      ButtonFace face,
                      final String text,
                      final char ch,
                      final BufferedImage emoji,
                      final String _do,
                      final String longPressDo) {
        this.id = id;
        this.face = face;
        this.action = new ButtonActionImpl(text, ch, emoji, () -> overrideDo != null ? overrideDo : _do);
        this.longPressAction = new ButtonActionImpl(text, ch, emoji, () -> overrideLongPressDo != null ? overrideLongPressDo : longPressDo);
    }

    @Override
    public String getId() {
        return id;
    }

    public ButtonFace getButtonFace() {
        return face;
    }

    @Override
    public ButtonAction getAction() {
        return action;
    }

    @Override
    public ButtonAction getLongPressAction() {
        return longPressAction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ButtonImpl button = (ButtonImpl)o;

        if (!id.equals(button.id)) return false;
        if (face != null ? !face.equals(button.face) : button.face != null) return false;
        return action.equals(button.action);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (face != null ? face.hashCode() : 0);
        result = 31 * result + action.hashCode();
        return result;
    }

    public String toString() {
        return id;
    }

    @Override
    public String getPartIdentity() {
        return "#" + id;
    }

    void setOverrideDo(String overrideDo) {
        this.overrideDo = overrideDo;
    }

    public void setOverrideLongPressDo(String overrideLongPressDo) {
        this.overrideLongPressDo = overrideLongPressDo;
    }

    private class ButtonActionImpl implements ButtonAction {

        private final String text;
        private final char ch;
        private final BufferedImage emoji;
        private final Supplier<String> doSupplier;

        private String cachedDo;
        private Iterable<String> cachedDoIterable = Collections.emptyList();

        public ButtonActionImpl(String text, char ch, BufferedImage emoji, Supplier<String> doSupplier) {
            this.text = text;
            this.ch = ch;
            this.emoji = emoji;
            this.doSupplier = doSupplier;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public char getChar() {
            return ch;
        }

        @Override
        public BufferedImage getEmoji() {
            return emoji;
        }

        @Override
        public Iterable<String> getDo() {
            return parseDoImpl(doSupplier.get());
        }

        private Iterable<String> parseDoImpl(String doString) {
            if (doString == null ? cachedDo == null : doString.equals(cachedDo)) {
                return cachedDoIterable;
            }
            cachedDoIterable = doString == null || doString.length() == 0 ? Collections.emptyList()
                                                                          : doString.indexOf(',') > 0 ? Arrays.asList(doString.split(","))
                                                                                                      : Collections.singleton(doString);
            cachedDo = doString;
            return cachedDoIterable;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ButtonAction action = (ButtonAction)o;

            if (text != null ? !text.equals(action.getText()) : action.getText() != null) return false;
            if (ch != action.getChar()) return false;
            if (!Objects.equals(getDo(), action.getDo())) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = text == null ? 0 : text.hashCode();
            result = result * 31 + (int)ch;
            result = result * 31 + getDo().hashCode();
            return result;
        }
    }
}

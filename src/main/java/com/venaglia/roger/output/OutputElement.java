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

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by ed on 9/23/16.
 */
public class OutputElement<T> {

    private final ElementType<T> type;
    private final T value;
    private final String textForm;

    private Rectangle rectangle;

    private OutputElement(ElementType<T> type, T value, String textForm) {
        this.type = type;
        this.value = value;
        this.textForm = textForm;
    }

    public ElementType<T> getElementType() {
        return type;
    }

    public T getValue() {
        return value;
    }

    public String asString() {
        return textForm;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public void setRectangle(Rectangle rectangle) {
        this.rectangle = rectangle;
    }

    public static OutputElement<String> build(String text) {
        return new OutputElement<>(ElementType.TEXT, text, text);
    }

    public static OutputElement<Character> build(char character) {
        if (character == ' ') {
            return new OutputElement<>(ElementType.CHAR, '\0', "");
        }
        return new OutputElement<>(ElementType.CHAR, character, String.valueOf(character));
    }

    public static OutputElement<BufferedImage> build(BufferedImage image, String altText) {
        return new OutputElement<>(ElementType.IMAGE, image, altText);
    }

    public static OutputElement<Void> special(ElementType<Void> type, String altText) {
        return new OutputElement<>(type, null, altText);
    }

}

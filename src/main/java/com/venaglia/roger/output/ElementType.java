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

import java.awt.image.BufferedImage;

/**
 * Created by ed on 9/23/16.
 */
public final class ElementType<T> {

    public static final ElementType<BufferedImage> IMAGE = new ElementType<>("image", BufferedImage.class);
    public static final ElementType<String> TEXT = new ElementType<>("text", String.class);
    public static final ElementType<Character> CHAR = new ElementType<>("char", Character.class);
    public static final ElementType<Character> SPACE = new ElementType<>("space", Character.class);
    public static final ElementType<Void> BOF = new ElementType<>("bof", Void.class);
    public static final ElementType<Void> EOL = new ElementType<>("eol", Void.class);
    public static final ElementType<Void> EOF = new ElementType<>("eof", Void.class);
    public static final ElementType<Void> CURSOR = new ElementType<>("cursor", Void.class);

    private final String name;
    private final Class<T> type;
    private final boolean textType;
    private final boolean imageType;

    public ElementType(String name, Class<T> type) {
        this.name = name;
        this.type = type;
        this.textType = String.class.equals(type) || Character.class.equals(type);
        this.imageType = BufferedImage.class.equals(type);
    }

    public Class<T> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public boolean isText() {
        return textType;
    }

    public boolean isImage() {
        return imageType;
    }

    public boolean joinWith(ElementType<?> typeThatFollows) {
        switch (typeThatFollows.getName()) {
            case "char":
            case "cursor":
                return Character.class.equals(this.type);
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return "ElementType<" + name.toUpperCase() + ">";
    }
}

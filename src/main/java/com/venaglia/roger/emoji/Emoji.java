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

package com.venaglia.roger.emoji;

import java.awt.image.BufferedImage;

/**
 * Created by ed on 9/23/16.
 */
public class Emoji {

    private final String id;
    private final BufferedImage image;
    private final String string;

    public Emoji(String id, BufferedImage image, String string) {
        assert id != null;
        assert id.length() > 0;
        assert id.matches("[a-z0-9_.*+-]+");
        assert image != null;
        assert string != null;
        assert string.length() > 0;
        assert string.matches("\\S+");
        this.id = id;
        this.image = image;
        this.string = string;
    }

    public String getId() {
        return id;
    }

    public BufferedImage getImage() {
        return image;
    }

    public String asString() {
        return string;
    }
}

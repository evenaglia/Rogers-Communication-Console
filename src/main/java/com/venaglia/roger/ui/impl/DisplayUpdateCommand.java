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

package com.venaglia.roger.ui.impl;

import com.venaglia.roger.ui.Command;

import java.util.Base64;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ed on 1/4/17.
 */
public class DisplayUpdateCommand implements Command {

    private static final Pattern MATCH_SHOW_RESPONSE = Pattern.compile("ok|err name ([0-9a-f]+)");

    private boolean sendImageData;
    private String imageData;
    private String hash;
    private DisplayNumber displayNumber;

    public DisplayUpdateCommand(byte[] imageDataRGB) {
        assert imageDataRGB.length == 128 * 160 * 3;
        this.sendImageData = true;
        this.imageData = Base64.getEncoder().encodeToString(imageDataRGB);
        this.hash = Sha256.digest(imageDataRGB);
        this.displayNumber = DisplayNumber.ALL;
    }

    public void setDisplayNumber(DisplayNumber displayNumber) {
        assert displayNumber != null;
        this.displayNumber = displayNumber;
    }

    @Override
    public String getCommand() {
        return "image";
    }

    @Override
    public String[] getArgs() {
        return sendImageData ? new String[]{ "store", hash, imageData } : new String[]{ "show", hash, displayNumber.getSelector() };
    }

    @Override
    public Pattern expectedResponsePattern() {
        return sendImageData ? MATCH_OK_RESPONSE : MATCH_SHOW_RESPONSE;
    }

    @Override
    public void handleResponse(Matcher matcher, Consumer<Command> queue) {
        if (sendImageData) {
            sendImageData = false;
            queue.accept(this); // now show that image
        } else if (matcher.group().startsWith("err ")) {
            sendImageData = true;
            queue.accept(this); // cache has expired, resend the image data
        }
    }
}

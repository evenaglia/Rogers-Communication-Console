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

package com.venaglia.roger.console.server;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Created by ed on 1/5/17.
 */
public interface Con {

    void hardReset() throws IOException;

    void softReset(byte selectorByte) throws IOException;

    void brightness(int value) throws IOException;

    void sleep(byte selectorByte) throws IOException;

    void wake(byte selectorByte) throws IOException;

    void updateImage(byte selectorByte, byte[] data) throws IOException;

    void readButtons(Consumer<Boolean> resultBuilder);

    void markButtons(Iterable<Boolean> buttons);

    void sendRaw(byte selectorByte, byte command, byte... bytes) throws IOException;
}

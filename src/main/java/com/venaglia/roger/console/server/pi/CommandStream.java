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

package com.venaglia.roger.console.server.pi;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by ed on 1/8/17.
 */
class CommandStream extends InputStream {

    private int index = 0;
    private int length = 0;
    private byte command;
    private byte[] data;

    void load(byte command, byte... data) {
        this.index = -1;
        this.length = data.length;
        this.command = command;
        this.data = data;
    }

    void unload() {
        index = 0;
        length = 0;
        command = 0x00;
        data = null;
    }

    @Override
    public int read() throws IOException {
        if (index >= length) {
            return -1;
        } else if (index == -1) {
            index = 0;
            return command & 0xFF;
        } else {
            return data[index++] & 0xFF;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len <= 0) {
            return 0;
        }
        if (index == -1) {
            b[off++] = command;
            len--;
            index = 0;
        }
        int bytes = Math.min(length - index, len);
        if (bytes > 0) {
            System.arraycopy(data, index, b, off, bytes);
            index += bytes;
            return bytes;
        } else {
            return 0;
        }
    }

    @Override
    public long skip(long len) throws IOException {
        if (len <= 0) {
            return 0;
        }
        if (index == -1) {
            len--;
            index = 0;
        }
        long bytes = Math.min(length - index, len);
        if (bytes > 0) {
            index += bytes;
            return bytes;
        } else {
            return 0;
        }
    }

    @Override
    public int available() throws IOException {
        return Math.max(length - index, 0);
    }
}

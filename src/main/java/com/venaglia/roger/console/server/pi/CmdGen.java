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

import static com.venaglia.roger.console.server.pi.ST7735R.DELAY;
import static java.lang.System.currentTimeMillis;

/**
 * Created by ed on 1/10/17.
 */
public class CmdGen {

    private static void executeCommandSequence(byte to, int... seq) {
        int i = 1;
        for (int c = 0; c < seq[0]; c++) {
            System.out.print("lcd cmd");
            System.out.print(toHex(to & 0xFF));
            System.out.print(toHex(seq[i++]));
            boolean delay = (seq[i] & DELAY) != 0;
            int dataBytes = seq[i++] & 0xFFFF;
            for (int j = 0; j < dataBytes; j++) {
                System.out.print(toHex(seq[i++]));
            }
            System.out.println();
            if (delay) {
                System.out.println("// Delay " + (seq[i++]) + "ms");
            }
        }
    }

    private static char[] toHex(int i) {
        return new char[]{
                ' ',
                '0',
                'x',
                "0123456789abcdef".charAt(i >> 4 & 0xF),
                "0123456789abcdef".charAt(i & 0xF)
        };
    }

    protected static void sleepUntil(long until) {
        for (long now = currentTimeMillis(); now < until; now = currentTimeMillis()) {
            try {
                Thread.sleep(until - now);
            } catch (InterruptedException e) {
                // don't care
            }
        }
    }

    public static void main(String[] args) {
        executeCommandSequence((byte)0x01, ST7735R.INIT_SEQ);
    }

}

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

/**
 * Created by ed on 1/26/17.
 */
class LUTils {

    static int[] genLUT(int inBits, int outBits) {
        assert inBits > 1 && inBits < 24;
        assert outBits > 0 && outBits < inBits;
        int mask = (1 << outBits) - 1;
        int shift = inBits - outBits;
        int round = 1 << (shift - 1);
        int[] lut = new int[1 << inBits];
        for (int i = 0, l = lut.length; i < l; i++) {
            lut[i] = Math.min((i + round >> shift), mask) & mask;
        }
        return lut;
    }

    static int[] shiftLUT(int bits, int[] lut) {
        int[] result = new int[lut.length];
        for (int i = 0, l = lut.length; i < l; i++) {
            result[i] = lut[i] << bits;
        }
        return result;
    }

    static int orLUT(int[] lut) {
        int result = 0;
        for (int value : lut) {
            result |= value;
        }
        return result;
    }
}

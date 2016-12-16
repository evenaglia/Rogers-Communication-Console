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

package com.venaglia.roger.ui;

/**
 * Created by ed on 10/25/16.
 */
public class Intervals {

    public long eventDelay() {
        return 200L;
    }

    public long hardButtonDelay() {
        return 500L;
    }

    public long click_a() {
        return 200L;
    }

    public long click_b() {
        return 1500L;
    }

    public long longPress_a() {
        return 2000L;
    }

    public long longPress_b() {
        return 10000L;
    }

    public long longPressRepeat() {
        return 1000L;
    }
}

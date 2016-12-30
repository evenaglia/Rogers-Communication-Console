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

package com.venaglia;

import com.pi4j.system.SystemInfo;

/**
 * Created by ed on 10/20/16.
 */
public enum Environment {
    DEVELOPMENT,
    PI;

    public static final Environment CURRENT_ENVIRONMENT;

    static {
        Environment env = DEVELOPMENT;
        boolean pi4jPresent = false;
        try {
            Class.forName("com.pi4j.io.gpio.GpioFactory");
            pi4jPresent = true;
        } catch (ClassNotFoundException e) {
            // the lib isn't present
        }
        try {
            String hw;
            if (pi4jPresent) {
                hw = SystemInfo.getHardware();
            } else {
                hw = "";
            }
            env = hw.startsWith("BCM") ? PI : DEVELOPMENT;
        } catch (Exception e) {
            // nope, probably not on a PI, or on linux for that matter.
        } finally {
            CURRENT_ENVIRONMENT = env;
        }
        if (CURRENT_ENVIRONMENT == DEVELOPMENT) {
            System.out.println("INFO - Running in DEVELOPMENT mode");
        } else {
            System.out.println("INFO - Running in PRODUCTION mode");
        }
    }

    public static void main(String[] args) {
        System.out.println("CURRENT_ENVIRONMENT = " + CURRENT_ENVIRONMENT);
    }
}

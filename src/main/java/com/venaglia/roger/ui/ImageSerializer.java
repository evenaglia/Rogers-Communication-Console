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

import com.google.inject.ImplementedBy;
import com.venaglia.roger.ui.impl.ImageSerializer444;

import java.awt.image.BufferedImage;

/**
 * Created by ed on 1/26/17.
 */
@ImplementedBy(ImageSerializer444.class)
public interface ImageSerializer {

    byte[] serialize(BufferedImage bufferedImage);
}

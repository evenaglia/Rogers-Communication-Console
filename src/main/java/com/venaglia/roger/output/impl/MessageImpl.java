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

package com.venaglia.roger.output.impl;

import com.venaglia.roger.output.ElementType;
import com.venaglia.roger.output.Message;
import com.venaglia.roger.output.OutputElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by ed on 9/30/16.
 */
public class MessageImpl implements Message {

    final List<OutputElement<?>> elements = new ArrayList<>();

    int cursorY = Integer.MIN_VALUE;

    @Override
    public int getSize() {
        return elements.size();
    }

    @Override
    public OutputElement<?> getElement(int index) {
        int size = elements.size();
        if (index < 0 || index > size) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        if (index == size) {
            return EOF;
        }
        return elements.get(index);
    }

    @Override
    public void writeTo(Appendable buffer) throws IOException {
        writeToImpl(buffer);
    }

    private <A extends Appendable> A writeToImpl(A buffer) throws IOException {
        assert buffer != null;
        ElementType<?> prevType = null;
        for (OutputElement<?> element : elements) {
            ElementType<?> type = element.getElementType();
            boolean noSpace = prevType == null || prevType.joinWith(type);
            if (!noSpace) buffer.append(' ');
            buffer.append(element.asString());
            prevType = type;
        }
        return buffer;
    }

    @Override
    public void writeTo(Consumer<OutputElement<?>> consumer) {
        assert consumer != null;
        for (OutputElement<?> element : elements) {
            consumer.accept(element);
        }
    }

    @Override
    public String asString() {
        try {
            return writeToImpl(new StringBuilder()).toString();
        } catch (IOException e) {
            throw new RuntimeException("StringBuilder threw IOException: " + e.getMessage(), e);
        }
    }
}

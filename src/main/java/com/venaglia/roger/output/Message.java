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

package com.venaglia.roger.output;

import java.io.IOException;
import java.io.Writer;
import java.util.function.Consumer;

/**
 * Represents a single message as a series of OutputElements.
 */
public interface Message {

    OutputElement<Void> EOL = OutputElement.special(ElementType.EOL, "\n");
    OutputElement<Void> EOF = OutputElement.special(ElementType.EOF, "");

    /**
     * @return the number of OutputElements in this message.
     */
    int getSize();

    /**
     * @param index the index of the element to return.
     * @return the OutputElement at the specified index. If index is equal to
     *         size, an EOF element is returned.
     * @throws ArrayIndexOutOfBoundsException if the specified index is greater
     *         than the size or less than zero.
     */
    OutputElement<?> getElement(int index);

    /**
     * Writes a textural representation of this message to passed Writer. No EOL
     * is appended, only a text representation of the message.
     * @param buffer An output to append the textural content.
     */
    void writeTo(Appendable buffer) throws IOException;

    /**
     * Sends all OutputElements in this message to the supplied consumer.
     * @param consumer The OutputElement consumer
     */
    void writeTo(Consumer<OutputElement<?>> consumer);

    /**
     * @return The content of this message as a string
     */
    String asString();
}

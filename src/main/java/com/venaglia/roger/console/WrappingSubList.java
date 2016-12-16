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

package com.venaglia.roger.console;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

/**
 * Created by ed on 9/4/16.
 */
public class WrappingSubList<E> extends AbstractList<E> {

    private List<E> list = Collections.emptyList();
    private int offset;
    private int size;
    private E blank;

    public void update(int start, int size, List<E> list, E blank) {
        this.blank = blank;
        assert list != null;
        assert size >= 0;
        assert size == 0 || list.size() > 0;
        this.list = list;
        this.offset = list.isEmpty() ? 0 : start % list.size();
        this.size = size;
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= size || list.isEmpty()) throw new ArrayIndexOutOfBoundsException(index);
        int limit = list.size();
        boolean wrap = size < limit;
        int i = index + offset;
        if (wrap || i < limit) {
            return list.get(i % limit);
        } else {
            return blank;
        }
    }

    @Override
    public int size() {
        return size;
    }
}

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

import com.venaglia.roger.output.ArchivedMessage;
import com.venaglia.roger.output.OutputElement;

import java.util.Date;
import java.util.List;

/**
 * Created by ed on 10/4/16.
 */
public class AchivedMessageImpl extends MessageImpl implements ArchivedMessage {

    private final Date archiveDate = new Date();

    public AchivedMessageImpl(List<OutputElement<?>> elements) {
        this.elements.addAll(elements);
    }

    @Override
    public Date getArchiveDate() {
        return archiveDate;
    }

    @Override
    public int compareTo(ArchivedMessage o) {
        return o.getArchiveDate().compareTo(archiveDate);
    }
}

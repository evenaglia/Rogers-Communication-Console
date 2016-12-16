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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.venaglia.roger.output.ActiveMessage;
import com.venaglia.roger.output.ActiveMessageListener;
import com.venaglia.roger.output.ArchivedMessage;
import com.venaglia.roger.output.ElementType;
import com.venaglia.roger.output.Message;
import com.venaglia.roger.output.OutputElement;
import com.venaglia.roger.output.OutputWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ed on 9/28/16.
 */
@Singleton
public class OutputWindowImpl extends JPanel implements OutputWindow {

    private static final int FRAME_PADDING = 4; // A little inset around the frame
    private static final int LINE_SPACING = 4; // Vertical space between lines
    private static final int H_SPACING = 6; // Horizontal space between elements
    private static final int V_SPACING = 8; // Vertical space between messages

    private final Font font;
    private final ScheduledExecutorService executorService;
    private final Lock messageUpdateLock = new ReentrantLock();

    private ActiveMessage activeMessage = new ActiveMessageImpl();
    private List<ArchivedMessage> archivedMessages = new ArrayList<>();
    private ActiveMessageDelegate messageDelegate = new ActiveMessageDelegate();
    private ScheduledFuture<?> cursorBlink = null;
    private boolean cursorOn = true;
    private volatile int cursorVersion = 0;

    @Inject
    OutputWindowImpl(Font font, ScheduledExecutorService executorService) {
        this.font = font.deriveFont(24.0f);
        this.executorService = executorService;
        this.setSize(1024, 768);
        this.setBackground(Color.BLACK);
        activeMessage.addListener(messageDelegate);
        activeMessage.addListener(new ActiveMessageListener() {
            @Override
            public void afterArchive(ArchivedMessage message) {
                messageUpdateLock.lock();
                try {
                    archivedMessages.add(message);
                } finally {
                    messageUpdateLock.unlock();
                }
                activeMessage = new ActiveMessageImpl();
                activeMessage.addListener(messageDelegate);
                activeMessage.addListener(this);
            }

            @Override
            public void changed() {
                final int expected = cursorVersion++;
                ActiveMessageImpl.CURSOR.setRectangle(null);
                cursorOn = true;
                repaint();
                cursorBlink.cancel(true);
                cursorBlink = scheduleCursorBlink(expected, executorService);
            }
        });
        cursorBlink = executorService.scheduleAtFixedRate(() -> blinkCursor(0), 2000L, 750L, TimeUnit.MILLISECONDS);
    }

    private ScheduledFuture<?> scheduleCursorBlink(int expected, ScheduledExecutorService executorService) {
        return executorService.scheduleAtFixedRate(() -> blinkCursor(expected), 2000L, 750L, TimeUnit.MILLISECONDS);
    }

    private void blinkCursor(long expectedVersion) {
        if (expectedVersion == cursorVersion) {
            cursorOn = !cursorOn;
            Rectangle r = ActiveMessageImpl.CURSOR.getRectangle();
            if (r != null) {
                repaint(r.x - 4, ((ActiveMessageImpl)activeMessage).cursorY + r.y - 4, r.width + 8, r.height + 8);
            } else {
                repaint();
            }
        }
    }

    @Override
    public ActiveMessage getActiveMessage() {
        return activeMessage;
    }

    @Override
    public List<ArchivedMessage> getArchivedMessages() {
        messageUpdateLock.lock();
        try {
            return Collections.unmodifiableList(archivedMessages);
        } finally {
            messageUpdateLock.unlock();
        }
    }

    @Override
    public void addActiveMessageListener(ActiveMessageListener listener) {
        messageDelegate.add(listener);
    }

    @Override
    public void removeActiveMessageListener(ActiveMessageListener listener) {
        messageDelegate.remove(listener);
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        Graphics2D g = (Graphics2D)graphics;
        int viewWidth = getWidth() - FRAME_PADDING - FRAME_PADDING;
        int cursorY = getHeight() - FRAME_PADDING;
        Metrics m = new Metrics(viewWidth, getFontMetrics(font));
        g.setFont(font);
        AffineTransform transform = g.getTransform();
        g.translate(0, cursorY + V_SPACING);
        try {
            for (Message message : getMessages()) {
                Dimension dimension = formatMessage(message, m);
                int advance = 0 - dimension.height - V_SPACING;
                cursorY += advance;
                ((MessageImpl)message).cursorY = cursorY;
                g.translate(0, advance);
                for (int i = 0, l = message.getSize(); i < l; i++) {
                    OutputElement<?> element = message.getElement(i);
                    Rectangle r = element.getRectangle();
                    switch (element.getElementType().getName()) {
                        case "text":
                        case "char":
                            g.setColor(Color.LIGHT_GRAY);
                            g.drawString(element.asString(), r.x, r.y);
                            break;
                        case "cursor":
                            if (cursorOn) {
                                g.setColor(Color.WHITE);
                                g.drawString("|", r.x, r.y + m.fontDrop);
                            }
                            break;
                        case "image":
                            g.drawImage((BufferedImage)element.getValue(), r.x, r.y, r.width, r.height, null);
                            break;
                    }
                }
                if (cursorY < 0) break;
            }
        } finally {
            g.setTransform(transform);
        }
    }

    private Dimension formatMessage(Message message, Metrics m) {
        int l = message.getSize();
        int[] xywh = new int[l * 4];
        int cursorX = 0;
        int cursorY = 0;
        int lineHeight = font.getSize();
        int lineStartIndex = 0;
        int wordStartIndex = -1;
        ElementType<?> prevType = ElementType.BOF;
        for (int i = 0, offset = 0; i < l; i++, offset += 4) {
            OutputElement<?> element = message.getElement(i);
            calculateSize(element, m.fm.getFontRenderContext(), xywh, offset + 2);
            ElementType<?> thisType = element.getElementType();
            int pad = prevType.joinWith(thisType) || cursorX == 0 ? 0 : H_SPACING;
            if ("char".equals(thisType.getName())) {
                if (wordStartIndex == -1) {
                    wordStartIndex = i;
                }
            } else if (wordStartIndex >= 0) {
                wordStartIndex = -1; // no longer in a word
            }
            int nextX = cursorX + pad + xywh[offset + 2];
            int nextY = cursorY;
            if (cursorX > 0 && nextX > m.viewWidth || "eol".equals(thisType.getName())) {
                // the next element will not fit on the current line, so let's align the current line and create a new
                // line for this element.
                int lineEndIndex = wordStartIndex > lineStartIndex ? wordStartIndex : i;
                for (int j = lineStartIndex, o = lineStartIndex * 4; j < lineEndIndex; j++, o += 4) {
                    xywh[o + 1] = cursorY + (lineHeight - xywh[o + 3]) / 2 + m.drop(message.getElement(j));
                }
                if (lineEndIndex < i) {
                    // we are wrapping a word, so move all the typed letters all the way to the left
                    int shiftLeft = xywh[lineEndIndex * 4];
                    for (int j = lineEndIndex, o = lineEndIndex * 4; j < i; j++, o += 4) {
                        xywh[o] -= shiftLeft;
                    }
                }
                nextX = 0;
                nextY = cursorY + LINE_SPACING + lineHeight;
                lineHeight = font.getSize();
                lineStartIndex = lineEndIndex;
            }
            xywh[offset] = cursorX;
            xywh[offset + 1] = cursorY;
            cursorX = nextX;
            cursorY = nextY;
            lineHeight = Math.max(lineHeight, xywh[offset + 3]);
            prevType = thisType;
        }
        for (int j = lineStartIndex, o = lineStartIndex * 4; j < l; j++, o += 4) {
            xywh[o + 1] = cursorY + (lineHeight - xywh[o + 3]) / 2 + m.drop(message.getElement(j));
        }
        int width = 0, height = 0;
        for (int i = 0, offset = 0; i < l; i++, offset += 4) {
            OutputElement<?> element = message.getElement(i);
            Rectangle rectangle = element.getRectangle();
            int x = xywh[offset], y = xywh[offset + 1];
            int w = xywh[offset + 2], h = xywh[offset + 3];
            width = Math.max(width, x + w);
            height = Math.max(height, y - m.drop(element) + h);
            if (rectangle == null || neq(rectangle.getX(), x) || neq(rectangle.getY(), y)) {
                rectangle = new Rectangle(x, y, w, h);
                element.setRectangle(rectangle);
            }
        }
        return new Dimension(width, height);
    }

    private boolean neq(double v1, int v2) {
        return Math.round((float)v1) != v2;
    }

    private void calculateSize(OutputElement<?> element, FontRenderContext frc, int[] buffer, int offset) {
        int w = 0, h = 0;
        ElementType<?> elementType = element.getElementType();
        Rectangle rectangle = element.getRectangle();
        if (rectangle != null && false) {
            w = Math.round((float)rectangle.getWidth());
            h = Math.round((float)rectangle.getHeight());
        } else {
            switch (elementType.getName()) {
                case "text":
                case "char":
                    String text = element.asString();
                    Rectangle2D bounds = font.getStringBounds(text, frc);
                    w = Math.round((float)bounds.getWidth());
                    h = Math.round((float)bounds.getHeight());
                    break;
                case "cursor":
                    bounds = font.getStringBounds("|", frc);
                    w = Math.round((float)bounds.getWidth());
                    h = Math.round((float)bounds.getHeight());
                    break;
                case "image":
                    BufferedImage image = (BufferedImage)(element).getValue();
                    w = image.getWidth();
                    h = image.getHeight();
                    break;

            }
        }
        buffer[offset] = w;
        buffer[offset + 1] = h;
    }

    private Iterable<Message> getMessages() {
        final Iterator<? extends Message> iter;
        messageUpdateLock.lock();
        try {
            iter = new ArrayList<Message>(archivedMessages).iterator();
        } finally {
            messageUpdateLock.unlock();
        }
        return () -> new Iterator<Message>() {

            private boolean first = true;

            @Override
            public boolean hasNext() {
                return first || iter.hasNext();
            }

            @Override
            public Message next() {
                try {
                    return first ? activeMessage : iter.next();
                } finally {
                    first = false;
                }
            }
        };
    }

    private static class Metrics {
        final int viewWidth;
        final FontMetrics fm;

        private final int fontDrop;

        public Metrics(int viewWidth, FontMetrics fm) {
            this.viewWidth = viewWidth;
            this.fm = fm;
            this.fontDrop = (fm.getAscent() + fm.getDescent()) / 2 - fm.getDescent();;
        }

        private int drop(OutputElement<?> element) {
            ElementType<?> type = element.getElementType();
            return type.isText() ? fontDrop : 0;
        }
    }
}

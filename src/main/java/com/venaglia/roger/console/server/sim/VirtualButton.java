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

package com.venaglia.roger.console.server.sim;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by ed on 8/28/16.
 */
public class VirtualButton extends JComponent {

    private static final int BUMPER_WIDTH = 72;
    private static final int BASE_WIDTH = 160;
    private static final int BASE_HEIGHT = 128;

    private BufferedImage altImage = new BufferedImage(160, 128, BufferedImage.TYPE_INT_ARGB);

    private final ButtonClass buttonClass;
    private final float width;
    private final float height;
    private final float imageWidth;
    private final Shape[] frame;
    private final BasicStroke stroke;
    private final MyMouseHandler mouseHandler;
    private final AtomicReference<BufferedImage> image = new AtomicReference<>(new BufferedImage(160, 128, BufferedImage.TYPE_INT_ARGB));
    private final ConsumerSupplier<Boolean> buttonState = new ConsumerSupplier<>(false);
    private final Lock updateLock = new ReentrantLock(true);

    private double rotate = 0.0;

    public VirtualButton(float scale) {
        this(scale, ButtonClass.DYNAMIC_IMAGE);
    }

    VirtualButton(float scale, ButtonClass buttonClass) {
        super();
        this.buttonClass = buttonClass;
        switch (buttonClass) {
            case LEFT_BUMPER:
            case RIGHT_BUMPER:
                width = BUMPER_WIDTH * scale;
                height = 160.0f * scale;
                break;
            case TOP_BUMPER:
                width = 320.0f * scale;
                height = BUMPER_WIDTH * scale;
                break;
            default:
                width = (BASE_WIDTH + 21.5f) * scale;
                height = BASE_HEIGHT * scale;
                break;
        }
        if (buttonClass.isHardButton()) {
            imageWidth = 0;
            setSize(Math.round(width), Math.round(height));
            float r = Math.min(width, height);
            frame = new Shape[] {
                    new RoundRectangle2D.Float(1, 1, width - 2, height - 2, r, r)
            };
        } else {
            imageWidth = BASE_WIDTH * scale;
            setSize(Math.round(240.0f * scale), Math.round(160.0f * scale));
            float dx = width * 0.5f;
            float dx2 = imageWidth * 0.5f;
            float dy = height * 0.5f;
            frame = new Shape[] {
                    new QuadCurve2D.Float(-dx, 0, -dx, -dy, -dx2, -dy),
                    new QuadCurve2D.Float(dx, 0, dx, -dy, dx2, -dy),
                    new QuadCurve2D.Float(-dx, 0, -dx, dy, -dx2, dy),
                    new QuadCurve2D.Float(dx, 0, dx, dy, dx2, dy),
                    new Line2D.Float(-dx2, -dy, dx2, -dy),
                    new Line2D.Float(-dx2, dy, dx2, dy)
            };
        }
        stroke = new BasicStroke(0.5f * scale);
        mouseHandler = new MyMouseHandler();
        mouseHandler.configure(buttonState);
        addMouseListener(mouseHandler);
    }

    public Supplier<Boolean> getButtonStateSupplier() {
        return buttonState;
    }

    public Consumer<Boolean> getButtonStateHighlighter() {
        return buttonState;
    }

    public void setImageBytesRgb(byte[] imageBytesRgb) {
        assert imageBytesRgb != null;
        assert imageBytesRgb.length == 160 * 128 * 3;
        updateLock.lock();
        try {
            int i = 0;
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 160; x++) {
                    int rgb = 0xFF;
                    rgb = rgb << 8 | (imageBytesRgb[i++] & 0xFF);
                    rgb = rgb << 8 | (imageBytesRgb[i++] & 0xFF);
                    rgb = rgb << 8 | (imageBytesRgb[i++] & 0xFF);
                    altImage.setRGB(x, y, rgb);
                }
            }
            BufferedImage img = image.get();
            image.set(altImage);
            altImage = img;
            repaint();
        } finally {
            updateLock.unlock();
        }
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        Graphics2D g = (Graphics2D)graphics;
        g.setColor(buttonState.get() ? Color.LIGHT_GRAY : Color.BLACK);
        g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
        g.setColor(Color.RED);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        if (buttonClass.isHardButton()) {
            g.setPaint(Color.DARK_GRAY);
            int w = getWidth() - 2;
            int h = getHeight() - 2;
            int r = Math.min(w, h);
            g.fillRoundRect(0, 0, w, h, r, r);
            g.setPaint(Color.WHITE);
            g.setStroke(stroke);
            for (Shape shape : frame) {
                g.draw(shape);
            }
        } else {
            AffineTransform xf = g.getTransform();
            try {
                g.transform(AffineTransform.getTranslateInstance(getWidth() * 0.5, getHeight() * 0.5));
                if (rotate != 0.0) {
                    g.transform(AffineTransform.getRotateInstance(rotate));
                }
                g.drawImage(image.get(), Math.round(imageWidth * -0.5f), Math.round(height * -0.5f), Math.round(imageWidth), Math.round(height), null);
                g.setPaint(Color.WHITE);
                g.setStroke(stroke);
                for (Shape shape : frame) {
                    g.draw(shape);
                }
            } finally {
                g.setTransform(xf);
            }
        }
    }

    public void setRotate(double rotate) {
        this.rotate = rotate;
    }

    private static class MyMouseHandler extends MouseAdapter {

        private enum State {
            MOUSE_UP,
            MOUSE_DOWN,
            MOUSE_DOWN_BUT_OUTSIDE,
        }

        private State state = State.MOUSE_UP;
        private Consumer<Boolean> stateListener = null;

        @Override
        public void mousePressed(MouseEvent e) {
            state = State.MOUSE_DOWN;
            if (stateListener != null) {
                stateListener.accept(true);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (state == State.MOUSE_DOWN && stateListener != null) {
                stateListener.accept(false);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (state == State.MOUSE_DOWN) {
                state = State.MOUSE_DOWN_BUT_OUTSIDE;
                if (stateListener != null) {
                    stateListener.accept(false);
                }
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            super.mouseEntered(e);
            if (state == State.MOUSE_DOWN_BUT_OUTSIDE) {
                state = State.MOUSE_UP;
                if (stateListener != null) {
                    stateListener.accept(true);
                }
            }
        }

        void configure(Consumer<Boolean> stateListener) {
            this.stateListener = stateListener;
        }
    }

    enum ButtonClass {
        DYNAMIC_IMAGE, LEFT_BUMPER, RIGHT_BUMPER, TOP_BUMPER;

        boolean isHardButton() {
            return this != DYNAMIC_IMAGE;
        }
    }

    private static class ConsumerSupplier<T> implements Consumer<T>, Supplier<T> {

        private volatile T value;

        public ConsumerSupplier(T value) {
            this.value = value;
        }

        @Override
        public void accept(T value) {
            this.value = value;
        }

        @Override
        public T get() {
            return value;
        }
    }
}

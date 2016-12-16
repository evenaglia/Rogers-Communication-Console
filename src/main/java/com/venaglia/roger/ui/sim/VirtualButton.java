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

package com.venaglia.roger.ui.sim;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.venaglia.roger.buttons.Button;
import com.venaglia.roger.buttons.ButtonFace;
import com.venaglia.roger.buttons.SimpleButtonListener;

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
import java.util.function.Supplier;

/**
 * Created by ed on 8/28/16.
 */
public class VirtualButton extends JComponent {

    private static final int BUMPER_WIDTH = 72;
    private static final int BASE_WIDTH = 160;
    private static final int BASE_HEIGHT = 128;
    private static final AtomicReference<BufferedImage> CHECKERBOARD = new AtomicReference<>();

    private volatile com.venaglia.roger.buttons.Button btn = Button.NIL;
    private SimpleButtonListener buttonListener;

    private final ButtonClass buttonClass;
    private final float width;
    private final float height;
    private final float imageWidth;
    private final float textBoxHeight;
    private final Shape[] frame;
    private final Font font;
    private final BasicStroke stroke;
    private final MyMouseHandler mouseHandler;

    private double rotate = 0.0;

    @Inject
    public VirtualButton(Font font, @Named("UIScale") float scale) {
        this(font, scale, ButtonClass.DYNAMIC_IMAGE);
    }

    VirtualButton(Font font, float scale, ButtonClass buttonClass) {
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
            imageWidth = textBoxHeight = 0;
            setSize(Math.round(width), Math.round(height));
            float r = Math.min(width, height);
            frame = new Shape[] {
                    new RoundRectangle2D.Float(0, 0, width, height, r, r)
            };
            this.font = null;
        } else {
            imageWidth = BASE_WIDTH * scale;
            textBoxHeight = (BASE_HEIGHT - 12.0f) * scale;
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
            this.font = font.deriveFont(12.0f * scale);
            if (CHECKERBOARD.get() == null) {
                BufferedImage checkerboard = new BufferedImage(160, 128, BufferedImage.TYPE_INT_ARGB);
                Graphics g = checkerboard.getGraphics();
                g.setColor(new Color(0, 0, 0, 32));
                for (int y = 0, i = 0; y < 128; y += 8, i = 8 - i) {
                    for (int x = i; x < 160; x += 16) {
                        g.fillRect(x, y, 8, 8);
                    }
                }
            }
        }
        stroke = new BasicStroke(0.5f * scale);
        mouseHandler = new MyMouseHandler();
        mouseHandler.configure(() -> btn, new SimpleButtonListener() {

            @Override
            public void handleButtonDown(Button button) {
                if (buttonListener != null) buttonListener.handleButtonDown(button);
            }

            @Override
            public void handleButtonUp(Button button) {
                if (buttonListener != null) buttonListener.handleButtonUp(button);
            }
        });
        addMouseListener(mouseHandler);
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        com.venaglia.roger.buttons.Button btn = this.btn;
        Graphics2D g = (Graphics2D)graphics;
        g.setColor(Color.RED);
        g.drawRect(0, 0, getWidth(), getHeight());
        if (buttonClass.isHardButton()) {
            g.setPaint(Color.DARK_GRAY);
            int w = getWidth();
            int h = getHeight();
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
                ButtonFace face = btn.getButtonFace();
                if (face != null) {
                    g.drawImage(face.getBufferedImage(), Math.round(imageWidth * -0.5f), Math.round(height * -0.5f), Math.round(imageWidth), Math.round(height), null);
                } else {
                    g.setPaint(Color.DARK_GRAY);
                    g.fillRect(Math.round(imageWidth * -0.5f), Math.round(height * -0.5f), Math.round(imageWidth), Math.round(height));
                }
                g.setPaint(Color.WHITE);
                g.setStroke(stroke);
                for (Shape shape : frame) {
                    g.draw(shape);
                }
                String label = face == null ? "" : face.getLabel();
                if (label != null && label.length() > 0) {
                    g.setFont(font);
                    int advance = g.getFontMetrics().stringWidth(label);
                    int descent = g.getFontMetrics().getMaxDescent();
                    g.drawString(label,
                                 Math.round(advance * -0.5f),
    //                             Math.round(0),
                                 Math.round(textBoxHeight * 0.5f - descent));
                }
            } finally {
                g.setTransform(xf);
            }
        }
    }

    public void setRotate(double rotate) {
        this.rotate = rotate;
    }

    public void setButton(Button btn) {
        if (btn == null) {
            btn = Button.NIL;
        }
        if (btn != this.btn) {
            this.btn = btn;
            repaint(1);
        }
    }

//    public boolean addButtonListener(ButtonListener buttonListener) {
//        assert buttonListener != null;
//        return buttonListeners.add(buttonListener);
//    }
//
//    public boolean removeButtonListener(ButtonListener buttonListener) {
//        assert buttonListener != null;
//        return buttonListeners.remove(buttonListener);
//    }

    public void setButtonListener(SimpleButtonListener buttonListener) {
        this.buttonListener = buttonListener;
    }

    private static class MyMouseHandler extends MouseAdapter {

        private enum State {
            MOUSE_UP,
            MOUSE_DOWN,
            MOUSE_DOWN_BUT_OUTSIDE,
        }

        private State state = State.MOUSE_UP;
        private Supplier<com.venaglia.roger.buttons.Button> buttonSupplier;
        private SimpleButtonListener buttonListener;

        @Override
        public void mousePressed(MouseEvent e) {
            state = State.MOUSE_DOWN;
            buttonListener.handleButtonDown(buttonSupplier.get());
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (state == State.MOUSE_DOWN && buttonListener != null) {
                Button button = buttonSupplier.get();
                buttonListener.handleButtonUp(button);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (state == State.MOUSE_DOWN) {
                state = State.MOUSE_DOWN_BUT_OUTSIDE;
                buttonListener.handleButtonUp(buttonSupplier.get());
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            super.mouseEntered(e);
            if (state == State.MOUSE_DOWN_BUT_OUTSIDE) {
                state = State.MOUSE_UP;
//                buttonListener.handleButtonDown(buttonSupplier.get());
            }
        }

        void configure(Supplier<com.venaglia.roger.buttons.Button> buttonSupplier, SimpleButtonListener buttonListener) {
            assert buttonSupplier != null;
            assert buttonListener != null;
            this.buttonSupplier = buttonSupplier;
            this.buttonListener = buttonListener;
        }
    }

    enum ButtonClass {
        DYNAMIC_IMAGE, LEFT_BUMPER, RIGHT_BUMPER, TOP_BUMPER;

        boolean isHardButton() {
            return this != DYNAMIC_IMAGE;
        }
    }
}

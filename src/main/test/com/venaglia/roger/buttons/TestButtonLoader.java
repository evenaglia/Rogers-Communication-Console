package com.venaglia.roger.buttons;

import com.venaglia.roger.ui.FontLoader;

/**
 * Created by ed on 10/13/16.
 */
public class TestButtonLoader {

    private static final ButtonSetLoader loader = new ButtonSetLoader();

    static {
        loader.setFont(new FontLoader().get());
    }

    public static Button getSimpleButton(String id, String text) {
        if (text.length() == 1 && isPrintable(text.charAt(0))) {
            return loader.createButton(id, null, text, null, text.charAt(0), null, null, null);
        } else {
            return loader.createButton(id, null, text, text, null, null, null, null);
        }
    }

    private static boolean isPrintable(char ch) {
        return ch >= ' ' && ch <= '~';
    }
}

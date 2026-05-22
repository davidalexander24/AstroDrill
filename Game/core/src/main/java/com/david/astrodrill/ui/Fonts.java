package com.david.astrodrill.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

/**
 * Cross-platform font factory. Uses libGDX's built-in BitmapFont (15pt Liberation Sans)
 * and scales it to the requested size. This works on both desktop and GWT/HTML — the
 * old FreeTypeFontGenerator approach couldn't compile for GWT.
 *
 * borderWidth and borderColor are accepted for API symmetry with the old freetype call
 * sites, but BitmapFont scaling doesn't produce true outlines — they are ignored.
 * If you want styled fonts back, generate a .fnt+.png with Hiero and load directly.
 */
public final class Fonts {

    /** libGDX's default BitmapFont is rendered at this nominal size. */
    private static final float BASE_SIZE = 15f;

    private Fonts() {}

    public static BitmapFont create(int size, Color color, float borderWidth) {
        BitmapFont font = new BitmapFont();
        font.getData().setScale(size / BASE_SIZE);
        if (color != null) font.setColor(color);
        return font;
    }

    public static BitmapFont create(int size, Color color) {
        return create(size, color, 0f);
    }

    public static BitmapFont create(int size) {
        return create(size, Color.WHITE, 0f);
    }
}

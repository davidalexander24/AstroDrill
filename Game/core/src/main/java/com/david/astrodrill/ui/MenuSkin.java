package com.david.astrodrill.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight programmatic skin for menu screens (Login/MainMenu/Leaderboard).
 * Avoids bundling uiskin.json + atlas assets. Caller must dispose() when the
 * owning screen disposes.
 */
public class MenuSkin implements Disposable {

    public final BitmapFont titleFont;
    public final BitmapFont buttonFont;
    public final BitmapFont labelFont;
    public final BitmapFont smallFont;

    public final TextButton.TextButtonStyle buttonStyle;
    public final TextButton.TextButtonStyle disabledButtonStyle;
    public final TextField.TextFieldStyle textFieldStyle;
    public final Label.LabelStyle titleStyle;
    public final Label.LabelStyle labelStyle;
    public final Label.LabelStyle errorStyle;
    public final Label.LabelStyle successStyle;
    public final Label.LabelStyle smallStyle;
    public final CheckBox.CheckBoxStyle checkBoxStyle;

    public final Drawable panelBackground;
    public final Drawable rowBackground;

    private final FreeTypeFontGenerator generator;
    private final List<Texture> textures = new ArrayList<>();

    public MenuSkin() {
        generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial.ttf"));
        titleFont = font(54, Color.WHITE, 2f);
        buttonFont = font(22, Color.WHITE, 1f);
        labelFont = font(18, Color.WHITE, 1f);
        smallFont = font(14, Color.LIGHT_GRAY, 0.5f);

        Drawable buttonUp = borderedSolid(0.0f, 0.7f, 1.0f, 0.8f, 0.05f, 0.1f, 0.2f, 0.8f, 64, 2);
        Drawable buttonOver = borderedSolid(0.2f, 0.9f, 1.0f, 1.0f, 0.1f, 0.2f, 0.4f, 0.9f, 64, 2);
        Drawable buttonDown = borderedSolid(0.0f, 0.4f, 0.6f, 0.8f, 0.02f, 0.05f, 0.1f, 0.8f, 64, 2);
        Drawable buttonDisabled = borderedSolid(0.3f, 0.3f, 0.4f, 0.5f, 0.1f, 0.1f, 0.15f, 0.6f, 64, 2);
        Drawable fieldBg = solid(0.05f, 0.05f, 0.1f, 0.8f);
        Drawable cursor = solid(0.9f, 0.9f, 1f, 1f);
        Drawable selection = solid(0.3f, 0.5f, 0.8f, 0.6f);
        Drawable checkOff = solidSize(0.1f, 0.1f, 0.15f, 0.95f, 24);
        Drawable checkOn = borderedSolid(0.1f, 0.1f, 0.15f, 0.95f, 0.3f, 0.6f, 0.9f, 1f, 24, 4);

        panelBackground = solid(0.02f, 0.04f, 0.1f, 0.7f);
        rowBackground = solid(0.12f, 0.12f, 0.18f, 0.5f);

        buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = buttonFont;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.overFontColor = new Color(0.7f, 0.9f, 1f, 1f);
        buttonStyle.up = buttonUp;
        buttonStyle.over = buttonOver;
        buttonStyle.down = buttonDown;

        disabledButtonStyle = new TextButton.TextButtonStyle();
        disabledButtonStyle.font = buttonFont;
        disabledButtonStyle.fontColor = new Color(0.55f, 0.55f, 0.55f, 1f);
        disabledButtonStyle.up = buttonDisabled;
        disabledButtonStyle.over = buttonDisabled;
        disabledButtonStyle.down = buttonDisabled;

        textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = labelFont;
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = fieldBg;
        textFieldStyle.cursor = cursor;
        textFieldStyle.selection = selection;

        titleStyle = new Label.LabelStyle(titleFont, new Color(0.7f, 0.9f, 1f, 1f));
        labelStyle = new Label.LabelStyle(labelFont, Color.WHITE);
        errorStyle = new Label.LabelStyle(labelFont, new Color(1f, 0.4f, 0.4f, 1f));
        successStyle = new Label.LabelStyle(labelFont, new Color(0.4f, 1f, 0.5f, 1f));
        smallStyle = new Label.LabelStyle(smallFont, Color.LIGHT_GRAY);

        checkBoxStyle = new CheckBox.CheckBoxStyle();
        checkBoxStyle.font = labelFont;
        checkBoxStyle.fontColor = Color.WHITE;
        checkBoxStyle.checkboxOff = checkOff;
        checkBoxStyle.checkboxOn = checkOn;
    }

    private BitmapFont font(int size, Color color, float border) {
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = size;
        p.color = color;
        p.borderWidth = border;
        p.borderColor = Color.BLACK;
        return generator.generateFont(p);
    }

    private Drawable solid(float r, float g, float b, float a) {
        return solidSize(r, g, b, a, 1);
    }

    private Drawable solidSize(float r, float g, float b, float a, int size) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(r, g, b, a);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        textures.add(tex);
        return new TextureRegionDrawable(new TextureRegion(tex));
    }

    private Drawable borderedSolid(float bgR, float bgG, float bgB, float bgA,
                                   float fgR, float fgG, float fgB, float fgA,
                                   int size, int pad) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(bgR, bgG, bgB, bgA);
        pm.fill();
        pm.setColor(fgR, fgG, fgB, fgA);
        pm.fillRectangle(pad, pad, size - pad * 2, size - pad * 2);
        Texture tex = new Texture(pm);
        pm.dispose();
        textures.add(tex);
        return new TextureRegionDrawable(new TextureRegion(tex));
    }

    @Override
    public void dispose() {
        titleFont.dispose();
        buttonFont.dispose();
        labelFont.dispose();
        smallFont.dispose();
        generator.dispose();
        for (Texture t : textures) t.dispose();
        textures.clear();
    }
}

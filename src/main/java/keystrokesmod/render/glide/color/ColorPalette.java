package keystrokesmod.render.glide.color;

import java.awt.Color;

import keystrokesmod.render.glide.GlideContext;
import keystrokesmod.render.glide.animation.ColorAnimation;

public class ColorPalette {

    private ColorAnimation[] backgroundColorAnimations = new ColorAnimation[3];
    private ColorAnimation[] fontColorAnimations = new ColorAnimation[3];

    public ColorPalette() {
        for (int i = 0; i < backgroundColorAnimations.length; i++) {
            backgroundColorAnimations[i] = new ColorAnimation();
        }

        for (int i = 0; i < fontColorAnimations.length; i++) {
            fontColorAnimations[i] = new ColorAnimation();
        }
    }

    public Color getBackgroundColor(ColorType type, int alpha) {
        return backgroundColorAnimations[type.getIndex()].getColor(getRawBackgroundColor(type, alpha));
    }

    public Color getBackgroundColor(ColorType type) {
        return getBackgroundColor(type, 255);
    }

    private Color getRawBackgroundColor(ColorType type, int alpha) {
        GlideTheme theme = getTheme();

        switch (type) {
            case DARK:
                return theme.getDarkBackgroundColor(alpha);
            case NORMAL:
                return theme.getNormalBackgroundColor(alpha);
            default:
                return new Color(255, 0, 0, alpha);
        }
    }

    public Color getFontColor(ColorType type, int alpha) {
        return fontColorAnimations[type.getIndex()].getColor(getRawFontColor(type, alpha));
    }

    public Color getFontColor(ColorType type) {
        return getFontColor(type, 255);
    }

    private Color getRawFontColor(ColorType type, int alpha) {
        GlideTheme theme = getTheme();

        switch (type) {
            case DARK:
                return theme.getDarkFontColor(alpha);
            case NORMAL:
                return theme.getNormalFontColor(alpha);
            default:
                return new Color(255, 0, 0, alpha);
        }
    }

    private GlideTheme getTheme() {
        GlideContext instance = GlideContext.getInstance();

        if (instance == null || instance.getColorManager() == null) {
            return GlideTheme.LIGHT;
        }

        return instance.getColorManager().getTheme();
    }

    public Color getMaterialRed(int alpha) {
        return new Color(232, 38, 52, alpha);
    }

    public Color getMaterialYellow(int alpha) {
        return new Color(255, 255, 0, alpha);
    }

    public Color getMaterialRed() {
        return getMaterialRed(255);
    }

    public Color getMaterialYellow() {
        return getMaterialYellow(255);
    }
}

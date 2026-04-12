package keystrokesmod.render.glide.gui.comp;

import java.awt.Color;

import keystrokesmod.render.glide.GlideContext;
import keystrokesmod.render.glide.animation.SimpleAnimation;
import keystrokesmod.render.glide.color.AccentColor;
import keystrokesmod.render.glide.color.ColorPalette;
import keystrokesmod.render.glide.color.ColorType;
import keystrokesmod.render.glide.nanovg.NvgManager;
import keystrokesmod.render.glide.util.MouseUtils;

public class GlideToggleButton {

    private float x, y, width, height;
    private boolean toggled;
    private final SimpleAnimation toggleAnimation;
    private final SimpleAnimation hoverAnimation;

    public GlideToggleButton(float x, float y, float width, float height, boolean toggled) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.toggled = toggled;
        this.toggleAnimation = new SimpleAnimation(toggled ? 1.0F : 0.0F);
        this.hoverAnimation = new SimpleAnimation();
    }

    public GlideToggleButton(float x, float y, float width, float height) {
        this(x, y, width, height, false);
    }

    public void draw(int mouseX, int mouseY, float partialTicks) {
        GlideContext ctx = GlideContext.getInstance();
        NvgManager nvg = ctx.getNvgManager();
        ColorPalette palette = ctx.getColorManager().getPalette();
        AccentColor accent = ctx.getColorManager().getCurrentColor();

        boolean isInside = MouseUtils.isInside(mouseX, mouseY, x, y, width, height);
        hoverAnimation.setAnimation(isInside ? 1.0F : 0.0F, 16);
        toggleAnimation.setAnimation(toggled ? 1.0F : 0.0F, 16);

        float animVal = toggleAnimation.getValue();
        float pillRadius = height / 2F;

        Color trackOff = palette.getBackgroundColor(ColorType.DARK);
        Color trackOn = accent.getInterpolateColor();

        int r = (int) (trackOff.getRed() + (trackOn.getRed() - trackOff.getRed()) * animVal);
        int g = (int) (trackOff.getGreen() + (trackOn.getGreen() - trackOff.getGreen()) * animVal);
        int b = (int) (trackOff.getBlue() + (trackOn.getBlue() - trackOff.getBlue()) * animVal);
        Color trackColor = new Color(
                Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, b)),
                255);

        nvg.drawRoundedRect(x, y, width, height, pillRadius, trackColor);

        float knobPadding = 2F;
        float knobSize = height - (knobPadding * 2);
        float knobMinX = x + knobPadding;
        float knobMaxX = x + width - knobSize - knobPadding;
        float knobX = knobMinX + (knobMaxX - knobMinX) * animVal;

        float hoverScale = 1.0F + (hoverAnimation.getValue() * 0.05F);
        float knobRadius = (knobSize / 2F) * hoverScale;

        nvg.drawCircle(
                knobX + knobSize / 2F,
                y + height / 2F,
                knobRadius,
                Color.WHITE);
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && MouseUtils.isInside(mouseX, mouseY, x, y, width, height)) {
            toggled = !toggled;
        }
    }

    public boolean isToggled() {
        return toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }
}

package keystrokesmod.render.glide.gui.comp;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;

import keystrokesmod.render.glide.GlideContext;
import keystrokesmod.render.glide.animation.SimpleAnimation;
import keystrokesmod.render.glide.color.AccentColor;
import keystrokesmod.render.glide.color.ColorPalette;
import keystrokesmod.render.glide.color.ColorType;
import keystrokesmod.render.glide.nanovg.NvgManager;
import keystrokesmod.render.glide.nanovg.font.NvgFonts;
import keystrokesmod.render.glide.util.MathUtils;
import keystrokesmod.render.glide.util.MouseUtils;

public class GlideSlider {

    private float x, y, width, height;
    private double value, min, max;
    private int decimalPlaces;
    private boolean dragging;
    private final SimpleAnimation handleAnimation;
    private final SimpleAnimation hoverAnimation;

    public GlideSlider(float x, float y, float width, float height, double value, double min, double max, int decimalPlaces) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.value = MathUtils.clamp((float) value, (float) min, (float) max);
        this.min = min;
        this.max = max;
        this.decimalPlaces = decimalPlaces;
        this.dragging = false;
        this.handleAnimation = new SimpleAnimation();
        this.hoverAnimation = new SimpleAnimation();
    }

    public GlideSlider(float x, float y, float width, float height, double value, double min, double max) {
        this(x, y, width, height, value, min, max, 2);
    }

    public void draw(int mouseX, int mouseY, float partialTicks) {
        GlideContext ctx = GlideContext.getInstance();
        NvgManager nvg = ctx.getNvgManager();
        ColorPalette palette = ctx.getColorManager().getPalette();
        AccentColor accent = ctx.getColorManager().getCurrentColor();

        boolean isInside = MouseUtils.isInside(mouseX, mouseY, x, y, width, height);
        hoverAnimation.setAnimation(isInside || dragging ? 1.0F : 0.0F, 16);

        if (dragging) {
            double ratio = (mouseX - x) / (double) width;
            ratio = Math.max(0, Math.min(1, ratio));
            value = min + (max - min) * ratio;
            value = roundToPlace(value, decimalPlaces);
            value = MathUtils.clamp((float) value, (float) min, (float) max);
        }

        float trackHeight = 4F;
        float trackY = y + (height / 2F) - (trackHeight / 2F);
        float trackRadius = trackHeight / 2F;

        nvg.drawRoundedRect(x, trackY, width, trackHeight, trackRadius,
                palette.getBackgroundColor(ColorType.DARK));

        double normalised = (value - min) / (max - min);
        float filledWidth = (float) (width * normalised);

        if (filledWidth > 0) {
            nvg.drawRoundedRect(x, trackY, filledWidth, trackHeight, trackRadius,
                    accent.getInterpolateColor());
        }

        float handleRadius = 5F + (hoverAnimation.getValue() * 2F);
        float handleX = x + filledWidth;
        float handleY = y + (height / 2F);

        nvg.drawCircle(handleX, handleY, handleRadius + 1F,
                new Color(0, 0, 0, (int) (hoverAnimation.getValue() * 40)));
        nvg.drawCircle(handleX, handleY, handleRadius, Color.WHITE);

        String valueText = formatValue(value);
        float textWidth = nvg.getTextWidth(valueText, 7F, NvgFonts.REGULAR);
        nvg.drawText(valueText,
                x + width + 6,
                y + (height / 2F) - (nvg.getTextHeight(valueText, 7F, NvgFonts.REGULAR) / 2F),
                palette.getFontColor(ColorType.NORMAL), 7F, NvgFonts.REGULAR);
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && MouseUtils.isInside(mouseX, mouseY, x, y, width, height)) {
            dragging = true;
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        dragging = false;
    }

    private String formatValue(double val) {
        if (decimalPlaces <= 0) {
            return String.valueOf((int) val);
        }
        return String.valueOf(roundToPlace(val, decimalPlaces));
    }

    private static double roundToPlace(double value, int places) {
        if (places < 0) {
            return value;
        }
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = MathUtils.clamp((float) value, (float) min, (float) max);
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public boolean isDragging() {
        return dragging;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
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

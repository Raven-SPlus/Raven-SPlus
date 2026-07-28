package keystrokesmod.render.glide.nanovg;

import java.awt.Color;

/**
 * Minimal color utilities needed by NvgManager, ported from Glide's ColorUtils.
 * For the full Raven color API, see {@link keystrokesmod.utility.render.ColorUtils}.
 */
public class NvgColorUtils {

    public static Color applyAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static float sin(double value) {
        return (float) Math.sin(value);
    }

    public static float cos(double value) {
        return (float) Math.cos(value);
    }
}

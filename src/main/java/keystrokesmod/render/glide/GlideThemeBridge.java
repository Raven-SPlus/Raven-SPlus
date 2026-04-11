package keystrokesmod.render.glide;

import keystrokesmod.utility.Theme;

import java.awt.Color;

public final class GlideThemeBridge {
    public Color resolveAccent(int gradientMode, double offset) {
        return new Color(Theme.getGradient(gradientMode, offset), true);
    }
}

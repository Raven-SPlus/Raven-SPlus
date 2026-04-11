package keystrokesmod.render;

public enum RenderMode {
    LEGACY,
    GLIDE;

    public static RenderMode fromString(String value, RenderMode fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase();
        for (RenderMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        return fallback;
    }
}

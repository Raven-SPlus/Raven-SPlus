package keystrokesmod.ui.splash;

/**
 * One-shot fade-in overlay for the main menu right after Forge startup splash finishes.
 */
public final class StartupFade {

    private static final long FADE_IN_MS = 550L;

    private static volatile boolean active = false;
    private static volatile long startMs = 0L;

    private StartupFade() {
    }

    public static void trigger() {
        active = true;
        startMs = System.currentTimeMillis();
    }

    public static boolean isActive() {
        return active;
    }

    /**
     * @return alpha in [0..1], where 1 = fully black, 0 = transparent.
     */
    public static float getAlpha01() {
        if (!active) return 0.0F;
        final long now = System.currentTimeMillis();
        final float t = (now - startMs) / (float) FADE_IN_MS;
        if (t >= 1.0F) {
            active = false;
            return 0.0F;
        }
        if (t <= 0.0F) return 1.0F;
        return 1.0F - t;
    }
}


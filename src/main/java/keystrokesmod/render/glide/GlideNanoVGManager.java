package keystrokesmod.render.glide;

/**
 * Raven-side compatibility adapter for Glide's NanoVG manager.
 * This initial migration stage keeps a no-op fallback so LWJGL2 remains stable.
 */
public final class GlideNanoVGManager {
    private final long contextHandle;

    public GlideNanoVGManager() {
        this.contextHandle = 0L;
    }

    public void setupAndDraw(Runnable task, boolean scale) {
        if (task != null) {
            task.run();
        }
    }

    public void setupAndDraw(Runnable task) {
        setupAndDraw(task, true);
    }

    public long getContextHandle() {
        return contextHandle;
    }

    public boolean isAvailable() {
        return contextHandle != 0L;
    }
}

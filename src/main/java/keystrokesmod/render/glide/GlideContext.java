package keystrokesmod.render.glide;

import keystrokesmod.render.glide.adapter.RavenHUDModAdapter;
import keystrokesmod.render.glide.adapter.RavenModAdapter;
import keystrokesmod.render.glide.adapter.RavenProfileAdapter;
import keystrokesmod.render.glide.color.GlideColorManager;
import keystrokesmod.render.glide.event.GlideEventBridge;
import keystrokesmod.render.glide.nanovg.NvgManager;

public final class GlideContext {
    private static final GlideContext INSTANCE = new GlideContext();

    private final GlideRenderCore renderCore = new GlideRenderCore();
    private GlideColorManager colorManager;
    private NvgManager nvgManager;
    private RavenModAdapter modAdapter;
    private RavenProfileAdapter profileAdapter;
    private RavenHUDModAdapter hudModAdapter;
    private GlideEventBridge eventBridge;
    private volatile boolean initialized;
    private volatile Throwable nvgFailure;

    private GlideContext() {
    }

    public static GlideContext getInstance() {
        return INSTANCE;
    }

    /**
     * Full initialization. Must be called once OpenGL context is ready
     * (typically from {@link GlideRenderCore#initialize()}).
     */
    public void init() {
        if (initialized) {
            return;
        }
        colorManager = new GlideColorManager();
        modAdapter = new RavenModAdapter();
        profileAdapter = new RavenProfileAdapter();
        hudModAdapter = new RavenHUDModAdapter();
        eventBridge = new GlideEventBridge();
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public GlideRenderCore getRenderCore() {
        return renderCore;
    }

    public GlideColorManager getColorManager() {
        return colorManager;
    }

    public NvgManager getNvgManager() {
        if (nvgManager != null) {
            return nvgManager;
        }
        if (!ensureNvgReady()) {
            throw new IllegalStateException(getNvgFailureMessage(), nvgFailure);
        }
        return nvgManager;
    }

    public synchronized boolean ensureNvgReady() {
        if (nvgManager != null) {
            return true;
        }
        if (nvgFailure != null) {
            return false;
        }
        try {
            nvgManager = new NvgManager();
            return true;
        } catch (Throwable t) {
            nvgFailure = t;
            return false;
        }
    }

    public String getNvgFailureMessage() {
        if (nvgFailure == null) {
            return "";
        }
        Throwable root = nvgFailure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message == null ? root.getClass().getSimpleName() : message;
    }

    public RavenModAdapter getModAdapter() {
        return modAdapter;
    }

    public RavenProfileAdapter getProfileAdapter() {
        return profileAdapter;
    }

    public RavenHUDModAdapter getHudModAdapter() {
        return hudModAdapter;
    }

    public GlideEventBridge getEventBridge() {
        return eventBridge;
    }
}

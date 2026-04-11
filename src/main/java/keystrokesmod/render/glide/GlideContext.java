package keystrokesmod.render.glide;

public final class GlideContext {
    private static final GlideContext INSTANCE = new GlideContext();

    private final GlideRenderCore renderCore = new GlideRenderCore();

    private GlideContext() {
    }

    public static GlideContext getInstance() {
        return INSTANCE;
    }

    public GlideRenderCore getRenderCore() {
        return renderCore;
    }
}

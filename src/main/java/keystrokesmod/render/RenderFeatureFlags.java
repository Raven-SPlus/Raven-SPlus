package keystrokesmod.render;

public final class RenderFeatureFlags {
    private static final String RENDER_MODE_PROPERTY = "raven.render.mode";
    private static final String GUI_INGAME_DISPATCH_PROPERTY = "raven.render.guiIngameDispatch";

    private static final RenderMode DEFAULT_RENDER_MODE = RenderMode.LEGACY;

    private static volatile RenderMode renderMode = readRenderMode();
    private static volatile boolean guiIngameDispatch = readGuiIngameDispatch();

    private RenderFeatureFlags() {
    }

    public static RenderMode getRenderMode() {
        return renderMode;
    }

    public static boolean isGlideRendererEnabled() {
        return renderMode == RenderMode.GLIDE;
    }

    public static boolean isGuiIngameDispatchEnabled() {
        return guiIngameDispatch;
    }

    public static void reloadFromSystemProperties() {
        renderMode = readRenderMode();
        guiIngameDispatch = readGuiIngameDispatch();
    }

    private static RenderMode readRenderMode() {
        return RenderMode.fromString(System.getProperty(RENDER_MODE_PROPERTY), DEFAULT_RENDER_MODE);
    }

    private static boolean readGuiIngameDispatch() {
        return Boolean.parseBoolean(System.getProperty(GUI_INGAME_DISPATCH_PROPERTY, "true"));
    }
}

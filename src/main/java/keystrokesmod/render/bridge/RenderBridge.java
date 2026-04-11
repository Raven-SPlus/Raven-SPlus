package keystrokesmod.render.bridge;

import keystrokesmod.Raven;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.render.RenderFeatureFlags;
import keystrokesmod.render.RenderMode;
import keystrokesmod.render.glide.GlideContext;
import keystrokesmod.render.glide.GlideRenderCore;
import keystrokesmod.utility.render.blur.HudBlurBatcher;
import net.minecraft.client.Minecraft;

public final class RenderBridge {
    private static final RenderBridge INSTANCE = new RenderBridge();

    private final GlideRenderCore glideRenderCore = GlideContext.getInstance().getRenderCore();
    private volatile boolean initialized;

    private RenderBridge() {
    }

    public static RenderBridge getInstance() {
        return INSTANCE;
    }

    public synchronized void init() {
        if (initialized) {
            return;
        }
        RenderFeatureFlags.reloadFromSystemProperties();
        glideRenderCore.initialize();
        initialized = true;
    }

    public static boolean isGuiIngameDispatchEnabled() {
        return RenderFeatureFlags.isGuiIngameDispatchEnabled();
    }

    public static RenderMode getRenderMode() {
        return RenderFeatureFlags.getRenderMode();
    }

    public void openClickGui(Minecraft mc) {
        if (mc == null) {
            return;
        }
        boolean opened = RenderFeatureFlags.isGlideRendererEnabled() && glideRenderCore.openClickGui(mc);
        if (!opened && Raven.clickGui != null) {
            mc.displayGuiScreen(Raven.clickGui);
            Raven.clickGui.initMain();
        }
    }

    public void openHudEditor(Minecraft mc) {
        if (mc == null) {
            return;
        }
        boolean opened = RenderFeatureFlags.isGlideRendererEnabled() && glideRenderCore.openHudEditor(mc);
        if (!opened) {
            HUD.openLegacyHudEditor();
        }
    }

    public void onGuiOverlayStart() {
        if (!RenderFeatureFlags.isGuiIngameDispatchEnabled()) {
            return;
        }
        glideRenderCore.onOverlayStart();
        HudBlurBatcher.beginOverlayFrame();
    }

    public void onGuiOverlayEnd() {
        if (!RenderFeatureFlags.isGuiIngameDispatchEnabled()) {
            return;
        }
        glideRenderCore.onOverlayEnd();
        HudBlurBatcher.endOverlayFrame();
    }
}

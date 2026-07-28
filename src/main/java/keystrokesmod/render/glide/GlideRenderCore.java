package keystrokesmod.render.glide;

import keystrokesmod.render.glide.blur.GlideShBlur;
import keystrokesmod.render.glide.event.GlideEventBridge;
import keystrokesmod.render.glide.gui.GlideGuiEditHUD;
import keystrokesmod.render.glide.gui.GlideGuiModMenu;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;

public final class GlideRenderCore {
    private final GlideShBlur blur = GlideShBlur.getInstance();
    private final GlideThemeBridge themeBridge = new GlideThemeBridge();
    private boolean warnedUnavailable;

    public void initialize() {
        GlideContext.getInstance().init();
    }

    public boolean openClickGui(Minecraft mc) {
        GlideContext context = GlideContext.getInstance();
        if (!context.isInitialized()) {
            return false;
        }
        if (!context.ensureNvgReady()) {
            warnUnavailable(context);
            return false;
        }
        mc.displayGuiScreen(new GlideGuiModMenu());
        return true;
    }

    public boolean openHudEditor(Minecraft mc) {
        GlideContext context = GlideContext.getInstance();
        if (!context.isInitialized()) {
            return false;
        }
        if (!context.ensureNvgReady()) {
            warnUnavailable(context);
            return false;
        }
        mc.displayGuiScreen(new GlideGuiEditHUD(false));
        return true;
    }

    public void onOverlayStart() {
    }

    public void onOverlayEnd() {
        GlideEventBridge bridge = GlideContext.getInstance().getEventBridge();
        if (bridge != null) {
            float pt = 1.0f;
            try {
                pt = Utils.getTimer().renderPartialTicks;
            } catch (Throwable ignored) {
            }
            bridge.fireRender2D(pt);
            bridge.fireRenderNotification();
        }
    }

    public GlideShBlur getBlur() {
        return blur;
    }

    public GlideThemeBridge getThemeBridge() {
        return themeBridge;
    }

    private void warnUnavailable(GlideContext context) {
        if (warnedUnavailable) {
            return;
        }
        warnedUnavailable = true;
        Utils.sendMessage("&cGlide renderer unavailable: &7" + context.getNvgFailureMessage());
    }
}

package keystrokesmod.render.glide;

import net.minecraft.client.Minecraft;

public final class GlideRenderCore {
    private final GlideNanoVGManager nanoVGManager = new GlideNanoVGManager();
    private final GlideScreenAnimation screenAnimation = new GlideScreenAnimation();
    private final GlideShBlur blur = new GlideShBlur();
    private final GlideThemeBridge themeBridge = new GlideThemeBridge();

    public void initialize() {
        // Reserved for future real NanoVG/Fusion bootstrap.
    }

    public boolean openClickGui(Minecraft mc) {
        // Phase 3 migration seam: fallback to legacy until full screen port lands.
        return false;
    }

    public boolean openHudEditor(Minecraft mc) {
        // Phase 3 migration seam: fallback to legacy until full HUD editor port lands.
        return false;
    }

    public void onOverlayStart() {
    }

    public void onOverlayEnd() {
    }

    public GlideNanoVGManager getNanoVGManager() {
        return nanoVGManager;
    }

    public GlideScreenAnimation getScreenAnimation() {
        return screenAnimation;
    }

    public GlideShBlur getBlur() {
        return blur;
    }

    public GlideThemeBridge getThemeBridge() {
        return themeBridge;
    }
}

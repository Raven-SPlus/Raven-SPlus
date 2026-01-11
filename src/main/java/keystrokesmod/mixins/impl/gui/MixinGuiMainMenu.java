package keystrokesmod.mixins.impl.gui;

import keystrokesmod.altmanager.AltManagerGui;
import keystrokesmod.altmanager.util.AltJsonHandler;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.ui.splash.StartupFade;
import keystrokesmod.utility.font.FontManager;
import keystrokesmod.utility.render.BackgroundUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.io.IOException;


@Mixin(value = GuiMainMenu.class, priority = 1983)
public abstract class MixinGuiMainMenu extends GuiScreen {
    @Unique
    private static final int LOGO_COLOR = new Color(255, 255, 255, 200).getRGB();

    @Shadow private int field_92022_t;

    @Shadow protected abstract boolean func_183501_a();

    @Shadow private GuiScreen field_183503_M;

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    public void onDrawScreen(int p_drawScreen_1_, int p_drawScreen_2_, float p_drawScreen_3_, CallbackInfo ci) {
        if (!ModuleManager.clientTheme.isEnabled() || !ModuleManager.clientTheme.mainMenu.isToggled())
            return;

        BackgroundUtils.renderBackground(this);

        // Draw centered title text - positioned above buttons to align nicely
        // Calculate title position relative to buttons (buttonStartY - spacing for title)
        int buttonStartY = this.height / 2 - 40;
        int titleY = buttonStartY - 70; // Position title higher up for better visual spacing
        FontManager.tenacity80.drawCenteredString("Raven S+", width / 2.0, titleY, LOGO_COLOR);

        // Draw buttons (they are already centered via initGui)
        super.drawScreen(p_drawScreen_1_, p_drawScreen_2_, p_drawScreen_3_);
        if (this.func_183501_a()) {
            this.field_183503_M.drawScreen(p_drawScreen_1_, p_drawScreen_2_, p_drawScreen_3_);
        }

        raven_APlus$drawStartupFadeOverlay();
        ci.cancel();
    }

    /**
     * Vanilla (non-themed) main menu path: draw a one-shot fade overlay at the end of rendering.
     */
    @Inject(method = "drawScreen", at = @At("TAIL"))
    public void raven_APlus$onDrawScreenTail(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        // If the themed path is enabled, the HEAD injection cancels and TAIL won't run.
        // So this only covers vanilla rendering.
        raven_APlus$drawStartupFadeOverlay();
    }

    @Unique
    private void raven_APlus$drawStartupFadeOverlay() {
        if (!StartupFade.isActive()) return;

        final float a01 = StartupFade.getAlpha01();
        if (a01 <= 0.0F) return;

        final int a = (int) (a01 * 255.0F);
        drawRect(0, 0, this.width, this.height, (a << 24));
    }

    @Inject(method = "initGui", at = @At("HEAD"), cancellable = true)
    public void onInitGui(CallbackInfo ci) {
        if (!ModuleManager.clientTheme.isEnabled() || !ModuleManager.clientTheme.mainMenu.isToggled())
            return;

        // Center buttons vertically - start a bit lower for better spacing with title
        int buttonStartY = this.height / 2 - 40;
        int buttonWidth = 200;
        int buttonHeight = 18;
        int buttonSpacing = 22;
        
        // All buttons are centered horizontally
        this.buttonList.add(new GuiButton(1, this.width / 2 - buttonWidth / 2, buttonStartY, buttonWidth, buttonHeight, "SinglePlayer"));
        this.buttonList.add(new GuiButton(2, this.width / 2 - buttonWidth / 2, buttonStartY + buttonSpacing, buttonWidth, buttonHeight, "MultiPlayer"));
        this.buttonList.add(new GuiButton(7, this.width / 2 - buttonWidth / 2, buttonStartY + buttonSpacing * 2, buttonWidth, buttonHeight, "Alt Manager"));
        this.buttonList.add(new GuiButton(6, this.width / 2 - buttonWidth / 2, buttonStartY + buttonSpacing * 3, buttonWidth, buttonHeight, "Mods"));
        
        // Options and Quit buttons - split width but still centered, moved down
        int splitButtonWidth = 98;
        this.buttonList.add(new GuiButton(0, this.width / 2 - splitButtonWidth - 1, buttonStartY + buttonSpacing * 4 + 12, splitButtonWidth, buttonHeight, "Options"));
        this.buttonList.add(new GuiButton(4, this.width / 2 + 1, buttonStartY + buttonSpacing * 4 + 12, splitButtonWidth, buttonHeight, "Quit"));

        this.mc.setConnectedToRealms(false);
        ci.cancel();
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    public void onActionPerformed(GuiButton button, CallbackInfo ci) throws IOException {
        if (!ModuleManager.clientTheme.isEnabled() || !ModuleManager.clientTheme.mainMenu.isToggled())
            return;

        if (button.id == 7) {
            AltJsonHandler.start();
            AltJsonHandler.loadAlts();
            this.mc.displayGuiScreen(new AltManagerGui((GuiMainMenu) (Object) this));
            ci.cancel();
            return;
        }
        
        // Let other buttons work normally - don't cancel for them
    }
}

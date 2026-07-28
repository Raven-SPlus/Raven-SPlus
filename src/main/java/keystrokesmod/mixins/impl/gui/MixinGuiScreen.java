package keystrokesmod.mixins.impl.gui;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.ClickGUI;
import keystrokesmod.module.impl.render.NoBackground;
import keystrokesmod.module.impl.player.ChestStealer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.render.BackgroundUtils;
import keystrokesmod.utility.render.blur.BlurStencilProvider;
import keystrokesmod.utility.render.blur.GlobalBlurManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {
    @Shadow
    protected Minecraft mc;

    @Shadow
    protected List<GuiButton> buttonList;

    @Unique
    private boolean ravenAPlus$batchedButtonBlurDone;

    @Inject(method = "drawDefaultBackground", at = @At("HEAD"), cancellable = true)
    public void onDrawDefaultBackground(CallbackInfo ci) {
        if (Utils.nullCheck() && (NoBackground.noRender() || ChestStealer.noChestRender()))
            ci.cancel();
    }

    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    public void onDrawBackground(int p_drawWorldBackground_1_, @NotNull CallbackInfo ci) {
        if (!ModuleManager.clientTheme.isEnabled() || !ModuleManager.clientTheme.background.isToggled())
            return;

        BackgroundUtils.renderBackground((GuiScreen) (Object) this);
        ci.cancel();
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void ravenAPlus$resetBatchedBlur(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        ravenAPlus$batchedButtonBlurDone = false;
    }

    /**
     * Batch button blur once per screen render:
     * - Build a stencil mask for all buttons (single pass).
     * - Run the blur shader once.
     * - Let buttons render normally on top.
     */
    @Redirect(
            method = "drawScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiButton;drawButton(Lnet/minecraft/client/Minecraft;II)V"
            )
    )
    private void ravenAPlus$batchBlurAndDrawButton(GuiButton button, Minecraft minecraft, int mouseX, int mouseY) {
        if (!ravenAPlus$batchedButtonBlurDone) {
            boolean clientThemeReady = ModuleManager.clientTheme != null;
            boolean clickGuiReady = ClickGUI.blurButtons != null && ClickGUI.buttonBlurStrength != null;
            boolean themedButtonsEnabled = clientThemeReady
                    && ModuleManager.clientTheme.isEnabled()
                    && ModuleManager.clientTheme.button.isToggled();
            boolean shouldBlur = clickGuiReady && ClickGUI.blurButtons.isToggled();
            if (!shouldBlur && themedButtonsEnabled && ModuleManager.clientTheme.buttonBlur != null) {
                shouldBlur = ModuleManager.clientTheme.buttonBlur.isToggled();
            }

            if (shouldBlur && this.buttonList != null && !this.buttonList.isEmpty()) {
                int blurRadius;
                if (clickGuiReady && ClickGUI.blurButtons.isToggled()) {
                    blurRadius = (int) ClickGUI.buttonBlurStrength.getInput();
                } else if (clientThemeReady && ModuleManager.clientTheme.blurStrength != null) {
                    blurRadius = (int) ModuleManager.clientTheme.blurStrength.getInput();
                } else {
                    blurRadius = 12;
                }
                float compression = blurRadius / 4.0f;

                // Pre-pass: write all button shapes to stencil (no color writes).
                GlobalBlurManager.startBlur();
                if (GlobalBlurManager.isBlurActive()) {
                    for (GuiButton b : this.buttonList) {
                        if (b == null || !b.visible) {
                            continue;
                        }

                        if (b instanceof BlurStencilProvider) {
                            ((BlurStencilProvider) b).ravenAPlus$writeButtonBlurStencil(mouseX, mouseY, true);
                        } else {
                            Gui.drawRect(b.xPosition, b.yPosition, b.xPosition + b.width, b.yPosition + b.height, -1);
                        }
                    }
                    GlobalBlurManager.endBlur(blurRadius, compression);
                }
            }
            ravenAPlus$batchedButtonBlurDone = true;
        }

        // Normal draw (our GuiButton mixin may cancel and render themed buttons here).
        button.drawButton(minecraft, mouseX, mouseY);
    }
}

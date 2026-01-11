package keystrokesmod.mixins.impl.gui;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.ClickGUI;
import keystrokesmod.utility.font.FontManager;
import keystrokesmod.utility.font.IFont;
import keystrokesmod.utility.render.*;
import keystrokesmod.utility.render.blur.BlurStencilProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.opengl.Display;

import java.awt.*;

@Mixin(GuiButton.class)
public abstract class MixinGuiButton extends Gui implements BlurStencilProvider {

    @Shadow public boolean visible;

    @Shadow @Final protected static ResourceLocation buttonTextures;

    @Shadow protected boolean hovered;

    @Shadow public int xPosition;

    @Shadow public int yPosition;

    @Shadow public int width;

    @Shadow public int height;

    @Shadow protected abstract int getHoverState(boolean p_getHoverState_1_);

    @Shadow protected abstract void mouseDragged(Minecraft p_mouseDragged_1_, int p_mouseDragged_2_, int p_mouseDragged_3_);

    @Shadow public boolean enabled;

    @Shadow public String displayString;

    @Unique
    private int ravenAPlus$hoverValue;

    @Unique
    private static double ravenAPlus$hoverStep() {
        // getDebugFPS() can drop to 0 / very low when the window is unfocused.
        // Clamp to avoid infinity / huge jumps (which make buttons suddenly look much darker).
        int fps = Minecraft.getDebugFPS();
        if (fps <= 0) fps = 60;
        fps = Math.max(10, Math.min(240, fps));
        double step = 4.0 * 150.0 / fps;
        return Math.min(step, 10.0);
    }

    @Unique
    private static boolean ravenAPlus$shouldBlurThisButton(boolean shouldBlurSettingEnabled, boolean hovered) {
        // Batched blur is handled at the GuiScreen level; per-button we only need to know if this
        // button should contribute a blur stencil.
        return shouldBlurSettingEnabled;
    }

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    public void onDrawButton(Minecraft minecraft, int x, int y, CallbackInfo ci) {
        if (!ModuleManager.clientTheme.isEnabled() || !ModuleManager.clientTheme.button.isToggled())
            return;

        if (this.visible) {
            IFont font = ModuleManager.clientTheme.smoothFont.isToggled() ? FontManager.tenacity20 : FontManager.getMinecraft();
            this.hovered = x >= this.xPosition && y >= this.yPosition && x < this.xPosition + this.width && y < this.yPosition + this.height;

            final double step = ravenAPlus$hoverStep();
            if (hovered) {
                ravenAPlus$hoverValue = (int) Math.min(ravenAPlus$hoverValue + step, 200);
            } else {
                ravenAPlus$hoverValue = (int) Math.max(ravenAPlus$hoverValue - step, 102);
            }

            Color rectColor = new Color(35, 37, 43, ravenAPlus$hoverValue);
            rectColor = raven_APlus$interpolateColorC(rectColor, ColorUtils.brighter(rectColor, 0.4f), -1);
            // When the window is inactive, Minecraft often reduces render rate and the UI can look harsher.
            // Slightly reduce the shadow alpha in that state to avoid the "sudden super dark halo" look.
            int shadowAlpha = Display.isActive() ? 50 : 22;
            RenderUtils.drawBloomShadow(xPosition - 3, yPosition - 3, width + 6, height + 6, 12, new Color(0, 0, 0, shadowAlpha), false);
            
            // Check if blur is enabled from either ClientTheme or ClickGUI module
            boolean shouldBlur = ModuleManager.clientTheme.buttonBlur.isToggled() || ClickGUI.blurButtons.isToggled();
            
            // Blur is now applied once per screen draw (batched). Here we only draw the visible layer.
            // If blur is disabled, this still looks correct (just without the glass effect).
            if (ravenAPlus$shouldBlurThisButton(shouldBlur, this.hovered)) {
                // no-op: blur is handled by GuiScreen batching
            }
            RRectUtils.drawRoundOutline(xPosition, this.yPosition, width, height, 3.5F, 0.0015f, rectColor, new Color(30, 30, 30, 100));

            this.mouseDragged(minecraft, x, y);

            font.drawCenteredString(ModuleManager.clientTheme.buttonLowerCase.isToggled() ? displayString.toLowerCase() : displayString, this.xPosition + this.width / 2.0f, this.yPosition + height / 2f - font.height() / 2f, -1);
        }

        ci.cancel();
    }

    @Unique
    @Contract("_, _, _ -> new")
    private static @NotNull Color raven_APlus$interpolateColorC(final @NotNull Color color1, final @NotNull Color color2, float amount) {
        amount = Math.min(1.0f, Math.max(0.0f, amount));
        return new Color(ColorUtils.interpolateInt(color1.getRed(), color2.getRed(), amount), ColorUtils.interpolateInt(color1.getGreen(), color2.getGreen(), amount), ColorUtils.interpolateInt(color1.getBlue(), color2.getBlue(), amount), ColorUtils.interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    @Override
    public void ravenAPlus$writeButtonBlurStencil(int mouseX, int mouseY, boolean shouldBlurSettingEnabled) {
        if (!this.visible) return;
        if (!shouldBlurSettingEnabled) return;
        if (!ravenAPlus$shouldBlurThisButton(true, false)) return;

        // Stencil-only: color writes are disabled during the batched blur pre-pass.
        RRectUtils.drawRound(this.xPosition, this.yPosition, this.width, this.height, 3.5F, new Color(255, 255, 255, 255));
    }
}

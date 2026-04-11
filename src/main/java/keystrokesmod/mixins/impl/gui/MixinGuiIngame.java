package keystrokesmod.mixins.impl.gui;

import keystrokesmod.render.bridge.RenderBridge;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public class MixinGuiIngame {
    @Inject(method = "renderGameOverlay", at = @At("HEAD"))
    private void ravenAPlus$beginOverlay(float partialTicks, CallbackInfo ci) {
        RenderBridge.getInstance().onGuiOverlayStart();
    }

    @Inject(method = "renderGameOverlay", at = @At("RETURN"))
    private void ravenAPlus$endOverlay(float partialTicks, CallbackInfo ci) {
        RenderBridge.getInstance().onGuiOverlayEnd();
    }
}

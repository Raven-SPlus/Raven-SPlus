package keystrokesmod.mixins.impl.client;

import keystrokesmod.module.impl.client.memoryfix.MemoryFixHelper;
import keystrokesmod.module.impl.client.memoryfix.ResourcePackImageScaler;
import net.minecraft.client.resources.AbstractResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.image.BufferedImage;

@Mixin(AbstractResourcePack.class)
public abstract class MixinAbstractResourcePack {
    @Inject(method = "getPackImage", at = @At("RETURN"), cancellable = true, require = 0)
    private void onGetPackImage(CallbackInfoReturnable<BufferedImage> cir) {
        scaleReturnValue(cir);
    }

    @Inject(method = "func_110586_a", at = @At("RETURN"), cancellable = true, require = 0)
    private void onGetPackImageObfuscated(CallbackInfoReturnable<BufferedImage> cir) {
        scaleReturnValue(cir);
    }

    private void scaleReturnValue(CallbackInfoReturnable<BufferedImage> cir) {
        if (MemoryFixHelper.shouldScalePackIcons()) {
            cir.setReturnValue(ResourcePackImageScaler.scalePackImage(cir.getReturnValue()));
        }
    }
}

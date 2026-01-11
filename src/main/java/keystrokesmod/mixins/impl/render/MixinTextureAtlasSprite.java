package keystrokesmod.mixins.impl.render;

import keystrokesmod.utility.render.SpriteUsageAccess;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureAtlasSprite.class)
public abstract class MixinTextureAtlasSprite implements SpriteUsageAccess {
    @Unique
    private boolean raven$used;

    @Override
    public boolean raven$isUsed() {
        return raven$used;
    }

    @Override
    public void raven$markUsed() {
        raven$used = true;
    }

    @Inject(method = "getMinU", at = @At("HEAD"))
    private void raven$markUsed_getMinU(CallbackInfoReturnable<Float> cir) {
        if (!raven$used) raven$used = true;
    }

    @Inject(method = "getMaxU", at = @At("HEAD"))
    private void raven$markUsed_getMaxU(CallbackInfoReturnable<Float> cir) {
        if (!raven$used) raven$used = true;
    }

    @Inject(method = "getMinV", at = @At("HEAD"))
    private void raven$markUsed_getMinV(CallbackInfoReturnable<Float> cir) {
        if (!raven$used) raven$used = true;
    }

    @Inject(method = "getMaxV", at = @At("HEAD"))
    private void raven$markUsed_getMaxV(CallbackInfoReturnable<Float> cir) {
        if (!raven$used) raven$used = true;
    }

    @Inject(method = "getInterpolatedU", at = @At("HEAD"))
    private void raven$markUsed_getInterpolatedU(double u, CallbackInfoReturnable<Float> cir) {
        if (!raven$used) raven$used = true;
    }

    @Inject(method = "getInterpolatedV", at = @At("HEAD"))
    private void raven$markUsed_getInterpolatedV(double v, CallbackInfoReturnable<Float> cir) {
        if (!raven$used) raven$used = true;
    }
}


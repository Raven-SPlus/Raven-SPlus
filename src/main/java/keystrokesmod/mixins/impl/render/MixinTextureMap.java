package keystrokesmod.mixins.impl.render;

import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.utility.render.SpriteUsageAccess;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextureMap.class)
public abstract class MixinTextureMap {
    @Redirect(
            method = "updateAnimations",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;updateAnimation()V")
    )
    private void raven$skipUnusedAnimatedTextures(TextureAtlasSprite sprite) {
        // Default to vanilla behavior until settings are initialized.
        if (Settings.smartAnimatedTextures == null || !Settings.smartAnimatedTextures.isToggled()) {
            sprite.updateAnimation();
            return;
        }

        if (!(sprite instanceof SpriteUsageAccess)) {
            sprite.updateAnimation();
            return;
        }

        if (((SpriteUsageAccess) sprite).raven$isUsed()) {
            sprite.updateAnimation();
        }
    }
}


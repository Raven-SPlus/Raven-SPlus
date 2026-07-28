package keystrokesmod.mixins.impl.network;

import keystrokesmod.module.impl.exploit.viaversionfix.ViaVersionFixHelper;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(C08PacketPlayerBlockPlacement.class)
public abstract class MixinC08PacketPlayerBlockPlacement {
    @Shadow private float facingX;
    @Shadow private float facingY;
    @Shadow private float facingZ;

    @Inject(method = "readPacketData", at = @At("RETURN"))
    private void onReadPacketData(PacketBuffer buf, CallbackInfo ci) throws IOException {
        if (ViaVersionFixHelper.isFixRightClickPacket()) {
            this.facingX *= 16.0F;
            this.facingY *= 16.0F;
            this.facingZ *= 16.0F;
        }
    }

    @Inject(method = "writePacketData", at = @At("HEAD"))
    private void onWritePacketData(PacketBuffer buf, CallbackInfo ci) throws IOException {
        if (ViaVersionFixHelper.isFixRightClickPacket()) {
            this.facingX /= 16.0F;
            this.facingY /= 16.0F;
            this.facingZ /= 16.0F;
        }
    }

    @Inject(method = "writePacketData", at = @At("RETURN"))
    private void afterWritePacketData(PacketBuffer buf, CallbackInfo ci) throws IOException {
        if (ViaVersionFixHelper.isFixRightClickPacket()) {
            this.facingX *= 16.0F;
            this.facingY *= 16.0F;
            this.facingZ *= 16.0F;
        }
    }
}

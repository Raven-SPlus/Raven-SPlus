package keystrokesmod.mixins.impl.entity;

import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.event.SafeWalkEvent;
import keystrokesmod.event.StepEvent;
import keystrokesmod.module.impl.other.RotationHandler;
import keystrokesmod.utility.Utils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.*;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow public double motionX;

    @Shadow public double motionZ;

    @Shadow public abstract AxisAlignedBB getEntityBoundingBox();

    @Shadow public double posY;

    @Redirect(method = "moveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSneaking()Z"))
    public boolean onSafeWalk(@NotNull Entity instance) {
        if (instance instanceof EntityPlayerSP) {
            SafeWalkEvent event = new SafeWalkEvent(instance.isSneaking());
            MinecraftForge.EVENT_BUS.post(event);
            return event.isSafeWalk();
        }
        return instance.isSneaking();
    }

    @Inject(method = "moveEntity(DDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setEntityBoundingBox(Lnet/minecraft/util/AxisAlignedBB;)V", ordinal = 8, shift = At.Shift.BY, by = 2))
    public void onPostStep(double x, double y, double z, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new StepEvent(this.getEntityBoundingBox().minY - this.posY));
    }

    /**
     * @author strangerrs
     * @reason moveFlying mixin with Continuous MoveFix support
     */
    @Inject(method = "moveFlying", at = @At("HEAD"), cancellable = true)
    public void moveFlying(float p_moveFlying_1_, float p_moveFlying_2_, float p_moveFlying_3_, CallbackInfo ci) {
        float yaw = ((Entity)(Object) this).rotationYaw;
        if((Object) this instanceof EntityPlayerSP) {
            float serverYaw = RotationHandler.getMovementYaw((Entity) (Object) this);
            
            // Continuous MoveFix: apply rotation correction here using continuous math.
            // Instead of snapping inputs to {-1, 0, 1} (which causes speed jumps that
            // trigger Intave's Gate 1 motion check), we rotate the strafe/forward vector
            // by the yaw difference WITHOUT rounding. The rotation matrix preserves exact
            // vector magnitude, so XZ speed stays perfectly constant at terminal velocity.
            // Reference: FireBounce's applyStrafeToPlayer (Rotation.kt).
            if (RotationHandler.getMoveFix() == RotationHandler.MoveFix.Continuous) {
                float clientYaw = ((Entity)(Object) this).rotationYaw;
                float diff = (clientYaw - serverYaw) * 3.1415927F / 180.0F;
                float cosD = MathHelper.cos(diff);
                float sinD = MathHelper.sin(diff);
                
                // Rotate inputs by yaw difference (continuous, no rounding)
                float newForward = p_moveFlying_2_ * cosD + p_moveFlying_1_ * sinD;
                float newStrafe  = p_moveFlying_1_ * cosD - p_moveFlying_2_ * sinD;
                
                p_moveFlying_1_ = newStrafe;
                p_moveFlying_2_ = newForward;
                yaw = serverYaw; // Use server yaw for the final sin/cos
                
                // Fire event with already-corrected values
                PrePlayerInputEvent prePlayerInput = new PrePlayerInputEvent(p_moveFlying_1_, p_moveFlying_2_, p_moveFlying_3_, yaw);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(prePlayerInput);
                if (prePlayerInput.isCanceled()) {
                    ci.cancel();
                    return;
                }
                p_moveFlying_1_ = prePlayerInput.getStrafe();
                p_moveFlying_2_ = prePlayerInput.getForward();
                p_moveFlying_3_ = prePlayerInput.getFriction();
                yaw = prePlayerInput.getYaw();
            } else {
                // Standard path: RotationHandler already adjusted inputs for Silent mode
                PrePlayerInputEvent prePlayerInput = new PrePlayerInputEvent(p_moveFlying_1_, p_moveFlying_2_, p_moveFlying_3_, serverYaw);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(prePlayerInput);
                if (prePlayerInput.isCanceled()) {
                    ci.cancel();
                    return;
                }
                p_moveFlying_1_ = prePlayerInput.getStrafe();
                p_moveFlying_2_ = prePlayerInput.getForward();
                p_moveFlying_3_ = prePlayerInput.getFriction();
                yaw = prePlayerInput.getYaw();
            }
        }

        float f = p_moveFlying_1_ * p_moveFlying_1_ + p_moveFlying_2_ * p_moveFlying_2_;
        if (f >= 1.0E-4F) {
            f = MathHelper.sqrt_float(f);
            if (f < 1.0F) {
                f = 1.0F;
            }

            f = p_moveFlying_3_ / f;
            p_moveFlying_1_ *= f;
            p_moveFlying_2_ *= f;
            float f1 = MathHelper.sin(yaw * 3.1415927F / 180.0F);
            float f2 = MathHelper.cos(yaw * 3.1415927F / 180.0F);
            this.motionX += p_moveFlying_1_ * f2 - p_moveFlying_2_ * f1;
            this.motionZ += p_moveFlying_2_ * f2 + p_moveFlying_1_ * f1;
        }
        ci.cancel();
    }

    @Redirect(method = "rayTrace", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getLook(F)Lnet/minecraft/util/Vec3;"))
    public Vec3 onGetLook(Entity instance, float partialTicks) {
        return RotationHandler.getLook(partialTicks);
    }
}
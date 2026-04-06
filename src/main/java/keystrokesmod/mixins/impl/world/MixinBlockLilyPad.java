package keystrokesmod.mixins.impl.world;

import keystrokesmod.module.impl.exploit.viaversionfix.ViaVersionFixHelper;
import net.minecraft.block.BlockLilyPad;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockLilyPad.class)
public abstract class MixinBlockLilyPad {
    @Inject(method = "getCollisionBoundingBox", at = @At("HEAD"), cancellable = true)
    private void onGetCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state, CallbackInfoReturnable<AxisAlignedBB> cir) {
        if (ViaVersionFixHelper.isFixLilyPadCollision()) {
            cir.setReturnValue(new AxisAlignedBB(
                    pos.getX() + 0.0625D,
                    pos.getY(),
                    pos.getZ() + 0.0625D,
                    pos.getX() + 0.9375D,
                    pos.getY() + 0.09375D,
                    pos.getZ() + 0.9375D
            ));
        }
    }
}

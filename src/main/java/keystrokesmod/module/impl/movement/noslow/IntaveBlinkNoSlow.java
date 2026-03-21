package keystrokesmod.module.impl.movement.noslow;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.impl.movement.NoSlow;
import keystrokesmod.module.impl.other.SlotHandler;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

/**
 * IntaveBlink NoSlow - ported from FireBounce.
 * Uses release/place packet blink when using item to bypass Intave.
 */
public class IntaveBlinkNoSlow extends INoSlow {

    private boolean shouldBlink = false;

    public IntaveBlinkNoSlow(String name, @NotNull NoSlow parent) {
        super(name, parent);
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (!Utils.nullCheck() || SlotHandler.getHeldItem() == null) return;

        boolean isUsingItem = mc.thePlayer.isUsingItem();

        if (!isUsingItem) {
            shouldBlink = false;
            return;
        }

        if (mc.thePlayer.ticksExisted % 5 == 0) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(SlotHandler.getHeldItem()));
            shouldBlink = true;
        }
    }

    @Override
    public void onDisable() {
        shouldBlink = false;
    }

    @Override
    public float getSlowdown() {
        return 1;
    }
}

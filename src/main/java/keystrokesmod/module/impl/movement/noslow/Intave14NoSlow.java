package keystrokesmod.module.impl.movement.noslow;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.impl.movement.NoSlow;
import keystrokesmod.module.impl.other.SlotHandler;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Intave14 NoSlow - ported from FireBounce.
 * Sends C07 RELEASE_USE_ITEM periodically when using item to bypass Intave14.
 */
public class Intave14NoSlow extends INoSlow {

    private long lastReleaseTime = 0;

    public Intave14NoSlow(String name, @NotNull NoSlow parent) {
        super(name, parent);
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (!Utils.nullCheck() || !mc.thePlayer.isUsingItem() || SlotHandler.getHeldItem() == null) return;

        long now = System.currentTimeMillis();
        if (now - lastReleaseTime >= 800) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            lastReleaseTime = now;
        }
        PacketUtils.sendPacket(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
    }

    @Override
    public void onDisable() {
        lastReleaseTime = 0;
    }

    @Override
    public float getSlowdown() {
        return 1;
    }
}

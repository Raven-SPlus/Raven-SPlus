package keystrokesmod.module.impl.player.nofall;

import keystrokesmod.event.PostUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.impl.player.NoFall;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class MatrixNoFall extends SubMode<NoFall> {
    private boolean matrixSend = false;
    private boolean timerWasSet = false;

    public MatrixNoFall(String name, @NotNull NoFall parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        matrixSend = false;
        timerWasSet = false;
    }

    @SubscribeEvent
    public void onUpdate(keystrokesmod.event.PostUpdateEvent event) {
        if (parent.noAction()) return;

        if (timerWasSet && mc.thePlayer.onGround) {
            Utils.resetTimer();
            timerWasSet = false;
        }

        if (mc.thePlayer.fallDistance - mc.thePlayer.motionY > 3) {
            mc.thePlayer.fallDistance = 0.0f;
            matrixSend = true;
            Utils.getTimer().timerSpeed = 0.5f;
            timerWasSet = true;
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent event) {
        if (event.getPacket() instanceof C03PacketPlayer && matrixSend) {
            matrixSend = false;
            event.setCanceled(true);
            
            C03PacketPlayer packet = (C03PacketPlayer) event.getPacket();
            PacketUtils.sendPacketNoEvent(new C04PacketPlayerPosition(packet.getPositionX(), packet.getPositionY(), packet.getPositionZ(), true));
            PacketUtils.sendPacketNoEvent(new C04PacketPlayerPosition(packet.getPositionX(), packet.getPositionY(), packet.getPositionZ(), false));
        }
    }

    @Override
    public void onDisable() {
        if (timerWasSet) {
            Utils.resetTimer();
            timerWasSet = false;
        }
    }
}

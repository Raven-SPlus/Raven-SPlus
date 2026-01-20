package keystrokesmod.module.impl.player.nofall;

import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.impl.player.NoFall;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class MatrixSpoofNoFall extends SubMode<NoFall> {
    private final SliderSetting minFallDistance;
    private final ButtonSetting legitTimer;

    private boolean timered = false;

    public MatrixSpoofNoFall(String name, @NotNull NoFall parent) {
        super(name, parent);
        this.registerSetting(minFallDistance = new SliderSetting("Min fall distance", 3, 0, 50, 0.5));
        this.registerSetting(legitTimer = new ButtonSetting("Legit timer", true));
    }

    @Override
    public void onEnable() {
        timered = false;
    }

    @Override
    public void onDisable() {
        if (timered) {
            Utils.resetTimer();
            timered = false;
        }
    }

    private boolean fallDamage() {
        return mc.thePlayer.fallDistance - mc.thePlayer.motionY > minFallDistance.getInput();
    }

    private boolean inVoidCheck() {
        if (parent.noAction()) return true;
        return !isInVoid();
    }

    private boolean isInVoid() {
        int px = (int) Math.floor(mc.thePlayer.posX);
        int pz = (int) Math.floor(mc.thePlayer.posZ);
        int startY = (int) Math.floor(mc.thePlayer.posY);

        for (int y = startY; y >= 0; y--) {
            Block block = mc.theWorld.getBlockState(new BlockPos(px, y, pz)).getBlock();
            if (block != Blocks.air) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent event) {
        try {
            if (fallDamage() && inVoidCheck()) {
                if (event.getPacket() instanceof C03PacketPlayer) {
                    event.setCanceled(true);

                    double px = mc.thePlayer.posX;
                    double py = mc.thePlayer.posY;
                    double pz = mc.thePlayer.posZ;

                    PacketUtils.sendPacketNoEvent(new C04PacketPlayerPosition(px, py, pz, true));
                    PacketUtils.sendPacketNoEvent(new C04PacketPlayerPosition(px, py, pz, false));

                    mc.thePlayer.fallDistance = 0f;

                    if (legitTimer.isToggled()) {
                        timered = true;
                        Utils.getTimer().timerSpeed = 0.2f;
                    }
                }
            } else if (timered) {
                Utils.resetTimer();
                timered = false;
            }
        } catch (Throwable ignored) {}
    }
}

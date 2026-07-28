package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * FastUse - fast eating/drinking - ported from FireBounce.
 * Modes: Instant, NCP, AAC, OldIntave, Matrix
 */
public class FastUse extends Module {

    private final ModeSetting mode;
    private final SliderSetting lowTimer;
    private final SliderSetting maxTimer;
    private final SliderSetting ticks;
    private final ButtonSetting noMove;

    private int isEating = 10;
    private boolean reset = false;
    private boolean usedTimer = false;

    public FastUse() {
        super("FastUse", category.player);
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Instant", "NCP", "AAC", "OldIntave", "Matrix"}, 1));
        this.registerSetting(lowTimer = new SliderSetting("Low timer", 0.3, 0.01, 1, 0.01, () -> mode.getInput() == 3));
        this.registerSetting(maxTimer = new SliderSetting("Max timer", 0.3, 0.01, 1, 0.01, () -> mode.getInput() == 3));
        this.registerSetting(ticks = new SliderSetting("Ticks", 1, 1, 20, 1, () -> mode.getInput() == 3));
        this.registerSetting(noMove = new ButtonSetting("No move", false));
    }

    private boolean isConsumingItem() {
        if (mc.thePlayer == null || mc.thePlayer.getHeldItem() == null) return false;
        return mc.thePlayer.isUsingItem();
    }

    @Override
    public void onDisable() {
        if (usedTimer) {
            Utils.getTimer().timerSpeed = 1.0f;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null) return;

        if (usedTimer) {
            Utils.getTimer().timerSpeed = 1.0f;
            usedTimer = false;
        }

        // OldIntave mode - Intave bypass for fast use
        if (mode.getInput() == 3) {
            if (isConsumingItem()) {
                reset = false;
                if (isEating >= 1) {
                    isEating--;
                    Utils.getTimer().timerSpeed = (float) lowTimer.getInput();
                    usedTimer = true;
                } else {
                    isEating = (int) ticks.getInput();
                    Utils.getTimer().timerSpeed = (float) maxTimer.getInput();
                    usedTimer = true;
                }
            } else {
                isEating = (int) ticks.getInput();
                if (!reset) {
                    Utils.getTimer().timerSpeed = 1.0f;
                    usedTimer = false;
                    reset = true;
                }
            }
            return;
        }

        if (!isConsumingItem()) return;

        switch ((int) mode.getInput()) {
            case 0: // Instant
                for (int i = 0; i < 35; i++) {
                    PacketUtils.sendPacketNoEvent(new C03PacketPlayer(mc.thePlayer.onGround));
                }
                mc.playerController.onStoppedUsingItem(mc.thePlayer);
                break;
            case 1: // NCP
                if (mc.thePlayer.getItemInUseCount() > 14) {
                    for (int i = 0; i < 20; i++) {
                        PacketUtils.sendPacketNoEvent(new C03PacketPlayer(mc.thePlayer.onGround));
                    }
                    mc.playerController.onStoppedUsingItem(mc.thePlayer);
                }
                break;
            case 2: // AAC
                Utils.getTimer().timerSpeed = 1.22f;
                usedTimer = true;
                break;
            case 4: // Matrix
                Utils.getTimer().timerSpeed = 0.5f;
                usedTimer = true;
                PacketUtils.sendPacketNoEvent(new C03PacketPlayer(mc.thePlayer.onGround));
                break;
        }
    }
}

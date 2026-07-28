package keystrokesmod.module.impl.world.tower;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.impl.world.Tower;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.TimerUtil;
import net.minecraft.stats.StatList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class TeleportTower extends SubMode<Tower> {
    private final SliderSetting teleportHeight;
    private final SliderSetting teleportDelay;
    private final ButtonSetting teleportGround;
    private final ButtonSetting teleportNoMotion;
    private final TimerUtil timer = new TimerUtil();

    public TeleportTower(String name, @NotNull Tower parent) {
        super(name, parent);
        this.registerSetting(teleportHeight = new SliderSetting("Teleport height", 1.15f, 0.1f, 5f, 0.01f));
        this.registerSetting(teleportDelay = new SliderSetting("Teleport delay", 0, 0, 20, 1));
        this.registerSetting(teleportGround = new ButtonSetting("Teleport ground", true));
        this.registerSetting(teleportNoMotion = new ButtonSetting("Teleport no motion", false));
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (parent.canTower()) {
            if (teleportNoMotion.isToggled()) {
                mc.thePlayer.motionY = 0.0;
            }
            if ((mc.thePlayer.onGround || !teleportGround.isToggled()) && 
                timer.hasTimeElapsed((long)teleportDelay.getInput() * 50)) {
                fakeJump();
                mc.thePlayer.setPositionAndUpdate(
                    mc.thePlayer.posX, 
                    mc.thePlayer.posY + teleportHeight.getInput(), 
                    mc.thePlayer.posZ
                );
                timer.reset();
            }
        }
    }

    private void fakeJump() {
        mc.thePlayer.isAirBorne = true;
        mc.thePlayer.triggerAchievement(StatList.jumpStat);
    }
}
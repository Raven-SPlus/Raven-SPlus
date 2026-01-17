package keystrokesmod.module.impl.world.tower;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.impl.world.Tower;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.MoveUtil;
import net.minecraft.stats.StatList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class ConstantMotionTower extends SubMode<Tower> {
    private final SliderSetting motion;
    private final SliderSetting jumpGround;
    private final ButtonSetting jumpPacket;
    private double jumpGroundPos = 0.0;

    public ConstantMotionTower(String name, @NotNull Tower parent) {
        super(name, parent);
        this.registerSetting(motion = new SliderSetting("Motion", 0.42f, 0.1f, 1f, 0.01f));
        this.registerSetting(jumpGround = new SliderSetting("Jump ground", 0.79f, 0.76f, 1f, 0.01f));
        this.registerSetting(jumpPacket = new ButtonSetting("Jump packet", true));
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (parent.canTower()) {
            if (mc.thePlayer.onGround) {
                if (jumpPacket.isToggled()) {
                    fakeJump();
                }
                jumpGroundPos = mc.thePlayer.posY;
                mc.thePlayer.motionY = motion.getInput();
            }
            if (mc.thePlayer.posY > jumpGroundPos + jumpGround.getInput()) {
                if (jumpPacket.isToggled()) {
                    fakeJump();
                }
                mc.thePlayer.setPosition(
                    mc.thePlayer.posX, 
                    Math.floor(mc.thePlayer.posY), 
                    mc.thePlayer.posZ
                );
                mc.thePlayer.motionY = motion.getInput();
                jumpGroundPos = mc.thePlayer.posY;
            }
        }
    }

    private void fakeJump() {
        mc.thePlayer.isAirBorne = true;
        mc.thePlayer.triggerAchievement(StatList.jumpStat);
    }
}

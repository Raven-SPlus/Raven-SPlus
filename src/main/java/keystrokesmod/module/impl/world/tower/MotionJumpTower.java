package keystrokesmod.module.impl.world.tower;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.impl.world.Tower;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.TimerUtil;
import net.minecraft.stats.StatList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class MotionJumpTower extends SubMode<Tower> {
    private final SliderSetting jumpMotion;
    private final SliderSetting jumpDelay;
    private final TimerUtil timer = new TimerUtil();

    public MotionJumpTower(String name, @NotNull Tower parent) {
        super(name, parent);
        this.registerSetting(jumpMotion = new SliderSetting("Jump motion", 0.42f, 0.3681289f, 0.79f, 0.01f));
        this.registerSetting(jumpDelay = new SliderSetting("Jump delay", 0, 0, 20, 1));
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (parent.canTower()) {
            if (mc.thePlayer.onGround && timer.hasTimeElapsed((long)jumpDelay.getInput() * 50)) {
                fakeJump();
                mc.thePlayer.motionY = jumpMotion.getInput();
                timer.reset();
            }
        } else if (!mc.thePlayer.onGround) {
            mc.thePlayer.isAirBorne = false;
            timer.reset();
        }
    }

    private void fakeJump() {
        mc.thePlayer.isAirBorne = true;
        mc.thePlayer.triggerAchievement(StatList.jumpStat);
    }
}
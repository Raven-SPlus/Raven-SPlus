package keystrokesmod.module.impl.world.tower;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.impl.world.Tower;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.MoveUtil;
import net.minecraft.stats.StatList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class Vulcan290Tower extends SubMode<Tower> {
    public Vulcan290Tower(String name, @NotNull Tower parent) {
        super(name, parent);
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (parent.canTower()) {
            if (mc.thePlayer.ticksExisted % 10 == 0) {
                // Prevent Flight Flag
                mc.thePlayer.motionY = -0.1;
                return;
            }

            fakeJump();

            if (mc.thePlayer.ticksExisted % 2 == 0) {
                mc.thePlayer.motionY = 0.7;
            } else {
                mc.thePlayer.motionY = MoveUtil.isMoving() ? 0.42 : 0.6;
            }
        }
    }

    private void fakeJump() {
        mc.thePlayer.isAirBorne = true;
        mc.thePlayer.triggerAchievement(StatList.jumpStat);
    }
}
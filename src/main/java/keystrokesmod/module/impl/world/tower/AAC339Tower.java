package keystrokesmod.module.impl.world.tower;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.impl.world.Tower;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.Utils;
import net.minecraft.stats.StatList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class AAC339Tower extends SubMode<Tower> {
    public AAC339Tower(String name, @NotNull Tower parent) {
        super(name, parent);
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (parent.canTower()) {
            if (mc.thePlayer.onGround) {
                fakeJump();
                mc.thePlayer.motionY = 0.4001;
            }
            Utils.getTimer().timerSpeed = 1f;
            if (mc.thePlayer.motionY < 0) {
                mc.thePlayer.motionY -= 0.00000945;
                Utils.getTimer().timerSpeed = 1.6f;
            }
        }
    }

    private void fakeJump() {
        mc.thePlayer.isAirBorne = true;
        mc.thePlayer.triggerAchievement(StatList.jumpStat);
    }
}
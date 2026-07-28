package keystrokesmod.module.impl.world.tower;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.impl.world.Tower;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import net.minecraft.stats.StatList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class PulldownTower extends SubMode<Tower> {
    private final SliderSetting triggerMotion;
    private final SliderSetting dragMotion;

    public PulldownTower(String name, @NotNull Tower parent) {
        super(name, parent);
        this.registerSetting(triggerMotion = new SliderSetting("Trigger motion", 0.1f, 0.0f, 0.2f, 0.01f));
        this.registerSetting(dragMotion = new SliderSetting("Drag motion", 1.0f, 0.1f, 1.0f, 0.01f));
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (parent.canTower()) {
            if (!mc.thePlayer.onGround && mc.thePlayer.motionY < triggerMotion.getInput()) {
                mc.thePlayer.motionY = -dragMotion.getInput();
            } else {
                fakeJump();
            }
        }
    }

    private void fakeJump() {
        mc.thePlayer.isAirBorne = true;
        mc.thePlayer.triggerAchievement(StatList.jumpStat);
    }
}
package keystrokesmod.module.impl.combat.velocity;

import keystrokesmod.event.PostVelocityEvent;
import keystrokesmod.module.impl.combat.Velocity;
import keystrokesmod.module.setting.impl.SubMode;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class MatrixReduce2Velocity extends SubMode<Velocity> {
    public MatrixReduce2Velocity(String name, @NotNull Velocity parent) {
        super(name, parent);
    }

    @SubscribeEvent
    public void onPostVelocity(PostVelocityEvent event) {
        if (mc.thePlayer.hurtTime > 0) {
            if (mc.thePlayer.onGround) {
                if (mc.thePlayer.hurtTime <= 6) {
                    mc.thePlayer.motionX *= 0.70;
                    mc.thePlayer.motionZ *= 0.70;
                }
                if (mc.thePlayer.hurtTime <= 5) {
                    mc.thePlayer.motionX *= 0.80;
                    mc.thePlayer.motionZ *= 0.80;
                }
            } else if (mc.thePlayer.hurtTime <= 10) {
                mc.thePlayer.motionX *= 0.60;
                mc.thePlayer.motionZ *= 0.60;
            }
        }
    }
}

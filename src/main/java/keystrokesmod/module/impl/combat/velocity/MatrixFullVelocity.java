package keystrokesmod.module.impl.combat.velocity;

import keystrokesmod.event.PostVelocityEvent;
import keystrokesmod.event.PreVelocityEvent;
import keystrokesmod.module.impl.combat.Velocity;
import keystrokesmod.module.setting.impl.SubMode;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class MatrixFullVelocity extends SubMode<Velocity> {
    public MatrixFullVelocity(String name, @NotNull Velocity parent) {
        super(name, parent);
    }

    @SubscribeEvent
    public void onPreVelocity(PreVelocityEvent event) {
        event.setMotionX((int) (event.getMotionX() * 0.4));
        event.setMotionZ((int) (event.getMotionZ() * 0.4));

        if (mc.thePlayer.onGround) {
            event.setMotionX((int) (event.getMotionX() * 0.8));
            event.setMotionZ((int) (event.getMotionZ() * 0.8));
        }

        event.setMotionY((int) (event.getMotionY() * 0.6));
    }

    @SubscribeEvent
    public void onPostVelocity(PostVelocityEvent event) {
        if (mc.thePlayer.hurtTime > 0) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionX *= 0.75;
                mc.thePlayer.motionZ *= 0.75;
            } else {
                mc.thePlayer.motionX *= 0.85;
                mc.thePlayer.motionZ *= 0.85;
            }
        }
    }
}

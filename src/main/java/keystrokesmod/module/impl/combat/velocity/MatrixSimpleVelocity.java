package keystrokesmod.module.impl.combat.velocity;

import keystrokesmod.event.PreVelocityEvent;
import keystrokesmod.module.impl.combat.Velocity;
import keystrokesmod.module.setting.impl.SubMode;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class MatrixSimpleVelocity extends SubMode<Velocity> {
    public MatrixSimpleVelocity(String name, @NotNull Velocity parent) {
        super(name, parent);
    }

    @SubscribeEvent
    public void onPreVelocity(PreVelocityEvent event) {
        event.setMotionX((int) (event.getMotionX() * 0.36));
        event.setMotionZ((int) (event.getMotionZ() * 0.36));

        if (mc.thePlayer.onGround) {
            event.setMotionX((int) (event.getMotionX() * 0.9));
            event.setMotionZ((int) (event.getMotionZ() * 0.9));
        }
    }
}

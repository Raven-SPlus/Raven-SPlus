package keystrokesmod.module.impl.movement.speed;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.impl.movement.Speed;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.MoveUtil;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Port of the Latest-Eclipse matrix speed:
 *
 * object LatestMatrix : Matrix() {
 *     init {
 *         task { event: StrafeEvent ->
 *             if (mc.thePlayer.isMoving) {
 *                 if (mc.thePlayer.onGround) {
 *                     mc.thePlayer.tryJump()
 *                     mc.thePlayer.strafe()
 *                 }
 *
 *                 if (mc.thePlayer.moveSpeed <= 0.2 && !mc.thePlayer.isCollidedHorizontally && !mc.thePlayer.onGround) {
 *                     mc.thePlayer.setSpeed(0.2f)
 *                 }
 *
 *                 if (mc.thePlayer.hurtTime <= 0) {
 *                     mc.thePlayer.motionY -= 0.0032
 *                 }
 *             }
 *         }.runIf { runnable && mode == "LatestMatrix" }
 *     }
 * }
 */
public class MatrixSpeed extends SubMode<Speed> {

    public MatrixSpeed(String name, @NotNull Speed parent) {
        super(name, parent);
    }

    @SubscribeEvent
    public void onPreUpdate(@NotNull PreUpdateEvent event) {
        // Respect global speed disable conditions and only act while moving
        if (parent.noAction() || !MoveUtil.isMoving()) {
            return;
        }

        // Auto-jump + strafe when on ground
        if (mc.thePlayer.onGround) {
            // Mimic tryJump(): don't override manual jump key presses
            if (!Utils.jumpDown()) {
                mc.thePlayer.jump();
            }
            MoveUtil.strafe();
        }

        // Ensure a minimum horizontal speed while airborne and not colliding
        double horizontalSpeed = Utils.getHorizontalSpeed();
        if (horizontalSpeed <= 0.2
                && !mc.thePlayer.isCollidedHorizontally
                && !mc.thePlayer.onGround) {
            Utils.setSpeed(0.2);
        }

        // Slightly reduce vertical motion when not recently hurt
        if (mc.thePlayer.hurtTime <= 0) {
            mc.thePlayer.motionY -= 0.0032;
        }
    }
}



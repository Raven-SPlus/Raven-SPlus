package keystrokesmod.module.impl.world.scaffold.rotation;

import keystrokesmod.event.RotationEvent;
import keystrokesmod.module.impl.world.Scaffold;
import keystrokesmod.module.impl.world.scaffold.IScaffoldRotation;
import keystrokesmod.utility.MoveUtil;
import keystrokesmod.utility.aim.RotationData;
import net.minecraft.util.MathHelper;
import org.jetbrains.annotations.NotNull;

public class StrictRotation extends IScaffoldRotation {
    private boolean jitterFlip = false;

    public StrictRotation(String name, @NotNull Scaffold parent) {
        super(name, parent);
    }

    @Override
    public @NotNull RotationData onRotation(float placeYaw, float placePitch, boolean forceStrict, @NotNull RotationEvent event) {
        boolean hasTarget = parent.rayCasted != null;
        if (hasTarget || forceStrict || !MoveUtil.isMoving()) {
            // Apply small imperfection to avoid perfect rotation (KillAura tech)
            float sens = parent.mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
            double gcd = sens * sens * sens * 1.2;
            
            // Add tiny GCD-aligned offset to break perfect patterns
            float nudge = (float) (gcd * 0.3);
            float finalYaw = placeYaw + (jitterFlip ? nudge : -nudge);
            float finalPitch = placePitch + (float) (gcd * 0.15 * (jitterFlip ? 1 : -1));
            jitterFlip = !jitterFlip;
            
            return new RotationData(finalYaw, finalPitch);
        }
        return new RotationData(parent.getYaw() + (Scaffold.isDiagonal() ? 0 : (float) parent.strafe.getInput()), 85);
    }
}
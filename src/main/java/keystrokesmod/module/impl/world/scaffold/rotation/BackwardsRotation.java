package keystrokesmod.module.impl.world.scaffold.rotation;

import keystrokesmod.event.RotationEvent;
import keystrokesmod.module.impl.world.Scaffold;
import keystrokesmod.module.impl.world.scaffold.IScaffoldRotation;
import keystrokesmod.utility.aim.RotationData;
import net.minecraft.util.MathHelper;
import org.jetbrains.annotations.NotNull;

public class BackwardsRotation extends IScaffoldRotation {
    private boolean jitterFlip = false;

    public BackwardsRotation(String name, @NotNull Scaffold parent) {
        super(name, parent);
    }

    @Override
    public @NotNull RotationData onRotation(float placeYaw, float placePitch, boolean forceStrict, @NotNull RotationEvent event) {
        if (parent.rayCasted != null) {
            // Apply small imperfection to avoid perfect rotation (KillAura tech)
            float sens = parent.mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
            double gcd = sens * sens * sens * 1.2;
            
            // Add tiny GCD-aligned offset to break perfect patterns
            float nudge = (float) (gcd * 0.2);
            float finalYaw = placeYaw + (jitterFlip ? nudge : -nudge);
            float finalPitch = placePitch + (float) (gcd * 0.1 * (jitterFlip ? 1 : -1));
            jitterFlip = !jitterFlip;
            
            return new RotationData(finalYaw, finalPitch);
        }
        // Add variance to pitch to avoid FairFight Scaffold C detection (exactly 85 degrees)
        // Vary pitch between 83-87 degrees instead of exactly 85
        // Always add small variance to avoid detection patterns
        float pitch = (float) (85.0 + keystrokesmod.utility.Utils.randomizeGaussian(0.0, 0.8, -1.5, 1.5));
        return new RotationData(parent.getYaw() + (Scaffold.isDiagonal() ? 0 : (float) parent.strafe.getInput()), pitch);
    }
}
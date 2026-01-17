package keystrokesmod.module.impl.world.scaffold.rotation;

import keystrokesmod.event.RotationEvent;
import keystrokesmod.module.impl.world.Scaffold;
import keystrokesmod.module.impl.world.scaffold.IScaffoldRotation;
import keystrokesmod.utility.aim.RotationData;
import net.minecraft.util.MathHelper;
import org.jetbrains.annotations.NotNull;

public class StabilizedRotation extends IScaffoldRotation {
    private boolean jitterFlip = false;

    public StabilizedRotation(String name, @NotNull Scaffold parent) {
        super(name, parent);
    }

    @Override
    public @NotNull RotationData onRotation(float placeYaw, float placePitch, boolean forceStrict, @NotNull RotationEvent event) {
        if (parent.rayCasted != null) {
            // Round yaw to nearest 45 degrees for stabilized rotation
            float roundYaw45 = Math.round(placeYaw / 45f) * 45f;
            
            // Apply KillAura-style imperfection to avoid perfect rotation
            float sens = parent.mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
            double gcd = sens * sens * sens * 1.2;
            
            // Add small GCD-aligned offset to break perfect patterns
            float nudge = (float) (gcd * 0.5);
            roundYaw45 += jitterFlip ? nudge : -nudge;
            jitterFlip = !jitterFlip;
            
            return new RotationData(roundYaw45, placePitch);
        }
        return new RotationData(event.getYaw(), event.getPitch());
    }
}
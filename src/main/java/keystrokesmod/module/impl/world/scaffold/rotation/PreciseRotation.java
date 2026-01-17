package keystrokesmod.module.impl.world.scaffold.rotation;

import keystrokesmod.event.RotationEvent;
import keystrokesmod.module.impl.world.Scaffold;
import keystrokesmod.module.impl.world.scaffold.IScaffoldRotation;
import keystrokesmod.utility.aim.RotationData;
import net.minecraft.util.MathHelper;
import org.jetbrains.annotations.NotNull;

public class PreciseRotation extends IScaffoldRotation {
    public PreciseRotation(String name, @NotNull Scaffold parent) {
        super(name, parent);
    }

    @Override
    public @NotNull RotationData onRotation(float placeYaw, float placePitch, boolean forceStrict, @NotNull RotationEvent event) {
        // Precise rotation with optional small imperfection to avoid perfect rotation flags
        float finalYaw = placeYaw;
        float finalPitch = placePitch;
        
        // Apply tiny imperfection to avoid "perfect" rotation detection
        if (parent.rayCasted != null) {
            float sens = parent.mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
            double gcd = sens * sens * sens * 1.2;
            // Add very small offset (much smaller than jitter) to break perfect patterns
            finalYaw += (float) (gcd * (Math.random() > 0.5 ? 0.1 : -0.1));
            finalPitch += (float) (gcd * (Math.random() > 0.5 ? 0.05 : -0.05));
        }
        
        return new RotationData(finalYaw, finalPitch);
    }
}
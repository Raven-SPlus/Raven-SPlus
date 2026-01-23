package keystrokesmod.utility.scaffold;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.IMinecraftInstance;
import keystrokesmod.utility.Utils;
import net.minecraft.util.MathHelper;

/**
 * Scaffold-specific utility methods copied from reference implementation.
 * Separated from main utilities to avoid conflicts.
 */
public class ScaffoldUtils implements IMinecraftInstance {
    public static int fadeEdge = 0;
    public static int groundTicks = 0;
    public static int inAirTicks = 0;
    public static final double offsetValue = 0.0000000000201;
    
    /**
     * Check if player is moving diagonally (for scaffold).
     * Copied from reference Utils.scaffoldDiagonal()
     * Uses client-side rotation for movement direction
     */
    public static boolean scaffoldDiagonal(boolean strict) {
        if (ModuleManager.scaffold == null || !Utils.nullCheck()) {
            return false;
        }
        // Use client-side rotation for movement direction
        float clientYaw = keystrokesmod.module.impl.other.RotationHandler.getRotationYaw();
        float back = MathHelper.wrapAngleTo180_float(clientYaw) - ModuleManager.scaffold.hardcodedYaw();
        float yaw = ((back % 360) + 360) % 360;
        yaw = yaw > 180 ? yaw - 360 : yaw;
        boolean isYawDiagonal = inBetween(-170, 170, yaw) && !inBetween(-10, 10, yaw) && !inBetween(80, 100, yaw) && !inBetween(-100, -80, yaw);
        if (strict) {
            isYawDiagonal = inBetween(-178.5, 178.5, yaw) && !inBetween(-1.5, 1.5, yaw) && !inBetween(88.5, 91.5, yaw) && !inBetween(-91.5, -88.5, yaw);
        }
        return isYawDiagonal;
    }
    
    private static boolean inBetween(double min, double max, double value) {
        return value >= min && value <= max;
    }
    
    /**
     * Update tick counters (called from Scaffold's onPreMotion)
     */
    public static void updateTicks() {
        if (!Utils.nullCheck()) return;
        if (mc.thePlayer.onGround) {
            groundTicks++;
            inAirTicks = 0;
        } else {
            inAirTicks++;
            groundTicks = 0;
        }
    }
}

package keystrokesmod.utility.render.blur;

import keystrokesmod.utility.render.ColorUtils;
import org.jetbrains.annotations.Range;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

/**
 * Reusable Gaussian weight buffer to avoid per-frame allocations.
 */
final class BlurKernelCache {
    private static final int MAX_RADIUS = 64;
    private static final FloatBuffer WEIGHT_BUFFER = BufferUtils.createFloatBuffer(MAX_RADIUS + 1);
    private static final float[] CACHED_WEIGHTS = new float[MAX_RADIUS + 1];
    private static int cachedRadius = -1;

    private BlurKernelCache() {
    }

    static FloatBuffer getWeights(@Range(from = 0, to = MAX_RADIUS) int radius) {
        int clampedRadius = Math.max(0, Math.min(MAX_RADIUS, radius));

        // Recompute weights only when radius changes
        if (cachedRadius != clampedRadius) {
            cachedRadius = clampedRadius;
            for (int i = 0; i <= clampedRadius; i++) {
                CACHED_WEIGHTS[i] = ColorUtils.calculateGaussianValue(i, clampedRadius / 2f);
            }
        }

        // Reuse buffer instance to avoid churn
        WEIGHT_BUFFER.clear();
        WEIGHT_BUFFER.put(CACHED_WEIGHTS, 0, clampedRadius + 1);
        WEIGHT_BUFFER.rewind();
        return WEIGHT_BUFFER;
    }
}

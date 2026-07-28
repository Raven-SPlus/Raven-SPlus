package keystrokesmod.utility.math;

/**
 * Interpolation utilities ported from {@code net.eclipse.libs.algorithm.interpolation}.
 * All methods are additive and do not alter existing Raven implementations.
 */
public final class InterpolationUtils {

    private InterpolationUtils() {
        // utility class
    }

    // region Linear interpolation (lerp)

    /**
     * Linear smoothing; the smaller the smoothing factor {@code t}, the smoother the transition.
     *
     * @param start          start value
     * @param end            end value
     * @param t              smoothing factor (0..1)
     * @param minDiffToReset if {@code |end - start| <= minDiffToReset}, snap directly to {@code end}
     */
    public static double lerp(double start, double end, double t, double minDiffToReset) {
        double diff = end - start;
        if (Math.abs(diff) <= minDiffToReset) {
            return end;
        }
        double clampedT = clamp(t, 0.0, 1.0);
        return start + diff * clampedT;
    }

    public static double lerp(double start, double end, double t) {
        return lerp(start, end, t, 0.0);
    }

    /**
     * Float overload of {@link #lerp(double, double, double, double)}.
     */
    public static float lerp(float start, float end, double t, float minDiffToReset) {
        return (float) lerp(start, end, t, (double) minDiffToReset);
    }

    public static float lerp(float start, float end, double t) {
        return lerp(start, end, t, 0.0f);
    }

    /**
     * Int overload of {@link #lerp(double, double, double, double)}.
     */
    public static int lerp(int start, int end, double t, int minDiffToReset) {
        return (int) lerp((double) start, (double) end, t, (double) minDiffToReset);
    }

    public static int lerp(int start, int end, double t) {
        return lerp(start, end, t, 0);
    }

    // endregion

    // region "Morph" interpolation (power-curve smoothing)

    /**
     * Curtain-style smoothing.
     * <ul>
     *     <li>{@code factor = 1.0}: linear smoothing</li>
     *     <li>{@code factor < 1.0}: inverse-curve smoothing</li>
     *     <li>{@code factor > 1.0}: curve smoothing</li>
     * </ul>
     *
     * @param index      current index value
     * @param indexStart start of index range
     * @param indexEnd   end of index range
     * @param valueStart start of value range
     * @param valueEnd   end of value range
     * @param factor     curve factor ({@code >= 0})
     * @return smoothed value within {@code [min(valueStart, valueEnd), max(valueStart, valueEnd)]}
     */
    public static double morph(double index,
                               double indexStart, double indexEnd,
                               double valueStart, double valueEnd,
                               double factor) {
        double f = Math.max(factor, 0.0);

        if (index == indexStart) return valueStart;
        if (index == indexEnd) return valueEnd;

        double iMin = Math.min(indexStart, indexEnd);
        double iMax = Math.max(indexStart, indexEnd);
        double vMin = Math.min(valueStart, valueEnd);
        double vMax = Math.max(valueStart, valueEnd);

        double iValue = clamp(index, iMin, iMax);
        double denom = Math.abs(indexEnd - indexStart);
        if (denom == 0.0) {
            return clamp(valueStart, vMin, vMax);
        }

        double scaled = Math.pow(Math.abs(iValue - indexStart), f);
        double rangePow = Math.pow(denom, f);

        double vValue = ((valueEnd - valueStart) / rangePow) * scaled + valueStart;
        return clamp(vValue, vMin, vMax);
    }

    public static float morph(float index,
                              float indexStart, float indexEnd,
                              float valueStart, float valueEnd,
                              double factor) {
        return (float) morph(index, indexStart, indexEnd, valueStart, valueEnd, factor);
    }

    public static int morph(int index,
                            int indexStart, int indexEnd,
                            int valueStart, int valueEnd,
                            double factor) {
        return (int) morph((double) index,
                (double) indexStart, (double) indexEnd,
                (double) valueStart, (double) valueEnd,
                factor);
    }

    // endregion

    // region helpers

    private static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    // endregion
}



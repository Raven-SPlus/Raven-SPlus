package keystrokesmod.utility.aim;

import keystrokesmod.utility.RotationUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rotation entropy analysis utilities.
 * <p>
 * Ported from {@code net.eclipse.libs.algorithm.entropy.RotationEntropy}
 * with adaptations to {@link RotationData} and the Raven client environment.
 * </p>
 *
 * @author Corona (original algorithms)
 */
public final class RotationEntropyUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private RotationEntropyUtils() {
        // utility class
    }

    /**
     * Compute normalized path vector entropy using a simple probability model.
     * Uses the ratio of unique path angles as a diversity estimate.
     *
     * @param rotations    list of rotation samples (newest last)
     * @param selectedSize number of recent samples to use (up to this many)
     * @return normalized entropy in [0, 1], or -1 if there is not enough data
     */
    public static double computePathVecEntropy(List<RotationData> rotations, int selectedSize) {
        // Guard: avoid low-precision results
        if (rotations == null || rotations.size() < 20) {
            return -1.0;
        }

        // Select recent rotation window
        int lastIndex = rotations.size() - 1;
        int start = Math.max(0, lastIndex - selectedSize);
        List<RotationData> selected = rotations.subList(start, lastIndex + 1);

        if (selected.size() < 2) {
            return -1.0;
        }

        // Compute rotation deltas and convert to mouse-input-like vectors
        double mouseFactor = getMouseInputFactor();
        List<int[]> deltas = new ArrayList<>(selected.size() - 1);
        for (int i = 1; i < selected.size(); i++) {
            RotationData prev = selected.get(i - 1);
            RotationData current = selected.get(i);
            RotationData diff = toCorrectRotation(getDiffRotation(prev, current));

            int dx = (int) (diff.getYaw() / mouseFactor);
            int dy = (int) (diff.getPitch() / mouseFactor);
            deltas.add(new int[]{dx, dy});
        }

        if (deltas.isEmpty()) {
            return -1.0;
        }

        // Normalize to path-vector plane angles
        List<Double> vectorAngles = new ArrayList<>(deltas.size());
        for (int[] d : deltas) {
            double angle = Math.atan2(d[1], d[0]) / Math.PI;
            vectorAngles.add(angle);
        }

        // Simple probability-based entropy proxy:
        // unique angle ratio as a measure of diversity
        Set<Double> unique = new HashSet<>(vectorAngles);
        double probability = unique.size() / (double) vectorAngles.size();

        return boundBy(probability, 0.0, 1.0);
    }

    /**
     * Compute normalized path entropy using Shannon entropy over a yaw/pitch grid.
     *
     * @param rotations    list of rotation samples (newest last)
     * @param selectedSize number of recent samples to use (up to this many)
     * @return normalized entropy in [0, 1], or -1 if there is not enough data
     */
    public static double computePathEntropy(List<RotationData> rotations, int selectedSize) {
        // Guard: avoid low-precision results
        if (rotations == null || rotations.size() < 50) {
            return -1.0;
        }

        // Select recent rotation window
        int lastIndex = rotations.size() - 1;
        int start = Math.max(0, lastIndex - selectedSize);
        List<RotationData> selected = rotations.subList(start, lastIndex + 1);

        if (selected.size() < 2) {
            return -1.0;
        }

        // Compute rotation deltas
        List<RotationData> deltas = new ArrayList<>(selected.size() - 1);
        for (int i = 1; i < selected.size(); i++) {
            RotationData prev = selected.get(i - 1);
            RotationData current = selected.get(i);
            RotationData diff = toCorrectRotation(getDiffRotation(prev, current));
            deltas.add(diff);
        }

        if (deltas.isEmpty()) {
            return -1.0;
        }

        // Smart grid size selection
        int binAmount;
        int size = deltas.size();
        if (size < 30) {
            binAmount = 6;
        } else if (size < 100) {
            binAmount = 8;
        } else if (size < 200) {
            binAmount = 12;
        } else {
            binAmount = 16;
        }
        int totalBins = binAmount * binAmount;

        // Shannon entropy over yaw/pitch grid
        int[] binCounts = new int[totalBins];
        float yawRange = 360f;
        float pitchRange = 180f;

        for (RotationData d : deltas) {
            float dyaw = d.getYaw();
            float dpitch = d.getPitch();

            int xIndex = boundBy((int) (((dyaw + 180f) / yawRange) * binAmount), 0, binAmount - 1);
            int yIndex = boundBy((int) (((dpitch + 90f) / pitchRange) * binAmount), 0, binAmount - 1);
            int binIndex = xIndex + yIndex * binAmount;

            binCounts[binIndex]++;
        }

        double totalSamples = deltas.size();
        double entropy = 0.0;
        for (int count : binCounts) {
            if (count > 0) {
                double p = count / totalSamples;
                entropy -= p * log2(p);
            }
        }

        // Normalize entropy
        double maxEntropy = log2(totalBins);
        double result = entropy / maxEntropy;

        return boundBy(result, 0.0, 1.0);
    }

    /**
     * Compute normalized static deviation entropy using Shannon entropy.
     * Measures randomness of deviation from the ideal rotation path.
     *
     * @param rotations      actual rotation samples
     * @param idealRotations ideal rotation samples (must be same length/order)
     * @param selectedSize   number of recent samples to use (up to this many)
     * @return normalized entropy in [0, 1], or -1 if there is not enough data or the lists mismatch
     */
    public static double computeStaticDevEntropy(List<RotationData> rotations,
                                                 List<RotationData> idealRotations,
                                                 int selectedSize) {
        // Guard: avoid low-precision results and mismatched data
        if (rotations == null || idealRotations == null ||
            rotations.size() < 50 || idealRotations.size() < 50 ||
            rotations.size() != idealRotations.size()) {
            return -1.0;
        }

        // Select recent rotation window
        int lastIndex = rotations.size() - 1;
        int start = Math.max(0, lastIndex - selectedSize);
        List<RotationData> selected = rotations.subList(start, lastIndex + 1);
        List<RotationData> selectedIdeal = idealRotations.subList(start, lastIndex + 1);

        if (selected.isEmpty() || selectedIdeal.isEmpty() || selected.size() != selectedIdeal.size()) {
            return -1.0;
        }

        // Compute deviations between actual and ideal rotations
        List<RotationData> devs = new ArrayList<>(selected.size());
        for (int i = 0; i < selected.size(); i++) {
            RotationData rotation = selected.get(i);
            RotationData ideal = selectedIdeal.get(i);
            RotationData diff = getDiffRotation(ideal, rotation);
            devs.add(diff);
        }

        double dataSize = devs.size();
        if (dataSize <= 0) {
            return -1.0;
        }

        // Compute static deviation entropy (deviation distribution randomness)
        int yawBins;
        if (dataSize < 30) {
            yawBins = 6;
        } else if (dataSize < 100) {
            yawBins = 8;
        } else if (dataSize < 200) {
            yawBins = 10;
        } else {
            yawBins = 12;
        }

        int pitchBins;
        if (dataSize < 30) {
            pitchBins = 4;
        } else if (dataSize < 100) {
            pitchBins = 6;
        } else if (dataSize < 200) {
            pitchBins = 8;
        } else {
            pitchBins = 10;
        }

        int totalBins = yawBins * pitchBins;
        int[] binCounts = new int[totalBins];

        float maxYawDev = 35f;
        float maxPitchDev = 20f;

        for (RotationData dev : devs) {
            float normYaw = (boundBy(dev.getYaw(), -maxYawDev, maxYawDev) + maxYawDev) / (2f * maxYawDev);
            float normPitch = (boundBy(dev.getPitch(), -maxPitchDev, maxPitchDev) + maxPitchDev) / (2f * maxPitchDev);

            int yawIndex = boundBy((int) (normYaw * yawBins), 0, yawBins - 1);
            int pitchIndex = boundBy((int) (normPitch * pitchBins), 0, pitchBins - 1);
            int binIndex = yawIndex + pitchIndex * yawBins;

            binCounts[binIndex]++;
        }

        double entropy = 0.0;
        for (int count : binCounts) {
            if (count > 0) {
                double p = count / dataSize;
                entropy -= p * log2(p);
            }
        }

        double maxEntropy = Math.log(totalBins);
        double result = entropy / maxEntropy;

        return boundBy(result, 0.0, 1.0);
    }

    /**
     * Compute normalized dynamic deviation entropy using Shannon entropy.
     * Measures randomness of how deviation changes over time.
     *
     * @param rotations      actual rotation samples
     * @param idealRotations ideal rotation samples (must be same length/order)
     * @param selectedSize   number of recent samples to use (up to this many)
     * @return normalized entropy in [0, 1], or -1 if there is not enough data or the lists mismatch
     */
    public static double computeDynamicDevEntropy(List<RotationData> rotations,
                                                  List<RotationData> idealRotations,
                                                  int selectedSize) {
        // Guard: avoid low-precision results and mismatched data
        if (rotations == null || idealRotations == null ||
            rotations.size() < 50 || idealRotations.size() < 50 ||
            rotations.size() != idealRotations.size()) {
            return -1.0;
        }

        // Select recent rotation window
        int lastIndex = rotations.size() - 1;
        int start = Math.max(0, lastIndex - selectedSize);
        List<RotationData> selected = rotations.subList(start, lastIndex + 1);
        List<RotationData> selectedIdeal = idealRotations.subList(start, lastIndex + 1);

        if (selected.isEmpty() || selectedIdeal.isEmpty() || selected.size() != selectedIdeal.size()) {
            return -1.0;
        }

        // Compute deviations between actual and ideal rotations
        List<RotationData> devs = new ArrayList<>(selected.size());
        for (int i = 0; i < selected.size(); i++) {
            RotationData rotation = selected.get(i);
            RotationData ideal = selectedIdeal.get(i);
            RotationData diff = getDiffRotation(ideal, rotation);
            devs.add(diff);
        }

        if (devs.size() < 2) {
            return -1.0;
        }

        // Compute dynamic deviation entropy (randomness of deviation changes)
        List<RotationData> deltaChanges = new ArrayList<>(devs.size() - 1);
        for (int i = 1; i < devs.size(); i++) {
            RotationData prev = devs.get(i - 1);
            RotationData curr = devs.get(i);
            RotationData diff = getDiffRotation(prev, curr);
            deltaChanges.add(diff);
        }

        double dataSize = deltaChanges.size();
        if (dataSize <= 0) {
            return -1.0;
        }

        int yawBins;
        if (dataSize < 20) {
            yawBins = 5;
        } else if (dataSize < 50) {
            yawBins = 7;
        } else if (dataSize < 100) {
            yawBins = 9;
        } else {
            yawBins = 11;
        }

        int pitchBins;
        if (dataSize < 20) {
            pitchBins = 4;
        } else if (dataSize < 50) {
            pitchBins = 5;
        } else if (dataSize < 100) {
            pitchBins = 6;
        } else {
            pitchBins = 7;
        }

        int totalBins = yawBins * pitchBins;
        int[] binCounts = new int[totalBins];

        float maxChangeRate = 5f;
        for (RotationData d : deltaChanges) {
            float normDYaw = (boundBy(d.getYaw(), -maxChangeRate, maxChangeRate) + maxChangeRate) / (2f * maxChangeRate);
            float normDPitch = (boundBy(d.getPitch(), -maxChangeRate, maxChangeRate) + maxChangeRate) / (2f * maxChangeRate);

            int yawIndex = boundBy((int) (normDYaw * yawBins), 0, yawBins - 1);
            int pitchIndex = boundBy((int) (normDPitch * pitchBins), 0, pitchBins - 1);
            int binIndex = yawIndex + pitchIndex * yawBins;

            binCounts[binIndex]++;
        }

        double entropy = 0.0;
        for (int count : binCounts) {
            if (count > 0) {
                double p = count / dataSize;
                entropy -= p * log2(p);
            }
        }

        double maxEntropy = Math.log(totalBins);
        double result = entropy / maxEntropy;

        return boundBy(result, 0.0, 1.0);
    }

    /**
     * Compute normalized fractal entropy based on path self-similarity.
     *
     * @param rotations    list of rotation samples (newest last)
     * @param selectedSize number of recent samples to use (up to this many)
     * @return estimated fractal dimension; returns 1.0 in low-data edge cases, or -1 on invalid input
     */
    public static double computeFractalEntropy(List<RotationData> rotations, int selectedSize) {
        // Guard: avoid low-precision results
        if (rotations == null || rotations.size() < 50) {
            return -1.0;
        }

        // Select recent rotation window
        int lastIndex = rotations.size() - 1;
        int start = Math.max(0, lastIndex - selectedSize);
        List<RotationData> selected = rotations.subList(start, lastIndex + 1);

        if (selected.isEmpty()) {
            return -1.0;
        }

        // Extract yaw and pitch paths
        int n = selected.size();
        double[] yawPath = new double[n];
        double[] pitchPath = new double[n];
        for (int i = 0; i < n; i++) {
            RotationData r = selected.get(i);
            yawPath[i] = r.getYaw();
            pitchPath[i] = r.getPitch();
        }

        // Compute fractal dimension using multi-scale path lengths
        int kMax = Math.min(10, n / 3);
        if (kMax < 3) {
            return 1.0;
        }
        if (n < kMax * 2) {
            return 1.0;
        }

        double[] lk = new double[kMax];
        for (int k = 1; k <= kMax; k++) {
            double sum = 0.0;
            int validCount = 0;

            for (int m = 0; m < k; m++) {
                // indices: m, m + k, m + 2k, ...
                List<Integer> indices = new ArrayList<>();
                for (int idx = m; idx < n; idx += k) {
                    indices.add(idx);
                }
                if (indices.size() < 2) {
                    continue;
                }

                double length = 0.0;
                for (int i = 1; i < indices.size(); i++) {
                    int idx1 = indices.get(i - 1);
                    int idx2 = indices.get(i);
                    double dy = yawPath[idx2] - yawPath[idx1];
                    double dp = pitchPath[idx2] - pitchPath[idx1];
                    length += Math.hypot(dy, dp);
                }

                if (length > 0.0) {
                    double normalized = length * (n - 1.0) / (indices.size() * (double) k);
                    sum += normalized;
                    validCount++;
                }
            }

            lk[k - 1] = validCount > 0 ? sum / validCount : 0.0;
        }

        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumX2 = 0.0;
        int validPoints = 0;

        for (int k = 1; k <= lk.length; k++) {
            if (lk[k - 1] <= 0.0) {
                continue;
            }

            double x = Math.log(1.0 / k);
            double y = Math.log(lk[k - 1]);

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
            validPoints++;
        }

        if (validPoints < 3) {
            return 1.0;
        }

        double numerator = validPoints * sumXY - sumX * sumY;
        double denominator = validPoints * sumX2 - sumX * sumX;

        double result = Math.abs(denominator) > 1e-10 ? numerator / denominator : 0.0;
        return result;
    }

    // region helpers

    /**
     * Compute a wrapped, corrected difference from {@code from} to {@code to}.
     */
    private static RotationData getDiffRotation(RotationData from, RotationData to) {
        if (from == null || to == null) {
            return new RotationData(0.0F, 0.0F);
        }
        float dyaw = RotationUtils.normalize180(to.getYaw() - from.getYaw());
        float dpitch = to.getPitch() - from.getPitch();
        dpitch = boundBy(dpitch, -90.0F, 90.0F);
        return new RotationData(dyaw, dpitch);
    }

    /**
     * Convert to a "corrected" rotation: yaw wrapped to [-180, 180], pitch clamped to [-90, 90].
     */
    private static RotationData toCorrectRotation(RotationData rotation) {
        float yaw = RotationUtils.normalize180(rotation.getYaw());
        float pitch = boundBy(rotation.getPitch(), -90.0F, 90.0F);
        return new RotationData(yaw, pitch);
    }

    private static double log2(double x) {
        return Math.log(x) / Math.log(2.0);
    }

    private static double boundBy(double v, double min, double max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static float boundBy(float v, float min, float max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static int boundBy(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    /**
     * Mouse input scaling factor, mirroring the original implementation:
     * {@code (sensitivity * 0.6f + 0.2f)^3 * 1.2f}.
     */
    private static float getMouseInputFactor() {
        float sensitivity = mc.gameSettings.mouseSensitivity;
        float base = sensitivity * 0.6f + 0.2f;
        return (float) (base * base * base * 1.2f);
    }

    // endregion
}



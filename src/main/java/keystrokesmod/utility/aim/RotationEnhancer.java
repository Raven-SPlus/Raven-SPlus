package keystrokesmod.utility.aim;

import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.math.InterpolationUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Enhanced rotation system that uses entropy analysis and interpolation
 * to create more human-like, anti-cheat-resistant rotation patterns.
 * <p>
 * Key improvements:
 * <ul>
 *   <li><b>Entropy-based randomization:</b> Monitors rotation patterns and adds
 *       randomization when entropy is too low (detectable patterns)</li>
 *   <li><b>Adaptive interpolation:</b> Uses morph() for natural curve-based smoothing
 *       instead of linear interpolation</li>
 *   <li><b>Rotation history tracking:</b> Maintains a buffer of recent rotations
 *       for pattern analysis</li>
 *   <li><b>Dynamic smoothing:</b> Adjusts smoothing factor based on rotation speed
 *       and entropy levels</li>
 * </ul>
 * </p>
 *
 * @author Corona (entropy algorithms), Enhanced for KillAura
 */
public class RotationEnhancer {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    // Rotation history for entropy analysis
    private final Queue<RotationData> rotationHistory = new LinkedList<>();
    private final Queue<RotationData> idealRotationHistory = new LinkedList<>();
    private static final int MAX_HISTORY_SIZE = 200;
    private static final int ENTROPY_ANALYSIS_SIZE = 120;
    
    // Entropy thresholds
    private static final double LOW_ENTROPY_THRESHOLD = 0.3;  // Below this, add randomization
    private static final double VERY_LOW_ENTROPY_THRESHOLD = 0.15;  // Very detectable
    
    // Adaptive smoothing parameters
    private double currentSmoothingFactor = 1.0;  // 1.0 = linear, <1.0 = inverse curve, >1.0 = curve
    private double adaptiveRandomization = 0.0;  // Amount of randomization to add
    private double lastAvgEntropy = 0.5;         // Cached avg entropy for feedback
    private boolean jitterAllowed = true;
    
    // Last applied rotation
    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;

    public void setJitterAllowed(boolean allow) {
        this.jitterAllowed = allow;
    }
    
    /**
     * Enhanced rotation calculation with entropy-based improvements.
     * 
     * @param targetYaw target yaw rotation
     * @param targetPitch target pitch rotation
     * @param currentYaw current yaw rotation
     * @param currentPitch current pitch rotation
     * @param rotationSpeed maximum rotation speed per tick
     * @param useAdaptiveSmoothing whether to use adaptive smoothing based on entropy
     * @return enhanced rotation [yaw, pitch]
     */
    public float[] enhanceRotation(
            float targetYaw, float targetPitch,
            float currentYaw, float currentPitch,
            float rotationSpeed,
            boolean useAdaptiveSmoothing) {
        
        // Record ideal rotation for entropy analysis
        RotationData idealRot = new RotationData(targetYaw, targetPitch);
        idealRotationHistory.offer(idealRot);
        if (idealRotationHistory.size() > MAX_HISTORY_SIZE) {
            idealRotationHistory.poll();
        }
        
        // Calculate rotation delta
        float deltaYaw = RotationUtils.normalize(targetYaw - currentYaw);
        float deltaPitch = targetPitch - currentPitch;
        
        // Analyze entropy if we have enough history
        if (useAdaptiveSmoothing && rotationHistory.size() >= 50) {
            analyzeAndAdapt();
        }
        
        // Apply adaptive randomization if entropy is low
        if (useAdaptiveSmoothing && adaptiveRandomization > 0.0 && jitterAllowed) {
            // Add small random variations to break patterns
            double randYaw = (Math.random() - 0.5) * adaptiveRandomization * 2.0;
            double randPitch = (Math.random() - 0.5) * adaptiveRandomization * 1.0;
            
            deltaYaw += (float) randYaw;
            deltaPitch += (float) randPitch;
            
            // Recalculate target with randomization
            targetYaw = currentYaw + deltaYaw;
            targetPitch = currentPitch + deltaPitch;
        }

        // Entropy feedback: widen path slightly when long-term entropy dips
        double entropyFeedback = Math.max(0.0, 0.5 - lastAvgEntropy);
        if (useAdaptiveSmoothing && entropyFeedback > 0.0) {
            float varianceScale = 1.0f + (float) (entropyFeedback * 0.3);
            deltaYaw *= varianceScale;
            deltaPitch *= varianceScale;
        }
        
        // Calculate distance to travel
        float distance = (float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
        
        float finalYaw, finalPitch;
        
        if (distance <= rotationSpeed) {
            // Close enough, snap to target
            finalYaw = targetYaw;
            finalPitch = targetPitch;
        } else {
            // Use morph() for natural curve-based interpolation
            if (useAdaptiveSmoothing) {
                // Adaptive smoothing: use morph with dynamic factor
                double progress = rotationSpeed / distance;
                progress = Math.max(0.0, Math.min(1.0, progress));
                
                // Map progress through morph curve for natural acceleration/deceleration
                double morphFactor = currentSmoothingFactor * (1.0 + entropyFeedback * 0.85);
                double smoothedProgress = InterpolationUtils.morph(
                    progress,
                    0.0, 1.0,  // index range
                    0.0, 1.0,  // value range
                    morphFactor
                );
                
                finalYaw = currentYaw + (float) (deltaYaw * smoothedProgress);
                finalPitch = currentPitch + (float) (deltaPitch * smoothedProgress);
            } else {
                // Linear interpolation (fallback)
                double ratio = rotationSpeed / distance;
                finalYaw = currentYaw + (float) (deltaYaw * ratio);
                finalPitch = currentPitch + (float) (deltaPitch * ratio);
            }
        }
        
        // Record actual rotation for entropy analysis
        RotationData actualRot = new RotationData(finalYaw, finalPitch);
        rotationHistory.offer(actualRot);
        if (rotationHistory.size() > MAX_HISTORY_SIZE) {
            rotationHistory.poll();
        }
        
        lastYaw = finalYaw;
        lastPitch = finalPitch;
        
        return new float[]{finalYaw, finalPitch};
    }
    
    /**
     * Analyze rotation entropy and adapt smoothing/randomization parameters.
     */
    private void analyzeAndAdapt() {
        List<RotationData> recentRotations = new ArrayList<>(rotationHistory);
        List<RotationData> recentIdeal = new ArrayList<>(idealRotationHistory);
        
        if (recentRotations.size() < 50 || recentIdeal.size() < 50) {
            return;
        }
        
        // Calculate path entropy (how random the rotation path is)
        double pathEntropy = RotationEntropyUtils.computePathEntropy(
            recentRotations, ENTROPY_ANALYSIS_SIZE
        );
        
        // Calculate static deviation entropy (how random the deviation from ideal is)
        double staticDevEntropy = RotationEntropyUtils.computeStaticDevEntropy(
            recentRotations, recentIdeal, ENTROPY_ANALYSIS_SIZE
        );
        
        // Calculate dynamic deviation entropy (how random the deviation changes are)
        double dynamicDevEntropy = RotationEntropyUtils.computeDynamicDevEntropy(
            recentRotations, recentIdeal, ENTROPY_ANALYSIS_SIZE
        );
        
        // Average entropy (lower = more detectable)
        double avgEntropy = (pathEntropy + staticDevEntropy + dynamicDevEntropy) / 3.0;
        lastAvgEntropy = avgEntropy;
        
        if (avgEntropy < 0) {
            // Not enough data yet
            return;
        }
        
        // Adapt smoothing factor based on entropy
        // Lower entropy = use more curve smoothing to break patterns
        if (avgEntropy < VERY_LOW_ENTROPY_THRESHOLD) {
            // Very detectable: use strong curve smoothing (factor > 1.0)
            currentSmoothingFactor = 1.8;
            adaptiveRandomization = 0.8;  // Add significant randomization
        } else if (avgEntropy < LOW_ENTROPY_THRESHOLD) {
            // Detectable: use moderate curve smoothing
            currentSmoothingFactor = 1.4;
            adaptiveRandomization = 0.4;
        } else if (avgEntropy < 0.5) {
            // Somewhat detectable: use slight curve smoothing
            currentSmoothingFactor = 1.2;
            adaptiveRandomization = 0.2;
        } else {
            // Good entropy: use near-linear smoothing (slight curve for natural feel)
            currentSmoothingFactor = 1.05;
            adaptiveRandomization = 0.0;
        }
    }
    
    /**
     * Get current entropy metrics for debugging/monitoring.
     * 
     * @return array [pathEntropy, staticDevEntropy, dynamicDevEntropy, avgEntropy]
     */
    public double[] getEntropyMetrics() {
        List<RotationData> recentRotations = new ArrayList<>(rotationHistory);
        List<RotationData> recentIdeal = new ArrayList<>(idealRotationHistory);
        
        if (recentRotations.size() < 50 || recentIdeal.size() < 50) {
            return new double[]{-1, -1, -1, -1};
        }
        
        double pathEntropy = RotationEntropyUtils.computePathEntropy(
            recentRotations, ENTROPY_ANALYSIS_SIZE
        );
        double staticDevEntropy = RotationEntropyUtils.computeStaticDevEntropy(
            recentRotations, recentIdeal, ENTROPY_ANALYSIS_SIZE
        );
        double dynamicDevEntropy = RotationEntropyUtils.computeDynamicDevEntropy(
            recentRotations, recentIdeal, ENTROPY_ANALYSIS_SIZE
        );
        
        double avgEntropy = (pathEntropy + staticDevEntropy + dynamicDevEntropy) / 3.0;
        
        return new double[]{pathEntropy, staticDevEntropy, dynamicDevEntropy, avgEntropy};
    }
    
    /**
     * Reset rotation history (call when disabling module or switching targets).
     */
    public void reset() {
        rotationHistory.clear();
        idealRotationHistory.clear();
        currentSmoothingFactor = 1.0;
        adaptiveRandomization = 0.0;
        lastYaw = 0.0f;
        lastPitch = 0.0f;
    }
    
    /**
     * Get the current adaptive smoothing factor.
     */
    public double getSmoothingFactor() {
        return currentSmoothingFactor;
    }
    
    /**
     * Get the current adaptive randomization amount.
     */
    public double getRandomization() {
        return adaptiveRandomization;
    }
}


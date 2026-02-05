package keystrokesmod.module.impl.combat.killaura;

import akka.japi.Pair;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.other.RotationHandler;
import keystrokesmod.script.classes.Vec3;
import keystrokesmod.utility.MoveUtil;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.aim.AimSimulator;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Advanced Rotation Mode - Comprehensive anti-detection rotation system
 * 
 * Designed to bypass:
 * - MX Anti-Cheat (Entropy, Distinct, Factor, Constant, Analysis, Statistics)
 * - Intave (Kalysa strafe-rotation, Heuristics patterns, deltaVL)
 * - Machine Learning based detection systems
 * 
 * Key features:
 * - Adaptive entropy management
 * - Strafe-rotation correlation breaking
 * - Dynamic GCD simulation
 * - Pattern diversity enforcement
 * - Combat phase awareness
 */
public class AdvancedRotationMode {
    private final KillAura parent;
    private final AimSimulator aimSimulator;
    private final Minecraft mc = Minecraft.getMinecraft();
    
    // Rotation state
    private float[] currentRotations = new float[]{0, 0};
    private float lastYaw = 0f;
    private float lastPitch = 0f;
    private float yawVelocity = 0f;
    private float pitchVelocity = 0f;
    
    // Entropy management (MX bypass)
    private final ArrayDeque<Float> yawDeltaWindow = new ArrayDeque<>(12);
    private final ArrayDeque<Float> pitchDeltaWindow = new ArrayDeque<>(12);
    private double prevYawEntropy = Double.NaN;
    private double prevPitchEntropy = Double.NaN;
    private int entropyTick = 0;
    private float lastAppliedYawDelta = 0f;
    private float lastAppliedPitchDelta = 0f;
    
    // Strafe-rotation correlation (Intave Kalysa bypass)
    private double lastStrafeValue = 0.0;
    private float lastMovementYaw = 0f;
    private int strafeDesyncTicks = 0;
    private boolean strafeDesyncActive = false;
    
    // Pattern diversity (Anti-ML)
    private final ArrayDeque<Float> recentYawSteps = new ArrayDeque<>(20);
    private final ArrayDeque<Float> recentPitchSteps = new ArrayDeque<>(20);
    private int patternBreakTicks = 0;
    private boolean patternBreakActive = false;
    
    // GCD simulation
    private float simulatedSensitivity = -1f;
    private float currentGcd = 0f;
    private int gcdVarianceTicks = 0;
    
    // Combat phase tracking
    private long lastAttackTime = 0L;
    private long combatStartTime = 0L;
    private int consecutiveHits = 0;
    private boolean inActiveCombat = false;
    
    // Micro-correction system
    private float microCorrectionYaw = 0f;
    private float microCorrectionPitch = 0f;
    private int microCorrectionTicks = 0;
    private boolean microCorrectionActive = false;
    
    // Jitter prevention
    private boolean jitterFlip = false;
    private long lastUpdateMs = 0L;
    
    // Aim point history for smooth transitions
    private Vec3 lastAimPoint = null;
    private Vec3 smoothedAimPoint = null;
    
    // Factor bypass (MX) - prevent small-big-small patterns
    private final ArrayDeque<Float> factorYawHistory = new ArrayDeque<>(5);
    private final ArrayDeque<Float> factorPitchHistory = new ArrayDeque<>(5);
    
    // Constant bypass - GCD ratio tracking
    private float constantLastYawStep = 0f;
    private float constantLastPitchStep = 0f;
    
    public AdvancedRotationMode(KillAura parent, AimSimulator aimSimulator) {
        this.parent = parent;
        this.aimSimulator = aimSimulator;
    }
    
    public void reset() {
        if (mc.thePlayer != null) {
            currentRotations = new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
            lastYaw = mc.thePlayer.rotationYaw;
            lastPitch = mc.thePlayer.rotationPitch;
        }
        
        yawVelocity = 0f;
        pitchVelocity = 0f;
        
        yawDeltaWindow.clear();
        pitchDeltaWindow.clear();
        prevYawEntropy = Double.NaN;
        prevPitchEntropy = Double.NaN;
        entropyTick = 0;
        lastAppliedYawDelta = 0f;
        lastAppliedPitchDelta = 0f;
        
        lastStrafeValue = 0.0;
        lastMovementYaw = 0f;
        strafeDesyncTicks = 0;
        strafeDesyncActive = false;
        
        recentYawSteps.clear();
        recentPitchSteps.clear();
        patternBreakTicks = 0;
        patternBreakActive = false;
        
        simulatedSensitivity = -1f;
        currentGcd = 0f;
        gcdVarianceTicks = 0;
        
        lastAttackTime = 0L;
        combatStartTime = 0L;
        consecutiveHits = 0;
        inActiveCombat = false;
        
        microCorrectionYaw = 0f;
        microCorrectionPitch = 0f;
        microCorrectionTicks = 0;
        microCorrectionActive = false;
        
        jitterFlip = false;
        lastUpdateMs = 0L;
        
        lastAimPoint = null;
        smoothedAimPoint = null;
        
        factorYawHistory.clear();
        factorPitchHistory.clear();
        
        constantLastYawStep = 0f;
        constantLastPitchStep = 0f;
    }
    
    /**
     * Main rotation calculation method
     */
    public float[] getRotations(EntityLivingBase target, AdvancedSettings settings) {
        if (target == null || target.isDead) {
            return handleNoTarget(settings);
        }
        
        long now = System.currentTimeMillis();
        updateCombatState(now);
        updateGcdSimulation(settings);
        
        // Get base aim point with prediction
        Vec3 aimPoint = calculateAimPoint(target, settings);
        
        // Apply aim point smoothing for natural movement
        aimPoint = smoothAimPoint(aimPoint, settings);
        
        // Calculate raw target rotations
        float[] targetRots = calculateTargetRotations(aimPoint);
        float targetYaw = targetRots[0];
        float targetPitch = targetRots[1];
        
        // Calculate deltas
        float yawDelta = RotationUtils.normalize(targetYaw - currentRotations[0]);
        float pitchDelta = targetPitch - currentRotations[1];
        
        // Apply rotation speed limiting
        float[] limitedDeltas = applySpeedLimiting(yawDelta, pitchDelta, target, settings);
        yawDelta = limitedDeltas[0];
        pitchDelta = limitedDeltas[1];
        
        // Apply strafe-rotation desync (Intave Kalysa bypass)
        float[] desyncedDeltas = applyStrafeDesync(yawDelta, pitchDelta, settings);
        yawDelta = desyncedDeltas[0];
        pitchDelta = desyncedDeltas[1];
        
        // Apply entropy bypass (MX bypass)
        float[] entropyDeltas = applyEntropyBypass(yawDelta, pitchDelta, settings);
        yawDelta = entropyDeltas[0];
        pitchDelta = entropyDeltas[1];
        
        // Apply factor bypass (MX - prevent small-big-small patterns)
        float[] factorDeltas = applyFactorBypass(yawDelta, pitchDelta, settings);
        yawDelta = factorDeltas[0];
        pitchDelta = factorDeltas[1];
        
        // Apply constant bypass (MX GCD patterns)
        float[] constantDeltas = applyConstantBypass(yawDelta, pitchDelta, settings);
        yawDelta = constantDeltas[0];
        pitchDelta = constantDeltas[1];
        
        // Apply micro-corrections for hit rate
        float[] correctedDeltas = applyMicroCorrections(yawDelta, pitchDelta, target, settings);
        yawDelta = correctedDeltas[0];
        pitchDelta = correctedDeltas[1];
        
        // Apply pattern diversity (Anti-ML)
        float[] diverseDeltas = applyPatternDiversity(yawDelta, pitchDelta, settings);
        yawDelta = diverseDeltas[0];
        pitchDelta = diverseDeltas[1];
        
        // Quantize to GCD
        yawDelta = quantizeToGcd(yawDelta, settings);
        pitchDelta = quantizeToGcd(pitchDelta, settings);
        
        // Apply final deltas
        float newYaw = MathHelper.wrapAngleTo180_float(currentRotations[0] + yawDelta);
        float newPitch = MathHelper.clamp_float(currentRotations[1] + pitchDelta, -90f, 90f);
        
        // Prevent flat pitch (MX/AGC AimAssistC bypass)
        if (shouldAllowJitter(target) && Math.abs(yawDelta) > 2.5f && Math.abs(pitchDelta) < 0.02f) {
            float minPitch = currentGcd * (jitterFlip ? 1f : -1f);
            newPitch = MathHelper.clamp_float(currentRotations[1] + minPitch, -90f, 90f);
            jitterFlip = !jitterFlip;
        }
        
        // Apply modulo bypass for silent rotations
        if (parent.rotationMode.getInput() == 1 && settings.useModuloBypass) {
            newYaw = applyModuloBypass(newYaw);
        }
        
        // Update state
        lastAppliedYawDelta = yawDelta;
        lastAppliedPitchDelta = pitchDelta;
        currentRotations[0] = newYaw;
        currentRotations[1] = newPitch;
        lastYaw = newYaw;
        lastPitch = newPitch;
        lastUpdateMs = now;
        
        // Track deltas for entropy
        trackDeltas(yawDelta, pitchDelta);
        
        return currentRotations;
    }
    
    private float[] handleNoTarget(AdvancedSettings settings) {
        inActiveCombat = false;
        consecutiveHits = 0;
        
        // Smooth return to player rotation
        float playerYaw = RotationHandler.getRotationYaw();
        float playerPitch = RotationHandler.getRotationPitch();
        
        currentRotations[0] = playerYaw;
        currentRotations[1] = playerPitch;
        
        return currentRotations;
    }
    
    private void updateCombatState(long now) {
        long timeSinceLastAttack = now - lastAttackTime;
        
        if (timeSinceLastAttack < 500) {
            if (!inActiveCombat) {
                combatStartTime = now;
                inActiveCombat = true;
            }
        } else if (timeSinceLastAttack > 2000) {
            inActiveCombat = false;
            consecutiveHits = 0;
        }
    }
    
    public void onAttack() {
        lastAttackTime = System.currentTimeMillis();
        consecutiveHits++;
    }
    
    private void updateGcdSimulation(AdvancedSettings settings) {
        if (simulatedSensitivity < 0) {
            simulatedSensitivity = mc.gameSettings.mouseSensitivity;
        }
        
        // Periodically vary sensitivity simulation to avoid pattern detection
        gcdVarianceTicks++;
        if (gcdVarianceTicks >= settings.gcdVarianceInterval && settings.enableGcdVariance) {
            gcdVarianceTicks = 0;
            float variance = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * settings.gcdVarianceAmount);
            simulatedSensitivity = MathHelper.clamp_float(
                mc.gameSettings.mouseSensitivity + variance,
                0.1f, 1.0f
            );
        }
        
        // Calculate GCD
        float sens = simulatedSensitivity * 0.6f + 0.2f;
        currentGcd = sens * sens * sens * 1.2f;
    }
    
    /**
     * Calculate optimal aim point with prediction and hitbox analysis
     */
    private Vec3 calculateAimPoint(EntityLivingBase target, AdvancedSettings settings) {
        Vec3 eyePos = Utils.getEyePos();
        AxisAlignedBB box = target.getEntityBoundingBox();
        
        // Apply prediction based on target velocity
        if (settings.enablePrediction && settings.predictionFactor > 0) {
            double vx = target.posX - target.lastTickPosX;
            double vy = target.posY - target.lastTickPosY;
            double vz = target.posZ - target.lastTickPosZ;
            
            float lead = settings.predictionFactor * (float) (mc.thePlayer.getDistanceToEntity(target) / 4.0);
            lead = Math.min(lead, settings.maxPredictionLead);
            
            box = box.offset(vx * lead, vy * lead, vz * lead);
        }
        
        // Select aim point based on mode
        Vec3 aimPoint;
        switch (settings.aimPointMode) {
            case CENTER:
                aimPoint = new Vec3(
                    (box.minX + box.maxX) / 2,
                    (box.minY + box.maxY) / 2,
                    (box.minZ + box.maxZ) / 2
                );
                break;
            case CHEST:
                aimPoint = new Vec3(
                    (box.minX + box.maxX) / 2,
                    target.posY + target.getEyeHeight() * 0.65,
                    (box.minZ + box.maxZ) / 2
                );
                break;
            case HEAD:
                aimPoint = new Vec3(
                    (box.minX + box.maxX) / 2,
                    target.posY + target.getEyeHeight() * 0.9,
                    (box.minZ + box.maxZ) / 2
                );
                break;
            case NEAREST:
            default:
                aimPoint = findNearestPoint(box, eyePos, settings);
                break;
        }
        
        // Add controlled noise for anti-detection
        if (settings.enableNoise && shouldAllowJitter(target)) {
            float noiseScale = settings.noiseScale;
            
            // Reduce noise when close
            double dist = mc.thePlayer.getDistanceToEntity(target);
            if (dist < settings.noiseReduceRange) {
                noiseScale *= (float) (dist / settings.noiseReduceRange);
            }
            
            double noiseX = (ThreadLocalRandom.current().nextDouble() - 0.5) * noiseScale;
            double noiseY = (ThreadLocalRandom.current().nextDouble() - 0.5) * noiseScale * 0.5;
            double noiseZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * noiseScale;
            
            aimPoint = new Vec3(
                aimPoint.x() + noiseX,
                MathHelper.clamp_double(aimPoint.y() + noiseY, box.minY + 0.1, box.maxY - 0.1),
                aimPoint.z() + noiseZ
            );
        }
        
        return aimPoint;
    }
    
    private Vec3 findNearestPoint(AxisAlignedBB box, Vec3 eyePos, AdvancedSettings settings) {
        // Sample multiple points and find the one requiring minimum rotation
        double bestScore = Double.MAX_VALUE;
        Vec3 bestPoint = new Vec3((box.minX + box.maxX) / 2, (box.minY + box.maxY) / 2, (box.minZ + box.maxZ) / 2);
        
        // Grid sample the bounding box
        int samples = settings.nearestSampleCount;
        for (int xi = 0; xi < samples; xi++) {
            for (int yi = 0; yi < samples; yi++) {
                for (int zi = 0; zi < samples; zi++) {
                    double t = (samples > 1) ? 1.0 / (samples - 1) : 0.5;
                    double x = box.minX + (box.maxX - box.minX) * (xi * t + 0.1 * (1 - t));
                    double y = box.minY + (box.maxY - box.minY) * (yi * t + 0.1 * (1 - t));
                    double z = box.minZ + (box.maxZ - box.minZ) * (zi * t + 0.1 * (1 - t));
                    
                    // Calculate rotation required
                    double dx = x - eyePos.x();
                    double dy = y - eyePos.y();
                    double dz = z - eyePos.z();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    
                    float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
                    float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
                    
                    // Score based on rotation delta
                    float yawDelta = Math.abs(RotationUtils.normalize(yaw - currentRotations[0]));
                    float pitchDelta = Math.abs(pitch - currentRotations[1]);
                    double score = yawDelta + pitchDelta * settings.pitchPreference;
                    
                    if (score < bestScore) {
                        bestScore = score;
                        bestPoint = new Vec3(x, y, z);
                    }
                }
            }
        }
        
        return bestPoint;
    }
    
    private Vec3 smoothAimPoint(Vec3 newPoint, AdvancedSettings settings) {
        if (lastAimPoint == null || !settings.enableAimSmoothing) {
            lastAimPoint = newPoint;
            smoothedAimPoint = newPoint;
            return newPoint;
        }
        
        float smoothFactor = settings.aimSmoothFactor;
        
        smoothedAimPoint = new Vec3(
            smoothedAimPoint.x() + (newPoint.x() - smoothedAimPoint.x()) * smoothFactor,
            smoothedAimPoint.y() + (newPoint.y() - smoothedAimPoint.y()) * smoothFactor,
            smoothedAimPoint.z() + (newPoint.z() - smoothedAimPoint.z()) * smoothFactor
        );
        
        lastAimPoint = newPoint;
        return smoothedAimPoint;
    }
    
    private float[] calculateTargetRotations(Vec3 point) {
        Vec3 eyePos = Utils.getEyePos();
        double dx = point.x() - eyePos.x();
        double dy = point.y() - eyePos.y();
        double dz = point.z() - eyePos.z();
        
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        
        return new float[]{MathHelper.wrapAngleTo180_float(yaw), MathHelper.clamp_float(pitch, -90f, 90f)};
    }
    
    private float[] applySpeedLimiting(float yawDelta, float pitchDelta, EntityLivingBase target, AdvancedSettings settings) {
        float maxYawSpeed = settings.maxYawSpeed;
        float maxPitchSpeed = settings.maxPitchSpeed;
        
        // Reduce speed based on distance (more natural)
        double dist = mc.thePlayer.getDistanceToEntity(target);
        if (dist < 2.5) {
            float distFactor = (float) (0.5 + dist / 5.0);
            maxYawSpeed *= distFactor;
            maxPitchSpeed *= distFactor;
        }
        
        // Add some randomization to avoid constant speed detection
        if (settings.enableSpeedRandomization) {
            float variance = 1.0f + (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * settings.speedRandomizationAmount);
            maxYawSpeed *= variance;
            maxPitchSpeed *= variance;
        }
        
        // Inertia-based smoothing
        float accel = settings.rotationAcceleration;
        yawVelocity += (yawDelta - yawVelocity) * accel;
        pitchVelocity += (pitchDelta - pitchVelocity) * accel;
        
        // Clamp velocities
        yawVelocity = MathHelper.clamp_float(yawVelocity, -maxYawSpeed, maxYawSpeed);
        pitchVelocity = MathHelper.clamp_float(pitchVelocity, -maxPitchSpeed, maxPitchSpeed);
        
        return new float[]{yawVelocity, pitchVelocity};
    }
    
    /**
     * Strafe-rotation desync to bypass Intave Kalysa detection
     * Kalysa detects when rotation changes perfectly correlate with strafe input changes
     */
    private float[] applyStrafeDesync(float yawDelta, float pitchDelta, AdvancedSettings settings) {
        if (!settings.enableStrafeDesync) {
            return new float[]{yawDelta, pitchDelta};
        }
        
        // Calculate current strafe value
        double motionX = mc.thePlayer.posX - mc.thePlayer.lastTickPosX;
        double motionZ = mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ;
        double strafeValue = motionX * motionX + motionZ * motionZ;
        
        // Check for strafe change
        double strafeDiff = Math.abs(strafeValue - lastStrafeValue);
        boolean strafeChanged = strafeDiff > 1e-5;
        lastStrafeValue = strafeValue;
        
        // If strafe changed and yaw also changed significantly, introduce desync
        if (strafeChanged && Math.abs(yawDelta) > 3.0f) {
            strafeDesyncTicks++;
            
            if (strafeDesyncTicks > settings.strafeDesyncThreshold) {
                strafeDesyncActive = true;
                strafeDesyncTicks = 0;
                
                // Delay the rotation slightly or modify it
                float desyncFactor = 0.7f + (float) (ThreadLocalRandom.current().nextDouble() * 0.3);
                yawDelta *= desyncFactor;
                
                // Or add a slight opposite movement to break correlation
                if (ThreadLocalRandom.current().nextBoolean()) {
                    float jitter = currentGcd * (ThreadLocalRandom.current().nextBoolean() ? 1f : -1f);
                    yawDelta += jitter;
                }
            }
        } else {
            strafeDesyncTicks = 0;
            strafeDesyncActive = false;
        }
        
        return new float[]{yawDelta, pitchDelta};
    }
    
    /**
     * Entropy bypass for MX Anti-Cheat
     * MX detects when Shannon entropy of yaw/pitch windows are identical or too similar
     */
    private float[] applyEntropyBypass(float yawDelta, float pitchDelta, AdvancedSettings settings) {
        if (!settings.enableEntropyBypass) {
            return new float[]{yawDelta, pitchDelta};
        }
        
        entropyTick++;
        
        // Build candidate windows
        ArrayDeque<Float> nextYawWindow = new ArrayDeque<>(yawDeltaWindow);
        ArrayDeque<Float> nextPitchWindow = new ArrayDeque<>(pitchDeltaWindow);
        nextYawWindow.addLast(yawDelta);
        nextPitchWindow.addLast(pitchDelta);
        if (nextYawWindow.size() > 10) nextYawWindow.pollFirst();
        if (nextPitchWindow.size() > 10) nextPitchWindow.pollFirst();
        
        double yawEntropy = computeShannonEntropy(nextYawWindow);
        double pitchEntropy = computeShannonEntropy(nextPitchWindow);
        
        // Check for perfect entropy match with previous window
        if (!Double.isNaN(prevYawEntropy) && Math.abs(yawEntropy - prevYawEntropy) < 1e-5) {
            // Nudge yaw to change entropy
            yawDelta = entropyNudge(yawDelta, currentGcd, settings.maxYawSpeed);
        }
        
        if (!Double.isNaN(prevPitchEntropy) && Math.abs(pitchEntropy - prevPitchEntropy) < 1e-5) {
            // Nudge pitch to change entropy
            pitchDelta = entropyNudge(pitchDelta, currentGcd, settings.maxPitchSpeed);
        }
        
        // Check for yaw/pitch entropy similarity
        if (Math.abs(yawEntropy - pitchEntropy) < 1e-5) {
            // Make them different
            pitchDelta = entropyNudge(pitchDelta, currentGcd, settings.maxPitchSpeed);
        }
        
        // Update previous entropy values
        prevYawEntropy = yawEntropy;
        prevPitchEntropy = pitchEntropy;
        
        // Periodically duplicate values to add pattern variation
        if (entropyTick % settings.entropyDuplicateInterval == 0 && Math.abs(lastAppliedYawDelta) > 1e-5) {
            yawDelta = lastAppliedYawDelta; // Reuse previous value
        }
        
        return new float[]{yawDelta, pitchDelta};
    }
    
    private float entropyNudge(float value, float gcd, float maxStep) {
        float nudge = gcd * (jitterFlip ? 1f : -1f);
        jitterFlip = !jitterFlip;
        return MathHelper.clamp_float(value + nudge, -maxStep, maxStep);
    }
    
    private double computeShannonEntropy(Iterable<Float> data) {
        Map<Double, Integer> freq = new HashMap<>();
        int count = 0;
        for (Float f : data) {
            freq.merge((double) f, 1, Integer::sum);
            count++;
        }
        if (count == 0) return 0.0;
        
        double entropy = 0.0;
        for (Integer c : freq.values()) {
            double p = (double) c / count;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }
    
    /**
     * Factor bypass for MX Anti-Cheat
     * MX detects small-big-small patterns (e.g., 0.5, 40, 0.5 degree rotations)
     */
    private float[] applyFactorBypass(float yawDelta, float pitchDelta, AdvancedSettings settings) {
        if (!settings.enableFactorBypass) {
            return new float[]{yawDelta, pitchDelta};
        }
        
        float absYaw = Math.abs(yawDelta);
        float absPitch = Math.abs(pitchDelta);
        
        // Check for huge rotation
        if (absYaw > 35.0f) {
            // Check history for small-big pattern
            boolean hasSmallBefore = false;
            for (Float f : factorYawHistory) {
                if (Math.abs(f) < 1.5f) {
                    hasSmallBefore = true;
                    break;
                }
            }
            
            if (hasSmallBefore) {
                // Spread the rotation across multiple ticks
                float maxAllowed = 28.0f + (float) (ThreadLocalRandom.current().nextDouble() * 7.0);
                yawDelta = Math.copySign(Math.min(absYaw, maxAllowed), yawDelta);
            }
        }
        
        // Track history
        factorYawHistory.addLast(yawDelta);
        factorPitchHistory.addLast(pitchDelta);
        if (factorYawHistory.size() > 3) factorYawHistory.pollFirst();
        if (factorPitchHistory.size() > 3) factorPitchHistory.pollFirst();
        
        return new float[]{yawDelta, pitchDelta};
    }
    
    /**
     * Constant bypass for MX Anti-Cheat
     * MX detects abnormal GCD values and ratio patterns
     */
    private float[] applyConstantBypass(float yawDelta, float pitchDelta, AdvancedSettings settings) {
        if (!settings.enableConstantBypass) {
            return new float[]{yawDelta, pitchDelta};
        }
        
        // Use a coarser grid to ensure GCD is above detection threshold
        float grid = Math.max(currentGcd, 0.05f);
        yawDelta = Math.round(yawDelta / grid) * grid;
        pitchDelta = Math.round(pitchDelta / grid) * grid;
        
        // Ratio clamping vs previous step
        if (Math.abs(constantLastYawStep) > 1e-4f) {
            float ratio = yawDelta / constantLastYawStep;
            float clampedRatio = MathHelper.clamp_float(ratio, -8f, 8f);
            if (Math.abs(clampedRatio) < 0.2f) {
                clampedRatio = 0.2f * Math.signum(clampedRatio == 0 ? 1f : clampedRatio);
            }
            yawDelta = constantLastYawStep * clampedRatio;
            yawDelta = Math.round(yawDelta / grid) * grid;
        }
        
        if (Math.abs(constantLastPitchStep) > 1e-4f) {
            float ratio = pitchDelta / constantLastPitchStep;
            float clampedRatio = MathHelper.clamp_float(ratio, -6f, 6f);
            if (Math.abs(clampedRatio) < 0.25f) {
                clampedRatio = 0.25f * Math.signum(clampedRatio == 0 ? 1f : clampedRatio);
            }
            pitchDelta = constantLastPitchStep * clampedRatio;
            pitchDelta = Math.round(pitchDelta / grid) * grid;
        }
        
        constantLastYawStep = yawDelta;
        constantLastPitchStep = pitchDelta;
        
        return new float[]{yawDelta, pitchDelta};
    }
    
    /**
     * Micro-corrections to improve hit rate when close to target
     */
    private float[] applyMicroCorrections(float yawDelta, float pitchDelta, EntityLivingBase target, AdvancedSettings settings) {
        if (!settings.enableMicroCorrections) {
            return new float[]{yawDelta, pitchDelta};
        }
        
        double dist = mc.thePlayer.getDistanceToEntity(target);
        if (dist > settings.microCorrectionRange) {
            microCorrectionActive = false;
            return new float[]{yawDelta, pitchDelta};
        }
        
        // Check if we're close to aiming at target
        float yawToTarget = Math.abs(RotationUtils.normalize(yawDelta));
        float pitchToTarget = Math.abs(pitchDelta);
        
        if (yawToTarget < 5.0f && pitchToTarget < 5.0f) {
            microCorrectionTicks++;
            
            if (microCorrectionTicks >= settings.microCorrectionDelay) {
                // Apply small correction boost
                float boostFactor = 1.0f + settings.microCorrectionStrength;
                yawDelta *= boostFactor;
                pitchDelta *= boostFactor;
                
                microCorrectionActive = true;
                microCorrectionTicks = 0;
            }
        } else {
            microCorrectionTicks = 0;
            microCorrectionActive = false;
        }
        
        return new float[]{yawDelta, pitchDelta};
    }
    
    /**
     * Pattern diversity to avoid ML detection
     */
    private float[] applyPatternDiversity(float yawDelta, float pitchDelta, AdvancedSettings settings) {
        if (!settings.enablePatternDiversity) {
            return new float[]{yawDelta, pitchDelta};
        }
        
        recentYawSteps.addLast(yawDelta);
        recentPitchSteps.addLast(pitchDelta);
        if (recentYawSteps.size() > 15) recentYawSteps.pollFirst();
        if (recentPitchSteps.size() > 15) recentPitchSteps.pollFirst();
        
        // Check for repetitive patterns
        if (recentYawSteps.size() >= 10) {
            int distinctYaw = new HashSet<>(recentYawSteps).size();
            
            // If too few distinct values, add variance
            if (distinctYaw < 5) {
                patternBreakTicks++;
                
                if (patternBreakTicks >= settings.patternBreakThreshold) {
                    patternBreakActive = true;
                    patternBreakTicks = 0;
                    
                    // Add controlled variance
                    float variance = currentGcd * (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 2);
                    yawDelta += variance;
                }
            } else {
                patternBreakTicks = 0;
                patternBreakActive = false;
            }
        }
        
        // Avoid consecutive identical jiff deltas (MX AimSmoothCheck)
        if (recentYawSteps.size() >= 3) {
            Float[] arr = recentYawSteps.toArray(new Float[0]);
            int n = arr.length;
            if (n >= 3) {
                float d1 = arr[n-1] - arr[n-2];
                float d2 = arr[n-2] - arr[n-3];
                
                // If jiff delta is zero for consecutive samples, add variance
                if (Math.abs(d1) < 1e-5 && Math.abs(d2) < 1e-5) {
                    yawDelta += currentGcd * (jitterFlip ? 1f : -1f);
                    jitterFlip = !jitterFlip;
                }
            }
        }
        
        return new float[]{yawDelta, pitchDelta};
    }
    
    private float quantizeToGcd(float delta, AdvancedSettings settings) {
        if (!settings.enableGcdQuantization) {
            return delta;
        }
        return Math.round(delta / currentGcd) * currentGcd;
    }
    
    private float applyModuloBypass(float yaw) {
        // Add offset to avoid modulo edge detection
        if (Math.abs(yaw) < 270f) {
            float offset = 720f;
            float sign = yaw == 0 ? 1f : Math.signum(yaw);
            yaw += sign * offset;
        }
        return yaw;
    }
    
    private void trackDeltas(float yawDelta, float pitchDelta) {
        yawDeltaWindow.addLast(yawDelta);
        pitchDeltaWindow.addLast(pitchDelta);
        if (yawDeltaWindow.size() > 10) yawDeltaWindow.pollFirst();
        if (pitchDeltaWindow.size() > 10) pitchDeltaWindow.pollFirst();
    }
    
    private boolean shouldAllowJitter(EntityLivingBase target) {
        if (target == null) return false;
        
        // Check if player is moving
        double playerDx = mc.thePlayer.posX - mc.thePlayer.lastTickPosX;
        double playerDz = mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ;
        boolean playerMoving = (playerDx * playerDx + playerDz * playerDz) > 1e-5;
        
        // Check if target is moving
        double targetDx = target.posX - target.lastTickPosX;
        double targetDz = target.posZ - target.lastTickPosZ;
        boolean targetMoving = (targetDx * targetDx + targetDz * targetDz) > 1e-5;
        
        return playerMoving || targetMoving;
    }
    
    public float[] getCurrentRotations() {
        return currentRotations;
    }
    
    public void setRotations(float[] rotations) {
        this.currentRotations = rotations;
        this.lastYaw = rotations[0];
        this.lastPitch = rotations[1];
    }
    
    public long getLastUpdateMs() {
        return lastUpdateMs;
    }
    
    /**
     * Settings class for the Advanced rotation mode
     */
    public static class AdvancedSettings {
        // General
        public float maxYawSpeed = 12.0f;
        public float maxPitchSpeed = 8.0f;
        public float rotationAcceleration = 0.4f;
        public boolean enableSpeedRandomization = true;
        public float speedRandomizationAmount = 0.2f;
        
        // Aim point
        public AimPointMode aimPointMode = AimPointMode.NEAREST;
        public int nearestSampleCount = 3;
        public float pitchPreference = 1.3f; // Higher = prefer less pitch movement
        
        // Prediction
        public boolean enablePrediction = true;
        public float predictionFactor = 2.0f;
        public float maxPredictionLead = 5.0f;
        
        // Noise
        public boolean enableNoise = true;
        public float noiseScale = 0.15f;
        public float noiseReduceRange = 2.5f;
        
        // Smoothing
        public boolean enableAimSmoothing = true;
        public float aimSmoothFactor = 0.7f;
        
        // GCD
        public boolean enableGcdQuantization = true;
        public boolean enableGcdVariance = true;
        public int gcdVarianceInterval = 40;
        public float gcdVarianceAmount = 0.05f;
        
        // Strafe desync (Intave)
        public boolean enableStrafeDesync = true;
        public int strafeDesyncThreshold = 3;
        
        // Entropy (MX)
        public boolean enableEntropyBypass = true;
        public int entropyDuplicateInterval = 6;
        
        // Factor (MX)
        public boolean enableFactorBypass = true;
        
        // Constant (MX)
        public boolean enableConstantBypass = true;
        
        // Micro-corrections
        public boolean enableMicroCorrections = true;
        public float microCorrectionRange = 3.5f;
        public int microCorrectionDelay = 2;
        public float microCorrectionStrength = 0.15f;
        
        // Pattern diversity (ML)
        public boolean enablePatternDiversity = true;
        public int patternBreakThreshold = 5;
        
        // Modulo bypass
        public boolean useModuloBypass = true;
    }
    
    public enum AimPointMode {
        NEAREST,
        CENTER,
        CHEST,
        HEAD
    }
}

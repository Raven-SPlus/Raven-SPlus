package keystrokesmod.module.impl.combat.killaura;

import akka.japi.Pair;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.other.RotationHandler;
import keystrokesmod.utility.MoveUtil;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.aim.AimSimulator;
import keystrokesmod.utility.aim.RotationEnhancer;
import keystrokesmod.utility.render.Animation;
import keystrokesmod.utility.render.Easing;
import keystrokesmod.utility.render.RenderUtils;
import keystrokesmod.script.classes.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public class KillAuraRotation {
    private final KillAura parent;
    private final Minecraft mc = Minecraft.getMinecraft();
    
    // Components
    private final AimSimulator aimSimulator = new AimSimulator();
    private final RotationEnhancer rotationEnhancer = new RotationEnhancer();
    
    // Visuals
    private @Nullable Animation animationX;
    private @Nullable Animation animationY;
    private @Nullable Animation animationZ;
    
    // State
    private float[] rotations = new float[]{0, 0};
    private EntityLivingBase previousTarget;
    private boolean isInterpolating = false;
    private long interpolationStartTime = 0;
    
    // V2 algorithm state
    private float lastYaw;
    private float lastPitch;
    
    // Smooth deceleration
    private float lastValidTargetYaw = 0.0f;
    private float lastValidTargetPitch = 0.0f;
    private boolean hasValidTarget = false;
    private float yawVelocity = 0.0f;
    private float pitchVelocity = 0.0f;
    private long lastUpdateTime = 0L;
    // General mode smoothing helpers
    private float generalYawCarry = 0.0f;
    private float generalPitchCarry = 0.0f;
    private boolean grimReady = false;
    private float lastGrimYaw = 0f;
    private float lastGrimPitch = 0f;
    private boolean jitterFlip = false;
    // Entropy bypass (General rotation) to desync yaw/pitch frequency buckets
    private int entropyTick = 0;
    private float entropyLastYawStep = 0f;
    private float entropyLastPitchStep = 0f;
    private boolean entropySkewFlip = false;
    private int entropyWindowIndex = 0;
    private boolean entropyWindowFlip = false;
    private final ArrayDeque<Float> entropyYawWindow = new ArrayDeque<>(12);
    private final ArrayDeque<Float> entropyPitchWindow = new ArrayDeque<>(12);
    private double entropyPrevYawEntropy = Double.NaN;
    private double entropyPrevPitchEntropy = Double.NaN;
    private float constantLastYawStep = 0f;
    private float constantLastPitchStep = 0f;
    
    // Overshoot
    private boolean isOvershooting = false;
    private boolean isCorrectingOvershoot = false;
    private float overshootTargetYaw = 0.0f;
    private float overshootTargetPitch = 0.0f;
    private float actualTargetYaw = 0.0f;
    private float actualTargetPitch = 0.0f;
    private int overshootTicks = 0;
    private static final int MAX_OVERSHOOT_TICKS = 5;
    private static final int MAX_CORRECTION_TICKS = 4;
    private long lastUpdateMs = 0L;

    public KillAuraRotation(KillAura parent) {
        this.parent = parent;
    }

    public void onEnable() {
        this.rotations = new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
        lastValidTargetYaw = mc.thePlayer.rotationYaw;
        lastValidTargetPitch = mc.thePlayer.rotationPitch;
        lastYaw = mc.thePlayer.rotationYaw;
        lastPitch = mc.thePlayer.rotationPitch;
        hasValidTarget = false;
        rotationEnhancer.reset();
        resetState();
    }

    public void onDisable() {
        rotationEnhancer.reset();
        
        // If disabling and we have modified rotations (e.g. +36000), ensure we snap back cleanly
        // to a valid client rotation to prevent flagging on the next packet.
        // However, simply resetting state here doesn't update the player's yaw/pitch field immediately.
        // The module's onDisable method should handle syncing the client player rotation if needed.
        // Here we just reset internal tracking.
        
        resetState();
        
        // Reset rotations to current player rotation to avoid using old, potentially large values
        if (mc.thePlayer != null) {
            this.rotations = new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
        }
    }
    
    public void resetState() {
        isInterpolating = false;
        previousTarget = null;
        isOvershooting = false;
        isCorrectingOvershoot = false;
        overshootTicks = 0;
        yawVelocity = 0.0f;
        pitchVelocity = 0.0f;
        lastUpdateTime = 0L;
        generalYawCarry = 0.0f;
        generalPitchCarry = 0.0f;
        grimReady = false;
        lastGrimYaw = 0f;
        lastGrimPitch = 0f;
        jitterFlip = false;
        entropyTick = 0;
        entropyLastYawStep = 0f;
        entropyLastPitchStep = 0f;
        entropySkewFlip = false;
        entropyWindowIndex = 0;
        entropyWindowFlip = false;
        entropyYawWindow.clear();
        entropyPitchWindow.clear();
        entropyPrevYawEntropy = Double.NaN;
        entropyPrevPitchEntropy = Double.NaN;
        constantLastYawStep = 0f;
        constantLastPitchStep = 0f;
    }

    /**
     * Jitter/extra noise should only apply while either the player or target is
     * actually moving. This keeps idle or stationary locking perfectly still.
     */
    private boolean shouldAllowJitter(EntityLivingBase target) {
        return isActuallyMoving(mc.thePlayer) || isActuallyMoving(target);
    }

    /**
     * "Actual movement" check based on position deltas (not input).
     * This better matches the user's expectation of "standing still".
     */
    private boolean isActuallyMoving(@Nullable EntityLivingBase entity) {
        if (entity == null || entity.isDead) return false;
        double dx = entity.posX - entity.lastTickPosX;
        double dy = entity.posY - entity.lastTickPosY;
        double dz = entity.posZ - entity.lastTickPosZ;
        // Squared distance threshold to ignore tiny floating error / rounding.
        // Note: packets/interpolation can cause micro-deltas even when "standing still".
        // Use a slightly larger epsilon so stationary aim doesn't jitter.
        return (dx * dx + dy * dy + dz * dz) > 1.0E-5;
    }

    public float[] getRotations(EntityLivingBase target) {
        // Select rotation algorithm
        int algo = parent.rotationAlgorithmMode != null ? (int) parent.rotationAlgorithmMode.getInput() : 0;
        if (algo == 1) {
            return getRotationsV2(target);
        } else if (algo == 2) {
            return getRotationsInertia(target);
        } else if (algo == 3) {
            return getRotationsGrim(target);
        } else if (algo == 4) {
            return getRotationsGeneralAdaptive(target);
        }
        return getRotationsClassic(target);
        }
        
    /**
     * Classic rotation algorithm (V1).
     * Separated for clearer maintenance across rotation modes.
     */
    private float[] getRotationsClassic(EntityLivingBase target) {
        // Configuration
        boolean playerMoving = MoveUtil.isMoving();
        boolean targetMoving = target != null && MoveUtil.isMoving(target);
        boolean allowJitter = shouldAllowJitter(target);
        aimSimulator.setJitterAllowed(allowJitter);
        rotationEnhancer.setJitterAllowed(allowJitter);

        aimSimulator.setNearest(parent.nearest.isToggled(), parent.nearestAccuracy.getInput());
        aimSimulator.setLazy(parent.lazy.isToggled(), parent.lazyAccuracy.getInput());
        
        final int prMode = (int) parent.pointRandomizationMode.getInput();
        boolean enableNoise = prMode != 0 && parent.noise.isToggled();
        if (!allowJitter) enableNoise = false;
        
        // Noise Logic
        float noiseMultiplier = 1.0f;
        if (enableNoise && parent.noiseRangeDecrease.isToggled() && target != null) {
            double distance = mc.thePlayer.getDistanceToEntity(target);
            double threshold = parent.noiseRangeDecreaseThreshold.getInput();
            double disableRange = parent.noiseRangeDecreaseDisable.getInput();
            
            if (distance <= disableRange) {
                enableNoise = false;
            } else if (distance < threshold) {
                double range = threshold - disableRange;
                if (range > 0) {
                    noiseMultiplier = (float) ((distance - disableRange) / range);
                    noiseMultiplier = Math.max(0.0f, Math.min(1.0f, noiseMultiplier));
                }
            }
        }
        
        float horizontalNoise = (float) parent.noiseHorizontal.getInput() * noiseMultiplier;
        float verticalNoise = (float) parent.noiseVertical.getInput() * noiseMultiplier;
        
        aimSimulator.setNoise(enableNoise,
                new Pair<>(horizontalNoise, verticalNoise),
                parent.noiseAimSpeed.getInput(), (long) parent.noiseDelay.getInput());
        
        int mappedMode = prMode == 0 ? 2 : prMode;
        aimSimulator.setPointRandomizationMode(allowJitter ? mappedMode : 0);
        aimSimulator.setDelay(parent.delayAim.isToggled(), (int) parent.delayAimAmount.getInput());

        boolean shouldMaintainRotation = parent.constant.isToggled() 
                && !parent.noAimToEntity() 
                && !(parent.constantOnlyIfNotMoving.isToggled() && (playerMoving || targetMoving));

        float targetYaw;
        float targetPitch;
        boolean hasTarget = target != null && !target.isDead;
        
        // Target Switch Interpolation
        if (hasTarget && isInterpolating && previousTarget != null && !previousTarget.isDead) {
            long elapsed = System.currentTimeMillis() - interpolationStartTime;
            float interpolationDuration = (float) parent.interpolationTime.getInput();
            float progress = interpolationDuration > 0 ? Math.min(1.0f, (float) elapsed / interpolationDuration) : 1.0f;
            
            Pair<Float, Float> oldResult = aimSimulator.getRotation(previousTarget);
            Pair<Float, Float> newResult = aimSimulator.getRotation(target);
            
            float oldYaw = oldResult.first();
            float oldPitch = oldResult.second();
            float newYaw = newResult.first();
            float newPitch = newResult.second();
            
            float easedProgress = progress < 0.5f 
                ? 2 * progress * progress 
                : 1 - (float) Math.pow(-2 * progress + 2, 2) / 2;
            
            float yawDelta = RotationUtils.normalize(newYaw - oldYaw);
            
            actualTargetYaw = oldYaw + yawDelta * easedProgress;
            actualTargetPitch = oldPitch + (newPitch - oldPitch) * easedProgress;
            hasValidTarget = true;
            lastValidTargetYaw = actualTargetYaw;
            lastValidTargetPitch = actualTargetPitch;
            
            targetYaw = actualTargetYaw;
            targetPitch = actualTargetPitch;
            
            if (progress >= 1.0f) {
                isInterpolating = false;
                previousTarget = null;
            }
        } else if (hasTarget) {
            Pair<Float, Float> result = aimSimulator.getRotation(target);
            actualTargetYaw = result.first();
            actualTargetPitch = result.second();
            hasValidTarget = true;
            lastValidTargetYaw = actualTargetYaw;
            lastValidTargetPitch = actualTargetPitch;
            
            targetYaw = actualTargetYaw;
            targetPitch = actualTargetPitch;
        } else {
             if (hasValidTarget) {
                targetYaw = lastValidTargetYaw;
                targetPitch = lastValidTargetPitch;
            } else {
                targetYaw = rotations[0];
                targetPitch = rotations[1];
            }
        }

        if (shouldMaintainRotation && hasValidTarget) {
            targetYaw = rotations[0];
            targetPitch = rotations[1];
        }

        // Rotation Speed
        double minSpeed = parent.minRotationSpeed.getInput();
        double maxSpeed = parent.maxRotationSpeed.getInput();
        double rotationSpeed = Utils.randomizeDouble(minSpeed, maxSpeed);
        
        if (hasTarget && target != null && !isOvershooting) {
            double distance = mc.thePlayer.getDistanceToEntity(target);
            double distanceMultiplier;
            if (distance <= 2.0) {
                distanceMultiplier = 0.4 + (distance / 2.0) * 0.3;
            } else if (distance <= 4.0) {
                distanceMultiplier = 0.7 + ((distance - 2.0) / 2.0) * 0.3;
            } else {
                distanceMultiplier = 1.0;
            }
            rotationSpeed = rotationSpeed * distanceMultiplier;
            rotationSpeed = Math.max(1.5, rotationSpeed);
        } else if (hasTarget && target != null && isOvershooting) {
            double distance = mc.thePlayer.getDistanceToEntity(target);
            double baseMultiplier = distance < 2.0 ? 0.6 : (distance < 4.0 ? 0.8 : 1.0);
            rotationSpeed = rotationSpeed * baseMultiplier;
            rotationSpeed = Math.max(2.0, rotationSpeed);
        }

        // Overshoot Logic
        if (hasTarget && parent.overshoot.isToggled() && rotationSpeed < 10 && allowJitter) {
            float currentYaw = rotations[0];
            float currentPitch = rotations[1];
            
            float yawDeltaToActual = RotationUtils.normalize(actualTargetYaw - currentYaw);
            float pitchDeltaToActual = actualTargetPitch - currentPitch;
            float distanceToActual = (float) Math.sqrt(yawDeltaToActual * yawDeltaToActual + pitchDeltaToActual * pitchDeltaToActual);
            
            boolean shouldStartOvershoot = false;
            if (!isOvershooting && !isCorrectingOvershoot) {
                double adjustedChance = parent.overshootChance.getInput();
                if (distanceToActual < 2.0f) {
                    adjustedChance *= 0.5;
                }
                shouldStartOvershoot = distanceToActual > 0.3f && Math.random() * 100.0 < adjustedChance;
            }
            
            if (shouldStartOvershoot) {
                float overshootMultiplier = (float) (1.0 + parent.overshootAmount.getInput() / 10.0);
                overshootTargetYaw = currentYaw + yawDeltaToActual * overshootMultiplier;
                overshootTargetPitch = currentPitch + pitchDeltaToActual * overshootMultiplier;
                isOvershooting = true;
                isCorrectingOvershoot = false;
                overshootTicks = 0;
            }
            
            if (isOvershooting) {
                float overshootYawDelta = RotationUtils.normalize(overshootTargetYaw - currentYaw);
                float overshootPitchDelta = overshootTargetPitch - currentPitch;
                float overshootDistance = (float) Math.sqrt(overshootYawDelta * overshootYawDelta + overshootPitchDelta * overshootPitchDelta);
                
                if (overshootDistance < 0.3f || overshootTicks >= MAX_OVERSHOOT_TICKS) {
                    isOvershooting = false;
                    isCorrectingOvershoot = true;
                    overshootTicks = 0;
                    targetYaw = actualTargetYaw;
                    targetPitch = actualTargetPitch;
                } else {
                    targetYaw = overshootTargetYaw;
                    targetPitch = overshootTargetPitch;
                    overshootTicks++;
                }
            } else if (isCorrectingOvershoot) {
                float correctionYawDelta = RotationUtils.normalize(actualTargetYaw - currentYaw);
                float correctionPitchDelta = actualTargetPitch - currentPitch;
                float correctionDistance = (float) Math.sqrt(correctionYawDelta * correctionYawDelta + correctionPitchDelta * correctionPitchDelta);
                
                if (correctionDistance < 0.2f || overshootTicks >= MAX_CORRECTION_TICKS) {
                    isCorrectingOvershoot = false;
                    targetYaw = actualTargetYaw;
                    targetPitch = actualTargetPitch;
                } else {
                    targetYaw = actualTargetYaw;
                    targetPitch = actualTargetPitch;
                    overshootTicks++;
                }
            }
        } else if (hasTarget) {
             isOvershooting = false;
             isCorrectingOvershoot = false;
        }

        // Smoothing / Enhancing
        boolean useAdaptiveSmoothing = parent.rotationSmoothing.isToggled() && rotationSpeed < 10;
        
        if (rotationSpeed < 10) {
            float[] enhanced = rotationEnhancer.enhanceRotation(
                targetYaw, targetPitch,
                rotations[0], rotations[1],
                (float) rotationSpeed,
                useAdaptiveSmoothing
            );
            targetYaw = enhanced[0];
            targetPitch = enhanced[1];
        } else {
            rotationEnhancer.enhanceRotation(
                targetYaw, targetPitch,
                rotations[0], rotations[1],
                10.0f,
                false
            );
            if (!hasTarget && hasValidTarget) {
                float decelSpeed = (float) Math.max(2.0, rotationSpeed * 0.3);
                targetYaw = AimSimulator.rotMove(targetYaw, rotations[0], decelSpeed);
                targetPitch = AimSimulator.rotMove(targetPitch, rotations[1], decelSpeed);
            }
        }

        float prevYaw = rotations[0];
        float prevPitch = rotations[1];
        float[] fixed = RotationUtils.fixRotation(targetYaw, targetPitch, prevYaw, prevPitch);
        fixed = finalizeRotations(fixed, prevYaw, prevPitch, hasTarget, allowJitter);
        this.rotations = fixed;
        this.lastUpdateMs = System.currentTimeMillis();
        return fixed;
    }
    
    /**
     * Inertia-based rotation algorithm - accelerates toward target and decelerates using friction.
     */
    private float[] getRotationsInertia(EntityLivingBase target) {
        long now = System.currentTimeMillis();
        if (lastUpdateTime == 0L) {
            lastUpdateTime = now;
        }
        float deltaTicks = Math.max(0.5f, Math.min(2.5f, (now - lastUpdateTime) / 50.0f));
        lastUpdateTime = now;
        
        boolean useNearestFromType = parent.v2RotationType != null && parent.v2RotationType.getInput() == 1;
        boolean useNearestSetting = parent.v2Nearest != null && parent.v2Nearest.isToggled();
        boolean useNearest = useNearestFromType || useNearestSetting;
        double nearestAcc = parent.nearestAccuracy != null ? parent.nearestAccuracy.getInput() : 1.0;
        aimSimulator.setNearest(useNearest, nearestAcc);
        
        boolean useLazy = parent.v2Lazy != null && parent.v2Lazy.isToggled();
        double lazyAcc = parent.lazyAccuracy != null ? parent.lazyAccuracy.getInput() : 0.95;
        aimSimulator.setLazy(useLazy, lazyAcc);
        
        final int prMode = parent.pointRandomizationMode != null ? (int) parent.pointRandomizationMode.getInput() : 0;
        boolean hasTarget = target != null && !target.isDead;
        final boolean allowJitter = shouldAllowJitter(target);
        aimSimulator.setJitterAllowed(allowJitter);

        boolean enableNoise = prMode != 0 && parent.v2Noise != null && parent.v2Noise.isToggled();
        if (!allowJitter) enableNoise = false;
        
        float noiseMultiplier = 1.0f;
        if (enableNoise && parent.noiseRangeDecrease != null && parent.noiseRangeDecrease.isToggled() && target != null) {
            double distance = mc.thePlayer.getDistanceToEntity(target);
            double threshold = parent.noiseRangeDecreaseThreshold.getInput();
            double disableRange = parent.noiseRangeDecreaseDisable.getInput();
            
            if (distance <= disableRange) {
                enableNoise = false;
            } else if (distance < threshold) {
                double range = threshold - disableRange;
                if (range > 0) {
                    noiseMultiplier = (float) ((distance - disableRange) / range);
                    noiseMultiplier = Math.max(0.0f, Math.min(1.0f, noiseMultiplier));
                }
            }
        }
        
        float horizontalNoise = parent.noiseHorizontal != null ? (float) parent.noiseHorizontal.getInput() * noiseMultiplier : 0.35f * noiseMultiplier;
        float verticalNoise = parent.noiseVertical != null ? (float) parent.noiseVertical.getInput() * noiseMultiplier : 0.5f * noiseMultiplier;
        
        aimSimulator.setNoise(enableNoise,
                new Pair<>(horizontalNoise, verticalNoise),
                parent.noiseAimSpeed != null ? parent.noiseAimSpeed.getInput() : 0.35,
                parent.noiseDelay != null ? (long) parent.noiseDelay.getInput() : 100);
        
        int mappedMode = prMode == 0 ? 2 : prMode;
        aimSimulator.setPointRandomizationMode(allowJitter ? mappedMode : 0);
        
        boolean useDelayAim = parent.v2DelayAim != null && parent.v2DelayAim.isToggled();
        int delayAimAmount = parent.delayAimAmount != null ? (int) parent.delayAimAmount.getInput() : 5;
        aimSimulator.setDelay(useDelayAim, delayAimAmount);
        
        boolean shouldMaintainRotation = parent.v2Constant != null && parent.v2Constant.isToggled()
                && !(parent.constantOnlyIfNotMoving != null && parent.constantOnlyIfNotMoving.isToggled() && (MoveUtil.isMoving() || MoveUtil.isMoving(target)));
        
        float desiredYaw;
        float desiredPitch;
        
        if (hasTarget) {
            Pair<Float, Float> result = aimSimulator.getRotation(target);
            desiredYaw = result.first();
            desiredPitch = result.second();
            actualTargetYaw = desiredYaw;
            actualTargetPitch = desiredPitch;
            hasValidTarget = true;
            lastValidTargetYaw = desiredYaw;
            lastValidTargetPitch = desiredPitch;
        } else if (hasValidTarget) {
            desiredYaw = lastValidTargetYaw;
            desiredPitch = lastValidTargetPitch;
        } else {
            desiredYaw = RotationHandler.getRotationYaw();
            desiredPitch = RotationHandler.getRotationPitch();
        }
        
        if (shouldMaintainRotation && hasValidTarget) {
            desiredYaw = rotations[0];
            desiredPitch = rotations[1];
        }
        
        float yawDelta = RotationUtils.normalize(desiredYaw - rotations[0]);
        float pitchDelta = desiredPitch - rotations[1];
        
        double deadZone = parent.inertiaDeadZone != null ? parent.inertiaDeadZone.getInput() : 0.0;
        if (Math.abs(yawDelta) < deadZone) {
            yawDelta = 0.0f;
            yawVelocity *= Math.pow(parent.inertiaFriction != null ? parent.inertiaFriction.getInput() : 0.9, deltaTicks * 1.5f);
        }
        if (Math.abs(pitchDelta) < deadZone) {
            pitchDelta = 0.0f;
            pitchVelocity *= Math.pow(parent.inertiaFriction != null ? parent.inertiaFriction.getInput() : 0.9, deltaTicks * 1.5f);
        }
        
        float acceleration = parent.inertiaAcceleration != null ? (float) parent.inertiaAcceleration.getInput() : 1.0f;
        float maxSpeed = parent.inertiaMaxSpeed != null ? (float) parent.inertiaMaxSpeed.getInput() : 12.0f;
        float friction = parent.inertiaFriction != null ? (float) parent.inertiaFriction.getInput() : 0.9f;
        
        float frictionFactor = (float) Math.pow(friction, deltaTicks);
        yawVelocity *= frictionFactor;
        pitchVelocity *= frictionFactor;
        
        float distanceScale = 1.0f;
        if (hasTarget) {
            double distance = mc.thePlayer.getDistanceToEntity(target);
            distanceScale = (float) Math.max(0.7, Math.min(1.3, distance / 3.5));
        } else {
            distanceScale = 0.85f;
        }
        
        yawVelocity += Math.signum(yawDelta) * acceleration * distanceScale * deltaTicks;
        pitchVelocity += Math.signum(pitchDelta) * acceleration * 0.65f * distanceScale * deltaTicks;
        
        yawVelocity = Math.max(-maxSpeed, Math.min(maxSpeed, yawVelocity));
        pitchVelocity = Math.max(-maxSpeed, Math.min(maxSpeed, pitchVelocity));
        
        float newYaw = rotations[0] + yawVelocity;
        float newPitch = rotations[1] + pitchVelocity;
        
        float prevYaw = rotations[0];
        float prevPitch = rotations[1];
        float[] fixed = RotationUtils.fixRotation(newYaw, newPitch, prevYaw, prevPitch);
        fixed = finalizeRotations(fixed, prevYaw, prevPitch, hasTarget, allowJitter);
        rotations = fixed;
        lastUpdateMs = System.currentTimeMillis();
        return rotations;
    }

    /**
     * Grim-oriented rotation builder: simulate mouse deltas with sensitivity GCD,
     * simple acceleration, and per-tick clamping to keep deltas within
     * human-like bounds. This avoids smooth math noise and favors discrete steps.
     */
    private float[] getRotationsGrim(EntityLivingBase target) {
        // Hard limits are conservative to pass common Grim heuristics
        final float maxYawStep = 8.5f;
        final float maxPitchStep = 7.0f;
        final float accel = 0.35f;
        final float stopThreshold = 0.15f;
        final float readyYawThreshold = 1.5f;
        final float readyPitchThreshold = 1.0f;

        // Compute desired look direction (no noise/randomization)
        aimSimulator.setNearest(true, 1.0);
        aimSimulator.setLazy(false, 1.0);
        aimSimulator.setNoise(false, new Pair<>(0f, 0f), 0, 0);
        aimSimulator.setPointRandomizationMode(0);
        aimSimulator.setDelay(false, 0);

        float targetYaw = rotations[0];
        float targetPitch = rotations[1];
        boolean hasTarget = target != null && !target.isDead;
        boolean allowJitter = shouldAllowJitter(target);
        aimSimulator.setJitterAllowed(allowJitter);
        if (hasTarget) {
            Pair<Float, Float> res = selectGrimPoint(target, rotations[0], rotations[1]);
            targetYaw = res.first();
            targetPitch = res.second();
        }

        float currentYaw = rotations[0];
        float currentPitch = rotations[1];
        float prevYaw = currentYaw;
        float prevPitch = currentPitch;

        float yawDiff = RotationUtils.normalize(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        // Desired per-tick step capped to realistic mouse movement
        float desiredYawStep = MathHelper.clamp_float(yawDiff, -maxYawStep, maxYawStep);
        float desiredPitchStep = MathHelper.clamp_float(pitchDiff, -maxPitchStep, maxPitchStep);
        // Quantize to mouse steps to stay sensitivity-aligned
        float gcd = getMouseGCD();
        desiredYawStep = Math.round(desiredYawStep / gcd) * gcd;
        desiredPitchStep = Math.round(desiredPitchStep / gcd) * gcd;

        // Simple acceleration towards desired step
        yawVelocity += (desiredYawStep - yawVelocity) * accel;
        pitchVelocity += (desiredPitchStep - pitchVelocity) * accel;

        // Sensitivity-based quantization (mouse GCD)
        float quantizedYaw = Math.round(yawVelocity / gcd) * gcd;
        float quantizedPitch = Math.round(pitchVelocity / gcd) * gcd;

        // Stop jitter when very close
        if (Math.abs(yawDiff) < stopThreshold) quantizedYaw = 0f;
        if (Math.abs(pitchDiff) < stopThreshold) quantizedPitch = 0f;

        rotations[0] = MathHelper.wrapAngleTo180_float(currentYaw + quantizedYaw);
        rotations[1] = MathHelper.clamp_float(currentPitch + quantizedPitch, -90f, 90f);

        // Keep Grim behavior untouched; General mode applies its own tweaks separately.

        // Avoid duplicate-look: if no change, nudge by tiny GCD step alternating
        if (allowJitter && Math.abs(RotationUtils.normalize(rotations[0] - lastGrimYaw)) < 1e-3 && Math.abs(rotations[1] - lastGrimPitch) < 1e-3) {
            // Keep nudges sensitivity-aligned (anti "sensitivity" checks).
            float nudge = gcd;
            rotations[0] = MathHelper.wrapAngleTo180_float(rotations[0] + (jitterFlip ? nudge : -nudge));
            jitterFlip = !jitterFlip;
        }

        rotations = finalizeRotations(rotations, prevYaw, prevPitch, hasTarget, allowJitter);

        lastGrimYaw = rotations[0];
        lastGrimPitch = rotations[1];

        grimReady = Math.abs(yawDiff) <= readyYawThreshold && Math.abs(pitchDiff) <= readyPitchThreshold;
        lastUpdateMs = System.currentTimeMillis();
        return rotations;
    }

    /**
     * General (adaptive) rotation algorithm:
     * - Uses AimSimulator noise/lead with anti-pattern shaping
     * - Mouse GCD quantization with soft caps to avoid MX distinct/entropy and AGC aim assists
     * - Small modulo bypass to avoid large yaw jumps
     */
    private float[] getRotationsGeneralAdaptive(EntityLivingBase target) {
        boolean hasTarget = target != null && !target.isDead;
        boolean allowJitter = shouldAllowJitter(target);
        aimSimulator.setJitterAllowed(allowJitter);
        rotationEnhancer.setJitterAllowed(allowJitter);
        // Configure AimSimulator with stronger anti-detection shaping already present in AimSimulator
        boolean useNearest = (parent.v2Nearest != null && parent.v2Nearest.isToggled()) || (parent.v2RotationType != null && parent.v2RotationType.getInput() == 1);
        double nearestAcc = parent.nearestAccuracy != null ? parent.nearestAccuracy.getInput() : 1.0;
        aimSimulator.setNearest(useNearest, nearestAcc);

        boolean useLazy = parent.v2Lazy != null && parent.v2Lazy.isToggled();
        double lazyAcc = parent.lazyAccuracy != null ? parent.lazyAccuracy.getInput() : 0.95;
        aimSimulator.setLazy(useLazy, lazyAcc);

        // Toggle point randomization mode smoothly to avoid long runs of identical buckets
        int prModeSetting = parent.pointRandomizationMode != null ? (int) parent.pointRandomizationMode.getInput() : 0;
        int prMode = prModeSetting == 0 ? 3 : prModeSetting; // default to smooth when off
        int effectivePrMode = allowJitter ? prMode : 0;
        boolean enableNoise = prModeSetting != 0 && parent.v2Noise != null && parent.v2Noise.isToggled();
        if (!allowJitter) enableNoise = false;

        float noiseMultiplier = 1.0f;
        if (enableNoise && parent.noiseRangeDecrease != null && parent.noiseRangeDecrease.isToggled() && hasTarget) {
            double distance = mc.thePlayer.getDistanceToEntity(target);
            double threshold = parent.noiseRangeDecreaseThreshold.getInput();
            double disableRange = parent.noiseRangeDecreaseDisable.getInput();
            if (distance <= disableRange) {
                enableNoise = false;
            } else if (distance < threshold) {
                double range = threshold - disableRange;
                if (range > 0) {
                    noiseMultiplier = (float) ((distance - disableRange) / range);
                    noiseMultiplier = Math.max(0.0f, Math.min(1.0f, noiseMultiplier));
                }
            }
        }

        float horizontalNoise = parent.noiseHorizontal != null ? (float) parent.noiseHorizontal.getInput() * noiseMultiplier : 0.35f * noiseMultiplier;
        float verticalNoise = parent.noiseVertical != null ? (float) parent.noiseVertical.getInput() * noiseMultiplier : 0.5f * noiseMultiplier;
        aimSimulator.setNoise(enableNoise,
                new Pair<>(horizontalNoise, verticalNoise),
                parent.noiseAimSpeed != null ? parent.noiseAimSpeed.getInput() : 0.35,
                parent.noiseDelay != null ? (long) parent.noiseDelay.getInput() : 100);
        aimSimulator.setPointRandomizationMode(effectivePrMode); // default already smoothed above

        boolean useDelayAim = parent.v2DelayAim != null && parent.v2DelayAim.isToggled();
        int delayAimAmount = parent.delayAimAmount != null ? (int) parent.delayAimAmount.getInput() : 5;
        aimSimulator.setDelay(useDelayAim, delayAimAmount);

        Pair<Float, Float> res = hasTarget ? aimSimulator.getRotation(target) : new Pair<>(RotationHandler.getRotationYaw(), RotationHandler.getRotationPitch());
        float targetYaw = res.first();
        float targetPitch = res.second();

        // Adaptive smoothing using RotationEnhancer
        float baseSpeed = hasTarget ? 6.0f : 3.5f;
        if (allowJitter) {
            baseSpeed *= (float) (0.9 + Math.random() * 0.25); // slight per-tick variance to break patterns
        }
        float[] enhanced = rotationEnhancer.enhanceRotation(
                targetYaw, targetPitch,
                rotations[0], rotations[1],
                baseSpeed,
                true
        );
        targetYaw = enhanced[0];
        targetPitch = enhanced[1];

        // Mouse-step quantization and soft caps to avoid clean lines
        float yawDiff = RotationUtils.normalize(targetYaw - rotations[0]);
        float pitchDiff = targetPitch - rotations[1];
        float gcd = getMouseGCD();
        float maxYawStep = 6.4f;
        float maxPitchStep = 4.2f;

        float stepYaw = MathHelper.clamp_float(yawDiff, -maxYawStep, maxYawStep);
        float stepPitch = MathHelper.clamp_float(pitchDiff, -maxPitchStep, maxPitchStep);
        // Slightly vary quantization window to avoid repetitive distinct buckets.
        // IMPORTANT: we still re-align back to the real mouse GCD afterwards to avoid "sensitivity" flags.
        float jitterGcd = allowJitter ? gcd * (float) (0.9 + Math.random() * 0.2) : gcd;
        stepYaw = Math.round(stepYaw / jitterGcd) * jitterGcd;
        stepPitch = Math.round(stepPitch / jitterGcd) * jitterGcd;

        // Break perfect grid/0.1 patterns (AGC AimAssistB) and flat pitch (AimAssistC)
        if (allowJitter) {
            // Add only GCD-aligned micro variance (avoid sensitivity checks).
            stepYaw += (Math.random() > 0.5 ? gcd : -gcd);
        }
        if (allowJitter && Math.abs(stepPitch) < 0.02f) {
            // Keep pitch micro-movement on GCD grid too.
            stepPitch = (Math.random() > 0.5 ? gcd : -gcd);
        }

        Pair<Float, Float> entropySteps = applyEntropyBypass(stepYaw, stepPitch, gcd, maxYawStep, maxPitchStep, allowJitter);
        stepYaw = entropySteps.first();
        stepPitch = entropySteps.second();

        float newYaw = MathHelper.wrapAngleTo180_float(rotations[0] + stepYaw);
        float newPitch = MathHelper.clamp_float(rotations[1] + stepPitch, -90f, 90f);

        float prevYaw = rotations[0];
        float prevPitch = rotations[1];
        float[] fixed = RotationUtils.fixRotation(newYaw, newPitch, prevYaw, prevPitch);
        fixed = finalizeRotations(fixed, prevYaw, prevPitch, hasTarget, allowJitter);
        rotations = fixed;
        lastUpdateMs = System.currentTimeMillis();
        return rotations;
    }
    
    private float[] getRotationsGeneral(EntityLivingBase target) {
        // General: Grim-style stepping, interpolation, and softened pitch with light noise
        // Enhanced with anti-cheat bypass for MX (Factor, Distinct) and AGC (AimAssistB, AimAssistC)
        final float maxYawStep = 8.5f;
        final float maxPitchStep = 4.0f;
        final float accel = 0.35f;
        final float stopThreshold = 0.15f;
        final float readyYawThreshold = 1.5f;
        final float readyPitchThreshold = 1.0f;

        boolean hasTarget = target != null && !target.isDead;
        final boolean allowJitter = shouldAllowJitter(target);
        aimSimulator.setJitterAllowed(allowJitter);

        // Configure aim simulator with optional noise (scaled down to ~20% strength)
        aimSimulator.setNearest(true, 1.0);
        aimSimulator.setLazy(false, 1.0);

        final int prMode = parent.pointRandomizationMode != null ? (int) parent.pointRandomizationMode.getInput() : 0;
        boolean enableNoise = prMode != 0 && parent.noise != null && parent.noise.isToggled();
        if (!allowJitter) enableNoise = false;
        float noiseMultiplier = 1.0f;
        if (enableNoise && parent.noiseRangeDecrease != null && parent.noiseRangeDecrease.isToggled() && target != null) {
            double distance = mc.thePlayer.getDistanceToEntity(target);
            double threshold = parent.noiseRangeDecreaseThreshold.getInput();
            double disableRange = parent.noiseRangeDecreaseDisable.getInput();

            if (distance <= disableRange) {
                enableNoise = false;
            } else if (distance < threshold) {
                double range = threshold - disableRange;
                if (range > 0) {
                    noiseMultiplier = (float) ((distance - disableRange) / range);
                    noiseMultiplier = Math.max(0.0f, Math.min(1.0f, noiseMultiplier));
                }
            }
        }

        float horizontalNoise = parent.noiseHorizontal != null ? (float) parent.noiseHorizontal.getInput() : 0.35f;
        float verticalNoise = parent.noiseVertical != null ? (float) parent.noiseVertical.getInput() : 0.5f;
        // Apply noise at 20% of configured strength (pitch especially reduced)
        horizontalNoise *= 0.2f * noiseMultiplier;
        verticalNoise *= 0.2f * noiseMultiplier;

        double aimSpeed = parent.noiseAimSpeed != null ? parent.noiseAimSpeed.getInput() : 0.35;
        long noiseDelay = parent.noiseDelay != null ? (long) parent.noiseDelay.getInput() : 100L;
        aimSimulator.setNoise(enableNoise, new Pair<>(horizontalNoise, verticalNoise), aimSpeed, noiseDelay);
        int mappedMode = prMode == 0 ? 2 : prMode;
        aimSimulator.setPointRandomizationMode(enableNoise ? mappedMode : 0);
        aimSimulator.setDelay(false, 0);

        float targetYaw;
        float targetPitch;

        if (hasTarget && isInterpolating && previousTarget != null && !previousTarget.isDead) {
            long elapsed = System.currentTimeMillis() - interpolationStartTime;
            float interpolationDuration = (float) parent.interpolationTime.getInput();
            float progress = interpolationDuration > 0 ? Math.min(1.0f, (float) elapsed / interpolationDuration) : 1.0f;

            Pair<Float, Float> oldResult = aimSimulator.getRotation(previousTarget);
            Pair<Float, Float> newResult = aimSimulator.getRotation(target);

            float oldYaw = oldResult.first();
            float oldPitch = oldResult.second();
            float newYaw = newResult.first();
            float newPitch = newResult.second();

            float easedProgress = progress < 0.5f
                    ? 2 * progress * progress
                    : 1 - (float) Math.pow(-2 * progress + 2, 2) / 2;

            float yawDelta = RotationUtils.normalize(newYaw - oldYaw);
            targetYaw = oldYaw + yawDelta * easedProgress;
            targetPitch = oldPitch + (newPitch - oldPitch) * easedProgress;

            actualTargetYaw = newYaw;
            actualTargetPitch = newPitch;
            hasValidTarget = true;
            lastValidTargetYaw = newYaw;
            lastValidTargetPitch = newPitch;

            if (progress >= 1.0f) {
                isInterpolating = false;
                previousTarget = null;
            }
        } else if (hasTarget) {
            Pair<Float, Float> res = aimSimulator.getRotation(target);
            targetYaw = res.first();
            targetPitch = res.second();
            actualTargetYaw = targetYaw;
            actualTargetPitch = targetPitch;
            hasValidTarget = true;
            lastValidTargetYaw = targetYaw;
            lastValidTargetPitch = targetPitch;
        } else {
            if (hasValidTarget) {
                targetYaw = lastValidTargetYaw;
                targetPitch = lastValidTargetPitch;
            } else {
                targetYaw = RotationHandler.getRotationYaw();
                targetPitch = RotationHandler.getRotationPitch();
            }
        }

        float currentYaw = rotations[0];
        float currentPitch = rotations[1];

        float yawDiff = RotationUtils.normalize(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        // Apply carry to spread large moves across multiple ticks (avoids 0-big-0 patterns)
        yawDiff += generalYawCarry;
        pitchDiff += generalPitchCarry;

        // Desired per-tick step capped to realistic mouse movement
        float desiredYawStep = MathHelper.clamp_float(yawDiff, -maxYawStep, maxYawStep);
        float desiredPitchStep = MathHelper.clamp_float(pitchDiff, -maxPitchStep, maxPitchStep);

        // Update carry for next tick
        generalYawCarry = yawDiff - desiredYawStep;
        generalPitchCarry = pitchDiff - desiredPitchStep;

        // Quantize to mouse steps to stay sensitivity-aligned
        float gcd = getMouseGCD();
        desiredYawStep = Math.round(desiredYawStep / gcd) * gcd;
        desiredPitchStep = Math.round(desiredPitchStep / gcd) * gcd;

        // MX Aim Factor bypass: Only apply when actually needed (huge rotations >35°)
        // This prevents small-huge-small patterns that MX detects
        float absYawStep = Math.abs(desiredYawStep);
        if (absYawStep > 35.0f) {
            // Limit huge rotations to spread across ticks, but don't force extra movement
            float maxAllowed = 32.0f; // More conservative limit
            desiredYawStep = Math.copySign(Math.min(absYawStep, maxAllowed), desiredYawStep);
            // Recalculate carry after limiting
            generalYawCarry = yawDiff - desiredYawStep;
        }

        // Minimal pitch movement when yaw is significant (AGC AimAssistC bypass)
        // Only apply when pitch would be exactly unchanged with significant yaw (>= 3.0)
        if (allowJitter && Math.abs(desiredYawStep) >= 2.8f && Math.abs(desiredPitchStep) < 0.05f) {
            // Add subtle pitch variation - just enough to avoid detection
            float minPitch = 0.12f + (float)(Math.random() * 0.08f); // 0.12-0.20 degrees
            desiredPitchStep = Math.copySign(minPitch, pitchDiff != 0 ? pitchDiff : (Math.random() > 0.5 ? 1 : -1));
            generalPitchCarry = pitchDiff - desiredPitchStep;
            desiredPitchStep = Math.round(desiredPitchStep / gcd) * gcd;
        }

        // Simple acceleration towards desired step
        yawVelocity += (desiredYawStep - yawVelocity) * accel;
        pitchVelocity += (desiredPitchStep - pitchVelocity) * accel;

        // Sensitivity-based quantization (mouse GCD)
        float quantizedYaw = Math.round(yawVelocity / gcd) * gcd;
        float quantizedPitch = Math.round(pitchVelocity / gcd) * gcd;
        Pair<Float, Float> entropySteps = applyEntropyBypass(quantizedYaw, quantizedPitch, gcd, maxYawStep, maxPitchStep, allowJitter);
        quantizedYaw = entropySteps.first();
        quantizedPitch = entropySteps.second();

        // Stop jitter when very close
        if (Math.abs(yawDiff) < stopThreshold) quantizedYaw = 0f;
        if (Math.abs(pitchDiff) < stopThreshold) quantizedPitch = 0f;

        rotations[0] = MathHelper.wrapAngleTo180_float(currentYaw + quantizedYaw);
        rotations[1] = MathHelper.clamp_float(currentPitch + quantizedPitch, -90f, 90f);

        // AGC AimAssistC bypass (final safety check): only if pitch is still unchanged with significant yaw
        float finalYawDelta = Math.abs(RotationUtils.normalize(rotations[0] - currentYaw));
        if (allowJitter && finalYawDelta >= 2.8f && Math.abs(rotations[1] - currentPitch) < 0.01f) {
            // Add minimal pitch variation - just enough to avoid detection
            float pitchJitter = 0.15f + (float)(Math.random() * 0.1f); // 0.15-0.25 degrees (much smaller)
            rotations[1] = MathHelper.clamp_float(rotations[1] + (jitterFlip ? pitchJitter : -pitchJitter), -90f, 90f);
            jitterFlip = !jitterFlip;
        }

        // AGC AimAssistB bypass (final safety check): only break exact 0.1 multiples when detected
        float finalYawDiff = Math.abs(RotationUtils.normalize(rotations[0] - currentYaw)) % 180.0f;
        if (allowJitter && finalYawDiff > 1.0f && finalYawDiff < 10.0f) { // Only check reasonable ranges
            float roundedCheck = Math.round(finalYawDiff * 10.0f) * 0.1f;
            float wholeDiff = Math.abs(Math.round(finalYawDiff) - finalYawDiff);
            // Only break if it's a perfect 0.1 multiple (not whole number)
            if (Math.abs(roundedCheck - finalYawDiff) < 0.001f && wholeDiff > 0.05f) {
                // Minimal noise, but still sensitivity-aligned
                float microNoise = (Math.random() > 0.5 ? gcd : -gcd);
                rotations[0] = MathHelper.wrapAngleTo180_float(rotations[0] + microNoise);
            }
        }

        // Avoid duplicate-look: if no change, nudge by tiny GCD step alternating
        if (allowJitter && Math.abs(RotationUtils.normalize(rotations[0] - lastGrimYaw)) < 1e-3 && Math.abs(rotations[1] - lastGrimPitch) < 1e-3) {
            float nudge = gcd;
            rotations[0] = MathHelper.wrapAngleTo180_float(rotations[0] + (jitterFlip ? nudge : -nudge));
            jitterFlip = !jitterFlip;
        }

        if (!hasTarget) {
            // Keep player-facing direction stable when idle (no target)
            yawVelocity = 0.0f;
            pitchVelocity = 0.0f;
            rotations[0] = RotationHandler.getRotationYaw();
            rotations[1] = RotationHandler.getRotationPitch();
            lastGrimYaw = rotations[0];
            lastGrimPitch = rotations[1];
            grimReady = false;
            lastUpdateMs = System.currentTimeMillis();
            return rotations;
        }

        // AimModulo360 bypass: ensure yaw stays out of modulo edge without huge spikes
        rotations = applyAimModuloBypass(rotations, hasTarget);

        lastGrimYaw = rotations[0];
        lastGrimPitch = rotations[1];

        grimReady = Math.abs(yawDiff) <= readyYawThreshold && Math.abs(pitchDiff) <= readyPitchThreshold;
        lastUpdateMs = System.currentTimeMillis();
        return rotations;
    }

    /**
     * Pick a stable aim point inside the target's bounding box to minimize sudden angular deltas.
     */
    private Pair<Float, Float> selectGrimPoint(EntityLivingBase target, float currentYaw, float currentPitch) {
        AxisAlignedBB box = target.getEntityBoundingBox();
        Vec3 eye = Utils.getEyePos();

        // Prioritize center and upper body (chest/head) over feet
        double[] xs = {box.minX + (box.maxX - box.minX) * 0.25, (box.minX + box.maxX) * 0.5, box.maxX - (box.maxX - box.minX) * 0.25};
        double[] ys = {
                target.posY + target.getEyeHeight() * 0.8, // Upper chest/neck
                target.posY + target.getEyeHeight() * 0.5, // Mid chest
                (box.minY + box.maxY) * 0.5 // Center
        };
        double[] zs = {box.minZ + (box.maxZ - box.minZ) * 0.25, (box.minZ + box.maxZ) * 0.5, box.maxZ - (box.maxZ - box.minZ) * 0.25};

        float bestYaw = currentYaw;
        float bestPitch = currentPitch;
        double bestScore = Double.MAX_VALUE;

        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    // Calculate rotations to this point
                    double diffX = x - eye.x();
                    double diffZ = z - eye.z();
                    double diffY = y - eye.y();
                    
                    double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
                    float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
                    float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));
                    
                    yaw = MathHelper.wrapAngleTo180_float(yaw);
                    
                    float yawDiff = MathHelper.wrapAngleTo180_float(yaw - currentYaw);
                    float pitchDiff = pitch - currentPitch;
                    
                    // Score based on distance from current rotation (minimize movement)
                    // Heavily penalize large pitch movements to prevent snapping to feet
                    double score = Math.abs(yawDiff) + Math.abs(pitchDiff) * 1.5;
                    
                    if (score < bestScore) {
                        bestScore = score;
                        bestYaw = yaw;
                        bestPitch = pitch;
                    }
                }
            }
        }
        return new Pair<>(bestYaw, bestPitch);
    }

    private float getMouseGCD() {
        float sens = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        return sens * sens * sens * 1.2f;
    }

    /**
     * Break MX Aim (Entropy) similarity/perfect windows by slightly desyncing yaw/pitch
     * frequency buckets while staying mouse-GCD aligned and within per-tick caps.
     */
    private Pair<Float, Float> applyEntropyBypass(float stepYaw, float stepPitch, float gcd,
                                                  float maxYawStep, float maxPitchStep, boolean allowJitter) {
        if (!allowJitter) {
            entropyLastYawStep = stepYaw;
            entropyLastPitchStep = stepPitch;
            entropyWindowIndex = 0;
            entropyWindowFlip = false;
            entropyYawWindow.clear();
            entropyPitchWindow.clear();
            entropyPrevYawEntropy = Double.NaN;
            entropyPrevPitchEntropy = Double.NaN;
            constantLastYawStep = stepYaw;
            constantLastPitchStep = stepPitch;
            return new Pair<>(stepYaw, stepPitch);
        }

        entropyTick++;
        int windowSlot = entropyWindowIndex % 10;
        if (windowSlot == 0) {
            entropyWindowFlip = !entropyWindowFlip; // alternate which axis gets duplicates each 10-sample window
        }
        boolean duplicateYawThisWindow = entropyWindowFlip;
        boolean duplicatePitchThisWindow = !entropyWindowFlip;

        float yawOut = stepYaw;
        float pitchOut = stepPitch;

        // Periodically reuse yaw step to create duplicates, while nudging pitch off-pattern.
        if (entropyTick % 6 == 0 && Math.abs(entropyLastYawStep) > 1e-5f) {
            yawOut = entropyLastYawStep;
            float skew = (entropySkewFlip ? gcd : -gcd);
            pitchOut = Math.round((pitchOut + skew) / gcd) * gcd;
        } else if (entropyTick % 4 == 0) {
            float skew = (entropySkewFlip ? gcd : -gcd);
            yawOut = Math.round((yawOut + skew) / gcd) * gcd;
        }

        // Force at least one duplicate per 10-sample window on alternating axes
        if (duplicateYawThisWindow && windowSlot == 3 && Math.abs(entropyLastYawStep) > 1e-5f) {
            yawOut = entropyLastYawStep;
        } else if (duplicatePitchThisWindow && windowSlot == 6 && Math.abs(entropyLastPitchStep) > 1e-5f) {
            pitchOut = entropyLastPitchStep;
        }

        // Ensure pitch doesn't flatline when yaw is active; keep it on-grid.
        if (Math.abs(yawOut) > 1.5f && Math.abs(pitchOut) < gcd * 0.75f) {
            float sign = entropySkewFlip ? 1f : -1f;
            pitchOut = sign * gcd;
        }

        // Occasionally mirror the previous pitch step to avoid matching yaw entropy shape.
        if (entropyTick % 9 == 0 && Math.abs(entropyLastPitchStep) > 1e-5f) {
            pitchOut = entropyLastPitchStep;
        }

        entropySkewFlip = !entropySkewFlip;

        yawOut = MathHelper.clamp_float(yawOut, -maxYawStep, maxYawStep);
        pitchOut = MathHelper.clamp_float(pitchOut, -maxPitchStep, maxPitchStep);

        yawOut = Math.round(yawOut / gcd) * gcd;
        pitchOut = Math.round(pitchOut / gcd) * gcd;

        // Keep deltas on a coarser shared grid and ratio-clamped to avoid MX Aim Constant (1/2/3)
        Pair<Float, Float> constantSteps = applyConstantBypass(yawOut, pitchOut, gcd, maxYawStep, maxPitchStep);
        yawOut = constantSteps.first();
        pitchOut = constantSteps.second();

        // Build candidate windows including the new steps
        ArrayDeque<Float> nextYawWindow = new ArrayDeque<>(entropyYawWindow);
        ArrayDeque<Float> nextPitchWindow = new ArrayDeque<>(entropyPitchWindow);
        nextYawWindow.addLast(yawOut);
        nextPitchWindow.addLast(pitchOut);
        if (nextYawWindow.size() > 10) nextYawWindow.pollFirst();
        if (nextPitchWindow.size() > 10) nextPitchWindow.pollFirst();

        double currentYawEntropy = computeShannonEntropy(nextYawWindow);
        double currentPitchEntropy = computeShannonEntropy(nextPitchWindow);

        // Break MX "perfect shannon entropy" (same entropy as previous window on each axis)
        if (!Double.isNaN(entropyPrevYawEntropy) && Math.abs(currentYawEntropy - entropyPrevYawEntropy) < 1.0E-5) {
            yawOut = entropyNudge(yawOut, gcd, maxYawStep, entropySkewFlip);
            nextYawWindow.pollLast();
            nextYawWindow.addLast(yawOut);
            currentYawEntropy = computeShannonEntropy(nextYawWindow);
        }
        if (!Double.isNaN(entropyPrevPitchEntropy) && Math.abs(currentPitchEntropy - entropyPrevPitchEntropy) < 1.0E-5) {
            pitchOut = entropyNudge(pitchOut, gcd, maxPitchStep, !entropySkewFlip);
            nextPitchWindow.pollLast();
            nextPitchWindow.addLast(pitchOut);
            currentPitchEntropy = computeShannonEntropy(nextPitchWindow);
        }

        // Break MX "similar shannon entropy" (yaw entropy == pitch entropy in the same window)
        if (Math.abs(currentYawEntropy - currentPitchEntropy) < 1.0E-5) {
            pitchOut = entropyNudge(pitchOut, gcd, maxPitchStep, entropySkewFlip);
            nextPitchWindow.pollLast();
            nextPitchWindow.addLast(pitchOut);
            currentPitchEntropy = computeShannonEntropy(nextPitchWindow);
        }

        // Final intra-window symmetry guard using uniqueness counts (legacy behavior kept)
        if (nextYawWindow.size() == 10 && nextPitchWindow.size() == 10) {
            int uniqYaw = new java.util.HashSet<>(nextYawWindow).size();
            int uniqPitch = new java.util.HashSet<>(nextPitchWindow).size();
            if (uniqYaw == uniqPitch) {
                pitchOut = entropyNudge(pitchOut, gcd, maxPitchStep, !entropySkewFlip);
                nextPitchWindow.pollLast();
                nextPitchWindow.addLast(pitchOut);
                currentPitchEntropy = computeShannonEntropy(nextPitchWindow);
            }
        }

        // Persist updated windows and entropies
        entropyYawWindow.clear();
        entropyYawWindow.addAll(nextYawWindow);
        entropyPitchWindow.clear();
        entropyPitchWindow.addAll(nextPitchWindow);

        entropyPrevYawEntropy = currentYawEntropy;
        entropyPrevPitchEntropy = currentPitchEntropy;

        entropyWindowIndex = (entropyWindowIndex + 1) % 10;

        entropyLastYawStep = yawOut;
        entropyLastPitchStep = pitchOut;
        return new Pair<>(yawOut, pitchOut);
    }

    private float entropyNudge(float value, float gcd, float maxStep, boolean positive) {
        float skew = positive ? gcd : -gcd;
        float adjusted = MathHelper.clamp_float(value + skew, -maxStep, maxStep);
        return Math.round(adjusted / gcd) * gcd;
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
     * Clamp deltas onto a shared coarse grid and keep the ratio to the previous
     * step bounded so MX AimConstant (1/2/3) sees a stable GCD and modest modulo.
     */
    private Pair<Float, Float> applyConstantBypass(float yawOut, float pitchOut, float baseGcd,
                                                   float maxYawStep, float maxPitchStep) {
        // Coarse grid keeps gcd >= ~0.05 (greater than MX 1/128 threshold ~0.0078)
        float grid = Math.max(baseGcd, 0.05f);
        float alignedYaw = Math.round(yawOut / grid) * grid;
        float alignedPitch = Math.round(pitchOut / grid) * grid;

        // Ratio clamp vs previous step to avoid huge modulo spikes
        if (Math.abs(constantLastYawStep) > 1.0E-4f) {
            float ratioYaw = alignedYaw / constantLastYawStep;
            float clampedRatio = Math.max(-8f, Math.min(8f, ratioYaw));
            if (Math.abs(clampedRatio) < 0.2f) clampedRatio = 0.2f * Math.signum(clampedRatio == 0 ? 1f : clampedRatio);
            alignedYaw = constantLastYawStep * clampedRatio;
            alignedYaw = Math.round(alignedYaw / grid) * grid;
        }
        if (Math.abs(constantLastPitchStep) > 1.0E-4f) {
            float ratioPitch = alignedPitch / constantLastPitchStep;
            float clampedRatio = Math.max(-6f, Math.min(6f, ratioPitch));
            if (Math.abs(clampedRatio) < 0.25f) clampedRatio = 0.25f * Math.signum(clampedRatio == 0 ? 1f : clampedRatio);
            alignedPitch = constantLastPitchStep * clampedRatio;
            alignedPitch = Math.round(alignedPitch / grid) * grid;
        }

        alignedYaw = MathHelper.clamp_float(alignedYaw, -maxYawStep, maxYawStep);
        alignedPitch = MathHelper.clamp_float(alignedPitch, -maxPitchStep, maxPitchStep);

        constantLastYawStep = alignedYaw;
        constantLastPitchStep = alignedPitch;
        return new Pair<>(alignedYaw, alignedPitch);
    }

    private float[] applyAimModuloBypass(float[] rots, boolean hasTarget) {
        if (!hasTarget) {
            return rots;
        }
        // Only necessary for silent rotations; avoid camera snapping
        if (parent.rotationMode != null && parent.rotationMode.getInput() != 1) {
            return rots;
        }
        float yaw = rots[0];
        if (Math.abs(yaw) < 270f) {
            float offset = 720f;
            float sign = yaw == 0 ? 1f : Math.signum(yaw);
            yaw += sign * offset;
        }
        rots[0] = yaw;
        return rots;
    }

    private float[] applyPitchStabilizer(float[] rot, float prevYaw, float prevPitch, boolean hasTarget, boolean allowJitter) {
        float yaw = rot[0];
        float pitch = rot[1];

        float dyaw = RotationUtils.normalize(yaw - prevYaw);
        float dpitch = pitch - prevPitch;

        float yawCap = hasTarget ? 45.0f : 25.0f;
        if (Math.abs(dyaw) > yawCap) {
            yaw = MathHelper.wrapAngleTo180_float(prevYaw + Math.signum(dyaw) * yawCap);
        }

        // Sanitize denormals and very small deltas that MX can flag
        if (Math.abs(dpitch) < 1e-6f) {
            pitch = prevPitch;
            dpitch = 0f;
        }

        float pitchCap = hasTarget ? 12.0f : 7.0f;
        if (Math.abs(dpitch) > pitchCap) {
            pitch = prevPitch + Math.signum(dpitch) * pitchCap;
        } else if (hasTarget && allowJitter && Math.abs(dpitch) < 0.012f) {
            // ensure a minimal change to avoid flat-line pitch flags
            // IMPORTANT: only when we intentionally allow "human noise".
            // When both player and target are stationary, forcing micro pitch changes
            // creates visible jitter in General mode (and similar algorithms).
            float gcd = getMouseGCD();
            pitch = prevPitch + Math.copySign(gcd, dpitch != 0.0f ? dpitch : (jitterFlip ? 1.0f : -1.0f));
        }

        // Remove denormal tiny values
        if (Math.abs(pitch) < 1e-6f) {
            pitch = 0f;
        }

        pitch = MathHelper.clamp_float(pitch, -90f, 90f);
        rot[0] = yaw;
        rot[1] = pitch;
        return rot;
    }

    private float[] finalizeRotations(float[] rot, float prevYaw, float prevPitch, boolean hasTarget, boolean allowJitter) {
        rot = applyPitchStabilizer(rot, prevYaw, prevPitch, hasTarget, allowJitter);

        // When there's no target: follow client-side rotation fully (no jitter/nudge)
        if (!hasTarget) {
            rot[0] = RotationHandler.getRotationYaw();
            rot[1] = RotationHandler.getRotationPitch();
            return rot;
        }

        rot = applyAimModuloBypass(rot, hasTarget);
        return rot;
    }

    public boolean isGrimReady() {
        return grimReady;
    }
    
    /**
     * V2 rotation algorithm - with all anti-cheat bypass techniques from classic mode
     */
    private float[] getRotationsV2(EntityLivingBase target) {
        // Initialize lastYaw/lastPitch from current rotation if needed
        if (target == null || target.isDead) {
            lastYaw = RotationHandler.getRotationYaw();
            lastPitch = RotationHandler.getRotationPitch();
            rotations[0] = lastYaw;
            rotations[1] = lastPitch;
            hasValidTarget = false;
            return rotations;
        }
        
        // Initialize lastYaw/lastPitch if not set
        if (lastYaw == 0 && lastPitch == 0) {
            lastYaw = RotationHandler.getRotationYaw();
            lastPitch = RotationHandler.getRotationPitch();
        }
        
        // Configuration - apply all bypass techniques
        // Rotation type: Instant (0) or Nearest (1) - this is the v2 rotation mode from KillAuraV2
        // Instant mode: nearest = false, Nearest mode: nearest = true (matches KillAuraV2 behavior)
        boolean useNearestFromType = parent.v2RotationType != null && parent.v2RotationType.getInput() == 1;
        boolean useNearestFromSetting = parent.v2Nearest != null && parent.v2Nearest.isToggled();
        // Combine rotation type with v2Nearest setting for flexibility
        boolean useNearest = useNearestFromType || useNearestFromSetting;
        double nearestAcc = parent.nearestAccuracy != null ? parent.nearestAccuracy.getInput() : (useNearestFromType ? 1.0 : 1.0);
        aimSimulator.setNearest(useNearest, nearestAcc);
        
        boolean useLazy = parent.v2Lazy != null && parent.v2Lazy.isToggled();
        double lazyAcc = parent.lazyAccuracy != null ? parent.lazyAccuracy.getInput() : 0.95;
        aimSimulator.setLazy(useLazy, lazyAcc);
        
        // Noise/Point Randomization
        final int prMode = parent.pointRandomizationMode != null ? (int) parent.pointRandomizationMode.getInput() : 0;
        boolean enableNoise = prMode != 0 && parent.v2Noise != null && parent.v2Noise.isToggled();
        boolean allowJitter = shouldAllowJitter(target);
        aimSimulator.setJitterAllowed(allowJitter);
        rotationEnhancer.setJitterAllowed(allowJitter);
        if (!allowJitter) enableNoise = false;
        
        // Noise Logic with range decrease
        float noiseMultiplier = 1.0f;
        if (enableNoise && parent.noiseRangeDecrease != null && parent.noiseRangeDecrease.isToggled()) {
            double distance = mc.thePlayer.getDistanceToEntity(target);
            double threshold = parent.noiseRangeDecreaseThreshold.getInput();
            double disableRange = parent.noiseRangeDecreaseDisable.getInput();
            
            if (distance <= disableRange) {
                enableNoise = false;
            } else if (distance < threshold) {
                double range = threshold - disableRange;
                if (range > 0) {
                    noiseMultiplier = (float) ((distance - disableRange) / range);
                    noiseMultiplier = Math.max(0.0f, Math.min(1.0f, noiseMultiplier));
                }
            }
        }
        
        float horizontalNoise = parent.noiseHorizontal != null ? (float) parent.noiseHorizontal.getInput() * noiseMultiplier : 0.35f * noiseMultiplier;
        float verticalNoise = parent.noiseVertical != null ? (float) parent.noiseVertical.getInput() * noiseMultiplier : 0.5f * noiseMultiplier;
        
        aimSimulator.setNoise(enableNoise,
                new Pair<>(horizontalNoise, verticalNoise),
                parent.noiseAimSpeed != null ? parent.noiseAimSpeed.getInput() : 0.35,
                parent.noiseDelay != null ? (long) parent.noiseDelay.getInput() : 100);
        
        int mappedMode = prMode == 0 ? 2 : prMode;
        aimSimulator.setPointRandomizationMode(allowJitter ? mappedMode : 0);
        
        // Delay Aim
        boolean useDelayAim = parent.v2DelayAim != null && parent.v2DelayAim.isToggled();
        int delayAimAmount = parent.delayAimAmount != null ? (int) parent.delayAimAmount.getInput() : 5;
        aimSimulator.setDelay(useDelayAim, delayAimAmount);
        
        // Constant mode
        boolean shouldMaintainRotation = parent.v2Constant != null && parent.v2Constant.isToggled() 
                && !(parent.constantOnlyIfNotMoving != null && parent.constantOnlyIfNotMoving.isToggled() && (MoveUtil.isMoving() || MoveUtil.isMoving(target)));
        
        // Get target rotations
        Pair<Float, Float> result = aimSimulator.getRotation(target);
        float targetYaw = result.first();
        float targetPitch = result.second();
        actualTargetYaw = targetYaw;
        actualTargetPitch = targetPitch;
        hasValidTarget = true;
        lastValidTargetYaw = targetYaw;
        lastValidTargetPitch = targetPitch;
        
        // Apply constant mode
        if (shouldMaintainRotation) {
            targetYaw = rotations[0];
            targetPitch = rotations[1];
        }
        
        // Rotation Speed - with min/max randomization
        double minSpeed = parent.v2MinRotationSpeed.getInput();
        double maxSpeed = parent.v2MaxRotationSpeed.getInput();
        double rotationSpeed = Utils.randomizeDouble(minSpeed, maxSpeed);
        if (!allowJitter) {
            rotationSpeed = Math.max(2.0, minSpeed);
        }
        
        // Distance-based rotation speed adjustments
        if (target != null && !isOvershooting) {
            double distance = mc.thePlayer.getDistanceToEntity(target);
            double distanceMultiplier;
            if (distance <= 2.0) {
                distanceMultiplier = 0.4 + (distance / 2.0) * 0.3;
            } else if (distance <= 4.0) {
                distanceMultiplier = 0.7 + ((distance - 2.0) / 2.0) * 0.3;
            } else {
                distanceMultiplier = 1.0;
            }
            rotationSpeed = rotationSpeed * distanceMultiplier;
            rotationSpeed = Math.max(2.0, rotationSpeed);
        } else if (target != null && isOvershooting) {
            double distance = mc.thePlayer.getDistanceToEntity(target);
            double baseMultiplier = distance < 2.0 ? 0.6 : (distance < 4.0 ? 0.8 : 1.0);
            rotationSpeed = rotationSpeed * baseMultiplier;
            rotationSpeed = Math.max(2.0, rotationSpeed);
        }
        
        // Overshoot Logic
        boolean useOvershoot = parent.v2Overshoot != null && parent.v2Overshoot.isToggled();
        if (target != null && useOvershoot && rotationSpeed < 20 && allowJitter) {
            float currentYaw = rotations[0];
            float currentPitch = rotations[1];
            
            float yawDeltaToActual = RotationUtils.normalize(actualTargetYaw - currentYaw);
            float pitchDeltaToActual = actualTargetPitch - currentPitch;
            float distanceToActual = (float) Math.sqrt(yawDeltaToActual * yawDeltaToActual + pitchDeltaToActual * pitchDeltaToActual);
            
            boolean shouldStartOvershoot = false;
            if (!isOvershooting && !isCorrectingOvershoot) {
                double adjustedChance = parent.overshootChance != null ? parent.overshootChance.getInput() : 30.0;
                if (distanceToActual < 2.0f) {
                    adjustedChance *= 0.5;
                }
                shouldStartOvershoot = distanceToActual > 0.3f && Math.random() * 100.0 < adjustedChance;
            }
            
            if (shouldStartOvershoot) {
                float overshootMultiplier = (float) (1.0 + (parent.overshootAmount != null ? parent.overshootAmount.getInput() : 2.0) / 10.0);
                overshootTargetYaw = currentYaw + yawDeltaToActual * overshootMultiplier;
                overshootTargetPitch = currentPitch + pitchDeltaToActual * overshootMultiplier;
                isOvershooting = true;
                isCorrectingOvershoot = false;
                overshootTicks = 0;
            }
            
            if (isOvershooting) {
                float overshootYawDelta = RotationUtils.normalize(overshootTargetYaw - currentYaw);
                float overshootPitchDelta = overshootTargetPitch - currentPitch;
                float overshootDistance = (float) Math.sqrt(overshootYawDelta * overshootYawDelta + overshootPitchDelta * overshootPitchDelta);
                
                if (overshootDistance < 0.3f || overshootTicks >= MAX_OVERSHOOT_TICKS) {
                    isOvershooting = false;
                    isCorrectingOvershoot = true;
                    overshootTicks = 0;
                    targetYaw = actualTargetYaw;
                    targetPitch = actualTargetPitch;
                } else {
                    targetYaw = overshootTargetYaw;
                    targetPitch = overshootTargetPitch;
                    overshootTicks++;
                }
            } else if (isCorrectingOvershoot) {
                float correctionYawDelta = RotationUtils.normalize(actualTargetYaw - currentYaw);
                float correctionPitchDelta = actualTargetPitch - currentPitch;
                float correctionDistance = (float) Math.sqrt(correctionYawDelta * correctionYawDelta + correctionPitchDelta * correctionPitchDelta);
                
                if (correctionDistance < 0.2f || overshootTicks >= MAX_CORRECTION_TICKS) {
                    isCorrectingOvershoot = false;
                    targetYaw = actualTargetYaw;
                    targetPitch = actualTargetPitch;
                } else {
                    targetYaw = actualTargetYaw;
                    targetPitch = actualTargetPitch;
                    overshootTicks++;
                }
            }
        } else if (target != null) {
            isOvershooting = false;
            isCorrectingOvershoot = false;
        }
        
        // MX / AGC Pitch Bypass - blend pitch instead of zeroing (avoid AGC flags)
        if (allowJitter && parent.mxPitchBypass != null && parent.mxPitchBypass.isToggled() && target != null) {
            float currentPitch = rotations[1];
            float currentYaw = rotations[0];
            float yawDelta = RotationUtils.normalize(targetYaw - currentYaw);
            boolean canHit = RotationUtils.isMouseOver(targetYaw, targetPitch, target, (float) parent.attackRange.getInput());
            if (canHit || Math.abs(yawDelta) < 2.8f) {
                float deltaPitch = targetPitch - currentPitch;
                float damp = 0.55f + (float) (Math.random() * 0.30); // reduce pitch move but keep non-zero
                float maxStep = 4.0f + (float) (Math.random() * 1.2); // cap spikes
                float blended = MathHelper.clamp_float(deltaPitch * damp, -maxStep, maxStep);
                float microJitter = (float) ((Math.random() - 0.5) * 0.35);
                targetPitch = RotationUtils.clampTo90(currentPitch + blended + microJitter);
            }
        }
        
        // Smoothing / Enhancing
        boolean useAdaptiveSmoothing = parent.v2RotationSmoothing != null && parent.v2RotationSmoothing.isToggled() && rotationSpeed < 20;
        
        if (rotationSpeed < 20) {
            float[] enhanced = rotationEnhancer.enhanceRotation(
                targetYaw, targetPitch,
                rotations[0], rotations[1],
                (float) rotationSpeed,
                useAdaptiveSmoothing
            );
            targetYaw = enhanced[0];
            targetPitch = enhanced[1];
        } else {
            rotationEnhancer.enhanceRotation(
                targetYaw, targetPitch,
                rotations[0], rotations[1],
                20.0f,
                false
            );
        }
        
        // Apply rotation smoothing
        float finalRotationSpeed = (float) rotationSpeed;
        if (finalRotationSpeed >= 20) {
            // Instant rotation
            lastYaw = targetYaw;
            lastPitch = targetPitch;
        } else {
            // Smooth rotation using AimSimulator.rotMove
            lastYaw = AimSimulator.rotMove(targetYaw, lastYaw, finalRotationSpeed);
            lastPitch = AimSimulator.rotMove(targetPitch, lastPitch, finalRotationSpeed);
        }
        
        float prevYaw = rotations[0];
        float prevPitch = rotations[1];
        float[] fixed = RotationUtils.fixRotation(lastYaw, lastPitch, prevYaw, prevPitch);
        fixed = finalizeRotations(fixed, prevYaw, prevPitch, true, allowJitter);
        rotations = fixed;
        lastUpdateMs = System.currentTimeMillis();
        return fixed;
    }
    
    public void handleTargetChange(EntityLivingBase oldTarget, EntityLivingBase newTarget) {
        if (newTarget != oldTarget && oldTarget != null && newTarget != null) {
            previousTarget = oldTarget;
            isInterpolating = true;
            interpolationStartTime = System.currentTimeMillis();
            yawVelocity = 0.0f;
            pitchVelocity = 0.0f;
        } else if (newTarget == null && oldTarget != null) {
            isInterpolating = false;
            previousTarget = null;
            isOvershooting = false;
            isCorrectingOvershoot = false;
            overshootTicks = 0;
            yawVelocity = 0.0f;
            pitchVelocity = 0.0f;
        } else if (newTarget != null && oldTarget == null) {
            isInterpolating = false;
            previousTarget = null;
            isOvershooting = false;
            isCorrectingOvershoot = false;
            overshootTicks = 0;
            yawVelocity = 0.0f;
            pitchVelocity = 0.0f;
        }
        
        if (newTarget == null && hasValidTarget) {
            float yawDiff = Math.abs(RotationUtils.normalize(rotations[0] - lastValidTargetYaw));
            float pitchDiff = Math.abs(rotations[1] - lastValidTargetPitch);
            if (yawDiff < 1.0f && pitchDiff < 1.0f) {
                hasValidTarget = false;
            }
        }
    }
    
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck()) return;
        if (ev.phase != TickEvent.Phase.START) return;
        
        // Always update rotations
        rotations = getRotations(KillAura.target);
        
        if (parent.rotationMode.getInput() == 2) {
            mc.thePlayer.rotationYaw = rotations[0];
            mc.thePlayer.rotationPitch = rotations[1];
        }
    }

    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Vec3 hitPos = aimSimulator.getHitPos();
        if (KillAura.target != null) {
            if (rotations != null && parent.dot.isToggled() && hitPos != null) {
                if (animationX == null || animationY == null || animationZ == null) {
                    animationX = new Animation(Easing.EASE_OUT_CIRC, 50);
                    animationY = new Animation(Easing.EASE_OUT_CIRC, 50);
                    animationZ = new Animation(Easing.EASE_OUT_CIRC, 50);

                    animationX.setValue(hitPos.x);
                    animationY.setValue(hitPos.y);
                    animationZ.setValue(hitPos.z);
                }
                animationX.run(hitPos.x);
                animationY.run(hitPos.y);
                animationZ.run(hitPos.z);
                RenderUtils.drawDot(new Vec3(animationX.getValue(), animationY.getValue(), animationZ.getValue()), parent.dotSize.getInput(), 0xFF0670BE);
            }
        } else {
            animationX = animationY = animationZ = null;
        }
    }
    
    public float[] getCurrentRotations() {
        return rotations;
    }
    
    public AimSimulator getAimSimulator() {
        return aimSimulator;
    }

    public void setRotations(float[] rotations) {
        this.rotations = rotations;
    }
    
    public long getLastUpdateMs() {
        return lastUpdateMs;
    }
}

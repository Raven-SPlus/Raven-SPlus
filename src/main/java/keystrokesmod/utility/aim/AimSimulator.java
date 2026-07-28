package keystrokesmod.utility.aim;

import akka.japi.Pair;
import keystrokesmod.module.impl.other.anticheats.utils.phys.Vec2;
import keystrokesmod.module.impl.other.anticheats.utils.world.PlayerRotation;
import keystrokesmod.script.classes.Vec3;
import keystrokesmod.utility.MoveUtil;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import lombok.Getter;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static keystrokesmod.Raven.mc;

public class AimSimulator {
    private double xRandom = 0;
    private double yRandom = 0;
    private double zRandom = 0;
    private long lastNoiseRandom = System.currentTimeMillis();
    private double lastNoiseDeltaX = 0;
    private double lastNoiseDeltaY = 0;
    private double lastNoiseDeltaZ = 0;
    private final List<AxisAlignedBB> boxHistory = new ArrayList<>(101);

    // Anti-detection state (entropy/distinctness guards)
    private final Deque<Float> yawDeltaHistory = new ArrayDeque<>(130);
    private final Deque<Float> pitchDeltaHistory = new ArrayDeque<>(130);
    private long lastEntropyCalc = 0L;
    private double entropyScore = 0.5;
    private double distinctScore = 0.5;
    private float lastYawSample = Float.NaN;
    private float lastPitchSample = Float.NaN;

    // Stationary guard: skip jitter/dither when neither side is moving
    private boolean jitterAllowed = true;

    private boolean nearest = false;
    private double nearestAcc = 0.8;

    private boolean lazy = false;
    private double lazyAcc = 0.95;

    private boolean noise = false;
    private Pair<Float, Float> noiseRandom = new Pair<>(0.35F, 0.5F);
    private double noiseSpeed = 1;
    private long noiseDelay = 100;

    private boolean delay = false;
    private int delayTicks = 1;

    /**
     * 0 = none (handled by callers by disabling noise)
     * 1 = basic (point jumps within hitbox, no interpolation)
     * 2 = linear (existing behaviour, linear interpolation)
     * 3 = smooth (new behaviour, curve-based interpolation)
     */
    private int pointRandomizationMode = 2;

    @Getter
    private Vec3 hitPos = Vec3.ZERO;

    public void setNearest(boolean value, @Range(from = 0, to = 1) double acc) {
        this.nearestAcc = acc;
        this.nearest = value;
    }

    public void setLazy(boolean value, @Range(from = 0, to = 1) double acc) {
        this.lazyAcc = acc;
        this.lazy = value;
    }

    public void setJitterAllowed(boolean allow) {
        this.jitterAllowed = allow;
    }

    public void setNoise(boolean value, Pair<Float, Float> noiseRandom, double noiseSpeed, long noiseDelay) {
        this.noiseRandom = noiseRandom;
        this.noiseSpeed = noiseSpeed / 100;
        this.noiseDelay = noiseDelay;
        this.noise = value;
    }

    public void setDelay(boolean value, int delayTicks) {
        this.delayTicks = delayTicks;
        this.delay = value;
    }

    /**
     * @param mode 0 = none, 1 = basic, 2 = linear, 3 = smooth
     */
    public void setPointRandomizationMode(int mode) {
        this.pointRandomizationMode = mode;
    }

    public @NotNull Pair<Float, Float> getRotation(@NotNull EntityLivingBase target) {
        AxisAlignedBB targetBox = target.getEntityBoundingBox();
        if (boxHistory.size() >= 101) {
            boxHistory.remove(boxHistory.size() - 1);
        }
        while (boxHistory.size() < 101) {
            boxHistory.add(0, targetBox);
        }

        float yaw, pitch;

        final double yDiff = target.posY - mc.thePlayer.posY;
        Vec3 targetPosition;

        AxisAlignedBB aimBox = delay ? boxHistory.get(delayTicks) : targetBox;
        if (nearest) {
            targetPosition = RotationUtils.getNearestPoint(aimBox, Utils.getEyePos());
            if (MoveUtil.isMoving() || MoveUtil.isMoving(target))
                targetPosition = targetPosition.add(Utils.randomizeDouble(nearestAcc - 1, 1 - nearestAcc) * 0.4, Utils.randomizeDouble(nearestAcc - 1, 1 - nearestAcc) * 0.4, Utils.randomizeDouble(nearestAcc - 1, 1 - nearestAcc) * 0.4);
        } else {
            targetPosition = new Vec3((aimBox.maxX + aimBox.minX) / 2, aimBox.minY + target.getEyeHeight() - 0.15, (aimBox.maxZ + aimBox.minZ) / 2);
        }

        if (yDiff >= 0 && lazy) {
            if (targetPosition.y() - yDiff > target.posY) {
                targetPosition = new Vec3(targetPosition.x(), targetPosition.y() - yDiff, targetPosition.z());
            } else {
                targetPosition = new Vec3(target.posX, target.posY + 0.2, target.posZ);
            }
            if (!target.onGround && (MoveUtil.isMoving() || MoveUtil.isMoving(target)))
                targetPosition.y += Utils.randomizeDouble(lazyAcc - 1, 1 - lazyAcc) * 0.4;
        }

        // Lead prediction + reaction offset to follow moving targets with variance
        targetPosition = applyLeadAndReaction(target, targetPosition, aimBox);

        if (noise) {
            // Adaptive scaling based on entropy/distinctness to avoid stable patterns
            double entropyPressure = Math.max(0.0, 0.45 - Math.min(entropyScore, distinctScore));
            double jitterScale = 1.0 + entropyPressure * 1.35; // More jitter when entropy is low
            double speedJitter = 0.9 + Math.random() * 0.22;   // Sensitivity-like jitter (break GCD)
            double adaptiveSpeed = noiseSpeed * speedJitter * jitterScale;

            if (System.currentTimeMillis() - lastNoiseRandom >= noiseDelay) {
                xRandom = random(noiseRandom.first()) * jitterScale;
                yRandom = random(noiseRandom.second()) * jitterScale;
                zRandom = random(noiseRandom.first()) * jitterScale;
                lastNoiseRandom = System.currentTimeMillis();
            }

            // Occasional burst noise inside attack windows (counter constant distinct / entropy)
            double burst = (entropyPressure > 0.25 && Math.random() < 0.18)
                ? (Math.random() - 0.5) * (noiseRandom.first() * 0.35)
                : 0.0;

            // Small dither to break consistent quantization
            double ditherX = (Math.random() - 0.5) * noiseRandom.first() * 0.12;
            double ditherY = (Math.random() - 0.5) * noiseRandom.second() * 0.12;
            double ditherZ = (Math.random() - 0.5) * noiseRandom.first() * 0.12;

            double scaledX = xRandom + ditherX + burst;
            double scaledY = yRandom + ditherY;
            double scaledZ = zRandom + ditherZ + burst;

            if (pointRandomizationMode == 1) { // basic – direct random point inside hitbox, no interpolation
                targetPosition.x = normal(targetBox.maxX, targetBox.minX, targetPosition.x + scaledX);
                targetPosition.y = normal(targetBox.maxY, targetBox.minY, targetPosition.y + scaledY);
                targetPosition.z = normal(targetBox.maxZ, targetBox.minZ, targetPosition.z + scaledZ);
            } else if (pointRandomizationMode == 2) { // linear – existing linear interpolated behaviour
                lastNoiseDeltaX = rotMove(scaledX, lastNoiseDeltaX, adaptiveSpeed);
                lastNoiseDeltaY = rotMove(scaledY, lastNoiseDeltaY, adaptiveSpeed);
                lastNoiseDeltaZ = rotMove(scaledZ, lastNoiseDeltaZ, adaptiveSpeed);

                targetPosition.x = normal(targetBox.maxX, targetBox.minX, targetPosition.x + lastNoiseDeltaX);
                targetPosition.y = normal(targetBox.maxY, targetBox.minY, targetPosition.y + lastNoiseDeltaY);
                targetPosition.z = normal(targetBox.maxZ, targetBox.minZ, targetPosition.z + lastNoiseDeltaZ);
            } else if (pointRandomizationMode == 3) { // smooth – curve-based interpolated behaviour
                // Use smooth interpolation with ease-in-out curves
                lastNoiseDeltaX = smoothRotMove(scaledX, lastNoiseDeltaX, adaptiveSpeed);
                lastNoiseDeltaY = smoothRotMove(scaledY, lastNoiseDeltaY, adaptiveSpeed);
                lastNoiseDeltaZ = smoothRotMove(scaledZ, lastNoiseDeltaZ, adaptiveSpeed);

                targetPosition.x = normal(targetBox.maxX, targetBox.minX, targetPosition.x + lastNoiseDeltaX);
                targetPosition.y = normal(targetBox.maxY, targetBox.minY, targetPosition.y + lastNoiseDeltaY);
                targetPosition.z = normal(targetBox.maxZ, targetBox.minZ, targetPosition.z + lastNoiseDeltaZ);
            }
        }

        yaw = PlayerRotation.getYaw(targetPosition);
        pitch = PlayerRotation.getPitch(targetPosition);

        // Anti-detection jitter on quantized / repeated deltas
        if (jitterAllowed) {
            Pair<Float, Float> jittered = applyMicroJitter(yaw, pitch);
            yaw = jittered.first();
            pitch = jittered.second();
            // Update entropy/distinctness trackers after jitter so we learn the shaped path
            updateEntropyGuards(yaw, pitch);
        } else {
            updateEntropyGuards(yaw, pitch);
        }

        hitPos = targetPosition;

        return new Pair<>(yaw, pitch);
    }

    private static float random(double multiple) {
        return (float) ((Math.random() - 0.5) * 2 * multiple);
    }

    private static double normal(double max, double min, double current) {
        if (current >= max) return max;
        return Math.max(current, min);
    }

    public static float rotMove(double target, double current, double diff) {
        return rotMoveNoRandom((float) target, (float) current, (float) diff);
    }

    public static float rotMoveNoRandom(float target, float current, float diff) {
        float delta;
        if (target > current) {
            float dist1 = target - current;
            float dist2 = current + 360 - target;
            if (dist1 > dist2) {  // wrapping the other way is shorter
                delta = -current - 360 + target;
            } else {
                delta = dist1;
            }
        } else if (target < current) {
            float dist1 = current - target;
            float dist2 = target + 360 - current;
            if (dist1 > dist2) {  // wrapping the other way is shorter
                delta = current + 360 + target;
            } else {
                delta = -dist1;
            }
        } else {
            return current;
        }

        delta = RotationUtils.normalize(delta);

        if (Math.abs(delta) < 0.1 * Math.random() + 0.1) {
            return current;
        } else if (Math.abs(delta) <= diff) {
            return current + delta;
        } else {
            if (delta < 0) {
                return current - diff;
            } else if (delta > 0) {
                return current + diff;
            } else {
                return current;
            }
        }
    }

    /**
     * Smooth rotation movement with curve interpolation (ease-in-out)
     * Used for point randomization mode 3 (Smooth)
     */
    private static double smoothRotMove(double target, double current, double maxSpeed) {
        double delta = target - current;
        
        if (Math.abs(delta) < 0.01) {
            return current;
        } else if (Math.abs(delta) <= maxSpeed) {
            return current + delta;
        } else {
            // Apply smooth interpolation with ease-in-out curve
            // Calculate progress (0 to 1) based on how much we're moving this frame
            double progress = maxSpeed / Math.abs(delta);
            
            // Apply ease-in-out curve: smooth acceleration and deceleration
            double easedProgress;
            if (progress < 0.5) {
                // Ease in: quadratic curve
                easedProgress = 2 * progress * progress;
            } else {
                // Ease out: quadratic curve
                easedProgress = 1 - Math.pow(-2 * progress + 2, 2) / 2;
            }
            
            // Apply the eased movement
            double movement = delta * easedProgress;
            if (Math.abs(movement) < 0.01) {
                movement = delta > 0 ? maxSpeed : -maxSpeed;
            }
            
            return current + movement;
        }
    }

    public static boolean yawEquals(float yaw1, float yaw2) {
        return RotationUtils.isYawClose(yaw1, yaw2, 0.1f);
    }

    public static boolean equals(@NotNull Vec2 rot1, @NotNull Vec2 rot2) {
        return yawEquals(rot1.x, rot2.x) && Math.abs(rot1.y - rot2.y) < 0.1;
    }

    // region anti-detection helpers

    private Pair<Float, Float> applyMicroJitter(float yaw, float pitch) {
        // Scale jitter intensity when entropy/distinctness drop (anti-pattern)
        double lowEntropyPressure = Math.max(0.0, 0.45 - Math.min(entropyScore, distinctScore));
        double jitterScale = 1.0 + lowEntropyPressure * 1.4; // up to ~1.63x

        // Sensitivity-like jitter to break GCD / fixed-step detections
        double sensitivityJitter = 0.92 + (Math.random() * 0.18);
        double baseJitter = 0.0085 * jitterScale * sensitivityJitter;

        // Quantization-aware nudge: if close to integer or 0.1 steps, push off-grid
        yaw = nudgeIfQuantized(yaw, baseJitter);
        pitch = nudgeIfQuantized(pitch, baseJitter * 0.6);

        // Light, always-on dither
        yaw += (float) ((Math.random() - 0.5) * baseJitter);
        pitch += (float) ((Math.random() - 0.5) * baseJitter * 0.75);

        // Burst noise for short windows when entropy is very low (counter MX rank/entropy)
        if (lowEntropyPressure > 0.25 && Math.random() < 0.18) {
            yaw += (float) ((Math.random() - 0.5) * baseJitter * 4.0);
            pitch += (float) ((Math.random() - 0.5) * baseJitter * 3.0);
        }

        // Keep pitch in reasonable range to avoid over-clamping elsewhere
        pitch = RotationUtils.clampTo90(pitch);
        return new Pair<>(RotationUtils.normalize(yaw), pitch);
    }

    private static float nudgeIfQuantized(float angle, double jitter) {
        float abs = Math.abs(angle);
        boolean nearInt = Math.abs(abs - Math.round(abs)) < 1.0E-3;
        boolean nearTenth = Math.abs(abs * 10 - Math.round(abs * 10)) < 1.0E-3;
        if (nearInt || nearTenth) {
            angle += (float) ((Math.random() - 0.5) * jitter * 6.0);
        }
        return angle;
    }

    private void updateEntropyGuards(float yaw, float pitch) {
        if (!Float.isNaN(lastYawSample)) {
            float dyaw = RotationUtils.normalize(yaw - lastYawSample);
            float dpitch = pitch - lastPitchSample;
            pushDelta(yawDeltaHistory, dyaw, 120);
            pushDelta(pitchDeltaHistory, dpitch, 120);
        }
        lastYawSample = yaw;
        lastPitchSample = pitch;

        long now = System.currentTimeMillis();
        if (now - lastEntropyCalc < 75 || yawDeltaHistory.size() < 24) {
            return;
        }
        lastEntropyCalc = now;

        entropyScore = (computeShannon(yawDeltaHistory) * 0.6) + (computeShannon(pitchDeltaHistory) * 0.4);
        distinctScore = (computeDistinct(yawDeltaHistory) + computeDistinct(pitchDeltaHistory)) * 0.5;

        // Normalize to [0,1]
        entropyScore = bound01(entropyScore);
        distinctScore = bound01(distinctScore);
    }

    private static void pushDelta(Deque<Float> deque, float value, int max) {
        if (deque.size() >= max) {
            deque.removeFirst();
        }
        deque.addLast(value);
    }

    private static double computeDistinct(Deque<Float> deque) {
        if (deque.isEmpty()) return 0.5;
        Set<Integer> uniques = new HashSet<>();
        for (float v : deque) {
            // Quantize to 0.01 deg to mirror small mouse deltas
            int bucket = (int) Math.round(v * 100.0f);
            uniques.add(bucket);
        }
        return uniques.size() / (double) deque.size();
    }

    private static double computeShannon(Deque<Float> deque) {
        if (deque.isEmpty()) return 0.5;
        Map<Integer, Integer> counts = new HashMap<>();
        for (float v : deque) {
            int bucket = (int) Math.round(v * 50.0f); // ~0.02 deg buckets
            counts.merge(bucket, 1, Integer::sum);
        }
        double total = deque.size();
        double entropy = 0.0;
        for (int c : counts.values()) {
            double p = c / total;
            entropy -= p * (Math.log(p) / Math.log(2.0));
        }
        double maxEntropy = Math.log(counts.size() == 0 ? 1 : counts.size()) / Math.log(2.0);
        if (maxEntropy <= 0.0) return 0.5;
        return entropy / maxEntropy;
    }

    private static double bound01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    // endregion

    private Vec3 applyLeadAndReaction(@NotNull EntityLivingBase target, @NotNull Vec3 base, @NotNull AxisAlignedBB aimBox) {
        if (!jitterAllowed) {
            return base;
        }

        double vx = target.posX - target.lastTickPosX;
        double vy = target.posY - target.lastTickPosY;
        double vz = target.posZ - target.lastTickPosZ;

        // Reaction window 90-200ms with entropy-driven variance
        double reactionMs = 90 + Math.random() * 110;
        double entropyPressure = Math.max(0.0, 0.45 - Math.min(entropyScore, distinctScore));
        reactionMs += entropyPressure * 120; // more lead when entropy is low to diversify paths

        double leadTicks = reactionMs / 50.0; // 20 tps -> 50ms per tick
        // Damp vertical lead slightly to avoid overshoot, add tiny noise
        double leadX = vx * leadTicks;
        double leadY = (vy * leadTicks) * 0.55 + (Math.random() - 0.5) * 0.02;
        double leadZ = vz * leadTicks;

        Vec3 lead = new Vec3(
            base.x() + leadX,
            base.y() + leadY,
            base.z() + leadZ
        );

        // Clamp inside box to respect aim bounds
        lead.x = normal(aimBox.maxX, aimBox.minX, lead.x());
        lead.y = normal(aimBox.maxY, aimBox.minY, lead.y());
        lead.z = normal(aimBox.maxZ, aimBox.minZ, lead.z());
        return lead;
    }
}

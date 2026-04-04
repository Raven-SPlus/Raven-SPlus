package keystrokesmod.module.impl.world;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.RotationEvent;
import keystrokesmod.module.impl.other.RotationHandler;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.aim.AimSimulator;
import keystrokesmod.utility.aim.RotationData;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;

final class ScaffoldRotationController {
    private final Scaffold scaffold;
    private final ScaffoldSessionState state;
    private final ScaffoldMovementController movementController;
    private final ScaffoldRotationMode[] rotations;
    private float lastVisualYaw;
    private float lastVisualPitch;
    private boolean hasVisualRotation;

    ScaffoldRotationController(Scaffold scaffold, ScaffoldSessionState state, ScaffoldMovementController movementController) {
        this.scaffold = scaffold;
        this.state = state;
        this.movementController = movementController;
        this.rotations = new ScaffoldRotationMode[]{
                (placeYaw, placePitch, forceStrict, event) -> new RotationData(event.getYaw(), event.getPitch()),
                (placeYaw, placePitch, forceStrict, event) -> computeSteppedRotation(false, event),
                (placeYaw, placePitch, forceStrict, event) -> computeSteppedRotation(false, event),
                (placeYaw, placePitch, forceStrict, event) -> computeSteppedRotation(true, event)
        };
    }

    void resetForEnable() {
        hasVisualRotation = false;
        state.hasServerRotation = false;
        state.hasTargetRotation = false;
        state.rotationReady = false;
        state.raytraceReady = false;
    }

    void applyPreMotion(PreMotionEvent event) {
        int mode = Math.max(0, Math.min(rotations.length - 1, (int) scaffold.rotation.getInput()));
        RotationData data = rotations[mode].onRotation(scaffold.placeYaw, scaffold.placePitch, mode == 3,
                new RotationEvent(event.getYaw(), event.getPitch(), RotationHandler.MoveFix.None));
        float targetYaw = data.getYaw();
        float targetPitch = data.getPitch();

        event.setYaw(targetYaw);
        event.setPitch(movementController.clampPitch(targetPitch));

        movementController.applyJumpFacingForward(event, RotationHandler.getRotationYaw());
        targetYaw = event.getYaw();
        targetPitch = event.getPitch();

        state.serverYaw = MathHelper.wrapAngleTo180_float(targetYaw);
        state.serverPitch = movementController.clampPitch(targetPitch);
        state.scaffoldYaw = state.serverYaw;
        state.scaffoldPitch = state.serverPitch;
        state.hasServerRotation = mode > 0;

        updateRaytraceReadiness(mode);

        if (mode == 0) {
            RotationUtils.serverRotations[0] = targetYaw;
            RotationUtils.serverRotations[1] = event.getPitch();
            hasVisualRotation = false;
            return;
        }

        smoothVisualRotation(event);
        RotationUtils.serverRotations[0] = state.serverYaw;
        RotationUtils.serverRotations[1] = state.serverPitch;
    }

    void applyMoveFix(RotationEvent event) {
        if (!scaffold.stopRotation()) {
            return;
        }
        event.setMoveFix(scaffold.rotation.getInput() >= 3
                ? RotationHandler.MoveFix.Strict
                : RotationHandler.MoveFix.Continuous);
        event.noSmoothBack();
    }

    boolean canSchedulePlace() {
        if (scaffold.rotation.getInput() <= 0) {
            return true;
        }
        if (!state.hasTargetRotation || !state.rotationReady) {
            return false;
        }
        if (scaffold.rotation.getInput() >= 3) {
            return state.raytraceReady;
        }
        return state.raytraceReady || state.ticksSincePlace > 2;
    }

    private void smoothVisualRotation(PreMotionEvent event) {
        float targetYaw = state.serverYaw;
        float targetPitch = state.serverPitch;

        if (!hasVisualRotation) {
            lastVisualYaw = targetYaw;
            lastVisualPitch = targetPitch;
            hasVisualRotation = true;
        }

        boolean strict = scaffold.rotation.getInput() >= 3;
        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(targetYaw - lastVisualYaw));
        float pitchDiff = Math.abs(targetPitch - lastVisualPitch);

        if (yawDiff > (strict ? 28F : 20F) || pitchDiff > (strict ? 16F : 12F)) {
            lastVisualYaw = targetYaw;
            lastVisualPitch = targetPitch;
        } else {
            float smoothSpeed = strict ? 42F : 58F;
            lastVisualYaw = AimSimulator.rotMove(lastVisualYaw, targetYaw, smoothSpeed);
            lastVisualPitch = AimSimulator.rotMove(lastVisualPitch, targetPitch, smoothSpeed * 0.9F);
        }

        event.setYaw(lastVisualYaw);
        event.setPitch(lastVisualPitch);
    }

    private RotationData computeSteppedRotation(boolean strict, RotationEvent event) {
        float currentYaw = state.hasServerRotation ? state.serverYaw : RotationHandler.getRotationYaw(event.getYaw());
        float currentPitch = state.hasServerRotation ? state.serverPitch : RotationHandler.getRotationPitch(event.getPitch());
        float fallbackYaw = MathHelper.wrapAngleTo180_float(currentYaw - scaffold.hardcodedYaw());
        float fallbackPitch = scaffold.getCurrentFace() == 1 ? 84.5F : 80.0F;

        float targetYaw = fallbackYaw;
        float targetPitch = fallbackPitch;
        if (state.blockRotations != null) {
            targetYaw = state.blockRotations[0];
            targetPitch = state.blockRotations[1];
        }

        if (state.rotateForward && scaffold.jumpFacingForwardEnabled()) {
            targetYaw = MathHelper.wrapAngleTo180_float(RotationHandler.getRotationYaw(event.getYaw()) - scaffold.hardcodedYaw() - 180F);
            targetPitch = 10F;
        } else if (!strict) {
            float motionYaw = movementController.getMotionYaw();
            float yawDiffToMotion = Math.abs(MathHelper.wrapAngleTo180_float(targetYaw - motionYaw));
            if (keystrokesmod.utility.Utils.isMoving() && yawDiffToMotion > 120F) {
                targetYaw = motionYaw - 120F * Math.signum(MathHelper.wrapAngleTo180_float(motionYaw - targetYaw));
            }
            targetPitch = Math.max(targetPitch, 73.0F);
        } else {
            targetPitch = Math.max(targetPitch, 78.0F);
        }

        state.targetYaw = MathHelper.wrapAngleTo180_float(targetYaw);
        state.targetPitch = movementController.clampPitch(targetPitch);
        state.hasTargetRotation = true;

        float yawGap = Math.abs(MathHelper.wrapAngleTo180_float(state.targetYaw - currentYaw));
        float pitchGap = Math.abs(state.targetPitch - currentPitch);
        boolean fastAcquire = yawGap > (strict ? 32F : 20F) || pitchGap > (strict ? 14F : 10F);

        float maxYawStep = strict
                ? (fastAcquire ? Math.min(55F, 14F + yawGap * 0.55F) : 20F)
                : (fastAcquire ? Math.min(78F, 18F + yawGap * 0.80F) : 30F);
        float maxPitchStep = strict
                ? (fastAcquire ? Math.min(26F, 8F + pitchGap * 0.70F) : 10F)
                : (fastAcquire ? Math.min(34F, 10F + pitchGap * 0.90F) : 16F);
        float accel = strict
                ? (fastAcquire ? 0.72F : 0.48F)
                : (fastAcquire ? 0.88F : 0.65F);
        float stopThreshold = strict ? 0.12F : 0.20F;

        float[] stepped = RotationUtils.stepTowardTarget(
                currentYaw,
                currentPitch,
                state.targetYaw,
                state.targetPitch,
                state.yawVelocity,
                state.pitchVelocity,
                maxYawStep,
                maxPitchStep,
                accel,
                stopThreshold
        );

        state.yawVelocity = stepped[2];
        state.pitchVelocity = stepped[3];

        float serverYaw = stepped[0];
        float serverPitch = movementController.clampPitch(stepped[1]);

        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(state.targetYaw - serverYaw));
        float pitchDiff = Math.abs(state.targetPitch - serverPitch);
        state.rotationReady = yawDiff <= (strict ? 2.25F : 6.0F) && pitchDiff <= (strict ? 1.75F : 4.0F);

        return new RotationData(serverYaw, serverPitch);
    }

    private void updateRaytraceReadiness(int mode) {
        if (mode <= 0 || state.currentPlacement == null) {
            state.raytraceReady = true;
            return;
        }

        MovingObjectPosition raycast = RotationUtils.rayTraceCustom(
                RotationUtils.mc.playerController.getBlockReachDistance(),
                state.serverYaw,
                state.serverPitch
        );

        state.raytraceReady = raycast != null
                && raycast.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && raycast.getBlockPos().equals(state.currentPlacement.blockPos)
                && raycast.sideHit == state.currentPlacement.enumFacing;
    }
}

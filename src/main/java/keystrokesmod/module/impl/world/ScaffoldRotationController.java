package keystrokesmod.module.impl.world;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.RotationEvent;
import keystrokesmod.module.impl.other.RotationHandler;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.aim.AimSimulator;
import keystrokesmod.utility.aim.RotationData;
import net.minecraft.util.MathHelper;

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
                (placeYaw, placePitch, forceStrict, event) -> {
                    float clientYaw = RotationHandler.getRotationYaw();
                    float yaw = clientYaw - scaffold.hardcodedYaw();
                    float pitch = currentFacePitch();
                    return new RotationData(yaw, pitch);
                },
                (placeYaw, placePitch, forceStrict, event) -> computeOffsetRotation(event, forceStrict),
                (placeYaw, placePitch, forceStrict, event) -> {
                    float yaw = state.blockRotations != null ? state.blockRotations[0] : RotationHandler.getRotationYaw() - scaffold.hardcodedYaw();
                    float pitch = state.blockRotations != null ? state.blockRotations[1] : 80F;
                    return new RotationData(yaw, pitch);
                }
        };
    }

    void resetForEnable() {
        hasVisualRotation = false;
    }

    void applyPreMotion(PreMotionEvent event) {
        int mode = Math.max(0, Math.min(rotations.length - 1, (int) scaffold.rotation.getInput()));
        RotationData data = rotations[mode].onRotation(scaffold.placeYaw, scaffold.placePitch, false,
                new RotationEvent(event.getYaw(), event.getPitch(), RotationHandler.MoveFix.None));
        event.setYaw(data.getYaw());
        event.setPitch(data.getPitch());

        movementController.applyJumpFacingForward(event, RotationHandler.getRotationYaw());
        event.setPitch(movementController.clampPitch(event.getPitch()));

        smoothVisualRotation(event);
        RotationUtils.serverRotations[0] = event.getYaw();
        RotationUtils.serverRotations[1] = event.getPitch();
    }

    void applyMoveFix(RotationEvent event) {
        if (!scaffold.stopRotation()) {
            return;
        }
        event.setMoveFix(RotationHandler.MoveFix.Continuous);
    }

    boolean canSchedulePlace() {
        int mode = Math.max(0, Math.min(rotations.length - 1, (int) scaffold.rotation.getInput()));
        return rotations[mode].onPreSchedulePlace();
    }

    private void smoothVisualRotation(PreMotionEvent event) {
        float targetYaw = event.getYaw();
        float targetPitch = event.getPitch();

        if (!hasVisualRotation) {
            lastVisualYaw = targetYaw;
            lastVisualPitch = targetPitch;
            hasVisualRotation = true;
        }

        lastVisualYaw = AimSimulator.rotMove(lastVisualYaw, targetYaw, 30F);
        lastVisualPitch = AimSimulator.rotMove(lastVisualPitch, targetPitch, 30F);
        event.setYaw(lastVisualYaw);
        event.setPitch(lastVisualPitch);
    }

    private float currentFacePitch() {
        float pitch = 79F;
        if (scaffold.getCurrentFace() == 1) {
            pitch = 87F;
        }
        return pitch;
    }

    private RotationData computeOffsetRotation(RotationEvent event, boolean forceStrict) {
        float clientYaw = RotationHandler.getRotationYaw();
        float moveAngle = (float) scaffold.getMovementAngle();
        float relativeYaw = clientYaw + moveAngle;
        float normalizedYaw = (relativeYaw % 360 + 360) % 360;
        float quad = normalizedYaw % 90;
        float side = MathHelper.wrapAngleTo180_float(movementController.getMotionYaw() - state.scaffoldYaw);
        float yawBackwards = MathHelper.wrapAngleTo180_float(clientYaw) - scaffold.hardcodedYaw();
        float blockYawOffset = MathHelper.wrapAngleTo180_float(yawBackwards - state.blockYaw);

        if (quad <= 5 || quad >= 85) {
            state.yawAngle = 127.40F;
            state.minOffset = 13F;
            state.minPitch = 75.48F;
        }
        if (quad > 5 && quad <= 15 || quad >= 75 && quad < 85) {
            state.yawAngle = 128.55F;
            state.minOffset = 11F;
            state.minPitch = 75.74F;
        }
        if (quad > 15 && quad <= 25 || quad >= 65 && quad < 75) {
            state.yawAngle = 129.70F;
            state.minOffset = 8F;
            state.minPitch = 75.95F;
        }
        if (quad > 25 && quad <= 32 || quad >= 58 && quad < 65) {
            state.yawAngle = 130.85F;
            state.minOffset = 6F;
            state.minPitch = 76.13F;
        }
        if (quad > 32 && quad <= 38 || quad >= 52 && quad < 58) {
            state.yawAngle = 131.80F;
            state.minOffset = 5F;
            state.minPitch = 76.41F;
        }
        if (quad > 38 && quad <= 42 || quad >= 48 && quad < 52) {
            state.yawAngle = 134.30F;
            state.minOffset = 4F;
            state.minPitch = 77.54F;
        }
        if (quad > 42 && quad <= 45 || quad >= 45 && quad < 48) {
            state.yawAngle = 137.85F;
            state.minOffset = 3F;
            state.minPitch = 77.93F;
        }

        if (state.enabledOffGround) {
            if (state.blockRotations != null) {
                state.scaffoldYaw = state.blockRotations[0];
                state.scaffoldPitch = state.blockRotations[1];
            } else {
                state.scaffoldYaw = clientYaw - scaffold.hardcodedYaw();
                state.scaffoldPitch = 78F;
            }
            return new RotationData(state.scaffoldYaw, state.scaffoldPitch);
        }

        if (state.blockRotations != null) {
            state.blockYaw = state.blockRotations[0];
            state.scaffoldPitch = Math.max(state.blockRotations[1], state.minPitch);
            state.yawOffset = blockYawOffset;
        } else {
            state.scaffoldPitch = state.minPitch;
            if (state.edge == 1) {
                state.firstStroke = System.currentTimeMillis();
            }
            state.yawOffset = 0;
        }

        if (!keystrokesmod.utility.Utils.isMoving() || keystrokesmod.utility.Utils.getHorizontalSpeed() == 0.0D) {
            return new RotationData(state.theYaw, state.scaffoldPitch);
        }

        float motionYaw = movementController.getMotionYaw();
        float newYaw = motionYaw - state.yawAngle * Math.signum(MathHelper.wrapAngleTo180_float(motionYaw - state.scaffoldYaw));
        state.scaffoldYaw = MathHelper.wrapAngleTo180_float(newYaw);

        if (quad > 5 && quad < 85) {
            if (quad < 45F) {
                if (state.firstStroke == 0) {
                    state.set2 = side < 0;
                }
                if (state.was452) {
                    state.firstStroke = System.currentTimeMillis();
                }
                state.was451 = true;
                state.was452 = false;
            } else {
                if (state.firstStroke == 0) {
                    state.set2 = side >= 0;
                }
                if (state.was451) {
                    state.firstStroke = System.currentTimeMillis();
                }
                state.was452 = true;
                state.was451 = false;
            }
        }

        double minSwitch = !keystrokesmod.utility.scaffold.ScaffoldUtils.scaffoldDiagonal(false) ? 9 : 15;
        if (side >= 0) {
            if (state.yawOffset <= 0 && state.firstStroke == 0 && quad <= 5 || quad >= 85) {
                if (state.yawOffset <= -minSwitch && !state.set2) {
                    state.firstStroke = System.currentTimeMillis();
                    state.set2 = false;
                }
            } else if (state.yawOffset >= minSwitch && state.firstStroke == 0 && quad <= 5 || quad >= 85) {
                if (!state.set2) {
                    state.firstStroke = System.currentTimeMillis();
                }
                state.set2 = true;
            }
            if (state.set2) {
                state.yawOffset = Math.max(0, Math.min(state.minOffset, state.yawOffset));
                state.theYaw = (state.scaffoldYaw + state.yawAngle * 2) - state.yawOffset;
                return new RotationData(state.theYaw, state.scaffoldPitch);
            }
        } else {
            if (state.set2) {
                state.yawOffset = Math.min(0, Math.max(-state.minOffset, state.yawOffset));
                state.theYaw = (state.scaffoldYaw - state.yawAngle * 2) - state.yawOffset;
                return new RotationData(state.theYaw, state.scaffoldPitch);
            }
        }

        if (side >= 0) {
            state.yawOffset = Math.max(-state.minOffset, Math.min(0, state.yawOffset));
        } else {
            state.yawOffset = Math.min(state.minOffset, Math.max(0, state.yawOffset));
        }
        state.theYaw = state.scaffoldYaw - state.yawOffset;
        return new RotationData(state.theYaw, state.scaffoldPitch);
    }
}

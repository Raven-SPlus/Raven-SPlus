package keystrokesmod.module.impl.world;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.MoveUtil;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.scaffold.ScaffoldUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

final class ScaffoldMovementController {
    private final Scaffold scaffold;
    private final ScaffoldSessionState state;
    private final Minecraft mc = Minecraft.getMinecraft();

    ScaffoldMovementController(Scaffold scaffold, ScaffoldSessionState state) {
        this.scaffold = scaffold;
        this.state = state;
    }

    void onPreMotion() {
        if (Utils.isMoving()) {
            state.scaffoldTicks++;
        } else {
            state.scaffoldTicks = 0;
        }
        state.canBlockFade = true;
        state.ticksSincePlace++;
        if (state.placeCooldownTicks > 0) {
            state.placeCooldownTicks--;
        }
    }

    void handleFastScaffoldJump() {
        boolean longJumpEnabled = ModuleManager.longJump != null && ModuleManager.longJump.isEnabled();
        if (Utils.keysDown() && scaffold.usingFastScaffold() && scaffold.getFastScaffoldMode() >= 1
                && !ModuleManager.tower.canTower() && !longJumpEnabled) {
            if (mc.thePlayer.onGround && Utils.isMoving() && state.scaffoldTicks > 1) {
                scaffold.rotateForward();
                mc.thePlayer.jump();
                double variance = scaffold.isGrimLegitMotion() ? Utils.randomizeDouble(0.0012, 0.0030) : Utils.randomizeDouble(0.0003, 0.0001);
                Utils.setSpeed(scaffold.getSpeed(scaffold.getSpeedLevel()) - variance);
                if (scaffold.getFastScaffoldMode() == 5 || scaffold.getFastScaffoldMode() == 2 && state.firstKeepYPlace) {
                    state.lowhop = true;
                }
                if (state.startYPos == -1 || Math.abs(state.startYPos - mc.thePlayer.posY) > 5) {
                    state.startYPos = mc.thePlayer.posY;
                    state.fastScaffoldKeepY = true;
                }
            }
        } else if (state.fastScaffoldKeepY) {
            state.fastScaffoldKeepY = false;
            state.firstKeepYPlace = false;
            state.startYPos = -1;
            state.keepYTicks = 0;
        }
    }

    void handleFloat(keystrokesmod.event.PreMotionEvent event) {
        boolean longJumpEnabled = ModuleManager.longJump != null && ModuleManager.longJump.isEnabled();
        boolean speedEnabled = ModuleManager.speed != null && ModuleManager.speed.isEnabled();
        if (scaffold.getSprintMode() == 2 && !scaffold.usingFastScaffold() && !speedEnabled && !ModuleManager.tower.canTower() && !longJumpEnabled) {
            state.floatWasEnabled = true;
            if (!state.floatStarted) {
                if (ScaffoldUtils.groundTicks > 8 && mc.thePlayer.onGround) {
                    state.floatKeepY = true;
                    state.startYPos = event.getPosY();
                    mc.thePlayer.jump();
                    if (Utils.isMoving()) {
                        double variance = scaffold.isGrimLegitMotion() ? Utils.randomizeDouble(0.0010, 0.0024) : Utils.randomizeDouble(0.0003, 0.0001);
                        Utils.setSpeed(scaffold.getSpeed(scaffold.getSpeedLevel()) - variance);
                    }
                    state.floatJumped = true;
                } else if (ScaffoldUtils.groundTicks <= 8 && mc.thePlayer.onGround) {
                    state.floatStarted = true;
                }
                if (state.floatJumped && !mc.thePlayer.onGround) {
                    state.floatStarted = true;
                }
            }

            if (state.floatStarted && mc.thePlayer.onGround) {
                state.floatKeepY = false;
                state.startYPos = -1;
                if (scaffold.moduleEnabled) {
                    if (!scaffold.isGrimLegitMotion()) {
                        event.setPosY(event.getPosY() + ScaffoldUtils.offsetValue);
                    }
                    if (Utils.isMoving()) {
                        Utils.setSpeed(scaffold.getFloatSpeed(scaffold.getSpeedLevel()));
                    }
                }
            }
        } else if (state.floatWasEnabled && scaffold.moduleEnabled) {
            if (state.floatKeepY) {
                state.startYPos = -1;
            }
            state.floatStarted = false;
            state.floatJumped = false;
            state.floatKeepY = false;
            state.floatWasEnabled = false;
        }
    }

    void applyJumpFacingForward(keystrokesmod.event.PreMotionEvent event, float clientYaw) {
        if (ScaffoldUtils.inAirTicks >= 1) {
            state.rotateForward = false;
        }
        if (state.rotateForward && scaffold.jumpFacingForwardEnabled()) {
            if (scaffold.rotation.getInput() > 0) {
                if (!state.rotatingForward) {
                    state.rotationDelay = 2;
                    state.rotatingForward = true;
                }
                float forwardYaw = clientYaw - scaffold.hardcodedYaw() - 180F;
                event.setYaw(forwardYaw);
                event.setPitch(10F);
            }
        } else {
            state.rotatingForward = false;
        }
    }

    void afterRotationApplied() {
        if (mc.thePlayer.onGround) {
            state.enabledOffGround = false;
        }
        if (state.rotationDelay > 0) {
            state.rotationDelay--;
        }
    }

    void handleKeepYPlacement() {
        if (!state.fastScaffoldKeepY || ModuleManager.tower.canTower()) {
            return;
        }

        state.keepYTicks++;
        if (scaffold.isGrimLegitMotion() && state.placeCooldownTicks > 0) {
            return;
        }

        if (scaffold.isGrimLegitMotion() && state.keepYJitterTicks < 0) {
            state.keepYJitterTicks = Utils.randomizeInt(0, 2);
        }
        int jitter = scaffold.isGrimLegitMotion() ? Math.max(0, state.keepYJitterTicks) : 0;
        int tickA = state.firstKeepYPlace ? 7 : 8;
        int tickB = 11;
        int tickC = 7;
        int tickD = 3;
        if ((int) mc.thePlayer.posY > (int) state.startYPos) {
            switch (scaffold.getFastScaffoldMode()) {
                case 1:
                    if ((!state.firstKeepYPlace && state.keepYTicks == tickA + jitter) || state.keepYTicks == tickB + jitter) {
                        scaffold.placeBlock(1, 0);
                        state.firstKeepYPlace = true;
                    }
                    break;
                case 2:
                    if ((!state.firstKeepYPlace && state.keepYTicks == tickA + jitter) || (state.firstKeepYPlace && state.keepYTicks == tickC + jitter)) {
                        scaffold.placeBlock(1, 0);
                        state.firstKeepYPlace = true;
                    }
                    break;
                case 3:
                    if (!state.firstKeepYPlace && state.keepYTicks == tickC + jitter) {
                        scaffold.placeBlock(1, 0);
                        state.firstKeepYPlace = true;
                    }
                    break;
                case 6:
                    if (!state.firstKeepYPlace && state.keepYTicks == tickD + jitter) {
                        scaffold.placeBlock(1, 0);
                        state.firstKeepYPlace = true;
                    }
                    break;
                default:
                    break;
            }
        }
        if (mc.thePlayer.onGround) {
            state.keepYTicks = 0;
            state.keepYJitterTicks = -1;
        }
        if ((int) mc.thePlayer.posY == (int) state.startYPos) {
            state.firstKeepYPlace = false;
        }
    }

    void handleMotionScale() {
        if (ModuleManager.tower.canTower() || !mc.thePlayer.onGround || scaffold.getMotionPercent() == 100) {
            return;
        }
        if (Utils.isMoving()) {
            double input = scaffold.getMotionPercent() / 100.0;
            double targetSpeed = MoveUtil.speed() * input;
            if (scaffold.isGrimLegitMotion()) {
                targetSpeed *= 0.985 + Utils.randomizeDouble(0.0, 0.012);
            }
            MoveUtil.strafe(targetSpeed);
        }
    }

    float clampPitch(float pitch) {
        return Math.min(pitch, 89.9F);
    }

    float getMotionYaw() {
        return MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)) - 90.0F);
    }
}

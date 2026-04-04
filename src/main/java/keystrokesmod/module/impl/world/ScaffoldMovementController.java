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
    }

    void handleFastScaffoldJump() {
        boolean longJumpEnabled = ModuleManager.longJump != null && ModuleManager.longJump.isEnabled();
        if (Utils.keysDown() && scaffold.usingFastScaffold() && scaffold.getFastScaffoldMode() >= 1
                && !ModuleManager.tower.canTower() && !longJumpEnabled) {
            if (mc.thePlayer.onGround && Utils.isMoving() && state.scaffoldTicks > 1) {
                scaffold.rotateForward();
                mc.thePlayer.jump();
                Utils.setSpeed(scaffold.getSpeed(scaffold.getSpeedLevel()) - Utils.randomizeDouble(0.0003, 0.0001));
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
                        Utils.setSpeed(scaffold.getSpeed(scaffold.getSpeedLevel()) - Utils.randomizeDouble(0.0003, 0.0001));
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
                    event.setPosY(event.getPosY() + ScaffoldUtils.offsetValue);
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
        if (state.edge != 1F) {
            state.firstStroke = System.currentTimeMillis();
            state.edge = 1F;
        }
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
        if ((int) mc.thePlayer.posY > (int) state.startYPos) {
            switch (scaffold.getFastScaffoldMode()) {
                case 1:
                    if ((!state.firstKeepYPlace && state.keepYTicks == 8) || state.keepYTicks == 11) {
                        scaffold.placeBlock(1, 0);
                        state.firstKeepYPlace = true;
                    }
                    break;
                case 2:
                    if ((!state.firstKeepYPlace && state.keepYTicks == 8) || (state.firstKeepYPlace && state.keepYTicks == 7)) {
                        scaffold.placeBlock(1, 0);
                        state.firstKeepYPlace = true;
                    }
                    break;
                case 3:
                    if (!state.firstKeepYPlace && state.keepYTicks == 7) {
                        scaffold.placeBlock(1, 0);
                        state.firstKeepYPlace = true;
                    }
                    break;
                case 6:
                    if (!state.firstKeepYPlace && state.keepYTicks == 3) {
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
            MoveUtil.strafe(MoveUtil.speed() * input);
        }
    }

    float clampPitch(float pitch) {
        return Math.min(pitch, 89.9F);
    }

    float getMotionYaw() {
        return MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)) - 90.0F);
    }
}

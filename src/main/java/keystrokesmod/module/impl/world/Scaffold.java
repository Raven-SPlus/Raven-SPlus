package keystrokesmod.module.impl.world;

import keystrokesmod.event.*;
import keystrokesmod.helper.ScaffoldBlockCountHelper;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.other.RotationHandler;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.mixins.impl.network.C08PacketPlayerBlockPlacementAccessor;
import keystrokesmod.utility.*;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.scaffold.ScaffoldUtils;
import keystrokesmod.utility.MoveUtil;
import net.minecraft.block.BlockTNT;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Full copy of Scaffold from reference implementation, adapted for current codebase.
 * Separated utilities in ScaffoldUtils package to avoid conflicts.
 */
public class Scaffold extends Module {
    // Core settings
    private final SliderSetting motion;
    public ModeSetting rotation;
    public ModeSetting fakeRotation;
    private ModeSetting sprint;
    private ModeSetting fastScaffold;
    private ModeSetting multiPlace;
    public ButtonSetting autoSwap;
    private ButtonSetting cancelKnockBack;
    private ButtonSetting fastOnRMB;
    public ButtonSetting highlightBlocks;
    private ButtonSetting jumpFacingForward;
    public ButtonSetting safeWalk;
    public ButtonSetting showBlockCount;
    private ButtonSetting silentSwing;
    
    // Intave 12 bypass settings
    private ButtonSetting intaveBypass;
    private ModeSetting intaveTimingMode;
    private ButtonSetting intaveRandomizeFacing;
    private ButtonSetting intaveTargetValidation;
    private ButtonSetting intaveSprintControl;
    private ButtonSetting intaveClickVariation;
    private ButtonSetting intaveTimingVariance;
    private ButtonSetting intaveMovementFix;

    // Mode options
    private static final String[] ROTATION_MODES = {"Disabled", "Simple", "Offset", "Precise"};
    private static final String[] FAKE_ROTATION_MODES = {"Disabled", "Strict", "Smooth", "Spin"};
    private static final String[] SPRINT_MODES = {"Disabled", "Vanilla", "Float"};
    private static final String[] FAST_SCAFFOLD_MODES = {"Disabled", "Jump B", "Jump C", "Jump D", "Keep-Y A", "Keep-Y B", "Jump A"};
    private static final String[] MULTI_PLACE_MODES = {"Disabled", "1 extra", "2 extra"};
    private static final String[] INTAVE_TIMING_MODES = {"None", "Safe", "Aggressive"};

    // Visual/UI state
    public Map<BlockPos, Timer> highlight = new HashMap<>();
    private ScaffoldBlockCountHelper scaffoldBlockCount;
    public AtomicInteger lastSlot = new AtomicInteger(-1);

    // Placement state
    public boolean hasSwapped;
    private boolean hasPlaced;
    private int blockSlot = -1;
    private Vec3 targetBlock;
    private PlaceData blockInfo;
    private PlaceData lastPlacement;
    private PlaceData pendingPlacement;
    private int pendingPlacementTick = -1;
    private EnumFacing lastPlacedFacing;
    private Vec3 hitVec, lookVec;
    private float[] blockRotations;
    public float scaffoldYaw, scaffoldPitch, blockYaw, yawOffset;
    private float lastPlacementYaw = 0;

    // Movement state
    private boolean rotateForward;
    private double startYPos = -1;
    public boolean fastScaffoldKeepY;
    private boolean firstKeepYPlace;
    private boolean rotatingForward;
    private int keepYTicks;
    public boolean lowhop;
    private int rotationDelay;

    // Float mode state
    private boolean floatJumped;
    private boolean floatStarted;
    private boolean floatWasEnabled;
    private boolean floatKeepY;

    // Rotation state
    private float fakeYaw1, fakeYaw2;
    private float minOffset;
    private float minPitch = 80F;
    private float edge;
    private long firstStroke;
    private float lastEdge2, yawAngle, theYaw;
    private float fakeYaw, fakePitch;
    private boolean set2;
    private boolean was451, was452;

    // Module state
    public boolean moduleEnabled;
    public boolean isEnabled;
    private boolean disabledModule;
    private boolean dontDisable, towerEdge;
    private int disableTicks;
    private int scaffoldTicks;
    private int currentFace;
    private boolean enabledOffGround = false;
    public boolean canBlockFade;
    
    // Intave 12 bypass state
    private long lastPlacementTime = 0;
    private long[] placementTimings = new long[8]; // Store last 8 placement intervals for variance
    private int timingIndex = 0;
    private int placementCount = 0;
    private int extraClickCounter = 0; // Track when to send extra clicks
    private java.util.Random intaveRandom = new java.util.Random();
    
    // Compatibility fields for old API
    public ButtonSetting tower;
    public SliderSetting strafe;
    public MovingObjectPosition placeBlock;
    public MovingObjectPosition rayCasted;
    public boolean place;
    public int offGroundTicks = 0;
    public int onGroundTicks = 0;
    public float placeYaw = 0;
    public float placePitch = 85;

    public Scaffold() {
        super("Scaffold", category.world);
        // Core settings
        this.registerSetting(motion = new SliderSetting("Motion", 100, 50, 150, 1, "%"));
        this.registerSetting(rotation = new ModeSetting("Rotation", ROTATION_MODES, 1));
        this.registerSetting(fakeRotation = new ModeSetting("Rotation (fake)", FAKE_ROTATION_MODES, 0));
        this.registerSetting(sprint = new ModeSetting("Sprint mode", SPRINT_MODES, 0));
        this.registerSetting(fastScaffold = new ModeSetting("Fast scaffold", FAST_SCAFFOLD_MODES, 0));
        this.registerSetting(multiPlace = new ModeSetting("Multi-place", MULTI_PLACE_MODES, 0));
        this.registerSetting(autoSwap = new ButtonSetting("Auto swap", true));
        this.registerSetting(cancelKnockBack = new ButtonSetting("Cancel knockback", false));
        this.registerSetting(fastOnRMB = new ButtonSetting("Fast on RMB", true));
        this.registerSetting(highlightBlocks = new ButtonSetting("Highlight blocks", true));
        this.registerSetting(jumpFacingForward = new ButtonSetting("Jump facing forward", false));
        this.registerSetting(safeWalk = new ButtonSetting("Safewalk", true));
        this.registerSetting(showBlockCount = new ButtonSetting("Show block count", true));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing", false));
        
        // Intave 12 bypass settings
        this.registerSetting(intaveBypass = new ButtonSetting("Intave 12 Bypass", true));
        this.registerSetting(intaveTimingMode = new ModeSetting("Intave Timing", INTAVE_TIMING_MODES, 1));
        this.registerSetting(intaveRandomizeFacing = new ButtonSetting("Randomize Facing", true));
        this.registerSetting(intaveTargetValidation = new ButtonSetting("Target Validation", true));
        this.registerSetting(intaveSprintControl = new ButtonSetting("Sprint Control", true));
        this.registerSetting(intaveClickVariation = new ButtonSetting("Click Variation", true));
        this.registerSetting(intaveTimingVariance = new ButtonSetting("Timing Variance", true));
        this.registerSetting(intaveMovementFix = new ButtonSetting("Movement Fix", true));
        
        // Compatibility settings
        this.registerSetting(tower = new ButtonSetting("Tower", false));
        this.registerSetting(strafe = new SliderSetting("Strafe", 0, -45, 45, 5));
    }

    public void onDisable() {
        if (ModuleManager.tower != null && ModuleManager.tower.canTower() && !Utils.isMoving()) {
            towerEdge = true;
        }
        disabledModule = true;
        moduleEnabled = false;
        placeBlock = null;
        rayCasted = null;
        place = false;
        pendingPlacement = null;
        pendingPlacementTick = -1;
        if (scaffoldBlockCount != null) {
            scaffoldBlockCount.beginFade();
        }
    }

    public void onEnable() {
        isEnabled = true;
        moduleEnabled = true;
        ScaffoldUtils.fadeEdge = 0;
        edge = -999999929;
        minPitch = 80F;
        if (!mc.thePlayer.onGround) {
            rotationDelay = 2;
            enabledOffGround = true;
        } else {
            rotationDelay = 0;
            enabledOffGround = false;
        }
        lastEdge2 = RotationHandler.getRotationYaw();

        FMLCommonHandler.instance().bus().register(scaffoldBlockCount = new ScaffoldBlockCountHelper(mc));
        lastSlot.set(-1);
        
        // Reset disabledModule state to allow re-enabling
        disabledModule = false;
        dontDisable = false;
        disableTicks = 0;
        
        // Reset Intave 12 bypass state
        lastPlacementTime = 0;
        placementTimings = new long[8];
        timingIndex = 0;
        placementCount = 0;
        extraClickCounter = 0;
        pendingPlacement = null;
        pendingPlacementTick = -1;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(MouseEvent e) {
        if (!isEnabled) {
            return;
        }
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        e.setCanceled(true);
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!isEnabled) {
            return;
        }
        
        // Update ScaffoldUtils ticks
        ScaffoldUtils.updateTicks();
        
        // Update server rotations for raycast
        RotationUtils.serverRotations[0] = e.getYaw();
        RotationUtils.serverRotations[1] = e.getPitch();
        
        if (Utils.isMoving()) {
            scaffoldTicks++;
        }
        else {
            scaffoldTicks = 0;
        }
        canBlockFade = true;
        
        // Use client-side rotation for movement direction
        float clientYaw = RotationHandler.getRotationYaw();
        
        // Check if LongJump is enabled (adapted from reference)
        boolean longJumpEnabled = ModuleManager.longJump != null && ModuleManager.longJump.isEnabled();
        
        if (Utils.keysDown() && usingFastScaffold() && fastScaffold.getInput() >= 1 && !ModuleManager.tower.canTower() && !longJumpEnabled) { // jump mode
            if (mc.thePlayer.onGround && Utils.isMoving()) {
                if (scaffoldTicks > 1) {
                    rotateForward();
                    mc.thePlayer.jump();
                    Utils.setSpeed(getSpeed(getSpeedLevel()) - Utils.randomizeDouble(0.0003, 0.0001));
                    if (fastScaffold.getInput() == 5 || fastScaffold.getInput() == 2 && firstKeepYPlace) {
                        lowhop = true;
                    }
                    if (startYPos == -1 || Math.abs(startYPos - e.posY) > 5) {
                        startYPos = e.posY;
                        fastScaffoldKeepY = true;
                    }
                }
            }
        }
        else if (fastScaffoldKeepY) {
            fastScaffoldKeepY = firstKeepYPlace = false;
            startYPos = -1;
            keepYTicks = 0;
        }

        //Float
        // Check if speed module is enabled (adapted from reference bHop)
        boolean speedEnabled = ModuleManager.speed != null && ModuleManager.speed.isEnabled();
        if (sprint.getInput() == 2 && !usingFastScaffold() && !speedEnabled && !ModuleManager.tower.canTower() && !longJumpEnabled) {
            floatWasEnabled = true;
            if (!floatStarted) {
                if (ScaffoldUtils.groundTicks > 8 && mc.thePlayer.onGround) {
                    floatKeepY = true;
                    startYPos = e.posY;
                    mc.thePlayer.jump();
                    if (Utils.isMoving()) {
                        Utils.setSpeed(getSpeed(getSpeedLevel()) - Utils.randomizeDouble(0.0003, 0.0001));
                    }
                    floatJumped = true;
                } else if (ScaffoldUtils.groundTicks <= 8 && mc.thePlayer.onGround) {
                    floatStarted = true;
                }
                if (floatJumped && !mc.thePlayer.onGround) {
                    floatStarted = true;
                }
            }

            if (floatStarted && mc.thePlayer.onGround) {
                floatKeepY = false;
                startYPos = -1;
                if (moduleEnabled) {
                    e.setPosY(e.getPosY() + ScaffoldUtils.offsetValue);
                    if (Utils.isMoving()) Utils.setSpeed(getFloatSpeed(getSpeedLevel()));
                }
            }
        } else if (floatWasEnabled && moduleEnabled) {
            if (floatKeepY) {
                startYPos = -1;
            }
            floatStarted = floatJumped = floatKeepY = floatWasEnabled = false;
        }

        if (targetBlock != null) {
            Vec3 lookAt = new Vec3(targetBlock.xCoord - lookVec.xCoord, targetBlock.yCoord - lookVec.yCoord, targetBlock.zCoord - lookVec.zCoord);
            blockRotations = RotationUtils.getRotations(lookAt);
            targetBlock = null;
            fakeYaw1 = clientYaw - hardcodedYaw();
        }
        if (blockRotations == null) {
            fakeYaw1 = clientYaw - hardcodedYaw();
        }

        switch ((int) rotation.getInput()) {
            case 1:
                scaffoldYaw = clientYaw - hardcodedYaw();
                scaffoldPitch = 79F;
                if (currentFace == 1) {
                    scaffoldPitch = 87F;
                }
                e.setYaw(scaffoldYaw);
                e.setPitch(scaffoldPitch);
                break;
            case 2:
                float moveAngle = (float) getMovementAngle();
                float relativeYaw = clientYaw + moveAngle;
                float normalizedYaw = (relativeYaw % 360 + 360) % 360;
                float quad = normalizedYaw % 90;

                float side = MathHelper.wrapAngleTo180_float(getMotionYaw() - scaffoldYaw);
                float yawBackwards = MathHelper.wrapAngleTo180_float(clientYaw) - hardcodedYaw();
                float blockYawOffset = MathHelper.wrapAngleTo180_float(yawBackwards - blockYaw);

                long strokeDelay = 250;

                if (quad <= 5 || quad >= 85) {
                    yawAngle = 127.40F;
                    minOffset = 13;
                    minPitch = 75.48F;
                }
                if (quad > 5 && quad <= 15 || quad >= 75 && quad < 85) {
                    yawAngle = 128.55F;
                    minOffset = 11;
                    minPitch = 75.74F;
                }
                if (quad > 15 && quad <= 25 || quad >= 65 && quad < 75) {
                    yawAngle = 129.70F;
                    minOffset = 8;
                    minPitch = 75.95F;
                }
                if (quad > 25 && quad <= 32 || quad >= 58 && quad < 65) {
                    yawAngle = 130.85F;
                    minOffset = 6;
                    minPitch = 76.13F;
                }
                if (quad > 32 && quad <= 38 || quad >= 52 && quad < 58) {
                    yawAngle = 131.80F;
                    minOffset = 5;
                    minPitch = 76.41F;
                }
                if (quad > 38 && quad <= 42 || quad >= 48 && quad < 52) {
                    yawAngle = 134.30F;
                    minOffset = 4;
                    minPitch = 77.54F;
                }
                if (quad > 42 && quad <= 45 || quad >= 45 && quad < 48) {
                    yawAngle = 137.85F;
                    minOffset = 3;
                    minPitch = 77.93F;
                }

                float offset = yawAngle;


                if (firstStroke > 0 && (System.currentTimeMillis() - firstStroke) > strokeDelay) {
                    firstStroke = 0;
                }
                if (enabledOffGround) {
                    if (blockRotations != null) {
                        scaffoldYaw = blockRotations[0];
                        scaffoldPitch = blockRotations[1];
                    }
                    else {
                        scaffoldYaw = clientYaw - hardcodedYaw();
                        scaffoldPitch = 78f;
                    }
                    e.setYaw(scaffoldYaw);
                    e.setPitch(scaffoldPitch);
                    break;
                }

                if (blockRotations != null) {
                    blockYaw = blockRotations[0];
                    scaffoldPitch = blockRotations[1];
                    yawOffset = blockYawOffset;
                    if (scaffoldPitch < minPitch) {
                        scaffoldPitch = minPitch;
                    }
                } else {
                    scaffoldPitch = minPitch;
                    if (edge == 1) {
                        firstStroke = System.currentTimeMillis();
                    }
                    yawOffset = 0;
                }
                if (!Utils.isMoving() || Utils.getHorizontalSpeed() == 0.0D) {
                    e.setYaw(theYaw);
                    e.setPitch(scaffoldPitch);
                    break;
                }

                float motionYaw = getMotionYaw();

                float newYaw = motionYaw - offset * Math.signum(
                        MathHelper.wrapAngleTo180_float(motionYaw - scaffoldYaw)
                );
                scaffoldYaw = MathHelper.wrapAngleTo180_float(newYaw);

                if (quad > 5 && quad < 85) {
                    if (quad < 45F) {
                        if (firstStroke == 0) {
                            if (side >= 0) {
                                set2 = false;
                            } else {
                                set2 = true;
                            }
                        }
                        if (was452) {
                            firstStroke = System.currentTimeMillis();
                        }
                        was451 = true;
                        was452 = false;
                    } else {
                        if (firstStroke == 0) {
                            if (side >= 0) {
                                set2 = true;
                            } else {
                                set2 = false;
                            }
                        }
                        if (was451) {
                            firstStroke = System.currentTimeMillis();
                        }
                        was452 = true;
                        was451 = false;
                    }
                }

                double minSwitch = (!ScaffoldUtils.scaffoldDiagonal(false)) ? 9 : 15;
                if (side >= 0) {
                    if (yawOffset <= -minSwitch && firstStroke == 0) {
                        if (quad <= 5 || quad >= 85) {
                            if (set2) {
                                firstStroke = System.currentTimeMillis();
                            }
                            set2 = false;
                        }
                    } else if (yawOffset >= 0 && firstStroke == 0) {
                        if (quad <= 5 || quad >= 85) {
                            if (yawOffset >= minSwitch) {
                                if (!set2) {
                                    firstStroke = System.currentTimeMillis();
                                }
                                set2 = true;
                            }
                        }
                    }
                    if (set2) {
                        if (yawOffset <= -0) yawOffset = -0;
                        if (yawOffset >= minOffset) yawOffset = minOffset;
                        theYaw = (scaffoldYaw + offset * 2) - yawOffset;
                        e.setYaw(theYaw);
                        e.setPitch(scaffoldPitch);
                        break;
                    }
                } else if (side <= -0) {
                    if (yawOffset >= minSwitch && firstStroke == 0) {
                        if (quad <= 5 || quad >= 85) {
                            if (set2) {
                                firstStroke = System.currentTimeMillis();
                            }
                            set2 = false;
                        }
                    } else if (yawOffset <= 0 && firstStroke == 0) {
                        if (quad <= 5 || quad >= 85) {
                            if (yawOffset <= -minSwitch) {
                                if (!set2) {
                                    firstStroke = System.currentTimeMillis();
                                }
                                set2 = true;
                            }
                        }
                    }
                    if (set2) {
                        if (yawOffset >= 0) yawOffset = 0;
                        if (yawOffset <= -minOffset) yawOffset = -minOffset;
                        theYaw = (scaffoldYaw - offset * 2) - yawOffset;
                        e.setYaw(theYaw);
                        e.setPitch(scaffoldPitch);
                        break;
                    }
                }

                if (side >= 0) {
                    if (yawOffset >= 0) yawOffset = 0;
                    if (yawOffset <= -minOffset) yawOffset = -minOffset;
                } else if (side <= -0) {
                    if (yawOffset <= -0) yawOffset = -0;
                    if (yawOffset >= minOffset) yawOffset = minOffset;
                }
                theYaw = scaffoldYaw - yawOffset;
                e.setYaw(theYaw);
                e.setPitch(scaffoldPitch);
                break;
            case 3:
                if (blockRotations != null) {
                    scaffoldYaw = blockRotations[0];
                    scaffoldPitch = blockRotations[1];
                }
                else {
                    scaffoldYaw = clientYaw - hardcodedYaw();
                    scaffoldPitch = 80F;
                }
                e.setYaw(scaffoldYaw);
                e.setPitch(scaffoldPitch);
                theYaw = e.getYaw();
                break;
        }
        if (edge != 1) {
            firstStroke = System.currentTimeMillis();
            edge = 1;
        }
        if (mc.thePlayer.onGround) {
            enabledOffGround = false;
        }

        //jump facing forward
        if (ScaffoldUtils.inAirTicks >= 1) {
            rotateForward = false;
        }
        if (rotateForward && jumpFacingForward.isToggled()) {
            if (rotation.getInput() > 0) {
                if (!rotatingForward) {
                    rotationDelay = 2;
                    rotatingForward = true;
                }
                float forwardYaw = (clientYaw - hardcodedYaw() - 180);
                e.setYaw(forwardYaw);
                e.setPitch(10);
            }
        }
        else {
            rotatingForward = false;
        }

        if (intaveBypass.isToggled() && intaveTargetValidation.isToggled() && pendingPlacement != null && blockRotations != null) {
            scaffoldYaw = blockRotations[0];
            scaffoldPitch = blockRotations[1];
            e.setYaw(scaffoldYaw);
            e.setPitch(scaffoldPitch);
            theYaw = scaffoldYaw;
        }

        // Tower integration removed - Tower doesn't have isVerticalTowering, yaw, pitch fields

        // Movement fix is now handled via RotationEvent -> MoveFix.Silent
        // This properly adjusts movement inputs through the rotation handler system

        //pitch fix
        if (e.getPitch() > 89.9F) {
            e.setPitch(89.9F);
        }

        if (rotationDelay > 0) --rotationDelay;

        //Fake rotations
        if (fakeRotation.getInput() > 0) {
            if (fakeRotation.getInput() == 1) {
                fakeYaw = fakeYaw1;
                if (blockRotations != null) {
                    fakePitch = blockRotations[1] + 5;
                } else {
                    fakePitch = scaffoldPitch;
                }
            }
            else if (fakeRotation.getInput() == 2) {
                fakeYaw2 = clientYaw - hardcodedYaw();
                float yawDifference = getAngleDifference(lastEdge2, fakeYaw2);
                float smoothingFactor = (1.0f - (65.0f / 100.0f));
                fakeYaw2 = (lastEdge2 + yawDifference * smoothingFactor);
                lastEdge2 = fakeYaw2;

                fakeYaw = fakeYaw2;
                if (blockRotations != null) {
                    fakePitch = blockRotations[1] + 5;
                } else {
                    fakePitch = scaffoldPitch;
                }
            }
            else if (fakeRotation.getInput() == 3) {
                fakeYaw += 25.71428571428571F;
                fakePitch = 90F;
            }
            RotationUtils.setFakeRotations(fakeYaw, fakePitch);
        }

        // Update server rotations after all adjustments
        RotationUtils.serverRotations[0] = e.getYaw();
        RotationUtils.serverRotations[1] = e.getPitch();
    }
    
    @SubscribeEvent
    public void onRotation(RotationEvent e) {
        if (!Utils.nullCheck() || !isEnabled) {
            return;
        }
        
        // Intave 12 bypass: Apply MoveFix to ensure movement matches server rotation
        // MoveFix.Silent makes the engine automatically adjust movement inputs
        // based on the difference between client view and server rotation
        // This is the proper way to handle movement/rotation sync (from reference impl)
        if (intaveBypass.isToggled() && intaveMovementFix.isToggled() && rotation.getInput() > 0) {
            e.setMoveFix(RotationHandler.MoveFix.Silent);
        }
    }

    @SubscribeEvent
    public void onPostMotion(PostMotionEvent e) {
        if (!Utils.nullCheck() || !isEnabled) {
            return;
        }
        if (pendingPlacement == null) {
            return;
        }
        if (pendingPlacementTick != mc.thePlayer.ticksExisted) {
            clearPendingPlacement();
            return;
        }
        PlaceData toPlace = pendingPlacement;
        clearPendingPlacement();
        place(toPlace);
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!isEnabled) {
            return;
        }
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement packet = (C08PacketPlayerBlockPlacement) e.getPacket();
            currentFace = packet.getPlacedBlockDirection();
            
            // Intave 12 bypass: Randomize block face look vectors to avoid suspicious values
            // Only apply if bypass is enabled and randomize facing is toggled
            if (!intaveBypass.isToggled() || !intaveRandomizeFacing.isToggled()) {
                return;
            }
            
            // Intave Check 4 (line 145-153): isSuspicious(f) where f == 0.0 || f == 0.5 || f >= 1.0
            // Intave Check 4 (line 154): f < 0.0 || f > 1.0 → FLAG
            // Special case (line 90): If all three are 0.0, Intave allows it
            C08PacketPlayerBlockPlacementAccessor accessor = (C08PacketPlayerBlockPlacementAccessor) packet;
            
            float facingX = packet.getPlacedBlockOffsetX();
            float facingY = packet.getPlacedBlockOffsetY();
            float facingZ = packet.getPlacedBlockOffsetZ();
            
            // Special case: If all three are 0.0, Intave allows it - but randomize individual suspicious values
            boolean allZero = Math.abs(facingX) < 0.001f && Math.abs(facingY) < 0.001f && Math.abs(facingZ) < 0.001f;
            
            if (!allZero) {
                // Randomize values to avoid 0.0, 0.5, and >=1.0
                facingX = randomizeFacingValue(facingX);
                facingY = randomizeFacingValue(facingY);
                facingZ = randomizeFacingValue(facingZ);
            } else {
                // All zero is allowed, but add tiny random offset to avoid pattern detection
                facingX = (float) (Math.random() * 0.01);
                facingY = (float) (Math.random() * 0.01);
                facingZ = (float) (Math.random() * 0.01);
            }
            
            accessor.setFacingX(facingX);
            accessor.setFacingY(facingY);
            accessor.setFacingZ(facingZ);
        }
    }
    
    /**
     * Randomizes block face look vector values to bypass Intave 12 detection.
     * Avoids suspicious values: 0.0, 0.5, and >=1.0
     * Intave Check 4 (line 145-153): isSuspicious(f) where f == 0.0 || f == 0.5 || f >= 1.0
     * If vl > 2 (all 3 values suspicious), scaffoldwalkVL increases (line 189-193)
     * 
     * Strategy: Ensure value is in safe range [0.05, 0.45] or [0.55, 0.95]
     */
    private float randomizeFacingValue(float value) {
        // Check if value is suspicious (0.0, 0.5, or >=1.0)
        boolean isSuspicious = Math.abs(value) < 0.001f || 
                               Math.abs(value - 0.5f) < 0.001f || 
                               value >= 1.0f || 
                               value < 0.0f;
        
        if (isSuspicious) {
            // Generate a completely safe random value
            // Safe ranges: [0.05, 0.45] or [0.55, 0.95]
            if (Math.random() < 0.5) {
                // Lower safe range: 0.05 to 0.45
                return (float) (0.05 + Math.random() * 0.40);
            } else {
                // Upper safe range: 0.55 to 0.95
                return (float) (0.55 + Math.random() * 0.40);
            }
        }
        
        // Value is not exactly suspicious, but check if it's close to suspicious values
        // Add small randomization to avoid patterns while staying in safe range
        
        // If too close to 0.0
        if (value < 0.05f) {
            return (float) (0.05 + Math.random() * 0.15); // 0.05 to 0.20
        }
        
        // If too close to 0.5 (within 0.05)
        if (value > 0.45f && value < 0.55f) {
            if (value < 0.5f) {
                return (float) (0.35 + Math.random() * 0.10); // 0.35 to 0.45
            } else {
                return (float) (0.55 + Math.random() * 0.10); // 0.55 to 0.65
            }
        }
        
        // If too close to 1.0
        if (value > 0.95f) {
            return (float) (0.80 + Math.random() * 0.15); // 0.80 to 0.95
        }
        
        // Value is already in safe range, add tiny variation to avoid exact patterns
        float variation = (float) ((Math.random() - 0.5) * 0.02); // ±0.01
        float result = value + variation;
        
        // Final safety clamp
        if (result < 0.05f) result = 0.05f + (float)(Math.random() * 0.05);
        if (result > 0.95f) result = 0.90f + (float)(Math.random() * 0.05);
        if (result > 0.45f && result < 0.55f) {
            result = result < 0.5f ? 0.42f : 0.58f;
        }
        
        return result;
    }

    @SubscribeEvent
    public void onSlotUpdate(SlotUpdateEvent e) {
        if (isEnabled) {
            lastSlot.set(e.slot);
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (!isEnabled) {
            return;
        }
        if (pendingPlacement != null && pendingPlacementTick != mc.thePlayer.ticksExisted) {
            clearPendingPlacement();
        }
        // LongJump.function removed - check isEnabled instead
        boolean longJumpEnabled = ModuleManager.longJump != null && ModuleManager.longJump.isEnabled();
        if (longJumpEnabled) {
            startYPos = -1;
        }
        if (holdingBlocks() && setSlot()) {

            // LongJump.stopModules removed - not needed

            if (KillAura.target != null) {
                return;
            }

            hasSwapped = true;
            int mode = (int) fastScaffold.getInput();
            if (rotation.getInput() == 0 || rotationDelay == 0) {
                placeBlock(0, 0);
            }
            // ModuleManager.tower.placeExtraBlock removed - Tower doesn't have this field
            if (fastScaffoldKeepY && !ModuleManager.tower.canTower()) {
                ++keepYTicks;
                if ((int) mc.thePlayer.posY > (int) startYPos) {
                    switch (mode) {
                        case 1:
                            if (!firstKeepYPlace && keepYTicks == 8 || keepYTicks == 11) {
                                placeBlock(1, 0);
                                firstKeepYPlace = true;
                            }
                            break;
                        case 2:
                            if (!firstKeepYPlace && keepYTicks == 8 || firstKeepYPlace && keepYTicks == 7) {
                                placeBlock(1, 0);
                                firstKeepYPlace = true;
                            }
                            break;
                        case 3:
                            if (!firstKeepYPlace && keepYTicks == 7) {
                                placeBlock(1, 0);
                                firstKeepYPlace = true;
                            }
                            break;
                        case 6:
                            if (!firstKeepYPlace && keepYTicks == 3) {
                                placeBlock(1, 0);
                                firstKeepYPlace = true;
                            }
                            break;
                    }
                }
                if (mc.thePlayer.onGround) keepYTicks = 0;
                if ((int) mc.thePlayer.posY == (int) startYPos) firstKeepYPlace = false;
            }
            handleMotion();
        }

        if (disabledModule) {
            if (hasPlaced && (towerEdge || floatStarted && Utils.isMoving())) {
                dontDisable = true;
            }

            if (dontDisable && ++disableTicks >= 2) {
                isEnabled = false;
            }
            this.lastPlacementYaw = 0;
            if (!dontDisable) {
                isEnabled = false;
            }
            if (!isEnabled) {
                disabledModule = dontDisable = false;
                disableTicks = 0;
                // ModuleManager.tower.speed removed - Tower doesn't have this field

                if (lastSlot.get() != -1) {
                    mc.thePlayer.inventory.currentItem = lastSlot.get();
                    lastSlot.set(-1);
                }
                blockSlot = -1;
                // IMixinItemRenderer removed - not available in current codebase
                // if (autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled()) {
                //     ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(false);
                //     ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(false);
                // }
                scaffoldBlockCount.beginFade();
                hasSwapped = hasPlaced = false;
                targetBlock = null;
                blockInfo = null;
                blockRotations = null;
                fastScaffoldKeepY = firstKeepYPlace = rotateForward = rotatingForward = floatStarted = floatJumped = floatWasEnabled = towerEdge =
                        was451 = was452 = enabledOffGround = false;
                rotationDelay = keepYTicks = scaffoldTicks = 0;
                firstStroke = 0;
                startYPos = -1;
                lookVec = null;
                lastPlacement = null;
                clearPendingPlacement();
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!isEnabled) {
            return;
        }
        if (!Utils.nullCheck() || !cancelKnockBack.isToggled()) {
            return;
        }
        if (e.getPacket() instanceof S12PacketEntityVelocity) {
            if (((S12PacketEntityVelocity) e.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
                e.setCanceled(true);
            }
        }
        else if (e.getPacket() instanceof S27PacketExplosion) {
            e.setCanceled(true);
        }
    }

    @Override
    public String getInfo() {
        String info;
        if (fastOnRMB.isToggled()) {
            info = Mouse.isButtonDown(1) && Utils.tabbedIn() ? FAST_SCAFFOLD_MODES[(int) fastScaffold.getInput()] : SPRINT_MODES[(int) sprint.getInput()];
        }
        else {
            info = fastScaffold.getInput() > 0 ? FAST_SCAFFOLD_MODES[(int) fastScaffold.getInput()] : SPRINT_MODES[(int) sprint.getInput()];
        }
        if (info.equals("§cDisabled")) {
            return "Disabled";
        }
        return info;
    }

    public boolean stopFastPlace() {
        return this.isEnabled();
    }

    float getAngleDifference(float from, float to) {
        float difference = (to - from) % 360.0F;
        if (difference < -180.0F) {
            difference += 360.0F;
        } else if (difference >= 180.0F) {
            difference -= 360.0F;
        }
        return difference;
    }

    public void rotateForward() {
        rotateForward = true;
        rotatingForward = false;
    }

    public boolean sprint() {
        if (isEnabled) {
            // Intave 12 bypass: Sprint control
            // CelerityCheck (line 149-151): When placing block under within 380ms with negative Y velocity
            // maxspeed is limited to 0.28, sprinting would exceed this
            if (intaveBypass.isToggled() && intaveSprintControl.isToggled()) {
                boolean recentPlacement = (System.currentTimeMillis() - lastPlacementTime) < 380;
                boolean isFalling = mc.thePlayer.motionY < 0.0;
                
                // Intave Check 6 (line 240-247): Sprinting + falling with target not adjacent → VL increase
                if (recentPlacement && isFalling && mc.thePlayer.isSprinting()) {
                    // Control sprint to avoid detection
                    // Only allow sprint if we're using fast scaffold modes
                    if (handleFastScaffolds() == 0) {
                        return false; // Don't sprint when falling and recently placed
                    }
                }
            }
            
            return handleFastScaffolds() > 0 || !holdingBlocks();
        }
        return false;
    }
    
    /**
     * Returns whether sprinting should be disabled for Intave bypass.
     * Used by external modules to check scaffold sprint state.
     */
    public boolean shouldDisableSprintForIntave() {
        if (!isEnabled || !intaveBypass.isToggled() || !intaveSprintControl.isToggled()) {
            return false;
        }
        
        boolean recentPlacement = (System.currentTimeMillis() - lastPlacementTime) < 380;
        boolean isFalling = mc.thePlayer.motionY < 0.0;
        
        // CelerityCheck integration: Limit speed during scaffold
        return recentPlacement && isFalling && handleFastScaffolds() == 0;
    }

    private int handleFastScaffolds() {
        if (fastOnRMB.isToggled()) {
            return Mouse.isButtonDown(1) && Utils.tabbedIn() ? (int) fastScaffold.getInput() : (int) sprint.getInput();
        }
        else {
            return fastScaffold.getInput() > 0 ? (int) fastScaffold.getInput() : (int) sprint.getInput();
        }
    }

    private boolean usingFastScaffold() {
        boolean speedEnabled = ModuleManager.speed != null && ModuleManager.speed.isEnabled();
        return fastScaffold.getInput() > 0 && (!fastOnRMB.isToggled() || (Mouse.isButtonDown(1) || speedEnabled) && Utils.tabbedIn());
    }

    public boolean canSafewalk() {
        if (!safeWalk.isToggled()) {
            return false;
        }
        if (usingFastScaffold()) {
            return false;
        }
        if (ModuleManager.tower.canTower()) {
            return false;
        }
        if (!isEnabled) {
            return false;
        }
        return true;
    }

    public boolean stopRotation() {
        return this.isEnabled() && rotation.getInput() > 0;
    }

    private void place(PlaceData block) {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !ContainerUtils.canBePlaced((ItemBlock) heldItem.getItem())) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        boolean isJumping = mc.thePlayer.motionY > 0.0;
        boolean isFalling = mc.thePlayer.motionY < 0.0;
        
        // Intave 12 bypass: Timing with variance to avoid balance detection
        // Key insight: Intave uses BalanceUtils to detect low variance (machine-like) timing
        // We need HIGH variance in placement intervals to appear human
        if (intaveBypass.isToggled() && intaveTimingMode.getInput() > 0) {
            if (lastPlacementTime > 0) {
                long timeSinceLastPlacement = currentTime - lastPlacementTime;
                
                long requiredDelay;
                if (intaveTimingVariance.isToggled()) {
                    // High-variance timing to defeat balance checks
                    // Intave calculates: balance = sum of (timing - avg)^2
                    // Low balance = consistent timing = machine = FLAG
                    // Solution: Alternate between short and long delays unpredictably
                    requiredDelay = calculateVariedDelay(intaveTimingMode.getInput() == 1);
                } else {
                    // Simple random delay (less effective against balance checks)
                    long minDelay = intaveTimingMode.getInput() == 1 ? 20 : 0;
                    long maxDelay = intaveTimingMode.getInput() == 1 ? 50 : 20;
                    requiredDelay = minDelay + (long)(Math.random() * (maxDelay - minDelay));
                }
                
                if (timeSinceLastPlacement < requiredDelay) {
                    return;
                }
            }
        }
        
        // Get raycast for target validation
        MovingObjectPosition raycast = RotationUtils.rayTraceCustom(mc.playerController.getBlockReachDistance(), RotationUtils.serverRotations[0], RotationUtils.serverRotations[1]);
        
        // Intave 12 bypass: Target validation
        // Check 5c (line 210-220): If targetBlock.distance(placedBlock) is 0 < f4 < 1 AND hsDist < 1.0
        // It sets lastTimeSuspiciousForScaffoldWalk, then FLAGS on 2nd occurrence within 2000ms
        // Solution: Use the raycast block as placement target when possible, or ensure hitVec is accurate
        if (intaveBypass.isToggled() && intaveTargetValidation.isToggled()) {
            boolean invalidTarget = false;
            if (raycast != null && raycast.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                // If our raycast hits the block we want to place against, use its hitVec
                // This ensures server-side getTargetBlock() matches our placement
                if (raycast.getBlockPos().equals(block.blockPos)) {
                    block.hitVec = raycast.hitVec;
                } else {
                    // Raycast hits different block - this is the problematic case for Check 5c
                    // Calculate distance: if 0 < dist < 1 and hsDist < 1, Intave gets suspicious
                    double targetDist = Math.sqrt(raycast.getBlockPos().distanceSq(block.blockPos));
                    double hsDist = Math.abs(block.blockPos.getY() + 1.0 - mc.thePlayer.posY);
                    
                    // Check 5c condition: f4 > 0 && f4 < 1 && hsDist < 1.0
                    // If this condition is true, Intave will flag on 2nd block
                    // We can't change what server sees, but we CAN place against the raycast block instead
                    if (targetDist > 0.0 && targetDist < 1.0 && hsDist < 1.0) {
                        // Try to place against the block we're actually looking at if valid
                        BlockPos lookingAt = raycast.getBlockPos();
                        BlockPos placePos = lookingAt.offset(raycast.sideHit);
                        
                        // Check if we can place at this position instead
                        if (BlockUtils.replaceable(placePos) && !BlockUtils.replaceable(lookingAt) &&
                                !BlockUtils.isInteractable(BlockUtils.getBlock(lookingAt))) {
                            // Use the block we're looking at - this prevents Check 5c
                            block.blockPos = lookingAt;
                            block.enumFacing = raycast.sideHit;
                            block.hitVec = raycast.hitVec;
                        } else {
                            invalidTarget = true;
                        }
                    }
                }
            }
            if (invalidTarget) {
                return;
            }
        }
        
        // Check 6 (line 240-247): Sprinting + falling + target NOT adjacent to placed block
        // Only skip if sprint control is enabled AND we're in a risky situation
        if (intaveBypass.isToggled() && intaveSprintControl.isToggled()) {
            if (mc.thePlayer.isSprinting() && isFalling && !isJumping) {
                if (raycast != null && raycast.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    BlockPos targetPos = raycast.getBlockPos();
                    BlockPos placedPos = block.blockPos;
                    
                    // Check if target is adjacent (NORTH/EAST/SOUTH/WEST) to placed block
                    boolean isAdjacent = targetPos.equals(placedPos) ||
                                        targetPos.add(0, 0, -1).equals(placedPos) ||
                                        targetPos.add(1, 0, 0).equals(placedPos) ||
                                        targetPos.add(0, 0, 1).equals(placedPos) ||
                                        targetPos.add(-1, 0, 0).equals(placedPos);
                    
                    // Also check if target is the block we're placing against
                    if (!isAdjacent) {
                        // Intave will increase scaffoldwalkVL - disable sprint temporarily
                        mc.thePlayer.setSprinting(false);
                    }
                }
            }
        }
        
        // Set compatibility fields
        if (raycast != null && raycast.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && raycast.getBlockPos().equals(block.blockPos) && raycast.sideHit.equals(block.enumFacing)) {
            placeBlock = raycast;
        } else {
            Vec3 hitVecForMOP = block.hitVec != null ? block.hitVec : new Vec3(block.blockPos.getX() + 0.5, block.blockPos.getY() + 0.5, block.blockPos.getZ() + 0.5);
            placeBlock = new MovingObjectPosition(hitVecForMOP, block.enumFacing, block.blockPos);
        }
        rayCasted = raycast;
        place = true;
        placeYaw = RotationUtils.serverRotations[0];
        placePitch = RotationUtils.serverRotations[1];
        
        // Execute placement
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, block.blockPos, block.enumFacing, block.hitVec)) {
            // Track timing for variance calculation
            if (lastPlacementTime > 0 && intaveBypass.isToggled() && intaveTimingVariance.isToggled()) {
                long interval = currentTime - lastPlacementTime;
                placementTimings[timingIndex] = interval;
                timingIndex = (timingIndex + 1) % placementTimings.length;
                placementCount++;
            }
            lastPlacementTime = currentTime;
            
            // Intave 12 bypass: Click variation to break 1:1 click-to-place ratio
            // MachineBlockCheck detects if clicks == 1 per placement (perfect efficiency)
            // Intave tracks PlayerInteractEvent (right-clicks), not swings
            // Humans typically spam right-click or occasionally miss, so we simulate extra interactions
            if (intaveBypass.isToggled() && intaveClickVariation.isToggled()) {
                extraClickCounter++;
                
                // Every 2-4 placements, send 1-2 extra right-click packets
                int extraClickThreshold = 2 + intaveRandom.nextInt(3); // 2, 3, or 4
                if (extraClickCounter >= extraClickThreshold) {
                    extraClickCounter = 0;
                    int extraClicks = 1 + intaveRandom.nextInt(2); // 1 or 2 extra
                    
                    for (int i = 0; i < extraClicks; i++) {
                        // Send an extra block interact on the same target (RIGHT_CLICK_BLOCK)
                        // This increments click counters without placing a new block.
                        sendExtraBlockInteract(heldItem, block);
                    }
                }
            }
            
            if (silentSwing.isToggled()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            } else {
                mc.thePlayer.swingItem();
                mc.getItemRenderer().resetEquippedProgress();
            }
            highlight.put(block.blockPos.offset(block.enumFacing), null);
            hasPlaced = true;
        }
    }
    
    // Compatibility method for old API
    public void place(MovingObjectPosition block, boolean extra) {
        if (block == null || block.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !ContainerUtils.canBePlaced((ItemBlock) heldItem.getItem())) {
            return;
        }
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, block.getBlockPos(), block.sideHit, block.hitVec)) {
            mc.thePlayer.swingItem();
            hasPlaced = true;
        }
    }

    public int totalBlocks() {
        int totalBlocks = 0;
        for (int i = 0; i < 9; ++i) {
            final ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBlock && ContainerUtils.canBePlaced((ItemBlock) stack.getItem()) && stack.stackSize > 0) {
                totalBlocks += stack.stackSize;
            }
        }
        return totalBlocks;
    }

    private boolean shouldDelayPlacementForIntave() {
        return intaveBypass.isToggled();
    }

    private void queuePlacement(int yOffset, int xOffset) {
        if (pendingPlacement != null) {
            return;
        }
        locateBlocks(yOffset, xOffset);
        if (blockInfo == null) {
            return;
        }
        lastPlacement = blockInfo;
        pendingPlacement = blockInfo;
        pendingPlacementTick = mc.thePlayer.ticksExisted;
        blockInfo = null;
    }

    private void clearPendingPlacement() {
        pendingPlacement = null;
        pendingPlacementTick = -1;
    }

    private void sendExtraBlockInteract(ItemStack heldItem, PlaceData block) {
        if (block == null || block.blockPos == null || block.enumFacing == null) {
            return;
        }
        Vec3 vec = block.hitVec != null
                ? block.hitVec
                : new Vec3(block.blockPos.getX() + 0.5, block.blockPos.getY() + 0.5, block.blockPos.getZ() + 0.5);
        float hitX = (float) (vec.xCoord - block.blockPos.getX());
        float hitY = (float) (vec.yCoord - block.blockPos.getY());
        float hitZ = (float) (vec.zCoord - block.blockPos.getZ());
        hitX = MathHelper.clamp_float(hitX, 0.0F, 1.0F);
        hitY = MathHelper.clamp_float(hitY, 0.0F, 1.0F);
        hitZ = MathHelper.clamp_float(hitZ, 0.0F, 1.0F);
        if (intaveBypass.isToggled()) {
            hitX = randomizeFacingValue(hitX);
            hitY = randomizeFacingValue(hitY);
            hitZ = randomizeFacingValue(hitZ);
        }
        mc.thePlayer.sendQueue.addToSendQueue(
                new C08PacketPlayerBlockPlacement(block.blockPos, block.enumFacing.getIndex(), heldItem, hitX, hitY, hitZ)
        );
    }

    private void placeBlock(int yOffset, int xOffset) {
        if (shouldDelayPlacementForIntave()) {
            queuePlacement(yOffset, xOffset);
            return;
        }
        locateAndPlaceBlock(yOffset, xOffset);
        int input = (int) multiPlace.getInput();
        if (sprint.getInput() == 0 && mc.thePlayer.onGround && !ModuleManager.tower.canTower() && !usingFastScaffold()) {
            return;
        }
        // ModuleManager.tower.tower removed - Tower doesn't have this field
        if (input >= 1) {
            locateAndPlaceBlock(yOffset, xOffset);
            if (input >= 2) {
                locateAndPlaceBlock(yOffset, xOffset);
            }
        }
    }

    private void locateAndPlaceBlock(int yOffset, int xOffset) {
        locateBlocks(yOffset, xOffset);
        if (blockInfo == null) {
            return;
        }
        lastPlacement = blockInfo;
        place(blockInfo);
        blockInfo = null;
    }

    private void locateBlocks(int yOffset, int xOffset) {
        List<PlaceData> blocksInfo = findBlocks(yOffset, xOffset);

        if (blocksInfo == null) {
            return;
        }

        double sumX = 0, sumY = !mc.thePlayer.onGround ? 0 : blocksInfo.get(0).blockPos.getY(), sumZ = 0;
        int index = 0;
        for (PlaceData blockssInfo : blocksInfo) {
            if (index > 1 || (!ScaffoldUtils.scaffoldDiagonal(false) && index > 0 && mc.thePlayer.onGround)) {
                break;
            }
            sumX += blockssInfo.blockPos.getX();
            if (!mc.thePlayer.onGround) {
                sumY += blockssInfo.blockPos.getY();
            }
            sumZ += blockssInfo.blockPos.getZ();
            index++;
        }

        double avgX = sumX / index;
        double avgY = !mc.thePlayer.onGround ? sumY / index : blocksInfo.get(0).blockPos.getY();
        double avgZ = sumZ / index;

        targetBlock = new Vec3(avgX, avgY, avgZ);

        PlaceData blockInfo2 = blocksInfo.get(0);
        int blockX = blockInfo2.blockPos.getX();
        int blockY = blockInfo2.blockPos.getY();
        int blockZ = blockInfo2.blockPos.getZ();
        EnumFacing blockFacing = lastPlacedFacing = blockInfo2.enumFacing;
        blockInfo = blockInfo2;
        if (intaveBypass.isToggled() && intaveTargetValidation.isToggled()) {
            targetBlock = new Vec3(blockX, blockY, blockZ);
        }

        // Calculate hitVec - the randomization will be applied in onSendPacket via block face offsets
        // Keep hitVec calculation similar to reference for placement accuracy
        double hitX = (blockX + 0.5D) + getCoord(blockFacing.getOpposite(), "x") * 0.5D;
        double hitY = (blockY + 0.5D) + getCoord(blockFacing.getOpposite(), "y") * 0.5D;
        double hitZ = (blockZ + 0.5D) + getCoord(blockFacing.getOpposite(), "z") * 0.5D;
        lookVec = new Vec3(0.5D + getCoord(blockFacing.getOpposite(), "x") * 0.5D, 0.5D + getCoord(blockFacing.getOpposite(), "y") * 0.5D, 0.5D + getCoord(blockFacing.getOpposite(), "z") * 0.5D);
        hitVec = new Vec3(hitX, hitY, hitZ);
        blockInfo.hitVec = hitVec;
    }

    private double getCoord(EnumFacing facing, String axis) {
        switch (axis) {
            case "x": return (facing == EnumFacing.WEST) ? -0.5 : (facing == EnumFacing.EAST) ? 0.5 : 0;
            case "y": return (facing == EnumFacing.DOWN) ? -0.5 : (facing == EnumFacing.UP) ? 0.5 : 0;
            case "z": return (facing == EnumFacing.NORTH) ? -0.5 : (facing == EnumFacing.SOUTH) ? 0.5 : 0;
        }
        return 0;
    }

    private List<PlaceData> findBlocks(int yOffset, int xOffset) {
        int x = (int) Math.floor(mc.thePlayer.posX + xOffset);
        int y = (int) Math.floor(((startYPos != -1) ? startYPos : mc.thePlayer.posY) + yOffset);
        int z = (int) Math.floor(mc.thePlayer.posZ);

        BlockPos base = new BlockPos(x, y - 1, z);

        if (!BlockUtils.replaceable(base)) {
            return null;
        }

        EnumFacing[] allFacings = getFacingsSorted();
        List<EnumFacing> validFacings = new ArrayList<>(5);
        for (EnumFacing facing : allFacings) {
            if (facing != EnumFacing.UP && placeConditions(facing, yOffset, xOffset)) {
                validFacings.add(facing);
            }
        }
        int maxYLayer = 2;
        List<PlaceData> possibleBlocks = new ArrayList<>();

        for (int dy = 1; dy <= maxYLayer; dy++) {
            BlockPos layerBase = new BlockPos(x, y - dy, z);
            if (dy == 1) {
                for (EnumFacing facing : validFacings) {
                    BlockPos neighbor = layerBase.offset(facing);
                    if (!BlockUtils.replaceable(neighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(neighbor))) {
                        possibleBlocks.add(new PlaceData(neighbor, facing.getOpposite()));
                    }
                }
            }
            for (EnumFacing facing : validFacings) {
                BlockPos adjacent = layerBase.offset(facing);
                if (BlockUtils.replaceable(adjacent)) {
                    for (EnumFacing nestedFacing : validFacings) {
                        BlockPos nestedNeighbor = adjacent.offset(nestedFacing);
                        if (!BlockUtils.replaceable(nestedNeighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(nestedNeighbor))) {
                            possibleBlocks.add(new PlaceData(nestedNeighbor, nestedFacing.getOpposite()));
                        }
                    }
                }
            }
            for (EnumFacing facing : validFacings) {
                BlockPos adjacent = layerBase.offset(facing);
                if (BlockUtils.replaceable(adjacent)) {
                    for (EnumFacing nestedFacing : validFacings) {
                        BlockPos nestedNeighbor = adjacent.offset(nestedFacing);
                        if (BlockUtils.replaceable(nestedNeighbor)) {
                            for (EnumFacing thirdFacing : validFacings) {
                                BlockPos thirdNeighbor = nestedNeighbor.offset(thirdFacing);
                                if (!BlockUtils.replaceable(thirdNeighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(thirdNeighbor))) {
                                    possibleBlocks.add(new PlaceData(thirdNeighbor, thirdFacing.getOpposite()));
                                }
                            }
                        }
                    }
                }
            }
        }

        return possibleBlocks.isEmpty() ? null : possibleBlocks;
    }

    private EnumFacing[] getFacingsSorted() {
        // Use client-side rotation instead of IAccessorEntityPlayerSP.getLastReportedYaw()
        float clientYaw = RotationHandler.getRotationYaw();
        EnumFacing lastFacing = EnumFacing.getHorizontal(MathHelper.floor_double((clientYaw * 4.0F / 360.0F) + 0.5D) & 3);

        EnumFacing perpClockwise = lastFacing.rotateY();
        EnumFacing perpCounterClockwise = lastFacing.rotateYCCW();

        EnumFacing opposite = lastFacing.getOpposite();

        float yaw = clientYaw % 360;
        if (yaw > 180) {
            yaw -= 360;
        }
        else if (yaw < -180) {
            yaw += 360;
        }

        // Calculates the difference from the last placed angle and gets the closest one
        float diffClockwise = Math.abs(MathHelper.wrapAngleTo180_float(yaw - getFacingAngle(perpClockwise)));
        float diffCounterClockwise = Math.abs(MathHelper.wrapAngleTo180_float(yaw - getFacingAngle(perpCounterClockwise)));

        EnumFacing firstPerp, secondPerp;
        if (diffClockwise <= diffCounterClockwise) {
            firstPerp = perpClockwise;
            secondPerp = perpCounterClockwise;
        }
        else {
            firstPerp = perpCounterClockwise;
            secondPerp = perpClockwise;
        }

        return new EnumFacing[]{EnumFacing.UP, EnumFacing.DOWN, lastFacing, firstPerp, secondPerp, opposite};
    }

    private float getFacingAngle(EnumFacing facing) {
        switch (facing) {
            case WEST:
                return 90;
            case NORTH:
                return 180;
            case EAST:
                return -90;
            default:
                return 0;
        }
    }

    private boolean placeConditions(EnumFacing enumFacing, int yCondition, int xCondition) {
        if (xCondition == -1) {
            return enumFacing == EnumFacing.EAST;
        }
        if (yCondition == 1) {
            return enumFacing == EnumFacing.DOWN;
        }

        return true;
    }

    float getMotionYaw() {
        return MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)) - 90.0F);
    }

    private int getSpeedLevel() {
        for (PotionEffect potionEffect : mc.thePlayer.getActivePotionEffects()) {
            if (potionEffect.getEffectName().equals("potion.moveSpeed")) {
                return potionEffect.getAmplifier() + 1;
            }
            return 0;
        }
        return 0;
    }

    double[] speedLevels = {0.48, 0.5, 0.52, 0.58, 0.68};

    double getSpeed(int speedLevel) {
        if (speedLevel >= 0) {
            return speedLevels[speedLevel];
        }
        return speedLevels[0];
    }

    double[] floatSpeedLevels = {0.2, 0.22, 0.28, 0.29, 0.3};

    double getFloatSpeed(int speedLevel) {
        double min = 0;
        double value = 0;
        double input = (motion.getInput() / 100);
        if (mc.thePlayer.moveStrafing != 0 && mc.thePlayer.moveForward != 0) min = 0.003;
        value = floatSpeedLevels[0] - min;
        if (speedLevel >= 0) {
            value = floatSpeedLevels[speedLevel] - min;
        }
        value *= input;
        return value;
    }

    private void handleMotion() {
        // Skip if tower mode, in air, or motion is vanilla (100%)
        if (ModuleManager.tower.canTower() || !mc.thePlayer.onGround || motion.getInput() == 100) {
            return;
        }
        
        // If MoveFix is enabled, don't manually modify motion
        // The rotation handler will properly adjust movement inputs
        if (intaveBypass.isToggled() && intaveMovementFix.isToggled()) {
            return;
        }
        
        // Use MoveUtil.strafe() for proper motion modification (from reference impl)
        // This is the correct way to modify speed while maintaining proper movement direction
        // Note: Still not fully compatible with Grim but better than direct motion modification
        if (Utils.isMoving()) {
            double input = motion.getInput() / 100.0;
            MoveUtil.strafe(MoveUtil.speed() * input);
        }
    }
    
    /**
     * Calculates a varied delay to defeat Intave's balance check.
     * 
     * Intave uses BalanceUtils.getSquaredBalanceFromLong() which calculates:
     * balance = sum((timing[i] - average)^2) / count
     * 
     * Low balance = consistent timing = machine-like = FLAG
     * High balance = varied timing = human-like = PASS
     * 
     * Strategy: Alternate between short and long delays with randomness
     * to create high variance while still being fast enough to scaffold.
     */
    private long calculateVariedDelay(boolean safeMode) {
        // Base ranges for safe/aggressive modes
        // Safe: 25-140ms range, Aggressive: 8-90ms range
        long minBase = safeMode ? 25 : 8;
        long maxBase = safeMode ? 140 : 90;
        long spikeExtra = safeMode ? 80 : 50;
        long maxClamp = maxBase + spikeExtra;
        
        // Use placement count to create a pattern with high variance
        // Every few placements, use a noticeably different delay
        int pattern = placementCount % 6;
        
        long delay;
        switch (pattern) {
            case 0:
                // Short delay
                delay = minBase + intaveRandom.nextInt(12);
                break;
            case 1:
                // Medium-short delay
                delay = minBase + 15 + intaveRandom.nextInt(25);
                break;
            case 2:
                // Medium delay
                delay = (minBase + maxBase) / 2 + intaveRandom.nextInt(30) - 15;
                break;
            case 3:
                // Long delay (creates high variance)
                delay = maxBase - 35 + intaveRandom.nextInt(30);
                break;
            case 4:
                // Occasional spike delay to increase balance variance
                delay = maxBase + 10 + intaveRandom.nextInt((int) spikeExtra);
                break;
            case 5:
            default:
                // Random in full range
                delay = minBase + intaveRandom.nextInt((int)(maxBase - minBase));
                break;
        }
        
        // Add additional random jitter (±8ms) for extra unpredictability
        delay += intaveRandom.nextInt(17) - 8;
        
        // Clamp to valid range
        return Math.max(minBase, Math.min(maxClamp, delay));
    }
    
    public float hardcodedYaw() {
        float simpleYaw = 0F;
        float f = 0.8F;

        if (mc.thePlayer.moveForward >= f) {
            simpleYaw -= 180;
            if (mc.thePlayer.moveStrafing >= f) simpleYaw += 45;
            if (mc.thePlayer.moveStrafing <= -f) simpleYaw -= 45;
        }
        else if (mc.thePlayer.moveForward == 0) {
            simpleYaw -= 180;
            if (mc.thePlayer.moveStrafing >= f) simpleYaw += 90;
            if (mc.thePlayer.moveStrafing <= -f) simpleYaw -= 90;
        }
        else if (mc.thePlayer.moveForward <= -f) {
            if (mc.thePlayer.moveStrafing >= f) simpleYaw -= 45;
            if (mc.thePlayer.moveStrafing <= -f) simpleYaw += 45;
        }
        return simpleYaw;
    }

    public boolean holdingBlocks() {
        // IMixinItemRenderer removed - not available in current codebase
        // if (autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled() && lastSlot.get() != mc.thePlayer.inventory.currentItem && totalBlocks() > 0) {
        //     ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(true);
        //     ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(true);
        // }
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (!autoSwap.isToggled() || getSlot() == -1) {
            if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !ContainerUtils.canBePlaced((ItemBlock) heldItem.getItem())) {
                return false;
            }
        }
        return true;
    }

    private double getMovementAngle() {
        double angle = Math.toDegrees(Math.atan2(-mc.thePlayer.moveStrafing, mc.thePlayer.moveForward));
        return angle == -0 ? 0 : angle;
    }

    public static int getSlot() {
        int slot = -1;
        int highestStack = -1;
        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemBlock && ContainerUtils.canBePlaced((ItemBlock) itemStack.getItem()) && itemStack.stackSize > 0) {
                if (Utils.getBedwarsStatus() == 2 && ((ItemBlock) itemStack.getItem()).getBlock() instanceof BlockTNT) {
                    continue;
                }
                // ModuleManager.autoSwap.sameType removed - field is private
                // if (itemStack != null && heldItem != null && (heldItem.getItem() instanceof ItemBlock) && ContainerUtils.canBePlaced((ItemBlock) heldItem.getItem()) && ModuleManager.autoSwap.sameType.isToggled() && !(itemStack.getItem().getClass().equals(heldItem.getItem().getClass()))) {
                //     continue;
                // }
                if (itemStack.stackSize > highestStack) {
                    highestStack = itemStack.stackSize;
                    slot = i;
                }
            }
        }
        return slot;
    }

    public boolean setSlot() {
        int slot = getSlot();
        if (slot == -1) {
            return false;
        }
        if (blockSlot == -1) {
            blockSlot = slot;
        }
        if (lastSlot.get() == -1) {
            lastSlot.set(mc.thePlayer.inventory.currentItem);
        }
        if (autoSwap.isToggled() && blockSlot != -1) {
            // ModuleManager.autoSwap.swapToGreaterStack removed - field is private
            // if (ModuleManager.autoSwap.swapToGreaterStack.isToggled()) {
            //     mc.thePlayer.inventory.currentItem = slot;
            // }
            // else {
            //     mc.thePlayer.inventory.currentItem = blockSlot;
            // }
            mc.thePlayer.inventory.currentItem = slot;
        }

        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !ContainerUtils.canBePlaced((ItemBlock) heldItem.getItem())) {
            blockSlot = -1;
            return false;
        }
        return true;
    }
    
    // Compatibility methods for old API
    public static boolean isDiagonal() {
        return ScaffoldUtils.scaffoldDiagonal(false);
    }
    
    public float getYaw() {
        return scaffoldYaw;
    }
    
    public boolean keepYPosition() {
        return fastScaffoldKeepY;
    }
    
    public boolean isGrimLegitMotion() {
        return false; // Not implemented in reference
    }

    static class PlaceData {
        EnumFacing enumFacing;
        BlockPos blockPos;
        Vec3 hitVec;

        PlaceData(BlockPos blockPos, EnumFacing enumFacing) {
            this.enumFacing = enumFacing;
            this.blockPos = blockPos;
        }
    }
}

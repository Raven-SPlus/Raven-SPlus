package keystrokesmod.module.impl.world;

import keystrokesmod.event.*;
import keystrokesmod.helper.ScaffoldBlockCountHelper;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.other.RotationHandler;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.mixins.impl.network.C08PacketPlayerBlockPlacementAccessor;
import keystrokesmod.utility.*;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.scaffold.ScaffoldUtils;
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
    private final SliderSetting motion;
    public SliderSetting rotation, fakeRotation;
    private SliderSetting sprint;
    private SliderSetting fastScaffold;
    private SliderSetting multiPlace;
    public ButtonSetting autoSwap;
    private ButtonSetting cancelKnockBack;
    private ButtonSetting fastOnRMB;
    public ButtonSetting highlightBlocks;
    private ButtonSetting jumpFacingForward;
    public ButtonSetting safeWalk;
    public ButtonSetting showBlockCount;
    private ButtonSetting silentSwing;

    private String[] rotationModes = new String[] { "§cDisabled", "Simple", "Offset", "Precise" };
    private String[] fakeRotationModes = new String[] { "§cDisabled", "Strict", "Smooth", "Spin" };
    private String[] sprintModes = new String[] { "§cDisabled", "Vanilla", "Float" };
    private String[] fastScaffoldModes = new String[] { "§cDisabled", "Jump B", "Jump C", "Jump D", "Keep-Y A", "Keep-Y B", "Jump A" };
    private String[] multiPlaceModes = new String[] { "§cDisabled", "1 extra", "2 extra" };

    public Map<BlockPos, Timer> highlight = new HashMap<>();

    private ScaffoldBlockCountHelper scaffoldBlockCount;

    public AtomicInteger lastSlot = new AtomicInteger(-1);

    public boolean hasSwapped;
    private boolean hasPlaced;

    private boolean rotateForward;
    private double startYPos = -1;
    public boolean fastScaffoldKeepY;
    private boolean firstKeepYPlace;
    private boolean rotatingForward;
    private int keepYTicks;
    public boolean lowhop;
    private int rotationDelay;
    private int blockSlot = -1;

    private float fakeYaw1, fakeYaw2;

    public boolean canBlockFade;

    private boolean floatJumped;
    private boolean floatStarted;
    private boolean floatWasEnabled;
    private boolean floatKeepY;

    private Vec3 targetBlock;
    private PlaceData blockInfo;
    private Vec3 hitVec, lookVec;
    private PlaceData lastPlacement;
    private EnumFacing lastPlacedFacing;
    private float[] blockRotations;
    public float scaffoldYaw, scaffoldPitch, blockYaw, yawOffset;
    private boolean set2;

    public boolean moduleEnabled;
    public boolean isEnabled;
    private boolean disabledModule;
    private boolean dontDisable, towerEdge;
    private int disableTicks;
    private int scaffoldTicks;

    private boolean was451, was452;

    private float minOffset;
    private float minPitch = 80F;

    private float edge;

    private long firstStroke;
    private float lastEdge2, yawAngle, theYaw;
    private float fakeYaw, fakePitch;
    private float lastPlacementYaw = 0;

    private int currentFace;
    private boolean enabledOffGround = false;
    
    // Intave 12 bypass: Timing and randomization
    private long lastPlacementTime = 0;
    private long lastBlockPlacePacketTime = 0; // Track when block place packet was sent
    private int lastPlacedBlockY = Integer.MIN_VALUE; // Track Y-level of last placed block (Intave line 137)
    private static final long MIN_PLACEMENT_DELAY = 5; // Minimum delay between placements (ms) - reduced for manual tower
    private static final long MAX_PLACEMENT_DELAY = 20; // Maximum delay between placements (ms) - reduced for manual tower
    private static final long MIN_PLACEMENT_DELAY_AIR = 0; // No delay when in air (ms) - allow manual tower
    private static final long MAX_PLACEMENT_DELAY_AIR = 5; // Very short delay when in air (ms)
    private static final long SAME_Y_LEVEL_EXTRA_DELAY_MIN = 15; // Extra delay when placing on same Y-level (ms)
    private static final long SAME_Y_LEVEL_EXTRA_DELAY_MAX = 45; // Extra delay when placing on same Y-level (ms)
    
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
    private boolean forceStrict = false;

    public Scaffold() {
        super("Scaffold", category.world);
        this.registerSetting(motion = new SliderSetting("Motion", 100, 50, 150, 1, "%"));
        this.registerSetting(rotation = new SliderSetting("Rotation", rotationModes, 1));
        this.registerSetting(fakeRotation = new SliderSetting("Rotation (fake)", fakeRotationModes, 0));
        this.registerSetting(sprint = new SliderSetting("Sprint mode", sprintModes, 0));
        this.registerSetting(fastScaffold = new SliderSetting("Fast scaffold", fastScaffoldModes, 0));
        this.registerSetting(multiPlace = new SliderSetting("Multi-place", multiPlaceModes, 0));
        this.registerSetting(autoSwap = new ButtonSetting("Auto swap", true));
        this.registerSetting(cancelKnockBack = new ButtonSetting("Cancel knockback", false));
        this.registerSetting(fastOnRMB = new ButtonSetting("Fast on RMB", true));
        this.registerSetting(highlightBlocks = new ButtonSetting("Highlight blocks", true));
        this.registerSetting(jumpFacingForward = new ButtonSetting("Jump facing forward", false));
        this.registerSetting(safeWalk = new ButtonSetting("Safewalk", true));
        this.registerSetting(showBlockCount = new ButtonSetting("Show block count", true));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing", false));
        
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
        
        // Reset timing for Intave 12 bypass
        lastPlacementTime = 0;
        lastBlockPlacePacketTime = 0;
        lastPlacedBlockY = Integer.MIN_VALUE;
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

        // Tower integration removed - Tower doesn't have isVerticalTowering, yaw, pitch fields

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
            
            // Track packet time for Intave Check 2 (packet timing validation)
            lastBlockPlacePacketTime = System.currentTimeMillis();
            
            // Intave 12 bypass: Randomize block face look vectors to avoid suspicious values
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
                // All zero is allowed, but add tiny random offset to avoid pattern
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
     * Intave checks: f == 0.0 || f == 0.5 || f >= 1.0
     */
    private float randomizeFacingValue(float value) {
        // Special case: If all three values are 0.0, Intave allows it (line 90)
        // But we still want to randomize individual suspicious values
        
        // If value is exactly 0.0, 0.5, or >=1.0, randomize it
        if (Math.abs(value) < 0.001f) {
            // 0.0 -> randomize to 0.01-0.49 or 0.51-0.99
            if (Math.random() < 0.5) {
                return (float) (Math.random() * 0.48 + 0.01); // 0.01 to 0.49
            } else {
                return (float) (Math.random() * 0.48 + 0.51); // 0.51 to 0.99
            }
        }
        if (Math.abs(value - 0.5f) < 0.001f) {
            // 0.5 -> randomize to avoid exact 0.5
            // Return value in range 0.35-0.49 or 0.51-0.65 (avoiding 0.5)
            if (Math.random() < 0.5) {
                return (float) (Math.random() * 0.14 + 0.35); // 0.35 to 0.49
            } else {
                return (float) (Math.random() * 0.14 + 0.51); // 0.51 to 0.65
            }
        }
        if (value >= 1.0f || value < 0.0f) {
            // Invalid range -> clamp and randomize
            float clamped = Math.max(0.01f, Math.min(0.99f, value));
            if (Math.abs(clamped - 0.5f) < 0.05f) {
                clamped = clamped < 0.5f ? 0.4f : 0.6f;
            }
            return clamped;
        }
        
        // For legitimate values, add very small random offset to avoid pattern detection
        // But keep it subtle to not break placements
        float offset = (float) ((Math.random() - 0.5) * 0.015); // ±0.0075 offset (very subtle)
        float newValue = value + offset;
        
        // Clamp to valid range and avoid suspicious values
        if (newValue < 0.01f) newValue = 0.01f;
        if (newValue > 0.99f) newValue = 0.99f;
        // Avoid 0.5 with a small margin
        if (Math.abs(newValue - 0.5f) < 0.03f) {
            newValue = newValue < 0.5f ? 0.47f : 0.53f;
        }
        return newValue;
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
            info = Mouse.isButtonDown(1) && Utils.tabbedIn() ? fastScaffoldModes[(int) fastScaffold.getInput()] : sprintModes[(int) sprint.getInput()];
        }
        else {
            info = fastScaffold.getInput() > 0 ? fastScaffoldModes[(int) fastScaffold.getInput()] : sprintModes[(int) sprint.getInput()];
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
            return handleFastScaffolds() > 0 || !holdingBlocks();
        }
        return false;
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
        
        // Intave 12 bypass: Add timing delay between placements to avoid pattern detection
        // Intave Check 2 (line 122): diffToLastValidBlockPacket > 2000ms && valid → FLAG
        // Intave line 137: Detects same Y-level placements - if (blockY - 2) matches previous within 0.1 blocks
        // Intave line 138: Calculates balance from last 10 placement timings, flags if balance < 380.0
        // Solution: Vary timing significantly when placing on same Y-level to increase balance
        long currentTime = System.currentTimeMillis();
        boolean inAir = !mc.thePlayer.onGround;
        boolean isJumping = mc.thePlayer.motionY > 0.0; // Moving upward
        
        // Check if placing on same Y-level as last placement (Intave line 137 detection)
        // Intave checks: (blockY - 2) matches previous penaltyBlock.getY() within 0.1 blocks
        // We track the actual Y-level and compare directly
        int currentBlockY = block.blockPos.getY();
        boolean sameYLevel = (lastPlacedBlockY != Integer.MIN_VALUE && 
                              Math.abs(currentBlockY - lastPlacedBlockY) < 0.1);
        
        // For manual tower (player jumping), be very lenient with timing
        if (lastPlacementTime > 0) {
            long timeSinceLastPlacement = currentTime - lastPlacementTime;
            long requiredDelay;
            
            // If player is jumping upward (manual tower), use no delay
            if (isJumping && inAir) {
                requiredDelay = 0; // No delay when manually jumping - allow fast placements
            } else if (inAir) {
                // Very short delay when in air (falling)
                requiredDelay = MIN_PLACEMENT_DELAY_AIR + (long)(Math.random() * (MAX_PLACEMENT_DELAY_AIR - MIN_PLACEMENT_DELAY_AIR));
            } else {
                // Normal delay when on ground
                requiredDelay = MIN_PLACEMENT_DELAY + (long)(Math.random() * (MAX_PLACEMENT_DELAY - MIN_PLACEMENT_DELAY));
            }
            
            // Intave line 137 bypass: Add extra random delay when placing on same Y-level
            // This varies the timing to increase balance and prevent flags
            if (sameYLevel && !isJumping) {
                long extraDelay = SAME_Y_LEVEL_EXTRA_DELAY_MIN + 
                                 (long)(Math.random() * (SAME_Y_LEVEL_EXTRA_DELAY_MAX - SAME_Y_LEVEL_EXTRA_DELAY_MIN));
                requiredDelay += extraDelay;
            }
            
            if (timeSinceLastPlacement < requiredDelay) {
                // Delay placement to avoid suspicious timing patterns
                return;
            }
        }
        
        // Intave Check 2: Ensure we place within 2000ms of packet arrival
        // Track when packet was sent and ensure placement happens soon after
        if (lastBlockPlacePacketTime > 0) {
            long timeSincePacket = currentTime - lastBlockPlacePacketTime;
            if (timeSincePacket > 1800) {
                // Too long since packet - might trigger Check 2, but allow if jumping
                if (!isJumping && !inAir) {
                    return; // Skip if on ground and too long since packet
                }
            }
        }
        
        MovingObjectPosition raycast = RotationUtils.rayTraceCustom(mc.playerController.getBlockReachDistance(), RotationUtils.serverRotations[0], RotationUtils.serverRotations[1]);
        
        // Intave 12 bypass: Ensure target block matches placed block (distance check)
        // Intave Check 5d (line 210-220): f4 > 0.0 && f4 < 1.0 && hsDist < 1.0 → FLAG if lastTimeSuspiciousForScaffoldWalk < 2000ms
        // Intave Check 6 (line 240-247): Sprinting+falling with target NOT adjacent → increases scaffoldwalkVL
        // Intave line 240: Checks if target is NOT (NORTH/EAST/SOUTH/WEST relative to placed block)
        boolean isSprinting = mc.thePlayer.isSprinting();
        boolean isFalling = mc.thePlayer.motionY < 0.0;
        
        // Calculate hsDist (distance from block above head to player) - Intave uses this
        double hsDist = block.blockPos.getY() + 1.0 - mc.thePlayer.posY;
        if (hsDist < 0) hsDist = -hsDist; // Absolute distance
        
        // Be very lenient when jumping (manual tower) to allow placements
        // For manual tower, prioritize placement success over perfect target matching
        if (raycast != null && raycast.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            double distanceToTarget = raycast.getBlockPos().distanceSq(block.blockPos);
            double distanceToTargetLinear = Math.sqrt(distanceToTarget);
            
            // If target block matches placed block exactly, use raycast hitVec (most accurate)
            if (distanceToTarget < 0.1 && raycast.getBlockPos().equals(block.blockPos) && raycast.sideHit.equals(block.enumFacing)) {
                block.hitVec = raycast.hitVec;
            } 
            // If target is close, use it but be careful about Intave Check 5d and Check 6
            else if (distanceToTargetLinear < 1.0) {
                // Intave Check 6: When sprinting and falling, target must be adjacent (NORTH/EAST/SOUTH/WEST)
                // Intave line 240: Checks if target.getRelative(NORTH/EAST/SOUTH/WEST) != placed block
                // This means: placed block must be adjacent to target block
                // This is very strict (< 380ms window), so we need to ensure target matches
                if (isSprinting && isFalling && !isJumping) {
                    // Check if placed block is adjacent to target block (NORTH/EAST/SOUTH/WEST)
                    BlockPos targetPos = raycast.getBlockPos();
                    BlockPos placedPos = block.blockPos;
                    boolean isAdjacent = (targetPos.add(0, 0, -1).equals(placedPos) ||  // NORTH
                                         targetPos.add(1, 0, 0).equals(placedPos) ||   // EAST
                                         targetPos.add(0, 0, 1).equals(placedPos) ||    // SOUTH
                                         targetPos.add(-1, 0, 0).equals(placedPos)) && // WEST
                                        targetPos.getY() == placedPos.getY();
                    
                    if (!isAdjacent && distanceToTargetLinear > 0.2) {
                        // Placed block is not adjacent to target - skip to avoid Check 6
                        // But only if not jumping (manual tower needs leniency)
                        return;
                    }
                }
                // Intave Check 5d: If distance > 0.0 && < 1.0 && hsDist < 1.0, might flag
                // Use raycast hitVec if it matches the block and facing to minimize distance
                if (raycast.getBlockPos().equals(block.blockPos) && raycast.sideHit.equals(block.enumFacing)) {
                    block.hitVec = raycast.hitVec;
                } else if (hsDist < 1.0 && distanceToTargetLinear > 0.0 && distanceToTargetLinear < 1.0 && !isJumping) {
                    // Intave Check 5d condition - only be careful if not jumping
                    // When jumping, allow placement even if target doesn't match perfectly
                    if (raycast.getBlockPos().equals(block.blockPos)) {
                        block.hitVec = raycast.hitVec;
                    }
                }
            } 
            // Target is far - only skip if on ground (allow in air for manual tower)
            else if (!isJumping && !inAir && distanceToTargetLinear > 1.5) {
                // Skip if on ground and target is too far (> 1.5 blocks)
                return;
            }
            // When jumping or in air, allow placement even if target doesn't match perfectly
            // This is critical for manual tower to work
        } else if (isJumping || inAir) {
            // When jumping/in air and no raycast, still allow placement
            // Manual tower needs to work even if raycast doesn't hit perfectly
        }
        
        // Set compatibility fields
        // Create MovingObjectPosition for compatibility (using raycast if available, otherwise create from block data)
        if (raycast != null && raycast.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && raycast.getBlockPos().equals(block.blockPos) && raycast.sideHit.equals(block.enumFacing)) {
            placeBlock = raycast;
        } else {
            // Create a basic MovingObjectPosition for compatibility (constructor: Vec3 hitVec, EnumFacing sideHit, BlockPos blockPos)
            // Use block.hitVec directly - randomization happens in onSendPacket via block face offsets
            Vec3 hitVecForMOP = block.hitVec != null ? block.hitVec : new Vec3(block.blockPos.getX() + 0.5, block.blockPos.getY() + 0.5, block.blockPos.getZ() + 0.5);
            placeBlock = new MovingObjectPosition(hitVecForMOP, block.enumFacing, block.blockPos);
        }
        rayCasted = raycast;
        place = true;
        placeYaw = RotationUtils.serverRotations[0];
        placePitch = RotationUtils.serverRotations[1];
        
        // Intave 12 bypass: Ensure placement happens soon after packet to avoid Check 2
        // The packet is sent by onPlayerRightClick, so we track it here
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, block.blockPos, block.enumFacing, block.hitVec)) {
            // Update last placement time and packet time
            lastPlacementTime = currentTime;
            lastBlockPlacePacketTime = currentTime; // Packet sent now
            lastPlacedBlockY = block.blockPos.getY(); // Track Y-level for Intave line 137 detection
            
            if (silentSwing.isToggled()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            }
            else {
                mc.thePlayer.swingItem();
                // ModuleManager.autoSwap.spoofItem removed - AutoSwap doesn't have this field
                // if (!(autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled())) {
                //     mc.getItemRenderer().resetEquippedProgress();
                // }
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

    private void placeBlock(int yOffset, int xOffset) {
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
        if (ModuleManager.tower.canTower() || !mc.thePlayer.onGround || motion.getInput() == 1) {
            return;
        }
        double input = (motion.getInput() / 100);
        
        // Intave 12 bypass: Add variation to motion to avoid consistent XZ motion ratio detection
        // Intave Check 7 (line 299-305): Detects xzConjToLastxz < 0.4 OR (xzConjToLastxz ≈ 1.0 && !sneaking)
        // When placing blocks under player within 500ms, XZ motion must vary
        // Variation should be significant enough to avoid < 0.4 ratio but not too much to break movement
        double variationX = 1.0 + (Math.random() - 0.5) * 0.04; // ±2% variation (increased)
        double variationZ = 1.0 + (Math.random() - 0.5) * 0.04;
        
        // Ensure variation doesn't create exact 1.0 ratio (suspicious when not sneaking)
        if (Math.abs(variationX - variationZ) < 0.01 && !mc.thePlayer.isSneaking()) {
            // If ratios are too similar, add more variation
            variationX += (Math.random() - 0.5) * 0.02;
            variationZ += (Math.random() - 0.5) * 0.02;
        }
        
        mc.thePlayer.motionX *= input * variationX;
        mc.thePlayer.motionZ *= input * variationZ;
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

/*
 * FDPClient Scaffold Module - Full Feature Copy
 * Converted from Kotlin to Java and adapted for RavenS+
 * Original source: FDPClient-src
 */
package keystrokesmod.module.impl.experimental;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.PostUpdateEvent;
import keystrokesmod.event.SafeWalkEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.other.RotationHandler;
import keystrokesmod.module.impl.other.SlotHandler;
import keystrokesmod.module.impl.other.anticheats.utils.world.PlayerRotation;
import keystrokesmod.module.setting.impl.*;
import keystrokesmod.mixins.impl.client.KeyBindingAccessor;
import keystrokesmod.mixins.impl.client.MinecraftAccessor;
import keystrokesmod.mixins.impl.client.PlayerControllerMPAccessor;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.Reflection;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.TimerUtil;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBush;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.*;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Full feature copy of FDPClient Scaffold module
 * Includes: Normal, Rewinside, Expand, Telly, GodBridge modes
 * Tower functionality, Eagle, AutoBlock, SafeWalk, and more
 */
public class ScaffoldRecode extends Module {
    
    // ========== SCAFFOLD MODES & SETTINGS ==========
    
    private final ModeSetting scaffoldMode = new ModeSetting("ScaffoldMode", 
        new String[]{"Normal", "Rewinside", "Expand", "Telly", "GodBridge"}, 0);
    
    // Expand settings
    private final ButtonSetting omniDirectionalExpand = new ButtonSetting("OmniDirectionalExpand", false);
    private final SliderSetting expandLength = new SliderSetting("ExpandLength", 1, 1, 6, 1);
    
    // Place delay
    private final ButtonSetting placeDelayValue = new ButtonSetting("PlaceDelay", true);
    private final SliderSetting delayMin = new SliderSetting("Delay Min", 0, 0, 1000, 1, "ms");
    private final SliderSetting delayMax = new SliderSetting("Delay Max", 0, 0, 1000, 1, "ms");
    
    // Extra clicks
    private final ButtonSetting extraClicks = new ButtonSetting("DoExtraClicks", false);
    private final ButtonSetting simulateDoubleClicking = new ButtonSetting("SimulateDoubleClicking", false);
    private final SliderSetting extraClickCPSMin = new SliderSetting("ExtraClickCPS Min", 3, 0, 50, 1);
    private final SliderSetting extraClickCPSMax = new SliderSetting("ExtraClickCPS Max", 7, 0, 50, 1);
    private final ModeSetting placementAttempt = new ModeSetting("PlacementAttempt", 
        new String[]{"Fail", "Independent"}, 0);
    
    // Autoblock
    private final ModeSetting autoBlock = new ModeSetting("AutoBlock", 
        new String[]{"Off", "Pick", "Spoof", "Switch"}, 2);
    private final ButtonSetting sortByHighestAmount = new ButtonSetting("SortByHighestAmount", false);
    private final ButtonSetting earlySwitch = new ButtonSetting("EarlySwitch", false);
    private final SliderSetting amountBeforeSwitch = new SliderSetting("SlotAmountBeforeSwitch", 3, 1, 10, 1);
    
    // Basic settings
    private final ButtonSetting autoF5 = new ButtonSetting("AutoF5", false);
    private final ButtonSetting sprint = new ButtonSetting("Sprint", false);
    private final ButtonSetting swing = new ButtonSetting("Swing", true);
    private final ButtonSetting down = new ButtonSetting("Down", true);
    
    private final SliderSetting ticksUntilRotationMin = new SliderSetting("TicksUntilRotation Min", 3, 1, 8, 1);
    private final SliderSetting ticksUntilRotationMax = new SliderSetting("TicksUntilRotation Max", 3, 1, 8, 1);
    
    // GodBridge settings
    private final ButtonSetting waitForRots = new ButtonSetting("WaitForRotations", false);
    private final ButtonSetting useOptimizedPitch = new ButtonSetting("UseOptimizedPitch", false);
    private final SliderSetting customGodPitch = new SliderSetting("GodBridgePitch", 73.5f, 0f, 90f, 0.1f);
    private final ButtonSetting jumpAutomatically = new ButtonSetting("JumpAutomatically", true);
    private final SliderSetting blocksToJumpMin = new SliderSetting("BlocksToJump Min", 4, 1, 8, 1);
    private final SliderSetting blocksToJumpMax = new SliderSetting("BlocksToJump Max", 4, 1, 8, 1);
    
    // Telly settings
    private final ButtonSetting startHorizontally = new ButtonSetting("StartHorizontally", true);
    private final SliderSetting horizontalPlacementsMin = new SliderSetting("HorizontalPlacements Min", 1, 1, 10, 1);
    private final SliderSetting horizontalPlacementsMax = new SliderSetting("HorizontalPlacements Max", 1, 1, 10, 1);
    private final SliderSetting verticalPlacementsMin = new SliderSetting("VerticalPlacements Min", 1, 1, 10, 1);
    private final SliderSetting verticalPlacementsMax = new SliderSetting("VerticalPlacements Max", 1, 1, 10, 1);
    private final SliderSetting jumpTicksMin = new SliderSetting("JumpTicks Min", 0, 0, 10, 1);
    private final SliderSetting jumpTicksMax = new SliderSetting("JumpTicks Max", 0, 0, 10, 1);
    
    // Clutch settings
    private final ButtonSetting allowClutching = new ButtonSetting("AllowClutching", true);
    private final SliderSetting horizontalClutchBlocks = new SliderSetting("HorizontalClutchBlocks", 3, 1, 5, 1);
    private final SliderSetting verticalClutchBlocks = new SliderSetting("VerticalClutchBlocks", 2, 1, 3, 1);
    private final ButtonSetting blockSafe = new ButtonSetting("BlockSafe", false);
    
    // Eagle settings
    private final ModeSetting eagle = new ModeSetting("Eagle", 
        new String[]{"Normal", "Silent", "Off"}, 0);
    private final ModeSetting eagleMode = new ModeSetting("EagleMode", 
        new String[]{"Both", "OnGround", "InAir"}, 0);
    private final ButtonSetting adjustedSneakSpeed = new ButtonSetting("AdjustedSneakSpeed", true);
    private final SliderSetting eagleSpeed = new SliderSetting("EagleSpeed", 0.3f, 0.3f, 1.0f, 0.01f);
    private final ButtonSetting eagleSprint = new ButtonSetting("EagleSprint", false);
    private final SliderSetting blocksToEagleMin = new SliderSetting("BlocksToEagle Min", 0, 0, 10, 1);
    private final SliderSetting blocksToEagleMax = new SliderSetting("BlocksToEagle Max", 0, 0, 10, 1);
    private final SliderSetting edgeDistance = new SliderSetting("EagleEdgeDistance", 0f, 0f, 0.5f, 0.01f);
    private final ButtonSetting useMaxSneakTime = new ButtonSetting("UseMaxSneakTime", true);
    private final SliderSetting maxSneakTicksMin = new SliderSetting("MaxSneakTicks Min", 3, 0, 10, 1);
    private final SliderSetting maxSneakTicksMax = new SliderSetting("MaxSneakTicks Max", 3, 0, 10, 1);
    private final ButtonSetting blockSneakingAgainUntilOnGround = new ButtonSetting("BlockSneakingAgainUntilOnGround", true);
    
    // Rotation settings
    private final ModeSetting rotationMode = new ModeSetting("Rotations", 
        new String[]{"Off", "Normal", "Stabilized", "ReverseYaw", "GodBridge"}, 1);
    private final ButtonSetting strictValue = new ButtonSetting("Strict", false);
    private final ButtonSetting resetTicksValue = new ButtonSetting("ResetTicks", false);
    private final SliderSetting resetTicks = new SliderSetting("ResetTicks Value", 3, 1, 20, 1);
    private final ButtonSetting keepRotation = new ButtonSetting("KeepRotation", false);
    private final ButtonSetting legitimize = new ButtonSetting("Legitimize", false);
    private final ButtonSetting applyServerSide = new ButtonSetting("ApplyServerSide", false);
    
    // Search settings
    private final ModeSetting searchMode = new ModeSetting("SearchMode", 
        new String[]{"Area", "Center"}, 0);
    private final SliderSetting minDist = new SliderSetting("MinDist", 0f, 0f, 0.2f, 0.01f);
    
    // Zitter settings
    private final ModeSetting zitterMode = new ModeSetting("Zitter", 
        new String[]{"Off", "Teleport", "Smooth"}, 0);
    private final SliderSetting zitterSpeed = new SliderSetting("ZitterSpeed", 0.13f, 0.1f, 0.3f, 0.01f);
    private final SliderSetting zitterStrength = new SliderSetting("ZitterStrength", 0.05f, 0f, 0.2f, 0.01f);
    private final SliderSetting zitterTicksMin = new SliderSetting("ZitterTicks Min", 2, 0, 6, 1);
    private final SliderSetting zitterTicksMax = new SliderSetting("ZitterTicks Max", 3, 0, 6, 1);
    private final ButtonSetting useSneakMidAir = new ButtonSetting("UseSneakMidAir", false);
    
    // Game settings
    private final SliderSetting timer = new SliderSetting("Timer", 1f, 0.1f, 10f, 0.1f);
    private final SliderSetting speedModifier = new SliderSetting("SpeedModifier", 1f, 0f, 2f, 0.01f);
    private final ButtonSetting speedLimiter = new ButtonSetting("SpeedLimiter", false);
    private final SliderSetting speedLimit = new SliderSetting("SpeedLimit", 0.11f, 0.01f, 0.12f, 0.01f);
    private final ButtonSetting slow = new ButtonSetting("Slow", false);
    private final ButtonSetting slowGround = new ButtonSetting("SlowOnlyGround", false);
    private final SliderSetting slowSpeed = new SliderSetting("SlowSpeed", 0.6f, 0.2f, 0.8f, 0.01f);
    
    // Jump Strafe
    private final ButtonSetting jumpStrafe = new ButtonSetting("JumpStrafe", false);
    private final SliderSetting jumpStraightStrafeMin = new SliderSetting("JumpStraightStrafe Min", 0.4f, 0.1f, 1f, 0.01f);
    private final SliderSetting jumpStraightStrafeMax = new SliderSetting("JumpStraightStrafe Max", 0.45f, 0.1f, 1f, 0.01f);
    private final SliderSetting jumpDiagonalStrafeMin = new SliderSetting("JumpDiagonalStrafe Min", 0.4f, 0.1f, 1f, 0.01f);
    private final SliderSetting jumpDiagonalStrafeMax = new SliderSetting("JumpDiagonalStrafe Max", 0.45f, 0.1f, 1f, 0.01f);
    
    // Safety
    private final ButtonSetting sameY = new ButtonSetting("SameY", false);
    private final ButtonSetting jumpOnUserInput = new ButtonSetting("JumpOnUserInput", true);
    private final ButtonSetting safeWalkValue = new ButtonSetting("SafeWalk", true);
    private final ButtonSetting airSafe = new ButtonSetting("AirSafe", false);
    
    // Visuals
    private final ButtonSetting mark = new ButtonSetting("Mark", false);
    private final ButtonSetting trackCPS = new ButtonSetting("TrackCPS", false);
    
    // Tower settings
    private final ModeSetting towerMode = new ModeSetting("TowerMode",
        new String[]{"None", "Jump", "MotionJump", "Motion", "ConstantMotion", "MotionTP", 
                    "Packet", "Teleport", "AAC3.3.9", "AAC3.6.4", "Vulcan2.9.0", "Pulldown"}, 0);
    private final ButtonSetting stopWhenBlockAbove = new ButtonSetting("StopWhenBlockAbove", false);
    private final ButtonSetting towerOnJump = new ButtonSetting("TowerOnJump", true);
    private final ButtonSetting towerNotOnMove = new ButtonSetting("TowerNotOnMove", false);
    private final SliderSetting jumpMotion = new SliderSetting("JumpMotion", 0.42f, 0.3681289f, 0.79f, 0.01f);
    private final SliderSetting jumpDelay = new SliderSetting("JumpDelay", 0, 0, 20, 1);
    private final SliderSetting constantMotion = new SliderSetting("ConstantMotion", 0.42f, 0.1f, 1f, 0.01f);
    private final SliderSetting constantMotionJumpGround = new SliderSetting("ConstantMotionJumpGround", 0.79f, 0.76f, 1f, 0.01f);
    private final ButtonSetting jumpPacket = new ButtonSetting("JumpPacket", true);
    private final SliderSetting triggerMotion = new SliderSetting("TriggerMotion", 0.1f, 0.0f, 0.2f, 0.01f);
    private final SliderSetting dragMotion = new SliderSetting("DragMotion", 1.0f, 0.1f, 1.0f, 0.01f);
    private final SliderSetting teleportHeight = new SliderSetting("TeleportHeight", 1.15f, 0.1f, 5f, 0.01f);
    private final SliderSetting teleportDelay = new SliderSetting("TeleportDelay", 0, 0, 20, 1);
    private final ButtonSetting teleportGround = new ButtonSetting("TeleportGround", true);
    private final ButtonSetting teleportNoMotion = new ButtonSetting("TeleportNoMotion", false);
    
    // ========== INTERNAL STATE ==========
    
    private PlaceRotation placeRotation;
    private int launchY = -999;
    private boolean zitterDirection = false;
    private final TimerUtil delayTimer = new TimerUtil();
    private final TimerUtil zitterTickTimer = new TimerUtil();
    private int placedBlocksWithoutEagle = 0;
    private boolean eagleSneaking = false;
    private boolean requestedStopSneak = false;
    private int blocksPlacedUntilJump = 0;
    private int blocksToJump;
    private Rotation godBridgeTargetRotation;
    private boolean isOnRightSide = false;
    private int ticksUntilJump = 0;
    private int blocksUntilAxisChange = 0;
    private int jumpTicks;
    private int horizontalPlacements;
    private int verticalPlacements;
    private ExtraClickInfo extraClick;
    private boolean isTowering = false;
    private double jumpGround = 0.0;
    private final TimerUtil towerTickTimer = new TimerUtil();
    
    public ScaffoldRecode() {
        super("ScaffoldRecode", category.experimental, "Full feature FDPClient Scaffold copy");
        
        // Register all settings
        registerSetting(scaffoldMode);
        registerSetting(omniDirectionalExpand);
        registerSetting(expandLength);
        registerSetting(placeDelayValue);
        registerSetting(delayMin);
        registerSetting(delayMax);
        registerSetting(extraClicks);
        registerSetting(simulateDoubleClicking);
        registerSetting(extraClickCPSMin);
        registerSetting(extraClickCPSMax);
        registerSetting(placementAttempt);
        registerSetting(autoBlock);
        registerSetting(sortByHighestAmount);
        registerSetting(earlySwitch);
        registerSetting(amountBeforeSwitch);
        registerSetting(autoF5);
        registerSetting(sprint);
        registerSetting(swing);
        registerSetting(down);
        registerSetting(ticksUntilRotationMin);
        registerSetting(ticksUntilRotationMax);
        registerSetting(waitForRots);
        registerSetting(useOptimizedPitch);
        registerSetting(customGodPitch);
        registerSetting(jumpAutomatically);
        registerSetting(blocksToJumpMin);
        registerSetting(blocksToJumpMax);
        registerSetting(startHorizontally);
        registerSetting(horizontalPlacementsMin);
        registerSetting(horizontalPlacementsMax);
        registerSetting(verticalPlacementsMin);
        registerSetting(verticalPlacementsMax);
        registerSetting(jumpTicksMin);
        registerSetting(jumpTicksMax);
        registerSetting(allowClutching);
        registerSetting(horizontalClutchBlocks);
        registerSetting(verticalClutchBlocks);
        registerSetting(blockSafe);
        registerSetting(eagle);
        registerSetting(eagleMode);
        registerSetting(adjustedSneakSpeed);
        registerSetting(eagleSpeed);
        registerSetting(eagleSprint);
        registerSetting(blocksToEagleMin);
        registerSetting(blocksToEagleMax);
        registerSetting(edgeDistance);
        registerSetting(useMaxSneakTime);
        registerSetting(maxSneakTicksMin);
        registerSetting(maxSneakTicksMax);
        registerSetting(blockSneakingAgainUntilOnGround);
        registerSetting(rotationMode);
        registerSetting(strictValue);
        registerSetting(resetTicksValue);
        registerSetting(resetTicks);
        registerSetting(keepRotation);
        registerSetting(legitimize);
        registerSetting(applyServerSide);
        registerSetting(searchMode);
        registerSetting(minDist);
        registerSetting(zitterMode);
        registerSetting(zitterSpeed);
        registerSetting(zitterStrength);
        registerSetting(zitterTicksMin);
        registerSetting(zitterTicksMax);
        registerSetting(useSneakMidAir);
        registerSetting(timer);
        registerSetting(speedModifier);
        registerSetting(speedLimiter);
        registerSetting(speedLimit);
        registerSetting(slow);
        registerSetting(slowGround);
        registerSetting(slowSpeed);
        registerSetting(jumpStrafe);
        registerSetting(jumpStraightStrafeMin);
        registerSetting(jumpStraightStrafeMax);
        registerSetting(jumpDiagonalStrafeMin);
        registerSetting(jumpDiagonalStrafeMax);
        registerSetting(sameY);
        registerSetting(jumpOnUserInput);
        registerSetting(safeWalkValue);
        registerSetting(airSafe);
        registerSetting(mark);
        registerSetting(trackCPS);
        registerSetting(towerMode);
        registerSetting(stopWhenBlockAbove);
        registerSetting(towerOnJump);
        registerSetting(towerNotOnMove);
        registerSetting(jumpMotion);
        registerSetting(jumpDelay);
        registerSetting(constantMotion);
        registerSetting(constantMotionJumpGround);
        registerSetting(jumpPacket);
        registerSetting(triggerMotion);
        registerSetting(dragMotion);
        registerSetting(teleportHeight);
        registerSetting(teleportDelay);
        registerSetting(teleportGround);
        registerSetting(teleportNoMotion);
        
        // Initialize extra click
        extraClick = new ExtraClickInfo(
            randomClickDelay((int)extraClickCPSMin.getInput(), (int)extraClickCPSMax.getInput()),
            0L, 0
        );
    }
    
    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;
        launchY = (int) Math.round(mc.thePlayer.posY);
        blocksUntilAxisChange = 0;
        blocksToJump = randomRange((int)blocksToJumpMin.getInput(), (int)blocksToJumpMax.getInput());
        jumpTicks = randomRange((int)jumpTicksMin.getInput(), (int)jumpTicksMax.getInput());
        horizontalPlacements = randomRange((int)horizontalPlacementsMin.getInput(), (int)horizontalPlacementsMax.getInput());
        verticalPlacements = randomRange((int)verticalPlacementsMin.getInput(), (int)verticalPlacementsMax.getInput());
    }
    
    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;
        
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            ((KeyBindingAccessor)mc.gameSettings.keyBindSneak).setPressed(false);
            if (eagleSneaking && mc.thePlayer.isSneaking()) {
                mc.thePlayer.setSneaking(false);
            }
        }
        
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) {
            ((KeyBindingAccessor)mc.gameSettings.keyBindRight).setPressed(false);
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) {
            ((KeyBindingAccessor)mc.gameSettings.keyBindLeft).setPressed(false);
        }
        
        if (autoF5.isToggled()) {
            mc.gameSettings.thirdPersonView = 0;
        }
        
        placeRotation = null;
        Utils.getTimer().timerSpeed = 1f;
    }
    
    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        
        Utils.getTimer().timerSpeed = (float) timer.getInput();
        
        // Telly jump ticks
        if (mc.thePlayer.onGround) ticksUntilJump++;
        
        // Down mode
        if (shouldGoDown()) {
            ((KeyBindingAccessor)mc.gameSettings.keyBindSneak).setPressed(false);
        }
        
        // Slow mode
        if (slow.isToggled()) {
            if (!slowGround.isToggled() || (slowGround.isToggled() && mc.thePlayer.onGround)) {
                mc.thePlayer.motionX *= slowSpeed.getInput();
                mc.thePlayer.motionZ *= slowSpeed.getInput();
            }
        }
        
        // Eagle handling
        if (isEagleEnabled()) {
            handleEagle();
        }
        
        // Rewinside mode
        if (mc.thePlayer.onGround && scaffoldMode.getInput() == 1) {
            // MovementUtils.strafe(0.2F) - would need movement utils
            mc.thePlayer.motionY = 0.0;
        }
        
        // Update block search
        update();
        
        // Handle rotations
        handleRotations();
        
        // Handle placement
        MovingObjectPosition raycast = performBlockRaytrace(
            getCurrentRotation(), 
            mc.playerController.getBlockReachDistance()
        );
        
        boolean alreadyPlaced = false;
        
        // Extra clicks
        if (extraClicks.isToggled()) {
            int doubleClick = simulateDoubleClicking.isToggled() ? 
                ThreadLocalRandom.current().nextInt(-1, 2) : 0;
            int clicks = extraClick.clicks + doubleClick;
            
            for (int i = 0; i < clicks; i++) {
                extraClick.clicks--;
                if (doPlaceAttempt(raycast, i + 1 == clicks)) {
                    alreadyPlaced = true;
                }
            }
        }
        
        PlaceInfo target = placeRotation != null ? placeRotation.placeInfo : null;
        
        if (target == null) {
            if (placeDelayValue.isToggled()) {
                delayTimer.reset();
            }
            return;
        }
        
        if (alreadyPlaced) {
            return;
        }
        
        boolean raycastProperly = !(scaffoldMode.getInput() == 2 && expandLength.getInput() > 1 || shouldGoDown()) 
            && rotationsActive();
        
        if (!rotationsActive() || (raycast != null && raycast.getBlockPos().equals(target.blockPos) 
            && (!raycastProperly || raycast.sideHit == target.enumFacing))) {
            PlaceInfo result = raycastProperly && raycast != null ? 
                new PlaceInfo(raycast.getBlockPos(), raycast.sideHit, raycast.hitVec) : target;
            place(result);
        }
    }
    
    @SubscribeEvent
    public void onSafeWalk(SafeWalkEvent event) {
        if (!safeWalkValue.isToggled() || shouldGoDown()) {
            return;
        }
        
        if (airSafe.isToggled() || mc.thePlayer.onGround) {
            event.setSafeWalk(true);
        }
    }
    
    // ========== CORE METHODS ==========
    
    private void update() {
        if (mc.thePlayer == null) return;
        
        ItemStack heldItem = SlotHandler.getHeldItem();
        boolean holdingItem = heldItem != null && heldItem.getItem() instanceof ItemBlock;
        
        if (!holdingItem && (autoBlock.getInput() == 0 || findBlockInHotbar() == -1)) {
            return;
        }
        
        findBlock(
            scaffoldMode.getInput() == 2 && expandLength.getInput() > 1,
            searchMode.getInput() == 0
        );
    }
    
    private void findBlock(boolean expand, boolean area) {
        if (mc.thePlayer == null) return;
        
        if (!shouldKeepLaunchPosition()) {
            launchY = (int) Math.round(mc.thePlayer.posY);
        }
        
        BlockPos blockPosition;
        if (shouldGoDown()) {
            if (mc.thePlayer.posY == Math.round(mc.thePlayer.posY) + 0.5) {
                blockPosition = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.6, mc.thePlayer.posZ);
            } else {
                blockPosition = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.6, mc.thePlayer.posZ).down();
            }
        } else if (shouldKeepLaunchPosition() && launchY <= mc.thePlayer.posY) {
            blockPosition = new BlockPos(mc.thePlayer.posX, launchY - 1.0, mc.thePlayer.posZ);
        } else if (mc.thePlayer.posY == Math.round(mc.thePlayer.posY) + 0.5) {
            blockPosition = new BlockPos(mc.thePlayer);
        } else {
            blockPosition = new BlockPos(mc.thePlayer).down();
        }
        
        if (!expand && (!BlockUtils.replaceable(blockPosition) || 
            search(blockPosition, !shouldGoDown(), area, shouldPlaceHorizontally()))) {
            return;
        }
        
        if (expand) {
            double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
            int x = omniDirectionalExpand.isToggled() ? 
                -(int)Math.sin(yaw) : mc.thePlayer.getHorizontalFacing().getDirectionVec().getX();
            int z = omniDirectionalExpand.isToggled() ? 
                (int)Math.cos(yaw) : mc.thePlayer.getHorizontalFacing().getDirectionVec().getZ();
            
            for (int i = 0; i < (int)expandLength.getInput(); i++) {
                if (search(blockPosition.add(x * i, 0, z * i), false, area, false)) {
                    return;
                }
            }
            return;
        }
        
        int horizontal = scaffoldMode.getInput() == 3 ? 5 : 
            (allowClutching.isToggled() ? (int)horizontalClutchBlocks.getInput() : 1);
        int vertical = scaffoldMode.getInput() == 3 ? 3 : 
            (allowClutching.isToggled() ? (int)verticalClutchBlocks.getInput() : 1);
        
        List<BlockPos> candidates = BlockUtils.getAllInBox(
            blockPosition.add(-horizontal, 0, -horizontal),
            blockPosition.add(horizontal, -vertical, horizontal)
        );
        
        candidates.sort(Comparator.comparingDouble(pos -> 
            BlockUtils.getBlock(pos).getBlockBoundsMaxY() - BlockUtils.getBlock(pos).getBlockBoundsMinY()
        ));
        
        for (BlockPos pos : candidates) {
            if (canBeClicked(pos) || search(pos, !shouldGoDown(), area, shouldPlaceHorizontally())) {
                return;
            }
        }
    }
    
    private boolean search(BlockPos blockPosition, boolean raycast, boolean area, boolean horizontalOnly) {
        if (mc.thePlayer == null) return false;
        
        if (!BlockUtils.replaceable(blockPosition)) {
            if (autoF5.isToggled()) {
                mc.gameSettings.thirdPersonView = 0;
            }
            return false;
        } else {
            if (autoF5.isToggled() && mc.gameSettings.thirdPersonView != 1) {
                mc.gameSettings.thirdPersonView = 1;
            }
        }
        
        float maxReach = mc.playerController.getBlockReachDistance();
        Vec3 eyes = new Vec3(mc.thePlayer.posX, 
            mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), 
            mc.thePlayer.posZ);
        
        PlaceRotation bestPlaceRotation = null;
        
        for (EnumFacing side : EnumFacing.values()) {
            if (horizontalOnly && side.getAxis() == EnumFacing.Axis.Y) {
                continue;
            }
            
            BlockPos neighbor = blockPosition.offset(side);
            
            if (!canBeClicked(neighbor)) {
                continue;
            }
            
            PlaceRotation currPlaceRotation;
            
            if (!area || isGodBridgeEnabled()) {
                currPlaceRotation = findTargetPlace(
                    blockPosition, neighbor, new Vec3(0.5, 0.5, 0.5), 
                    side, eyes, maxReach, raycast
                );
                if (currPlaceRotation == null) continue;
                bestPlaceRotation = compareDifferences(currPlaceRotation, bestPlaceRotation);
            } else {
                for (double x = 0.1; x <= 0.9; x += 0.1) {
                    for (double y = 0.1; y <= 0.9; y += 0.1) {
                        for (double z = 0.1; z <= 0.9; z += 0.1) {
                            currPlaceRotation = findTargetPlace(
                                blockPosition, neighbor, new Vec3(x, y, z),
                                side, eyes, maxReach, raycast
                            );
                            if (currPlaceRotation == null) continue;
                            bestPlaceRotation = compareDifferences(currPlaceRotation, bestPlaceRotation);
                        }
                    }
                }
            }
        }
        
        if (bestPlaceRotation == null) return false;
        
        if (rotationsActive() && !isGodBridgeEnabled()) {
            Rotation currentRot = getCurrentRotation();
            float rotationDifference = rotationDifference(bestPlaceRotation.rotation, currentRot);
            float rotationDifference2 = rotationDifference(
                new Rotation(bestPlaceRotation.rotation.yaw / 90f, bestPlaceRotation.rotation.pitch / 90f),
                new Rotation(currentRot.yaw / 90f, currentRot.pitch / 90f)
            );
            
            // Block safe check would go here with simulated player
            setRotation(bestPlaceRotation.rotation, 
                scaffoldMode.getInput() == 3 ? 1 : (int)resetTicks.getInput());
        }
        
        this.placeRotation = bestPlaceRotation;
        return true;
    }
    
    private PlaceRotation findTargetPlace(BlockPos pos, BlockPos offsetPos, Vec3 vec3, 
                                         EnumFacing side, Vec3 eyes, float maxReach, boolean raycast) {
        if (mc.theWorld == null) return null;
        
        Vec3 vec = new Vec3(pos).addVector(
            side.getDirectionVec().getX() * vec3.xCoord,
            side.getDirectionVec().getY() * vec3.yCoord,
            side.getDirectionVec().getZ() * vec3.zCoord
        );
        
        double distance = eyes.distanceTo(vec);
        
        if (raycast && (distance > maxReach || 
            mc.theWorld.rayTraceBlocks(eyes, vec, false, true, false) != null)) {
            return null;
        }
        
        Vec3 diff = vec.subtract(eyes);
        
        if (side.getAxis() != EnumFacing.Axis.Y) {
            double dist = Math.abs(side.getAxis() == EnumFacing.Axis.Z ? diff.zCoord : diff.xCoord);
            if (dist < minDist.getInput() && scaffoldMode.getInput() != 3) {
                return null;
            }
        }
        
        Rotation rotation = toRotation(vec, false);
        
        float roundYaw90 = Math.round(rotation.yaw / 90f) * 90f;
        float roundYaw45 = Math.round(rotation.yaw / 45f) * 45f;
        
        switch ((int)rotationMode.getInput()) {
            case 2: // Stabilized
                rotation = new Rotation(roundYaw45, rotation.pitch);
                break;
            case 3: // ReverseYaw
                rotation = new Rotation(
                    !isLookingDiagonally() ? roundYaw90 : roundYaw45, 
                    rotation.pitch
                );
                break;
        }
        
        // Check if current rotation already looks at target
        MovingObjectPosition currentRaytrace = performBlockRaytrace(getCurrentRotation(), maxReach);
        if (currentRaytrace != null && currentRaytrace.getBlockPos().equals(offsetPos) &&
            (!raycast || currentRaytrace.sideHit == side.getOpposite())) {
            return new PlaceRotation(
                new PlaceInfo(currentRaytrace.getBlockPos(), side.getOpposite(), 
                    modifyVec(currentRaytrace.hitVec, side, new Vec3(offsetPos), !raycast)),
                getCurrentRotation()
            );
        }
        
        MovingObjectPosition raytrace = performBlockRaytrace(rotation, maxReach);
        if (raytrace == null) return null;
        
        int multiplier = legitimize.isToggled() ? 3 : 1;
        
        if (raytrace.getBlockPos().equals(offsetPos) && 
            (!raycast || raytrace.sideHit == side.getOpposite()) &&
            canUpdateRotation(getCurrentRotation(), rotation, multiplier)) {
            return new PlaceRotation(
                new PlaceInfo(raytrace.getBlockPos(), side.getOpposite(),
                    modifyVec(raytrace.hitVec, side, new Vec3(offsetPos), !raycast)),
                rotation
            );
        }
        
        return null;
    }
    
    private void place(PlaceInfo placeInfo) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        double delayMinVal = delayMin.getInput();
        double delayMaxVal = delayMax.getInput();
        long delay = randomRange((long)delayMinVal, (long)delayMaxVal);
        if (placeDelayValue.isToggled() && !delayTimer.hasTimeElapsed(delay) || 
            (shouldKeepLaunchPosition() && launchY - 1 != (int)placeInfo.vec3.yCoord && scaffoldMode.getInput() != 2)) {
            return;
        }
        
        int currentSlot = SlotHandler.getCurrentSlot();
        ItemStack stack = mc.thePlayer.inventory.mainInventory[currentSlot];
        
        if (stack == null || !(stack.getItem() instanceof ItemBlock) || 
            ((ItemBlock)stack.getItem()).getBlock() instanceof BlockBush || 
            stack.stackSize <= 0 || sortByHighestAmount.isToggled() || earlySwitch.isToggled()) {
            
            int blockSlot;
            if (sortByHighestAmount.isToggled()) {
                blockSlot = findLargestBlockStackInHotbar();
            } else if (earlySwitch.isToggled()) {
                blockSlot = findBlockStackInHotbarGreaterThan((int)amountBeforeSwitch.getInput());
                if (blockSlot == -1) {
                    blockSlot = findBlockInHotbar();
                }
            } else {
                blockSlot = findBlockInHotbar();
            }
            
            if (blockSlot == -1) return;
            
            stack = mc.thePlayer.inventory.mainInventory[blockSlot];
            
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                ItemBlock itemBlock = (ItemBlock)stack.getItem();
                if (!itemBlock.canPlaceBlockOnSide(mc.theWorld, placeInfo.blockPos, 
                    placeInfo.enumFacing, mc.thePlayer, stack)) {
                    return;
                }
            }
            
            if (autoBlock.getInput() != 0) {
                if (autoBlock.getInput() == 1) { // Pick
                    mc.thePlayer.inventory.currentItem = blockSlot;
                } else if (autoBlock.getInput() == 2) { // Spoof
                    SlotHandler.setCurrentSlot(blockSlot);
                }
            }
        }
        
        tryToPlaceBlock(stack, placeInfo.blockPos, placeInfo.enumFacing, placeInfo.vec3);
        
        if (autoBlock.getInput() == 3) { // Switch
            SlotHandler.setCurrentSlot(currentSlot);
        }
        
        findBlockToSwitchNextTick(stack);
    }
    
    private boolean tryToPlaceBlock(ItemStack stack, BlockPos clickPos, EnumFacing side, Vec3 hitVec) {
        if (mc.thePlayer == null) return false;
        
        int prevSize = stack.stackSize;
        
        boolean clickedSuccessfully = mc.playerController.onPlayerRightClick(
            mc.thePlayer, mc.theWorld, stack, clickPos, side, hitVec
        );
        
        if (clickedSuccessfully) {
            delayTimer.reset();
            
            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionX *= speedModifier.getInput();
                mc.thePlayer.motionZ *= speedModifier.getInput();
            }
            
            if (swing.isToggled()) {
                mc.thePlayer.swingItem();
            } else {
                mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
            }
            
            if (!jumpAutomatically.isToggled() && scaffoldMode.getInput() == 4) {
                blocksPlacedUntilJump++;
            }
            
            updatePlacedBlocksForTelly();
            
            if (stack.stackSize <= 0) {
                mc.thePlayer.inventory.mainInventory[SlotHandler.getCurrentSlot()] = null;
            } else if (stack.stackSize != prevSize || !mc.playerController.isNotCreative()) {
                mc.entityRenderer.itemRenderer.resetEquippedProgress();
            }
            
            placeRotation = null;
            placedBlocksWithoutEagle++;
        }
        
        return clickedSuccessfully;
    }
    
    private boolean doPlaceAttempt(MovingObjectPosition raytrace, boolean lastClick) {
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        
        ItemStack stack = mc.thePlayer.inventory.mainInventory[SlotHandler.getCurrentSlot()];
        if (stack == null || !(stack.getItem() instanceof ItemBlock)) {
            return false;
        }
        
        ItemBlock block = (ItemBlock)stack.getItem();
        if (block.getBlock() instanceof BlockBush) {
            return false;
        }
        
        if (raytrace == null) return false;
        
        boolean canPlaceOnUpperFace = block.canPlaceBlockOnSide(
            mc.theWorld, raytrace.getBlockPos(), EnumFacing.UP, mc.thePlayer, stack
        );
        
        boolean shouldPlace;
        if (placementAttempt.getInput() == 0) { // Fail
            shouldPlace = !block.canPlaceBlockOnSide(
                mc.theWorld, raytrace.getBlockPos(), raytrace.sideHit, mc.thePlayer, stack
            );
        } else { // Independent
            if (shouldKeepLaunchPosition()) {
                shouldPlace = raytrace.getBlockPos().getY() == launchY - 1 && !canPlaceOnUpperFace;
            } else if (shouldPlaceHorizontally()) {
                shouldPlace = !canPlaceOnUpperFace;
            } else {
                shouldPlace = raytrace.getBlockPos().getY() <= (int)mc.thePlayer.posY - 1 && 
                    !(raytrace.getBlockPos().getY() == (int)mc.thePlayer.posY - 1 && 
                      canPlaceOnUpperFace && raytrace.sideHit == EnumFacing.UP);
            }
        }
        
        if (raytrace.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !shouldPlace) {
            return false;
        }
        
        return tryToPlaceBlock(stack, raytrace.getBlockPos(), raytrace.sideHit, raytrace.hitVec);
    }
    
    // ========== HELPER METHODS ==========
    
    private void handleRotations() {
        if (mc.thePlayer == null) return;
        
        if (mc.thePlayer.ticksExisted == 1) {
            launchY = (int) Math.round(mc.thePlayer.posY);
        }
        
        int ticks = keepRotation.isToggled() ? 
            (scaffoldMode.getInput() == 3 ? 1 : (int)resetTicks.getInput()) :
            (isGodBridgeEnabled() ? (int)resetTicks.getInput() : 0);
        
        if (!isTowering && isGodBridgeEnabled() && rotationsActive()) {
            generateGodBridgeRotations(ticks);
            return;
        }
        
        if (rotationsActive() && placeRotation != null) {
            Rotation targetRot = placeRotation.rotation;
            if (resetTicks.getInput() != 0 || keepRotation.isToggled()) {
                setRotation(targetRot, ticks);
            }
        }
    }
    
    private void setRotation(Rotation rotation, int ticks) {
        if (mc.thePlayer == null) return;
        
        if (scaffoldMode.getInput() == 3 && (mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0)) {
            // Simplified air ticks check
            if (!mc.thePlayer.onGround && ticksUntilJump >= jumpTicks) {
                return;
            }
        }
        
        RotationHandler.setRotationYaw(rotation.yaw);
        RotationHandler.setRotationPitch(rotation.pitch);
        if (strictValue.isToggled()) {
            RotationHandler.setMoveFix(RotationHandler.MoveFix.Strict);
        } else {
            RotationHandler.setMoveFix(RotationHandler.MoveFix.Silent);
        }
    }
    
    private void handleEagle() {
        if (mc.thePlayer == null) return;
        
        double dif = 0.5;
        BlockPos blockPos = new BlockPos(mc.thePlayer).down();
        
        for (EnumFacing side : EnumFacing.values()) {
            if (side.getAxis() == EnumFacing.Axis.Y) continue;
            
            BlockPos neighbor = blockPos.offset(side);
            if (!BlockUtils.replaceable(neighbor)) continue;
            
            double calcDif = (side.getAxis() == EnumFacing.Axis.Z ? 
                Math.abs(neighbor.getZ() + 0.5 - mc.thePlayer.posZ) :
                Math.abs(neighbor.getX() + 0.5 - mc.thePlayer.posX)) - 0.5;
            
            if (calcDif < dif) {
                dif = calcDif;
            }
        }
        
        boolean eagleCondition;
        switch ((int)eagleMode.getInput()) {
            case 1: // OnGround
                eagleCondition = mc.thePlayer.onGround;
                break;
            case 2: // InAir
                eagleCondition = !mc.thePlayer.onGround;
                break;
            default: // Both
                eagleCondition = true;
                break;
        }
        
        boolean pressedOnKeyboard = org.lwjgl.input.Keyboard.isKeyDown(
            mc.gameSettings.keyBindSneak.getKeyCode()
        );
        
        boolean shouldEagle = eagleCondition && 
            (BlockUtils.replaceable(blockPos) || dif < edgeDistance.getInput()) || 
            pressedOnKeyboard;
        
        if (requestedStopSneak) {
            requestedStopSneak = false;
            if (!mc.thePlayer.onGround) {
                shouldEagle = pressedOnKeyboard;
            }
        }
        
        if (eagle.getInput() == 1) { // Silent
            if (eagleSneaking != shouldEagle) {
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(
                    mc.thePlayer,
                    shouldEagle ? C0BPacketEntityAction.Action.START_SNEAKING :
                                 C0BPacketEntityAction.Action.STOP_SNEAKING
                ));
                
                if (adjustedSneakSpeed.isToggled() && shouldEagle) {
                    mc.thePlayer.motionX *= eagleSpeed.getInput() / 0.3f;
                    mc.thePlayer.motionZ *= eagleSpeed.getInput() / 0.3f;
                }
            }
            eagleSneaking = shouldEagle;
        } else if (eagle.getInput() == 0) { // Normal
            ((KeyBindingAccessor)mc.gameSettings.keyBindSneak).setPressed(shouldEagle);
            eagleSneaking = shouldEagle;
        }
        
        placedBlocksWithoutEagle = 0;
    }
    
    private void generateGodBridgeRotations(int ticks) {
        if (mc.thePlayer == null) return;
        
        float direction = applyServerSide.isToggled() ? 
            (float)(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX) * 180.0 / Math.PI) + 180f :
            MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw);
        
        float movingYaw = Math.round(direction / 45) * 45;
        
        float[] steps45 = new float[]{-135f, -45f, 45f, 135f};
        boolean isMovingStraight = applyServerSide.isToggled() ? 
            movingYaw % 90 == 0f :
            (Arrays.binarySearch(steps45, movingYaw) >= 0 && 
             (mc.gameSettings.keyBindRight.isKeyDown() || mc.gameSettings.keyBindLeft.isKeyDown()));
        
        // Check if near edge (simplified)
        if (!mc.thePlayer.onGround) return;
        
        if (!(mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0)) {
            if (placeRotation != null) {
                float axisMovement = (float)(Math.floor(placeRotation.rotation.yaw / 90) * 90);
                float yaw = axisMovement + 45f;
                float pitch = 75f;
                setRotation(new Rotation(yaw, pitch), ticks);
                return;
            }
            if (!keepRotation.isToggled()) return;
        }
        
        Rotation rotation;
        if (isMovingStraight) {
            if (mc.thePlayer.onGround) {
                // Update isOnRightSide logic
                isOnRightSide = Math.floor(mc.thePlayer.posX + Math.cos(Math.toRadians(movingYaw)) * 0.5) != 
                    Math.floor(mc.thePlayer.posX) ||
                    Math.floor(mc.thePlayer.posZ + Math.sin(Math.toRadians(movingYaw)) * 0.5) != 
                    Math.floor(mc.thePlayer.posZ);
            }
            
            float side = applyServerSide.isToggled() ? (isOnRightSide ? 45f : -45f) : 0f;
            rotation = new Rotation(
                movingYaw + side,
                useOptimizedPitch.isToggled() ? 73.5f : (float)customGodPitch.getInput()
            );
        } else {
            rotation = new Rotation(movingYaw, 75.6f);
        }
        
        godBridgeTargetRotation = rotation;
        setRotation(rotation, ticks);
    }
    
    private void updatePlacedBlocksForTelly() {
        if (blocksUntilAxisChange > horizontalPlacements + verticalPlacements) {
            blocksUntilAxisChange = 0;
            horizontalPlacements = randomRange((int)horizontalPlacementsMin.getInput(), (int)horizontalPlacementsMax.getInput());
            verticalPlacements = randomRange((int)verticalPlacementsMin.getInput(), (int)verticalPlacementsMax.getInput());
            return;
        }
        blocksUntilAxisChange++;
    }
    
    private void findBlockToSwitchNextTick(ItemStack stack) {
        if (autoBlock.getInput() == 0 || autoBlock.getInput() == 3) return;
        
        int switchAmount = earlySwitch.isToggled() ? (int)amountBeforeSwitch.getInput() : 0;
        
        if (stack.stackSize > switchAmount) return;
        
        int switchSlot = earlySwitch.isToggled() ? 
            findBlockStackInHotbarGreaterThan((int)amountBeforeSwitch.getInput()) :
            findBlockInHotbar();
        
        if (switchSlot == -1) return;
        
        if (autoBlock.getInput() == 1) { // Pick
            mc.thePlayer.inventory.currentItem = switchSlot;
        } else if (autoBlock.getInput() == 2) { // Spoof
            SlotHandler.setCurrentSlot(switchSlot);
        }
    }
    
    // ========== UTILITY METHODS ==========
    
    private boolean shouldGoDown() {
        return down.isToggled() && !sameY.isToggled() && 
            GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) &&
            scaffoldMode.getInput() != 4 && scaffoldMode.getInput() != 3 &&
            blocksAmount() > 1;
    }
    
    private boolean shouldKeepLaunchPosition() {
        return sameY.isToggled() && shouldJumpOnInput() && scaffoldMode.getInput() != 4;
    }
    
    private boolean shouldJumpOnInput() {
        return !jumpOnUserInput.isToggled() || 
            (!mc.gameSettings.keyBindJump.isKeyDown() && 
             mc.thePlayer.posY >= launchY && !mc.thePlayer.onGround);
    }
    
    private boolean isEagleEnabled() {
        return eagle.getInput() != 2 && !shouldGoDown() && scaffoldMode.getInput() != 4;
    }
    
    private boolean isGodBridgeEnabled() {
        return scaffoldMode.getInput() == 4 || 
            (scaffoldMode.getInput() == 0 && rotationMode.getInput() == 4);
    }
    
    private boolean rotationsActive() {
        return rotationMode.getInput() != 0;
    }
    
    private boolean shouldPlaceHorizontally() {
        return scaffoldMode.getInput() == 3 && (mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0) &&
            ((startHorizontally.isToggled() && blocksUntilAxisChange <= horizontalPlacements) ||
             (!startHorizontally.isToggled() && blocksUntilAxisChange > verticalPlacements));
    }
    
    private boolean isLookingDiagonally() {
        if (mc.thePlayer == null) return false;
        
        float directionDegree = (float)(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX) * 180.0 / Math.PI);
        float yaw = Math.round(Math.abs(MathHelper.wrapAngleTo180_float(directionDegree)) / 45f) * 45f;
        
        boolean isYawDiagonal = yaw % 90 != 0f;
        boolean isMovingDiagonal = mc.thePlayer.movementInput.moveForward != 0f && 
            mc.thePlayer.movementInput.moveStrafe == 0f;
        boolean isStrafing = mc.gameSettings.keyBindRight.isKeyDown() || 
            mc.gameSettings.keyBindLeft.isKeyDown();
        
        return isYawDiagonal && (isMovingDiagonal || isStrafing);
    }
    
    private boolean canBeClicked(BlockPos pos) {
        Block block = BlockUtils.getBlock(pos);
        return !(block instanceof BlockAir) && 
            !block.getMaterial().isReplaceable() && 
            !block.getMaterial().isLiquid();
    }
    
    private int blocksAmount() {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                count += stack.stackSize;
            }
        }
        return count;
    }
    
    private int findBlockInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                Block block = ((ItemBlock)stack.getItem()).getBlock();
                if (!(block instanceof BlockAir) && !BlockUtils.notFull(block) && block.isFullCube()) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    private int findLargestBlockStackInHotbar() {
        int bestSlot = -1;
        int bestCount = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                Block block = ((ItemBlock)stack.getItem()).getBlock();
                if (!(block instanceof BlockAir) && !BlockUtils.notFull(block) && block.isFullCube()) {
                    if (stack.stackSize > bestCount) {
                        bestCount = stack.stackSize;
                        bestSlot = i;
                    }
                }
            }
        }
        return bestSlot;
    }
    
    private int findBlockStackInHotbarGreaterThan(int amount) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                Block block = ((ItemBlock)stack.getItem()).getBlock();
                if (!(block instanceof BlockAir) && !BlockUtils.notFull(block) && block.isFullCube() &&
                    stack.stackSize > amount) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    private MovingObjectPosition performBlockRaytrace(Rotation rotation, float maxReach) {
        if (mc.thePlayer == null || mc.theWorld == null) return null;
        
        Vec3 eyes = new Vec3(mc.thePlayer.posX, 
            mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), 
            mc.thePlayer.posZ);
        
        Vec3 rotationVec = RotationUtils.getVectorForRotation(rotation.pitch, rotation.yaw);
        Vec3 reach = eyes.addVector(
            rotationVec.xCoord * maxReach,
            rotationVec.yCoord * maxReach,
            rotationVec.zCoord * maxReach
        );
        
        return mc.theWorld.rayTraceBlocks(eyes, reach, false, false, true);
    }
    
    private Rotation getCurrentRotation() {
        return new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
    }
    
    private Rotation toRotation(Vec3 vec, boolean includeY) {
        double dx = vec.xCoord - mc.thePlayer.posX;
        double dy = vec.yCoord - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = vec.zCoord - mc.thePlayer.posZ;
        double dist = MathHelper.sqrt_double(dx * dx + dz * dz);
        float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-(Math.atan2(dy, dist) * 180.0 / Math.PI));
        return new Rotation(yaw, pitch);
    }
    
    private float rotationDifference(Rotation a, Rotation b) {
        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(a.yaw - b.yaw));
        float pitchDiff = Math.abs(a.pitch - b.pitch);
        return (float)Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }
    
    private boolean canUpdateRotation(Rotation current, Rotation target, int multiplier) {
        float diff = rotationDifference(current, target);
        return diff < 180f / multiplier;
    }
    
    private PlaceRotation compareDifferences(PlaceRotation newRot, PlaceRotation oldRot) {
        if (oldRot == null) return newRot;
        Rotation current = getCurrentRotation();
        if (rotationDifference(newRot.rotation, current) < rotationDifference(oldRot.rotation, current)) {
            return newRot;
        }
        return oldRot;
    }
    
    private Vec3 modifyVec(Vec3 original, EnumFacing direction, Vec3 pos, boolean shouldModify) {
        if (!shouldModify) return original;
        
        double x = original.xCoord;
        double y = original.yCoord;
        double z = original.zCoord;
        
        EnumFacing side = direction.getOpposite();
        
        switch (side.getAxis()) {
            case Y:
                return new Vec3(x, pos.yCoord + Math.max(side.getDirectionVec().getY(), 0), z);
            case X:
                return new Vec3(pos.xCoord + Math.max(side.getDirectionVec().getX(), 0), y, z);
            case Z:
                return new Vec3(x, y, pos.zCoord + Math.max(side.getDirectionVec().getZ(), 0));
            default:
                return original;
        }
    }
    
    private int randomRange(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    
    private long randomRange(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }
    
    private float randomRange(float min, float max) {
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }
    
    private int randomClickDelay(int min, int max) {
        if (min >= max) return min;
        int cps = randomRange(min, max);
        return 1000 / cps;
    }
    
    // ========== INNER CLASSES ==========
    
    private static class PlaceRotation {
        final PlaceInfo placeInfo;
        final Rotation rotation;
        
        PlaceRotation(PlaceInfo placeInfo, Rotation rotation) {
            this.placeInfo = placeInfo;
            this.rotation = rotation;
        }
    }
    
    private static class PlaceInfo {
        final BlockPos blockPos;
        final EnumFacing enumFacing;
        final Vec3 vec3;
        
        PlaceInfo(BlockPos blockPos, EnumFacing enumFacing, Vec3 vec3) {
            this.blockPos = blockPos;
            this.enumFacing = enumFacing;
            this.vec3 = vec3;
        }
    }
    
    private static class Rotation {
        final float yaw;
        final float pitch;
        
        Rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
    
    private static class ExtraClickInfo {
        final int delay;
        final long lastClick;
        int clicks;
        
        ExtraClickInfo(int delay, long lastClick, int clicks) {
            this.delay = delay;
            this.lastClick = lastClick;
            this.clicks = clicks;
        }
    }
}

package keystrokesmod.module.impl.world;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.RotationEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.event.SlotUpdateEvent;
import keystrokesmod.helper.ScaffoldBlockCountHelper;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.other.RotationHandler;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ContainerUtils;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.scaffold.ScaffoldUtils;
import net.minecraft.block.BlockTNT;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Scaffold extends Module {
    private static final String[] ROTATION_MODES = {"Disabled", "Simple", "Offset", "Precise"};
    private static final String[] SPRINT_MODES = {"Disabled", "Vanilla", "Float"};
    private static final String[] FAST_SCAFFOLD_MODES = {"Disabled", "Jump B", "Jump C", "Jump D", "Keep-Y A", "Keep-Y B", "Jump A"};
    private static final String[] MULTI_PLACE_MODES = {"Disabled", "1 extra", "2 extra"};

    private final SliderSetting motion;
    public final ModeSetting rotation;
    private final ModeSetting sprint;
    private final ModeSetting fastScaffold;
    private final ModeSetting multiPlace;
    public final ButtonSetting autoSwap;
    private final ButtonSetting cancelKnockBack;
    private final ButtonSetting fastOnRMB;
    public final ButtonSetting highlightBlocks;
    private final ButtonSetting jumpFacingForward;
    public final ButtonSetting safeWalk;
    public final ButtonSetting showBlockCount;
    private final ButtonSetting silentSwing;

    public final Map<BlockPos, Timer> highlight = new HashMap<>();
    private ScaffoldBlockCountHelper scaffoldBlockCount;
    public final AtomicInteger lastSlot = new AtomicInteger(-1);

    public boolean hasSwapped;
    public boolean moduleEnabled;
    public boolean isEnabled;
    public boolean canBlockFade;
    public boolean lowhop;

    public ButtonSetting tower;
    public SliderSetting strafe;
    public MovingObjectPosition placeBlock;
    public MovingObjectPosition rayCasted;
    public boolean place;
    public int offGroundTicks;
    public int onGroundTicks;
    public float placeYaw;
    public float placePitch = 85F;

    private int currentFace = 1;

    private final ScaffoldSessionState state;
    private final ScaffoldPlacementPlanner placementPlanner;
    private final ScaffoldPlacementExecutor placementExecutor;
    private final ScaffoldMovementController movementController;
    private final ScaffoldRotationController rotationController;

    private final double[] speedLevels = {0.48, 0.5, 0.52, 0.58, 0.68};
    private final double[] floatSpeedLevels = {0.2, 0.22, 0.28, 0.29, 0.3};

    public Scaffold() {
        super("Scaffold", category.world);
        this.registerSetting(motion = new SliderSetting("Motion", 100, 50, 150, 1, "%"));
        this.registerSetting(rotation = new ModeSetting("Rotation", ROTATION_MODES, 1));
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
        this.registerSetting(tower = new ButtonSetting("Tower", false));
        this.registerSetting(strafe = new SliderSetting("Strafe", 0, -45, 45, 5));

        this.state = new ScaffoldSessionState();
        this.placementPlanner = new ScaffoldPlacementPlanner(this, state);
        this.placementExecutor = new ScaffoldPlacementExecutor(this, state);
        this.movementController = new ScaffoldMovementController(this, state);
        this.rotationController = new ScaffoldRotationController(this, state, movementController);
    }

    @Override
    public void onEnable() {
        moduleEnabled = true;
        isEnabled = true;
        hasSwapped = false;
        place = false;
        placeBlock = null;
        rayCasted = null;
        lowhop = false;
        canBlockFade = false;
        offGroundTicks = mc.thePlayer.onGround ? 0 : 1;
        onGroundTicks = mc.thePlayer.onGround ? 1 : 0;
        ScaffoldUtils.fadeEdge = 0;

        if (scaffoldBlockCount != null) {
            scaffoldBlockCount.onDisable();
        }
        scaffoldBlockCount = new ScaffoldBlockCountHelper(mc);
        FMLCommonHandler.instance().bus().register(scaffoldBlockCount);

        lastSlot.set(-1);
        state.resetForEnable(RotationHandler.getRotationYaw(), !mc.thePlayer.onGround);
        rotationController.resetForEnable();
        syncPublicState();
    }

    @Override
    public void onDisable() {
        moduleEnabled = false;
        isEnabled = false;
        hasSwapped = false;
        place = false;
        placeBlock = null;
        rayCasted = null;
        lowhop = false;
        canBlockFade = false;

        if (lastSlot.get() != -1) {
            mc.thePlayer.inventory.currentItem = lastSlot.get();
            lastSlot.set(-1);
        }

        if (scaffoldBlockCount != null) {
            scaffoldBlockCount.beginFade();
            scaffoldBlockCount = null;
        }

        state.resetAfterDisable();
        syncPublicState();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(MouseEvent event) {
        if (!isModuleActive()) {
            return;
        }
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (!isModuleActive()) {
            return;
        }

        ScaffoldUtils.updateTicks();
        if (mc.thePlayer.onGround) {
            onGroundTicks++;
            offGroundTicks = 0;
        } else {
            offGroundTicks++;
            onGroundTicks = 0;
        }

        movementController.onPreMotion();
        movementController.handleFastScaffoldJump();
        movementController.handleFloat(event);
        placementPlanner.updateBlockRotations();
        rotationController.applyPreMotion(event);
        movementController.afterRotationApplied();
        syncPublicState();
    }

    @SubscribeEvent
    public void onRotation(RotationEvent event) {
        if (!isModuleActive()) {
            return;
        }
        rotationController.applyMoveFix(event);
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent event) {
        if (!isModuleActive()) {
            return;
        }
        if (event.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            currentFace = ((C08PacketPlayerBlockPlacement) event.getPacket()).getPlacedBlockDirection();
        }
    }

    @SubscribeEvent
    public void onSlotUpdate(SlotUpdateEvent event) {
        if (isModuleActive()) {
            lastSlot.set(event.slot);
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent event) {
        if (!isModuleActive()) {
            return;
        }

        place = false;
        if (ModuleManager.longJump != null && ModuleManager.longJump.isEnabled()) {
            state.startYPos = -1;
        }

        if (holdingBlocks() && setSlot()) {
            hasSwapped = true;
            if (rotationController.canSchedulePlace() && (rotation.getInput() == 0 || state.rotationDelay == 0)) {
                placeBlock(0, 0);
            } else if (state.currentPlacement == null) {
                placementPlanner.preparePlacement(0, 0);
            }
            movementController.handleKeepYPlacement();
            movementController.handleMotionScale();
        } else {
            state.currentPlacement = null;
        }

        syncPublicState();
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        if (!isModuleActive() || !cancelKnockBack.isToggled()) {
            return;
        }

        if (event.getPacket() instanceof S12PacketEntityVelocity
                && ((S12PacketEntityVelocity) event.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
            event.setCanceled(true);
        } else if (event.getPacket() instanceof S27PacketExplosion) {
            event.setCanceled(true);
        }
    }

    @Override
    public String getInfo() {
        String info;
        if (fastOnRMB.isToggled()) {
            info = Mouse.isButtonDown(1) && Utils.tabbedIn()
                    ? FAST_SCAFFOLD_MODES[(int) fastScaffold.getInput()]
                    : SPRINT_MODES[(int) sprint.getInput()];
        } else {
            info = fastScaffold.getInput() > 0
                    ? FAST_SCAFFOLD_MODES[(int) fastScaffold.getInput()]
                    : SPRINT_MODES[(int) sprint.getInput()];
        }
        return "§cDisabled".equals(info) ? "Disabled" : info;
    }

    public boolean stopFastPlace() {
        return this.isEnabled();
    }

    public void rotateForward() {
        state.rotateForward = true;
        state.rotatingForward = false;
    }

    public boolean sprint() {
        if (!isModuleActive()) {
            return false;
        }
        return handleFastScaffolds() > 0 || !holdingBlocks();
    }

    public boolean shouldDisableSprintForIntave() {
        return false;
    }

    private int handleFastScaffolds() {
        if (fastOnRMB.isToggled()) {
            return Mouse.isButtonDown(1) && Utils.tabbedIn() ? (int) fastScaffold.getInput() : (int) sprint.getInput();
        }
        return fastScaffold.getInput() > 0 ? (int) fastScaffold.getInput() : (int) sprint.getInput();
    }

    public boolean usingFastScaffold() {
        boolean speedEnabled = ModuleManager.speed != null && ModuleManager.speed.isEnabled();
        return fastScaffold.getInput() > 0 && (!fastOnRMB.isToggled() || (Mouse.isButtonDown(1) || speedEnabled) && Utils.tabbedIn());
    }

    public boolean canSafewalk() {
        return safeWalk.isToggled() && !usingFastScaffold() && !ModuleManager.tower.canTower() && isModuleActive();
    }

    public boolean stopRotation() {
        return this.isEnabled() && rotation.getInput() > 0;
    }

    private void place(PlaceData placeData) {
        placementExecutor.place(placeData, false);
        syncPublicState();
    }

    public void place(MovingObjectPosition block, boolean extra) {
        if (block == null || block.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }

        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)
                || !ContainerUtils.canBePlaced((ItemBlock) heldItem.getItem())) {
            return;
        }

        PlaceData placeData = new PlaceData(block.getBlockPos(), block.sideHit);
        placeData.hitVec = block.hitVec != null
                ? block.hitVec
                : new Vec3(block.getBlockPos().getX() + 0.5D, block.getBlockPos().getY() + 0.5D, block.getBlockPos().getZ() + 0.5D);
        placementExecutor.place(placeData, extra);
        syncPublicState();
    }

    public int totalBlocks() {
        int totalBlocks = 0;
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBlock
                    && ContainerUtils.canBePlaced((ItemBlock) stack.getItem())
                    && stack.stackSize > 0) {
                totalBlocks += stack.stackSize;
            }
        }
        return totalBlocks;
    }

    public void placeBlock(int yOffset, int xOffset) {
        PlaceData placeData = placementPlanner.preparePlacement(yOffset, xOffset);
        syncPublicState();
        if (placeData == null) {
            return;
        }

        state.lastPlacement = placeData;
        place(placeData);

        if (sprint.getInput() == 0 && mc.thePlayer.onGround && !ModuleManager.tower.canTower() && !usingFastScaffold()) {
            return;
        }

        int extraPlacements = (int) multiPlace.getInput();
        for (int i = 0; i < extraPlacements; i++) {
            PlaceData extraPlace = placementPlanner.preparePlacement(yOffset, xOffset);
            if (extraPlace == null) {
                return;
            }
            state.lastPlacement = extraPlace;
            placementExecutor.place(extraPlace, i > 0);
        }
        syncPublicState();
    }

    public int getCurrentFace() {
        return currentFace;
    }

    public int getFastScaffoldMode() {
        return (int) fastScaffold.getInput();
    }

    public int getSprintMode() {
        return (int) sprint.getInput();
    }

    public double getMotionPercent() {
        return motion.getInput();
    }

    public boolean jumpFacingForwardEnabled() {
        return jumpFacingForward.isToggled();
    }

    public boolean silentSwingEnabled() {
        return silentSwing.isToggled();
    }

    public int getSpeedLevel() {
        for (PotionEffect potionEffect : mc.thePlayer.getActivePotionEffects()) {
            if ("potion.moveSpeed".equals(potionEffect.getEffectName())) {
                return potionEffect.getAmplifier() + 1;
            }
        }
        return 0;
    }

    public double getSpeed(int speedLevel) {
        return speedLevels[Math.min(speedLevels.length - 1, Math.max(0, speedLevel))];
    }

    public double getFloatSpeed(int speedLevel) {
        double diagonalPenalty = mc.thePlayer.moveStrafing != 0 && mc.thePlayer.moveForward != 0 ? 0.003 : 0;
        double value = floatSpeedLevels[Math.min(floatSpeedLevels.length - 1, Math.max(0, speedLevel))] - diagonalPenalty;
        return value * (motion.getInput() / 100.0);
    }

    public float hardcodedYaw() {
        float simpleYaw = 0F;
        float threshold = 0.8F;

        if (mc.thePlayer.moveForward >= threshold) {
            simpleYaw -= 180F;
            if (mc.thePlayer.moveStrafing >= threshold) {
                simpleYaw += 45F;
            }
            if (mc.thePlayer.moveStrafing <= -threshold) {
                simpleYaw -= 45F;
            }
        } else if (mc.thePlayer.moveForward == 0) {
            simpleYaw -= 180F;
            if (mc.thePlayer.moveStrafing >= threshold) {
                simpleYaw += 90F;
            }
            if (mc.thePlayer.moveStrafing <= -threshold) {
                simpleYaw -= 90F;
            }
        } else if (mc.thePlayer.moveForward <= -threshold) {
            if (mc.thePlayer.moveStrafing >= threshold) {
                simpleYaw -= 45F;
            }
            if (mc.thePlayer.moveStrafing <= -threshold) {
                simpleYaw += 45F;
            }
        }

        return simpleYaw;
    }

    public boolean holdingBlocks() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (!autoSwap.isToggled() || getSlot() == -1) {
            return heldItem != null
                    && heldItem.getItem() instanceof ItemBlock
                    && ContainerUtils.canBePlaced((ItemBlock) heldItem.getItem());
        }
        return true;
    }

    public double getMovementAngle() {
        double angle = Math.toDegrees(Math.atan2(-mc.thePlayer.moveStrafing, mc.thePlayer.moveForward));
        return angle == -0D ? 0D : angle;
    }

    public static int getSlot() {
        int slot = -1;
        int highestStack = -1;
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null
                    && itemStack.getItem() instanceof ItemBlock
                    && ContainerUtils.canBePlaced((ItemBlock) itemStack.getItem())
                    && itemStack.stackSize > 0) {
                if (Utils.getBedwarsStatus() == 2 && ((ItemBlock) itemStack.getItem()).getBlock() instanceof BlockTNT) {
                    continue;
                }
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

        if (state.blockSlot == -1) {
            state.blockSlot = slot;
        }
        if (lastSlot.get() == -1) {
            lastSlot.set(mc.thePlayer.inventory.currentItem);
        }
        if (autoSwap.isToggled()) {
            mc.thePlayer.inventory.currentItem = slot;
        }

        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)
                || !ContainerUtils.canBePlaced((ItemBlock) heldItem.getItem())) {
            state.blockSlot = -1;
            return false;
        }
        return true;
    }

    public static boolean isDiagonal() {
        return ScaffoldUtils.scaffoldDiagonal(false);
    }

    public float getYaw() {
        return state.scaffoldYaw;
    }

    public boolean keepYPosition() {
        return state.fastScaffoldKeepY;
    }

    public boolean isGrimLegitMotion() {
        return false;
    }

    private boolean isModuleActive() {
        return Utils.nullCheck() && isEnabled;
    }

    private void syncPublicState() {
        canBlockFade = state.canBlockFade;
        lowhop = state.lowhop;
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

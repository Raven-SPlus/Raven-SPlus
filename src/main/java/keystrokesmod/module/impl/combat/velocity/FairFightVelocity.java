package keystrokesmod.module.impl.combat.velocity;

import keystrokesmod.Raven;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.PreVelocityEvent;
import keystrokesmod.event.PostVelocityEvent;
import keystrokesmod.module.impl.combat.Velocity;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.module.setting.utils.ModeOnly;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.MoveUtil;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * FairFight Anti-Cheat Velocity Bypass
 * 
 * Exploits:
 * 1. VelocityB only checks ticks 1-4, so we delay reduction to tick 5+
 * 2. Wall proximity resets predictedXZ to 0, completely disabling the check
 * 3. VelocityD is disabled when tickSinceNearWall < 3
 * 4. Sprint+jump adds 0.2 tolerance per tick (ab)used for extra reduction
 */
public class FairFightVelocity extends SubMode<Velocity> {
    private final DescriptionSetting desc = new DescriptionSetting("Bypasses FairFight's velocity checks");
    
    private final ModeSetting mode;
    private final SliderSetting horizontal;
    private final SliderSetting vertical;
    private final SliderSetting delayTicks;
    private final ButtonSetting wallAbuse;
    private final ButtonSetting sprintJumpAbuse;
    private final SliderSetting jumpResetChance;
    private final ButtonSetting reduceGradually;
    private final SliderSetting reductionSteps;
    private final ButtonSetting onlyHorizontal;
    
    // Jump settings (like LegitVelocity)
    private final ButtonSetting jump;
    private final ButtonSetting jumpInInv;
    private final ModeSetting jumpDelayMode;
    private final SliderSetting jumpMinDelay;
    private final SliderSetting jumpMaxDelay;
    private final SliderSetting jumpChance;
    private final ButtonSetting ignoreLiquid;
    
    // State tracking
    private int ticksSinceVelocity = 100;
    private double pendingMotionX = 0;
    private double pendingMotionY = 0;
    private double pendingMotionZ = 0;
    private boolean hasStoredVelocity = false;
    private int reductionStep = 0;
    private boolean shouldJump = false;
    
    private static final String[] MODES = {"Delay", "Wall Abuse", "Combined"};
    
    public FairFightVelocity(String name, @NotNull Velocity parent) {
        super(name, parent);
        this.registerSetting(desc);
        this.registerSetting(mode = new ModeSetting("Mode", MODES, 2));
        this.registerSetting(horizontal = new SliderSetting("Horizontal", 0.0, 0.0, 100.0, 1.0, "%"));
        this.registerSetting(vertical = new SliderSetting("Vertical", 100.0, 0.0, 100.0, 1.0, "%"));
        this.registerSetting(delayTicks = new SliderSetting("Delay ticks", 5, 4, 10, 1, 
                "FF checks ticks 1-4 only", new ModeOnly(mode, 0, 2)));
        this.registerSetting(wallAbuse = new ButtonSetting("Wall proximity abuse", true, 
                "Move towards wall to reset predicted velocity"));
        this.registerSetting(sprintJumpAbuse = new ButtonSetting("Sprint jump abuse", true,
                "Jump while sprinting for +0.2 tolerance"));
        this.registerSetting(jumpResetChance = new SliderSetting("Jump reset chance", 70, 0, 100, 1, "%",
                sprintJumpAbuse::isToggled));
        this.registerSetting(reduceGradually = new ButtonSetting("Reduce gradually", false,
                "Spread reduction over multiple ticks"));
        this.registerSetting(reductionSteps = new SliderSetting("Reduction steps", 3, 2, 6, 1,
                reduceGradually::isToggled));
        this.registerSetting(onlyHorizontal = new ButtonSetting("Only horizontal", true,
                "Only reduce horizontal KB (safer)"));
        
        // Jump settings
        this.registerSetting(jump = new ButtonSetting("Jump", true, "Jump on velocity for extra reduction"));
        this.registerSetting(jumpInInv = new ButtonSetting("Jump in inv", false, jump::isToggled));
        this.registerSetting(jumpDelayMode = new ModeSetting("Jump delay mode", new String[]{"Delay", "Chance"}, 0, jump::isToggled));
        this.registerSetting(jumpMinDelay = new SliderSetting("Jump min delay", 0, 0, 150, 1, "ms", 
                () -> jump.isToggled() && jumpDelayMode.getInput() == 0));
        this.registerSetting(jumpMaxDelay = new SliderSetting("Jump max delay", 50, 0, 150, 1, "ms", 
                () -> jump.isToggled() && jumpDelayMode.getInput() == 0));
        this.registerSetting(jumpChance = new SliderSetting("Jump chance", 80, 0, 100, 1, "%", 
                () -> jump.isToggled() && jumpDelayMode.getInput() == 1));
        this.registerSetting(ignoreLiquid = new ButtonSetting("Ignore liquid", true, jump::isToggled));
    }
    
    @Override
    public void guiUpdate() {
        Utils.correctValue(jumpMinDelay, jumpMaxDelay);
    }
    
    @Override
    public void onDisable() {
        reset();
    }
    
    private void reset() {
        ticksSinceVelocity = 100;
        hasStoredVelocity = false;
        pendingMotionX = pendingMotionY = pendingMotionZ = 0;
        reductionStep = 0;
        shouldJump = false;
    }
    
    @SubscribeEvent
    public void onPreVelocity(@NotNull PreVelocityEvent event) {
        if (!Utils.nullCheck()) return;
        
        int modeVal = (int) mode.getInput();
        
        // Mode 0 (Delay) or Mode 2 (Combined): Store velocity and cancel, apply later
        if (modeVal == 0 || modeVal == 2) {
            // Store the raw velocity values
            pendingMotionX = event.getMotionX() / 8000.0;
            pendingMotionY = event.getMotionY() / 8000.0;
            pendingMotionZ = event.getMotionZ() / 8000.0;
            hasStoredVelocity = true;
            ticksSinceVelocity = 0;
            reductionStep = 0;
            
            // Cancel the velocity packet - we'll apply modified version later
            event.setMotionX(0);
            event.setMotionY(onlyHorizontal.isToggled() ? event.getMotionY() : 0);
            event.setMotionZ(0);
        }
        // Mode 1 (Wall Abuse): Apply reduction immediately if near wall
        else if (modeVal == 1) {
            if (isNearWall()) {
                // Wall resets predictedXZ to 0, so we can reduce freely
                event.setMotionX((int) (event.getMotionX() * horizontal.getInput() / 100));
                event.setMotionZ((int) (event.getMotionZ() * horizontal.getInput() / 100));
                if (!onlyHorizontal.isToggled()) {
                    event.setMotionY((int) (event.getMotionY() * vertical.getInput() / 100));
                }
            }
            ticksSinceVelocity = 0;
        }
    }
    
    @SubscribeEvent
    public void onPostVelocity(PostVelocityEvent event) {
        if (!Utils.nullCheck()) return;
        
        // Sprint jump abuse: jump on velocity for extra tolerance (old behavior, still works)
        if (sprintJumpAbuse.isToggled() && mc.thePlayer.onGround && mc.thePlayer.isSprinting()) {
            if (jumpResetChance.getInput() >= 100 || Math.random() * 100 < jumpResetChance.getInput()) {
                shouldJump = true;
            }
        }
        
        // LegitVelocity-style jump
        if (jump.isToggled()) {
            if (mc.thePlayer.maxHurtTime <= 0) return;
            if (ignoreLiquid.isToggled() && Utils.inLiquid()) return;
            
            switch ((int) jumpDelayMode.getInput()) {
                case 0: // Delay mode
                    long delay = (long) (Math.random() * (jumpMaxDelay.getInput() - jumpMinDelay.getInput()) + jumpMinDelay.getInput());
                    if (jumpMaxDelay.getInput() == 0 || delay == 0) {
                        if (canJump()) mc.thePlayer.jump();
                    } else {
                        Raven.getExecutor().schedule(() -> {
                            if (canJump()) mc.thePlayer.jump();
                        }, delay, TimeUnit.MILLISECONDS);
                    }
                    break;
                case 1: // Chance mode
                    if (jumpChance.getInput() >= 100 || Math.random() * 100 < jumpChance.getInput()) {
                        if (canJump()) mc.thePlayer.jump();
                    }
                    break;
            }
        }
    }
    
    private boolean canJump() {
        return mc.thePlayer != null && mc.thePlayer.onGround && (jumpInInv.isToggled() || mc.currentScreen == null);
    }
    
    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) return;
        
        ticksSinceVelocity++;
        
        // Handle sprint jump abuse
        if (shouldJump && mc.thePlayer.onGround) {
            mc.thePlayer.jump();
            shouldJump = false;
        }
        
        int modeVal = (int) mode.getInput();
        
        // Delayed velocity application (after FF's 4-tick check window)
        if (hasStoredVelocity && (modeVal == 0 || modeVal == 2)) {
            int delayTicksVal = (int) delayTicks.getInput();
            
            // Combined mode: also check wall proximity
            boolean wallNearby = modeVal == 2 && wallAbuse.isToggled() && isNearWall();
            
            // Apply velocity after the detection window OR if near wall
            if (ticksSinceVelocity >= delayTicksVal || wallNearby) {
                applyModifiedVelocity();
            }
        }
    }
    
    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (!Utils.nullCheck()) return;
        
        // Wall abuse mode: continuously try to move towards walls during velocity
        int modeVal = (int) mode.getInput();
        if ((modeVal == 1 || modeVal == 2) && wallAbuse.isToggled()) {
            if (ticksSinceVelocity > 0 && ticksSinceVelocity <= 4) {
                // During check window, try to brush a wall
                nudgeTowardsWall();
            }
        }
    }
    
    private void applyModifiedVelocity() {
        if (!hasStoredVelocity) return;
        
        double hMult = horizontal.getInput() / 100.0;
        double vMult = vertical.getInput() / 100.0;
        
        if (reduceGradually.isToggled()) {
            int totalSteps = (int) reductionSteps.getInput();
            if (reductionStep < totalSteps) {
                // Calculate partial reduction per step
                double stepProgress = (double) (reductionStep + 1) / totalSteps;
                double currentHMult = 1.0 - ((1.0 - hMult) * stepProgress);
                double currentVMult = 1.0 - ((1.0 - vMult) * stepProgress);
                
                if (reductionStep == 0) {
                    // First step: apply initial velocity with partial reduction
                    mc.thePlayer.motionX += pendingMotionX * currentHMult;
                    if (!onlyHorizontal.isToggled()) {
                        mc.thePlayer.motionY += pendingMotionY * currentVMult;
                    } else {
                        mc.thePlayer.motionY += pendingMotionY;
                    }
                    mc.thePlayer.motionZ += pendingMotionZ * currentHMult;
                } else {
                    // Subsequent steps: reduce existing motion
                    double reductionFactor = currentHMult / (1.0 - ((1.0 - hMult) * ((double) reductionStep / totalSteps)));
                    mc.thePlayer.motionX *= reductionFactor;
                    mc.thePlayer.motionZ *= reductionFactor;
                }
                
                reductionStep++;
                if (reductionStep >= totalSteps) {
                    hasStoredVelocity = false;
                }
            }
        } else {
            // Instant application with full reduction
            mc.thePlayer.motionX += pendingMotionX * hMult;
            if (!onlyHorizontal.isToggled()) {
                mc.thePlayer.motionY += pendingMotionY * vMult;
            } else {
                mc.thePlayer.motionY += pendingMotionY;
            }
            mc.thePlayer.motionZ += pendingMotionZ * hMult;
            hasStoredVelocity = false;
        }
    }
    
    /**
     * Check if player is near a wall (within ~0.3 blocks)
     * FairFight uses cuboid 0.300001 expansion for nearWall check
     */
    private boolean isNearWall() {
        if (mc.thePlayer == null) return false;
        
        double px = mc.thePlayer.posX;
        double py = mc.thePlayer.posY;
        double pz = mc.thePlayer.posZ;
        
        // Check blocks around player at feet and head level
        for (double yOff = 0; yOff <= 1.8; yOff += 0.9) {
            for (double xOff = -0.35; xOff <= 0.35; xOff += 0.7) {
                for (double zOff = -0.35; zOff <= 0.35; zOff += 0.7) {
                    if (xOff == 0 && zOff == 0) continue;
                    
                    BlockPos pos = new BlockPos(px + xOff, py + yOff, pz + zOff);
                    Block block = BlockUtils.getBlock(pos);
                    if (block != Blocks.air && !BlockUtils.isFluid(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Slightly adjust motion to move towards nearest wall during velocity check window
     * This triggers FairFight's nearWall condition, resetting predictedXZ to 0
     */
    private void nudgeTowardsWall() {
        if (mc.thePlayer == null || !MoveUtil.isMoving()) return;
        
        double px = mc.thePlayer.posX;
        double py = mc.thePlayer.posY;
        double pz = mc.thePlayer.posZ;
        
        // Find nearest wall direction
        double nearestDist = Double.MAX_VALUE;
        double nudgeX = 0, nudgeZ = 0;
        
        for (int xDir = -1; xDir <= 1; xDir++) {
            for (int zDir = -1; zDir <= 1; zDir++) {
                if (xDir == 0 && zDir == 0) continue;
                
                // Check for blocks in this direction
                for (double dist = 0.3; dist <= 2.0; dist += 0.5) {
                    BlockPos checkPos = new BlockPos(px + xDir * dist, py, pz + zDir * dist);
                    Block block = BlockUtils.getBlock(checkPos);
                    if (block != Blocks.air && !BlockUtils.isFluid(block)) {
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nudgeX = xDir * 0.02;
                            nudgeZ = zDir * 0.02;
                        }
                        break;
                    }
                }
            }
        }
        
        // Apply tiny nudge towards wall (won't significantly affect movement)
        if (nearestDist < 1.5) {
            mc.thePlayer.motionX += nudgeX;
            mc.thePlayer.motionZ += nudgeZ;
        }
    }
}

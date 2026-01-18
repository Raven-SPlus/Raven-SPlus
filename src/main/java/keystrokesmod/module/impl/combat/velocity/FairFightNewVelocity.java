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
 * FairFight Velocity bypass (updated).
 *
 * Key insight: VelocityD uses emulation that expects ORIGINAL velocity values.
 * If we modify the packet, emulation detects mismatch at tick 1.
 *
 * Solution:
 * - DON'T modify velocity packet at all (keep 100% initially)
 * - Wait until tick 9+ (VelocityD checks ticks 1-8, returns when ticks > 8)
 * - Only then reduce horizontal motion
 * - Wall assist can disable VelocityD earlier (if nearWall for 3+ ticks)
 */
public class FairFightNewVelocity extends SubMode<Velocity> {
    private final DescriptionSetting desc = new DescriptionSetting("Waits for VelocityD window to end before reducing");

    private final SliderSetting horizontal;
    private final SliderSetting vertical;
    private final SliderSetting reduceSteps;
    private final ButtonSetting wallAssist;
    private final ButtonSetting onlyReduceAfterD;
    
    // Jump settings (like LegitVelocity)
    private final ButtonSetting jump;
    private final ButtonSetting jumpInInv;
    private final ModeSetting jumpDelayMode;
    private final SliderSetting jumpMinDelay;
    private final SliderSetting jumpMaxDelay;
    private final SliderSetting jumpChance;
    private final ButtonSetting ignoreLiquid;

    private boolean active = false;
    private int ticksSinceVelocity = 100;
    private int wallTicks = 0;
    private boolean reductionStarted = false;
    private int reductionStep = 0;
    private double startMotionX = 0;
    private double startMotionZ = 0;

    public FairFightNewVelocity(String name, @NotNull Velocity parent) {
        super(name, parent);
        this.registerSetting(desc);
        this.registerSetting(horizontal = new SliderSetting("Horizontal", 0, 0, 100, 1, "%"));
        this.registerSetting(vertical = new SliderSetting("Vertical", 100, 0, 100, 1, "%"));
        this.registerSetting(reduceSteps = new SliderSetting("Reduce steps", 3, 1, 6, 1));
        this.registerSetting(wallAssist = new ButtonSetting("Wall assist", true,
                "Nudge toward walls to disable VelocityD earlier"));
        this.registerSetting(onlyReduceAfterD = new ButtonSetting("Only reduce after D", true,
                "Wait for VelocityD window (tick 8) before reducing"));
        
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
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        active = false;
        ticksSinceVelocity = 100;
        wallTicks = 0;
        reductionStarted = false;
        reductionStep = 0;
        startMotionX = startMotionZ = 0;
    }

    @SubscribeEvent
    public void onPreVelocity(PreVelocityEvent event) {
        if (!Utils.nullCheck()) return;

        // DON'T modify velocity packet - VelocityD emulation expects original values
        // Just track that we received velocity
        ticksSinceVelocity = 0;
        wallTicks = 0;
        active = true;
        reductionStarted = false;
        reductionStep = 0;
        
        // Only modify vertical if requested (vertical doesn't affect VelocityD emulation as much)
        if (vertical.getInput() < 100) {
            event.setMotionY((int) (event.getMotionY() * vertical.getInput() / 100.0));
        }
    }
    
    @SubscribeEvent
    public void onPostVelocity(PostVelocityEvent event) {
        if (!Utils.nullCheck()) return;
        
        // LegitVelocity-style jump
        // IMPORTANT: Don't jump immediately - wait at least 1 tick to avoid VelocityD tick=1 flag
        if (jump.isToggled()) {
            if (mc.thePlayer.maxHurtTime <= 0) return;
            if (ignoreLiquid.isToggled() && Utils.inLiquid()) return;
            
            switch ((int) jumpDelayMode.getInput()) {
                case 0: // Delay mode
                    long delay = (long) (Math.random() * (jumpMaxDelay.getInput() - jumpMinDelay.getInput()) + jumpMinDelay.getInput());
                    // Ensure minimum delay of 50ms (roughly 1 tick) to avoid tick=1 flag
                    delay = Math.max(delay, 50);
                    Raven.getExecutor().schedule(() -> {
                        if (canJump() && ticksSinceVelocity > 0) mc.thePlayer.jump();
                    }, delay, TimeUnit.MILLISECONDS);
                    break;
                case 1: // Chance mode
                    // Delay jump by at least 1 tick to avoid tick=1 flag
                    Raven.getExecutor().schedule(() -> {
                        if (jumpChance.getInput() >= 100 || Math.random() * 100 < jumpChance.getInput()) {
                            if (canJump() && ticksSinceVelocity > 0) mc.thePlayer.jump();
                        }
                    }, 50, TimeUnit.MILLISECONDS);
                    break;
            }
        }
    }
    
    private boolean canJump() {
        return mc.thePlayer != null && mc.thePlayer.onGround && (jumpInInv.isToggled() || mc.currentScreen == null);
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck() || !active) return;

        ticksSinceVelocity++;
        
        // Track wall proximity for VelocityD exemption
        if (isNearWall()) {
            wallTicks++;
        } else {
            wallTicks = 0;
        }
        
        // At tick 1, ensure we don't interfere with emulation
        // The emulation expects motion = velocity + playerInput
        // If player has existing motion, it might cause mismatch
        // Solution: Try to get wall exemption immediately, or wait
        if (ticksSinceVelocity == 1) {
            // If we can get wall exemption, do it aggressively
            if (wallAssist.isToggled() && wallTicks == 0) {
                // Try to nudge toward wall immediately in PreMotion
                // This will be handled in onPreMotion
            }
            // Don't modify motion at tick 1 - let it be natural
            return;
        }
        
        // VelocityD returns when: ticks > 8 OR tickSinceNearWall < 3
        // So we can reduce when either:
        // 1. ticks > 8 (VelocityD stops checking)
        // 2. We've been near wall for 3+ ticks (VelocityD exempts)
        boolean velocityDSafe = ticksSinceVelocity > 8 || (wallAssist.isToggled() && wallTicks >= 3);
        
        // If onlyReduceAfterD is off, start reducing after tick 4 (VelocityB window)
        if (!onlyReduceAfterD.isToggled() && ticksSinceVelocity > 4) {
            velocityDSafe = true;
        }
        
        if (!velocityDSafe) return;
        
        // Start reduction
        if (!reductionStarted) {
            reductionStarted = true;
            reductionStep = 0;
            startMotionX = mc.thePlayer.motionX;
            startMotionZ = mc.thePlayer.motionZ;
        }
        
        double hMult = horizontal.getInput() / 100.0;
        int totalSteps = (int) reduceSteps.getInput();
        
        if (reductionStep < totalSteps) {
            // Gradual reduction over multiple ticks
            double progress = (double) (reductionStep + 1) / totalSteps;
            double currentMult = 1.0 - ((1.0 - hMult) * progress);
            
            mc.thePlayer.motionX = startMotionX * currentMult;
            mc.thePlayer.motionZ = startMotionZ * currentMult;
            
            reductionStep++;
        }
        
        // Deactivate after reduction complete and some buffer ticks
        if (reductionStep >= totalSteps && ticksSinceVelocity > 15) {
            active = false;
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (!Utils.nullCheck() || !active) return;

        // Wall assist: nudge toward walls during VelocityD window to get exemption
        // At tick 1, be more aggressive to get exemption immediately
        if (wallAssist.isToggled() && ticksSinceVelocity > 0 && ticksSinceVelocity <= 8 && wallTicks < 3) {
            if (ticksSinceVelocity == 1) {
                // At tick 1, try harder to get near wall
                nudgeTowardsWall();
                // Also try a second time with slightly larger nudge
                if (wallTicks == 0) {
                    nudgeTowardsWallAggressive();
                }
            } else {
                nudgeTowardsWall();
            }
        }
    }
    
    private void nudgeTowardsWallAggressive() {
        if (mc.thePlayer == null || !MoveUtil.isMoving()) return;

        double px = mc.thePlayer.posX;
        double py = mc.thePlayer.posY;
        double pz = mc.thePlayer.posZ;

        double nearestDist = Double.MAX_VALUE;
        double nudgeX = 0.0;
        double nudgeZ = 0.0;

        for (int xDir = -1; xDir <= 1; xDir++) {
            for (int zDir = -1; zDir <= 1; zDir++) {
                if (xDir == 0 && zDir == 0) continue;

                for (double dist = 0.2; dist <= 2.5; dist += 0.3) {
                    BlockPos checkPos = new BlockPos(px + xDir * dist, py, pz + zDir * dist);
                    Block block = BlockUtils.getBlock(checkPos);
                    if (block != Blocks.air && !BlockUtils.isFluid(block)) {
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            // Larger nudge at tick 1
                            nudgeX = xDir * 0.05;
                            nudgeZ = zDir * 0.05;
                        }
                        break;
                    }
                }
            }
        }

        if (nearestDist < 2.0) {
            mc.thePlayer.motionX += nudgeX;
            mc.thePlayer.motionZ += nudgeZ;
        }
    }
    
    private boolean isNearWall() {
        if (mc.thePlayer == null) return false;
        
        int px = (int) Math.floor(mc.thePlayer.posX);
        int py = (int) Math.floor(mc.thePlayer.posY);
        int pz = (int) Math.floor(mc.thePlayer.posZ);
        
        int[][] offsets = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        
        for (int yOff = 0; yOff <= 1; yOff++) {
            for (int[] off : offsets) {
                BlockPos pos = new BlockPos(px + off[0], py + yOff, pz + off[1]);
                Block block = BlockUtils.getBlock(pos);
                if (block != Blocks.air && !BlockUtils.isFluid(block)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void nudgeTowardsWall() {
        if (mc.thePlayer == null || !MoveUtil.isMoving()) return;

        int px = (int) Math.floor(mc.thePlayer.posX);
        int py = (int) Math.floor(mc.thePlayer.posY);
        int pz = (int) Math.floor(mc.thePlayer.posZ);

        double nearestDist = Double.MAX_VALUE;
        double nudgeX = 0.0;
        double nudgeZ = 0.0;

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        
        for (int[] dir : directions) {
            for (int dist = 1; dist <= 4; dist++) {
                BlockPos checkPos = new BlockPos(px + dir[0] * dist, py, pz + dir[1] * dist);
                Block block = BlockUtils.getBlock(checkPos);
                if (block != Blocks.air && !BlockUtils.isFluid(block)) {
                    double actualDist = dist;
                    if (actualDist < nearestDist) {
                        nearestDist = actualDist;
                        nudgeX = dir[0] * 0.02;
                        nudgeZ = dir[1] * 0.02;
                    }
                    break;
                }
            }
        }

        if (nearestDist < 3.0) {
            mc.thePlayer.motionX += nudgeX;
            mc.thePlayer.motionZ += nudgeZ;
        }
    }
}

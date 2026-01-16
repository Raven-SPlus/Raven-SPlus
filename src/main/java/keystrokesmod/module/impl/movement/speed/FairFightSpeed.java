package keystrokesmod.module.impl.movement.speed;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.impl.movement.Speed;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.MoveUtil;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * FairFight Anti-Cheat Speed Bypass
 * 
 * Exploits discovered in SpeedA:
 * 1. Attacking adds +0.006 threshold for 9 ticks
 * 2. Flowing water adds +0.02 threshold
 * 3. Sneak at edge gives full attributeSpeed tolerance
 * 4. "Offset motion" (C03 packets) gives 0.035 tolerance for 21 ticks
 * 5. Teleport gives 0.05 threshold for 5 ticks
 * 
 * SpeedB bypass:
 * - Touch ground every 2 ticks OR stay near walls
 * 
 * SpeedC bypass:
 * - Keep sprint angle below 50.5° from movement direction
 */
public class FairFightSpeed extends SubMode<Speed> {
    private final DescriptionSetting desc = new DescriptionSetting("Exploits FairFight SpeedA thresholds");
    
    private final SliderSetting speedBoost;
    private final ButtonSetting attackBoost;
    private final SliderSetting attackRange;
    private final ButtonSetting groundHop;
    private final ButtonSetting safeJump;
    private final SliderSetting hopHeight;
    private final ButtonSetting strafeLimiter;
    private final SliderSetting maxStrafeAngle;
    private final ButtonSetting frictionAbuse;
    private final ButtonSetting autoSprint;
    
    // State tracking
    private int ticksSinceAttack = 100;
    private int groundTicks = 0;
    private double lastTargetSpeed = 0.0;
    
    public FairFightSpeed(String name, @NotNull Speed parent) {
        super(name, parent);
        this.registerSetting(desc);
        this.registerSetting(speedBoost = new SliderSetting("Speed boost %", 6, 0, 20, 0.5,
                "Extra speed within threshold tolerance"));
        this.registerSetting(attackBoost = new ButtonSetting("Attack boost", true,
                "Attack nearby entities for +0.006 threshold"));
        this.registerSetting(attackRange = new SliderSetting("Attack range", 4.0, 2.0, 6.0, 0.5,
                attackBoost::isToggled));
        this.registerSetting(groundHop = new ButtonSetting("Ground hop", true,
                "Bhop with ground touches to bypass SpeedB"));
        this.registerSetting(safeJump = new ButtonSetting("Safe jump height", true,
                "Use vanilla jump height (prevents Fly A/D)"));
        this.registerSetting(hopHeight = new SliderSetting("Hop height", 0.42, 0.2, 0.6, 0.01,
                () -> groundHop.isToggled() && !safeJump.isToggled()));
        this.registerSetting(strafeLimiter = new ButtonSetting("Strafe limiter", true,
                "Keep angle <50.5° to bypass SpeedC"));
        this.registerSetting(maxStrafeAngle = new SliderSetting("Max strafe angle", 45, 30, 50, 1,
                strafeLimiter::isToggled));
        this.registerSetting(frictionAbuse = new ButtonSetting("Friction abuse", true,
                "Exploit ground friction for extra speed"));
        this.registerSetting(autoSprint = new ButtonSetting("Auto sprint", true));
    }
    
    @Override
    public void onDisable() {
        Utils.resetTimer();
        groundTicks = 0;
        ticksSinceAttack = 100;
        lastTargetSpeed = 0.0;
    }
    
    @SubscribeEvent
    public void onPreUpdate(@NotNull PreUpdateEvent event) {
        if (parent.noAction()) return;
        if (!MoveUtil.isMoving()) return;
        
        // Track ticks
        ticksSinceAttack++;
        
        if (mc.thePlayer.onGround) {
            groundTicks++;
        } else {
            groundTicks = 0;
        }
        
        // Auto sprint
        if (autoSprint.isToggled() && !mc.thePlayer.isSprinting()) {
            mc.thePlayer.setSprinting(true);
        }
        
        // Attack boost: attack nearby entities to gain +0.006 threshold for 9 ticks
        if (attackBoost.isToggled() && ticksSinceAttack >= 8) {
            Entity target = findNearestEntity();
            if (target != null) {
                mc.thePlayer.swingItem();
                mc.playerController.attackEntity(mc.thePlayer, target);
                ticksSinceAttack = 0;
            }
        }
    }
    
    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (parent.noAction()) return;
        if (!MoveUtil.isMoving()) return;
        
        // Calculate base speed boost within FairFight's thresholds
        double boost = calculateSafeBoost();
        double targetSpeed = getTargetSpeed(boost);
        lastTargetSpeed = targetSpeed;
        
        // Ground hop: touch ground frequently to bypass SpeedB air strafe check
        if (groundHop.isToggled()) {
            if (mc.thePlayer.onGround && groundTicks >= 1) {
                // Jump with controlled vanilla height to avoid Fly A/D
                mc.thePlayer.motionY = getJumpMotion();

                // Apply speed boost on ground (uses lastGround attribute calculation)
                MoveUtil.strafe(targetSpeed);
            }
        } else {
            // Simple ground speed
            if (mc.thePlayer.onGround) {
                MoveUtil.strafe(targetSpeed);
            }
        }
        
        // Strafe limiter: prevent SpeedC detection
        if (strafeLimiter.isToggled() && mc.thePlayer.isSprinting()) {
            limitStrafeAngle();
        }
        
        // Friction abuse: use ground friction mechanics
        if (frictionAbuse.isToggled() && mc.thePlayer.onGround) {
            applyFrictionBoost();
        }
    }
    
    /**
     * Calculate safe speed boost based on FairFight's threshold additions
     * 
     * Base threshold: 1E-6 (or 0.035 with offset motion)
     * + 0.006 if attacked in last 9 ticks
     * + Speed potion effects
     */
    private double calculateSafeBoost() {
        double baseBoost = speedBoost.getInput() / 100.0;
        
        // Extra boost from attack threshold
        if (attackBoost.isToggled() && ticksSinceAttack < 9) {
            baseBoost += 0.003; // Conservative: use half of the 0.006 tolerance
        }
        
        // Speed potion amplifies the attribute, giving more tolerance
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            int amp = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1;
            baseBoost += 0.01 * amp; // Extra tolerance from speed effect
        }
        
        return baseBoost;
    }

    private double getTargetSpeed(double boost) {
        double allowed = MoveUtil.getAllowedHorizontalDistance(true);
        double maxSpeed = allowed * (1.0 + boost);
        double current = MoveUtil.speed();

        // Smooth acceleration to avoid SpeedA spikes
        double accel = allowed * 0.05;
        if (current >= maxSpeed) {
            return current;
        }
        return Math.min(current + accel, maxSpeed);
    }

    private double getJumpMotion() {
        if (!safeJump.isToggled()) {
            return hopHeight.getInput();
        }

        double jumpMotion = MoveUtil.JUMP_HEIGHT;
        if (mc.thePlayer.isPotionActive(Potion.jump)) {
            int amp = mc.thePlayer.getActivePotionEffect(Potion.jump).getAmplifier() + 1;
            jumpMotion += 0.1 * amp;
        }
        return jumpMotion;
    }
    
    /**
     * Limit strafe angle to stay under SpeedC's 50.5° detection threshold
     */
    private void limitStrafeAngle() {
        if (mc.thePlayer.motionX == 0 && mc.thePlayer.motionZ == 0) return;
        
        // Calculate movement direction
        double moveDir = Math.toDegrees(-Math.atan2(mc.thePlayer.motionX, mc.thePlayer.motionZ));
        double yaw = mc.thePlayer.rotationYaw;
        
        // Calculate angle between movement and facing direction
        double angle = Math.abs(((moveDir - yaw + 180) % 360) - 180);
        
        // If angle exceeds limit, adjust motion to comply
        double maxAngle = maxStrafeAngle.getInput();
        if (angle > maxAngle && angle < (180 - maxAngle)) {
            // Calculate compliant direction
            double targetDir;
            if (angle < 90) {
                targetDir = Math.toRadians(yaw + maxAngle);
            } else {
                targetDir = Math.toRadians(yaw + 180 - maxAngle);
            }
            
            // Maintain speed magnitude but adjust direction
            double speed = Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX 
                    + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
            mc.thePlayer.motionX = -Math.sin(targetDir) * speed;
            mc.thePlayer.motionZ = Math.cos(targetDir) * speed;
        }
    }
    
    /**
     * Exploit friction mechanics for extra ground speed
     * FairFight calculates: attribute * 0.16277136 / friction^3 for ground movement
     */
    private void applyFrictionBoost() {
        // On ground, we get more acceleration due to friction calculation
        // Slippery blocks (ice) give higher multiplier
        double speed = MoveUtil.speed();
        double baseSpeed = mc.thePlayer.isSprinting() ? 0.13 : 0.1;
        
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            int amp = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1;
            baseSpeed *= (1.0 + amp * 0.2);
        }
        
        // Only boost if below expected speed (avoid detection)
        if (speed < baseSpeed * 1.15) {
            MoveUtil.strafe(Math.min(speed * 1.02, lastTargetSpeed));
        }
    }
    
    private Entity findNearestEntity() {
        double range = attackRange.getInput();
        List<Entity> entities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(
                mc.thePlayer, 
                mc.thePlayer.getEntityBoundingBox().expand(range, range, range)
        );
        
        Entity nearest = null;
        double nearestDist = range;
        
        for (Entity entity : entities) {
            if (entity == mc.thePlayer) continue;
            if (!entity.isEntityAlive()) continue;
            if (entity.isInvisible()) continue;
            
            double dist = mc.thePlayer.getDistanceToEntity(entity);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entity;
            }
        }
        
        return nearest;
    }
    
    @Override
    public String getInfo() {
        return String.format("%.1f%%", speedBoost.getInput());
    }
}

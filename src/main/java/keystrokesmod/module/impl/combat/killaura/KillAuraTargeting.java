package keystrokesmod.module.impl.combat.killaura;

import akka.japi.Pair;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.TargetFilter;
import keystrokesmod.utility.TimerUtil;
import keystrokesmod.utility.Utils;
import keystrokesmod.script.classes.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
// Removed net.minecraft.util.Vec3 import to avoid conflict

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class KillAuraTargeting {
    private final KillAura parent;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil switchTimer = new TimerUtil();
    private int targetIndex = 0;
    private final List<EntityLivingBase> availableTargets = new ArrayList<>();

    public KillAuraTargeting(KillAura parent) {
        this.parent = parent;
    }

    public void onDisable() {
        availableTargets.clear();
        targetIndex = 0;
    }

    public List<EntityLivingBase> getAvailableTargets() {
        return availableTargets;
    }

    public String getStatus() {
        if (parent.targetingMode.getInput() == 1 && KillAura.target != null && !availableTargets.isEmpty()) {
            return String.valueOf(targetIndex + 1);
        }
        return null;
    }

    public EntityLivingBase updateTarget(float[] rotations) {
        availableTargets.clear();
        
        final Vec3 eyePos = Utils.getEyePos();
        
        // Fix for Stream typing issue: use explicit type for collect
        List<Pair<EntityLivingBase, Double>> validTargets = mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityLivingBase)
                .map(entity -> (EntityLivingBase) entity)
                .filter(entity -> entity != mc.thePlayer)
                .filter(entity -> TargetFilter.isValidTargetWithChecks(entity, parent.ignoreTeammates.isToggled(), parent.targetInvisible.isToggled()))
                .filter(entity -> parent.fov.getInput() == 360 || Utils.inFov((float) parent.fov.getInput(), entity))
                .filter(entity -> parent.hitThroughBlocks.isToggled() || !KillAura.behindBlocks(rotations, entity))
                .map(entity -> new Pair<>(entity, eyePos.distanceTo(RotationUtils.getNearestPoint(entity.getEntityBoundingBox(), eyePos))))
                .filter(pair -> pair.second() <= Math.max(parent.blockRange.getInput(), Math.max(parent.swingRange.getInput(), parent.preAimRange.getInput())))
                .collect(Collectors.toList());

        for (Pair<EntityLivingBase, Double> pair : validTargets) {
            EntityLivingBase entity = pair.first();
            double dist = pair.second();
            
            if (dist <= parent.preAimRange.getInput()) {
                availableTargets.add(entity);
            }
        }

        if (availableTargets.isEmpty()) {
            return null;
        }

        // Sort targets
        Comparator<EntityLivingBase> comparator = null;
        switch ((int) parent.sortMode.getInput()) {
            case 0: // Health
                comparator = Comparator.comparingDouble(EntityLivingBase::getHealth);
                break;
            case 1: // HurtTime
                comparator = Comparator.comparingDouble(e -> (double) e.hurtTime);
                break;
            case 2: // Distance
                comparator = Comparator.comparingDouble(e -> mc.thePlayer.getDistanceSqToEntity(e));
                break;
            case 3: // Yaw
                comparator = Comparator.comparingDouble(e -> RotationUtils.distanceFromYaw(e, false));
                break;
        }
        
        if (comparator != null) {
            availableTargets.sort(comparator);
        }

        // Select target based on mode
        EntityLivingBase newTarget;
        switch ((int) parent.targetingMode.getInput()) {
            case 1: // Switch
                int targetCount = Math.max(2, (int) parent.targets.getInput());
                int maxTargets = Math.min(targetCount, availableTargets.size());
                if (maxTargets < 2) {
                    newTarget = availableTargets.get(0);
                } else {
                    if (switchTimer.hasTimeElapsed((long) parent.switchDelay.getInput(), true)) {
                        targetIndex = (targetIndex + 1) % maxTargets;
                    }
                    if (targetIndex >= maxTargets) {
                        targetIndex = 0;
                    }
                    newTarget = availableTargets.get(targetIndex);
                }
                break;
            case 0: // Single
            default:
                newTarget = availableTargets.get(0);
                targetIndex = 0;
                break;
        }
        
        return newTarget;
    }
}

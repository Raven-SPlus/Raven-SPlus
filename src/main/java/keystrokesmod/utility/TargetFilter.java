package keystrokesmod.utility;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for filtering entities based on target settings.
 * Provides methods to check if an entity should be targeted based on the global target filter settings.
 */
public class TargetFilter {
    
    /**
     * Checks if an entity should be targeted based on the target filter settings.
     * 
     * @param entity The entity to check
     * @return true if the entity should be targeted, false otherwise
     */
    public static boolean isValidTarget(@NotNull Entity entity) {
        if (!(entity instanceof EntityLivingBase)) {
            return false;
        }
        
        EntityLivingBase livingBase = (EntityLivingBase) entity;
        
        // Check players
        if (entity instanceof EntityPlayer) {
            return Settings.targetPlayers.isToggled();
        }
        
        // Check golems (Iron Golems)
        if (entity instanceof EntityIronGolem) {
            return Settings.targetGolems.isToggled();
        }
        
        // Check villagers
        if (entity instanceof EntityVillager) {
            return Settings.targetVillagers.isToggled();
        }
        
        // Check passive entities (except golems and villagers which are handled separately)
        // Wolves are handled separately as they can be passive (tamed) or neutral (untamed)
        if (entity instanceof EntityAnimal && !(entity instanceof EntityIronGolem) && !(entity instanceof EntityVillager) && !(entity instanceof EntityWolf)) {
            return Settings.targetPassive.isToggled();
        }
        
        // Check wolves - can be passive (tamed) or neutral (untamed)
        if (entity instanceof EntityWolf) {
            EntityWolf wolf = (EntityWolf) entity;
            if (wolf.isTamed()) {
                // Tamed wolf is passive
                return Settings.targetPassive.isToggled();
            } else {
                // Untamed wolf is neutral
                return Settings.targetNeutral.isToggled();
            }
        }
        
        // Check neutral entities (except golems and villagers which are handled separately)
        if (isNeutralEntity(entity) && !(entity instanceof EntityIronGolem) && !(entity instanceof EntityVillager)) {
            return Settings.targetNeutral.isToggled();
        }
        
        // Check hostile entities (including slime, except golems and villagers which are handled separately)
        if (isHostileEntity(entity) && !(entity instanceof EntityIronGolem) && !(entity instanceof EntityVillager)) {
            return Settings.targetHostile.isToggled();
        }
        
        // Check other entities (armor stands, etc.)
        if (entity instanceof EntityArmorStand || isOtherEntity(entity)) {
            return Settings.targetOthers.isToggled();
        }
        
        return false;
    }
    
    /**
     * Checks if an entity is a neutral entity.
     * Neutral entities include: Enderman, Zombie Pigman, Spider, Cave Spider
     * Note: Wolves are handled separately as they can be passive (tamed) or neutral (untamed)
     */
    private static boolean isNeutralEntity(@NotNull Entity entity) {
        return entity instanceof EntityEnderman ||
               entity instanceof EntityPigZombie ||
               entity instanceof EntitySpider ||
               entity instanceof EntityCaveSpider;
    }
    
    /**
     * Checks if an entity is a hostile entity.
     * Hostile entities include: Zombie, Skeleton, Creeper, Spider, Cave Spider, Enderman,
     * Blaze, Ghast, Silverfish, Witch, Slime, Magma Cube, Guardian, Zombie Pigman
     */
    private static boolean isHostileEntity(@NotNull Entity entity) {
        return entity instanceof EntityZombie ||
               entity instanceof EntitySkeleton ||
               entity instanceof EntityCreeper ||
               entity instanceof EntitySpider ||
               entity instanceof EntityCaveSpider ||
               entity instanceof EntityEnderman ||
               entity instanceof EntityBlaze ||
               entity instanceof EntityGhast ||
               entity instanceof EntitySilverfish ||
               entity instanceof EntityWitch ||
               entity instanceof EntitySlime ||
               entity instanceof EntityMagmaCube ||
               entity instanceof EntityGuardian ||
               entity instanceof EntityPigZombie;
    }
    
    /**
     * Checks if an entity is an "other" entity type.
     * This includes entities that don't fit into the other categories.
     */
    private static boolean isOtherEntity(@NotNull Entity entity) {
        // Armor stands are already checked separately
        // Add other entity types here if needed
        return false;
    }
    
    /**
     * Checks if an entity should be targeted, with additional checks for players.
     * This method includes checks for friends, teammates, bots, and death state.
     * 
     * @param entity The entity to check
     * @param ignoreTeammates Whether to ignore teammates
     * @param targetInvisible Whether to target invisible entities
     * @return true if the entity should be targeted, false otherwise
     */
    public static boolean isValidTargetWithChecks(@NotNull Entity entity, boolean ignoreTeammates, boolean targetInvisible) {
        if (!isValidTarget(entity)) {
            return false;
        }
        
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            
            // Check if friended
            if (Utils.isFriended(player)) {
                return false;
            }
            
            // Check if dead
            if (player.deathTime != 0) {
                return false;
            }
            
            // Check if bot
            if (ModuleManager.antiBot != null && ModuleManager.antiBot.isEnabled() && 
                keystrokesmod.module.impl.world.AntiBot.isBot(player)) {
                return false;
            }
            
            // Check teammates
            if (ignoreTeammates && Utils.isTeamMate(player)) {
                return false;
            }
        }
        
        // Check invisible
        if (!targetInvisible && entity.isInvisible()) {
            return false;
        }
        
        return true;
    }
}


package keystrokesmod.module.impl.other.anticheats.utils.world;

import keystrokesmod.Raven;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.EntityLivingBase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LevelUtils {
    // Cache for player list to avoid rebuilding every tick
    private static long cachedPlayersTick = -1;
    private static List<AbstractClientPlayer> cachedPlayers = null;
    private static WorldClient cachedPlayersWorld = null;
    
    // Cache for entity list
    private static long cachedEntitiesTick = -1;
    private static List<EntityLivingBase> cachedEntities = null;
    private static WorldClient cachedEntitiesWorld = null;

    public static WorldClient getClientLevel() {
        return Objects.requireNonNull(Raven.mc.theWorld);
    }

    public static @NotNull List<EntityLivingBase> getEntities(@NotNull WorldClient level) {
        // Check cache validity
        long currentTick = level.getTotalWorldTime();
        if (cachedEntities != null && cachedEntitiesWorld == level && cachedEntitiesTick == currentTick) {
            return cachedEntities;
        }
        
        List<EntityLivingBase> result = new ArrayList<>();
        // Use iterator instead of forEach to avoid lambda overhead
        for (Object entity : level.loadedEntityList) {
            if (entity instanceof EntityLivingBase) {
                result.add((EntityLivingBase) entity);
            }
        }
        
        // Update cache
        cachedEntities = result;
        cachedEntitiesTick = currentTick;
        cachedEntitiesWorld = level;
        
        return result;
    }

    public static @NotNull List<AbstractClientPlayer> getPlayers(@NotNull WorldClient level) {
        // Check cache validity
        long currentTick = level.getTotalWorldTime();
        if (cachedPlayers != null && cachedPlayersWorld == level && cachedPlayersTick == currentTick) {
            return cachedPlayers;
        }
        
        List<AbstractClientPlayer> result = new ArrayList<>();
        // Use iterator instead of forEach to avoid lambda overhead
        for (Object entity : level.loadedEntityList) {
            if (entity instanceof AbstractClientPlayer) {
                result.add((AbstractClientPlayer) entity);
            }
        }
        
        // Update cache
        cachedPlayers = result;
        cachedPlayersTick = currentTick;
        cachedPlayersWorld = level;
        
        return result;
    }

    public static @NotNull List<EntityLivingBase> getEntities() {
        return getEntities(getClientLevel());
    }

    public static @NotNull List<AbstractClientPlayer> getPlayers() {
        return getPlayers(getClientLevel());
    }
    
    /**
     * Clears the cache when world changes
     */
    public static void clearCache() {
        cachedPlayers = null;
        cachedEntities = null;
        cachedPlayersWorld = null;
        cachedEntitiesWorld = null;
        cachedPlayersTick = -1;
        cachedEntitiesTick = -1;
    }
}

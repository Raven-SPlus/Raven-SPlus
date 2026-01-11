package keystrokesmod.module.impl.world;

import com.mojang.authlib.GameProfile;
import keystrokesmod.Raven;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.render.Freecam;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.script.classes.Vec3;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

class AngleRecord {
    float yaw;
    long timestamp;
    
    AngleRecord(float yaw, long timestamp) {
        this.yaw = yaw;
        this.timestamp = timestamp;
    }
}

class MarkedBot {
    EntityPlayer player;
    long markTime;
    
    MarkedBot(EntityPlayer player, long markTime) {
        this.player = player;
        this.markTime = markTime;
    }
}

class ClipData {
    int clipCount;
    long lastClipTime;
    long markTime;
    boolean wasClipping;
    
    ClipData() {
        this.clipCount = 0;
        this.lastClipTime = 0;
        this.markTime = 0;
        this.wasClipping = false;
    }
}

class NPCData {
    double lastPosX;
    double lastPosY;
    double lastPosZ;
    boolean hasMoved;
    long spawnTime;
    
    NPCData(EntityPlayer player) {
        this.lastPosX = player.posX;
        this.lastPosY = player.posY;
        this.lastPosZ = player.posZ;
        this.hasMoved = false;
        this.spawnTime = System.currentTimeMillis();
    }
    
    boolean update(EntityPlayer player) {
        double deltaX = Math.abs(player.posX - lastPosX);
        double deltaY = Math.abs(player.posY - lastPosY);
        double deltaZ = Math.abs(player.posZ - lastPosZ);
        
        // Check if position changed (ignore rotation and swing)
        if (deltaX > 0.001 || deltaY > 0.001 || deltaZ > 0.001) {
            hasMoved = true;
            lastPosX = player.posX;
            lastPosY = player.posY;
            lastPosZ = player.posZ;
        }
        
        return hasMoved;
    }
}

public class AntiBot extends Module {
    private static final HashMap<EntityPlayer, Long> entities = new HashMap<>();
    private static final Set<EntityPlayer> filteredBot = new HashSet<>();
    private static final HashMap<EntityPlayer, LinkedList<AngleRecord>> playerAngleHistory = new HashMap<>();
    private static final LinkedList<AngleRecord> localPlayerAngleHistory = new LinkedList<>();
    private static final HashMap<EntityPlayer, MarkedBot> angleMimickingBots = new HashMap<>();
    private static final Set<EntityPlayer> spawnInCombatBots = new HashSet<>();
    private static final HashMap<EntityPlayer, ClipData> clipDataMap = new HashMap<>();
    private static final HashMap<EntityPlayer, NPCData> npcDataMap = new HashMap<>();
    private static final HashMap<EntityPlayer, Long> intaveSpawnTs = new HashMap<>();
    private static float lastLocalYaw = Float.NaN;
    private static long lastCombatTime = 0;
    private static final long COMBAT_TIMEOUT = 3000; // 3 seconds
    
    private static ButtonSetting entitySpawnDelay;
    private static SliderSetting delay;
    private static ButtonSetting pitSpawn;
    private static ButtonSetting tablist;
    private static ButtonSetting matrix;
    private static ButtonSetting cancelBotHit;
    private static ButtonSetting debug;
    private static ButtonSetting whitelistGolem;
    private static ButtonSetting whitelistSilverfish;
    private static ButtonSetting whitelistChicken;
    private static ButtonSetting intavePseudoBots;
    private static SliderSetting intavePseudoRange;
    private static SliderSetting intavePseudoRecentMs;
    
    // Angle mimicking (moved outside matrix, but kept close)
    private static ButtonSetting angleMimicking;
    private static SliderSetting angleMimickingRange;
    private static ModeSetting angleMimickingMode;
    private static SliderSetting angleMimickingFixedTime;
    private static SliderSetting angleMimickingExpireTime;
    
    // Spawn in combat
    private static ButtonSetting spawnInCombat;
    private static SliderSetting spawnInCombatRange;
    
    // Clip inside ground
    private static ButtonSetting clipInsideGround;
    private static SliderSetting clipInsideGroundTimes;
    private static SliderSetting clipInsideGroundExpireTime;
    private static ButtonSetting clipInsideGroundWhileMoving;
    
    // NPC Check
    private static ButtonSetting npcCheck;
    
    public AntiBot() {
        super("AntiBot", Module.category.world, 0);
        this.registerSetting(entitySpawnDelay = new ButtonSetting("Entity spawn delay", false));
        this.registerSetting(delay = new SliderSetting("Delay", 7.0, 0.5, 15.0, 0.5, " second", entitySpawnDelay::isToggled));
        this.registerSetting(tablist = new ButtonSetting("Tab list", false));
        this.registerSetting(matrix = new ButtonSetting("MatrixTest", false));
        this.registerSetting(debug = new ButtonSetting("Debug", false, matrix::isToggled));
        
        // Angle mimicking (moved outside matrix, but kept close)
        this.registerSetting(angleMimicking = new ButtonSetting("Angle mimicking", false));
        this.registerSetting(angleMimickingRange = new SliderSetting("Range", 3.0, 1.0, 10.0, 0.5, " blocks", angleMimicking::isToggled));
        this.registerSetting(angleMimickingMode = new ModeSetting("Delay mode", new String[]{"Latency", "Fixed time"}, 0, angleMimicking::isToggled));
        this.registerSetting(angleMimickingFixedTime = new SliderSetting("Fixed delay", 200.0, 0.0, 1000.0, 10.0, "ms", () -> angleMimicking.isToggled() && (int)angleMimickingMode.getInput() == 1));
        this.registerSetting(angleMimickingExpireTime = new SliderSetting("Expire time", 5000.0, 1000.0, 30000.0, 500.0, "ms", angleMimicking::isToggled));
        
        // Spawn in combat (next to angle mimicking)
        this.registerSetting(spawnInCombat = new ButtonSetting("Spawn in combat", false));
        this.registerSetting(spawnInCombatRange = new SliderSetting("Range", 10.0, 1.0, 50.0, 1.0, " blocks", spawnInCombat::isToggled));
        
        // Clip inside ground (next to spawn in combat)
        this.registerSetting(clipInsideGround = new ButtonSetting("Clip inside ground", false));
        this.registerSetting(clipInsideGroundTimes = new SliderSetting("Clip times", 3.0, 1.0, 20.0, 1.0, "", clipInsideGround::isToggled));
        this.registerSetting(new DescriptionSetting("Counts distinct clip occurrences. Each time a player transitions from not clipping to clipping inside blocks/ground, the count increases. If 'While moving' is enabled, only clips while the player is moving are counted.", clipInsideGround::isToggled));
        this.registerSetting(clipInsideGroundExpireTime = new SliderSetting("Expire time", 10000.0, 1000.0, 60000.0, 1000.0, "ms", clipInsideGround::isToggled));
        this.registerSetting(clipInsideGroundWhileMoving = new ButtonSetting("While moving", true, clipInsideGround::isToggled));
        
        // NPC Check
        this.registerSetting(npcCheck = new ButtonSetting("NPC Check", false));
        
        this.registerSetting(pitSpawn = new ButtonSetting("Pit spawn", false));
        this.registerSetting(cancelBotHit = new ButtonSetting("Cancel bot hit", false));
        this.registerSetting(whitelistGolem = new ButtonSetting("Whitelist golems", false));
        this.registerSetting(whitelistSilverfish = new ButtonSetting("Whitelist silverfishes", false));
        this.registerSetting(whitelistChicken = new ButtonSetting("Whitelist chickens", false));
        this.registerSetting(intavePseudoBots = new ButtonSetting("Intave pseudo bots", true));
        this.registerSetting(intavePseudoRange = new SliderSetting("Intave range", 4.5, 2.0, 8.0, 0.5, " blocks", intavePseudoBots::isToggled));
        this.registerSetting(intavePseudoRecentMs = new SliderSetting("Intave recent", 12000.0, 1000.0, 20000.0, 500.0, "ms", intavePseudoBots::isToggled));
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        // Track when we're in combat
        if (spawnInCombat.isToggled()) {
            lastCombatTime = System.currentTimeMillis();
        }
    }
    
    private static boolean isInCombat() {
        if (mc.thePlayer == null) return false;
        // Check if player is in combat (has hurtTime or recently attacked)
        return mc.thePlayer.hurtTime > 0 || (System.currentTimeMillis() - lastCombatTime < COMBAT_TIMEOUT);
    }

    @SubscribeEvent
    public void c(final EntityJoinWorldEvent entityJoinWorldEvent) {
        if (entityJoinWorldEvent.entity instanceof EntityPlayer && entityJoinWorldEvent.entity != mc.thePlayer) {
            EntityPlayer player = (EntityPlayer) entityJoinWorldEvent.entity;
            intaveSpawnTs.put(player, System.currentTimeMillis());
            
            if (entitySpawnDelay.isToggled()) {
                entities.put(player, System.currentTimeMillis());
            }
            
            // Check if player spawned during combat
            if (spawnInCombat.isToggled() && isInCombat()) {
                // We're in combat, check if player is within range
                if (mc.thePlayer != null) {
                    double distance = mc.thePlayer.getDistanceToEntity(player);
                    if (distance <= spawnInCombatRange.getInput()) {
                        spawnInCombatBots.add(player);
                    }
                }
            }
            
            // Initialize NPC check data
            if (npcCheck.isToggled()) {
                npcDataMap.put(player, new NPCData(player));
            }
            
            // Initialize clip data
            if (clipInsideGround.isToggled()) {
                clipDataMap.put(player, new ClipData());
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent event) {
        if (cancelBotHit.isToggled() && event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() == C02PacketUseEntity.Action.ATTACK) {
                if (isBot(packet.getEntityFromWorld(mc.theWorld))) {
                    event.setCanceled(true);
                }
            }
        }
    }

    public void onUpdate() {
        // Track combat state
        if (spawnInCombat.isToggled() && mc.thePlayer != null) {
            if (mc.thePlayer.hurtTime > 0) {
                lastCombatTime = System.currentTimeMillis();
            }
        }
        
        if (entitySpawnDelay.isToggled() && !entities.isEmpty()) {
            entities.values().removeIf(n -> n < System.currentTimeMillis() - (long)(delay.getInput() * 1000));
        }

        final HashMap<String, EntityPlayer> players = new HashMap<>();
        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (filteredBot.contains(p)) continue;

            String name = p.getName();
            if (players.containsKey(name)) {
                if (debug.isToggled()) Utils.sendMessage("Filtered bot: " + p.getName() + ".");

                EntityPlayer exists = players.get(name);
                Vec3 thePlayer = new Vec3(mc.thePlayer);
                double existsDistance = thePlayer.distanceTo(exists);
                double curDistance = thePlayer.distanceTo(p);

                if (existsDistance > curDistance) {
                    filteredBot.add(p);
                } else {
                    filteredBot.add(exists);
                }
                break;
            }
            players.put(name, p);
        }

        // Track angles for angle mimicking detection (now independent of matrix)
        if (angleMimicking.isToggled() && mc.thePlayer != null) {
            long currentTime = System.currentTimeMillis();
            
            // Record local player's current angle only if rotating
            float localYaw = mc.thePlayer.rotationYaw;
            float rotationThreshold = 0.5f; // Minimum rotation change to consider as rotating
            
            if (Float.isNaN(lastLocalYaw) || Math.abs(normalizeYaw(localYaw - lastLocalYaw)) > rotationThreshold) {
                localPlayerAngleHistory.addLast(new AngleRecord(localYaw, currentTime));
                lastLocalYaw = localYaw;
                
                // Keep only recent history (last 5 seconds)
                while (!localPlayerAngleHistory.isEmpty() && currentTime - localPlayerAngleHistory.getFirst().timestamp > 5000) {
                    localPlayerAngleHistory.removeFirst();
                }
            }
            
            double maxRange = angleMimickingRange.getInput();
            
            // Record angles for nearby players and check expiration
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player == mc.thePlayer || player.isDead) continue;
                
                // Check if player is within range
                double distance = mc.thePlayer.getDistanceToEntity(player);
                boolean isInRange = distance <= maxRange;
                
                // Remove from tracking if out of range
                if (!isInRange) {
                    playerAngleHistory.remove(player);
                    // Remove from marked bots if out of range
                    if (angleMimickingBots.containsKey(player)) {
                        angleMimickingBots.remove(player);
                    }
                    continue;
                }
                
                // Record player's angle
                float playerYaw = player.rotationYaw;
                LinkedList<AngleRecord> history = playerAngleHistory.getOrDefault(player, new LinkedList<>());
                history.addLast(new AngleRecord(playerYaw, currentTime));
                
                // Keep only recent history
                while (!history.isEmpty() && currentTime - history.getFirst().timestamp > 5000) {
                    history.removeFirst();
                }
                
                playerAngleHistory.put(player, history);
                
                // Check expiration for marked bots
                MarkedBot markedBot = angleMimickingBots.get(player);
                if (markedBot != null) {
                    long expireTime = (long)angleMimickingExpireTime.getInput();
                    if (currentTime - markedBot.markTime >= expireTime) {
                        // Check if still matching bot behavior before expiring
                        if (!checkAngleMimicking(player, currentTime)) {
                            angleMimickingBots.remove(player);
                        } else {
                            // Still matching, refresh the mark time
                            markedBot.markTime = currentTime;
                        }
                    }
                }
            }
            
            // Remove players that are no longer in the world
            Iterator<EntityPlayer> iterator = playerAngleHistory.keySet().iterator();
            while (iterator.hasNext()) {
                EntityPlayer player = iterator.next();
                if (!mc.theWorld.playerEntities.contains(player) || player.isDead) {
                    iterator.remove();
                    angleMimickingBots.remove(player);
                }
            }
        }
        
        // Clip inside ground detection
        if (clipInsideGround.isToggled() && mc.thePlayer != null) {
            long currentTime = System.currentTimeMillis();
            
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player == mc.thePlayer || player.isDead) continue;
                
                ClipData clipData = clipDataMap.get(player);
                if (clipData == null) {
                    clipData = new ClipData();
                    clipDataMap.put(player, clipData);
                }
                
                // Check if player is clipping inside ground
                AxisAlignedBB playerBB = player.getEntityBoundingBox();
                boolean isClipping = BlockUtils.insideBlock(playerBB);
                
                // Check "While moving" condition
                boolean shouldCheck = true;
                if (clipInsideGroundWhileMoving.isToggled()) {
                    double deltaX = Math.abs(player.posX - player.lastTickPosX);
                    double deltaZ = Math.abs(player.posZ - player.lastTickPosZ);
                    double deltaY = Math.abs(player.posY - player.lastTickPosY);
                    shouldCheck = (deltaX > 0.001 || deltaY > 0.001 || deltaZ > 0.001);
                }
                
                // Only count when transitioning from not clipping to clipping
                if (isClipping && shouldCheck && !clipData.wasClipping) {
                    clipData.clipCount++;
                    clipData.lastClipTime = currentTime;
                    
                    // Mark as bot if clip count exceeds threshold
                    if (clipData.clipCount >= (int)clipInsideGroundTimes.getInput()) {
                        clipData.markTime = currentTime;
                    }
                }
                
                clipData.wasClipping = isClipping && shouldCheck;
                
                // Handle expiration
                if (clipData.markTime > 0) {
                    long expireTime = (long)clipInsideGroundExpireTime.getInput();
                    if (currentTime - clipData.markTime >= expireTime) {
                        // Reset if expired
                        clipData.markTime = 0;
                        clipData.clipCount = 0;
                    }
                } else {
                    // Reset clip count if not clipping for a while
                    if (!isClipping && currentTime - clipData.lastClipTime > 1000) {
                        clipData.clipCount = 0;
                        clipData.wasClipping = false;
                    }
                }
            }
            
            // Clean up removed players
            clipDataMap.keySet().removeIf(p -> !mc.theWorld.playerEntities.contains(p) || p.isDead);
        }
        
        // NPC Check
        if (npcCheck.isToggled()) {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player == mc.thePlayer || player.isDead) continue;
                
                NPCData npcData = npcDataMap.get(player);
                if (npcData == null) {
                    npcData = new NPCData(player);
                    npcDataMap.put(player, npcData);
                }
                
                // Update position tracking (don't count rotation and swing)
                npcData.update(player);
            }
            
            // Clean up removed players
            npcDataMap.keySet().removeIf(p -> !mc.theWorld.playerEntities.contains(p) || p.isDead);
        }
        
        // Clean up intave spawn tracking for removed players
        intaveSpawnTs.keySet().removeIf(p -> !mc.theWorld.playerEntities.contains(p) || p.isDead);
    }

    public void onDisable() {
        entities.clear();
        filteredBot.clear();
        playerAngleHistory.clear();
        localPlayerAngleHistory.clear();
        angleMimickingBots.clear();
        spawnInCombatBots.clear();
        clipDataMap.clear();
        npcDataMap.clear();
        intaveSpawnTs.clear();
        lastLocalYaw = Float.NaN;
        lastCombatTime = 0;
    }

    public static boolean isBot(Entity entity) {
        if (!ModuleManager.antiBot.isEnabled()) {
            return false;
        }
        if (Freecam.freeEntity != null && Freecam.freeEntity == entity) {
            return true;
        }
        if (whitelistGolem.isToggled() && entity instanceof EntityIronGolem) {
            return false;
        }
        if (whitelistSilverfish.isToggled() && entity instanceof EntitySilverfish) {
            return false;
        }
        if (whitelistChicken.isToggled() && entity instanceof EntityChicken) {
            return false;
        }
        if (!(entity instanceof EntityPlayer)) {
            return true;
        }
        final EntityPlayer entityPlayer = (EntityPlayer) entity;
        if (entitySpawnDelay.isToggled() && !entities.isEmpty() && entities.containsKey(entityPlayer)) {
            return true;
        }
        if (matrix.isToggled() && filteredBot.contains(entityPlayer)) {
            return true;
        }
        if (entityPlayer.isDead) {
            return true;
        }
        if (entityPlayer.getName().isEmpty()) {
            return true;
        }
        if (!getTablist().contains(entityPlayer.getName()) && tablist.isToggled()) {
            return true;
        }
        if (entityPlayer.getHealth() != 20.0f && entityPlayer.getName().startsWith("§c")) {
            return true;
        }
        if (pitSpawn.isToggled() && entityPlayer.posY >= 114 && entityPlayer.posY <= 130 && entityPlayer.getDistance(0, 114, 0) <= 25) {
            if (Utils.isHypixel()) {
                List<String> sidebarLines = Utils.getSidebarLines();
                if (!sidebarLines.isEmpty() && Utils.stripColor(sidebarLines.get(0)).contains("THE HYPIXEL PIT")) {
                    return true;
                }
            }
        }
        if (entityPlayer.maxHurtTime == 0) {
            if (entityPlayer.getHealth() == 20.0f) {
                String unformattedText = entityPlayer.getDisplayName().getUnformattedText();
                if (unformattedText.length() == 10 && unformattedText.charAt(0) != '§') {
                    return true;
                }
                if (unformattedText.length() == 12 && entityPlayer.isPlayerSleeping() && unformattedText.charAt(0) == '§') {
                    return true;
                }
                if (unformattedText.length() >= 7 && unformattedText.charAt(2) == '[' && unformattedText.charAt(3) == 'N' && unformattedText.charAt(6) == ']') {
                    return true;
                }
                if (entityPlayer.getName().contains(" ")) {
                    return true;
                }
            } else if (entityPlayer.isInvisible()) {
                String unformattedText = entityPlayer.getDisplayName().getUnformattedText();
                if (unformattedText.length() >= 3 && unformattedText.charAt(0) == '§' && unformattedText.charAt(1) == 'c') {
                    return true;
                }
            }
        }
        
        // Angle mimicking detection (now independent of matrix)
        if (angleMimicking.isToggled() && mc.thePlayer != null) {
            // Always check, even if already marked
            long currentTime = System.currentTimeMillis();
            if (checkAngleMimicking(entityPlayer, currentTime)) {
                // Mark as bot or refresh expire time if already marked
                if (angleMimickingBots.containsKey(entityPlayer)) {
                    angleMimickingBots.get(entityPlayer).markTime = currentTime;
                } else {
                    angleMimickingBots.put(entityPlayer, new MarkedBot(entityPlayer, currentTime));
                }
                return true;
            }
            
            // If already marked but no longer matching, still return true (handled by expiration in onUpdate)
            if (angleMimickingBots.containsKey(entityPlayer)) {
                return true;
            }
        }
        
        // Spawn in combat detection
        if (spawnInCombat.isToggled() && spawnInCombatBots.contains(entityPlayer)) {
            return true;
        }
        
        // Clip inside ground detection
        if (clipInsideGround.isToggled()) {
            ClipData clipData = clipDataMap.get(entityPlayer);
            if (clipData != null && clipData.markTime > 0) {
                long currentTime = System.currentTimeMillis();
                long expireTime = (long)clipInsideGroundExpireTime.getInput();
                if (currentTime - clipData.markTime < expireTime) {
                    return true;
                } else {
                    // Expired, reset
                    clipData.markTime = 0;
                    clipData.clipCount = 0;
                }
            }
        }
        
        // NPC Check
        if (npcCheck.isToggled()) {
            NPCData npcData = npcDataMap.get(entityPlayer);
            if (npcData != null && !npcData.hasMoved) {
                return true;
            }
        }

        // Intave pseudo/back bot detection: invisible, very recent, close, mostly stationary, not using potion invis
        if (intavePseudoBots.isToggled()) {
            long now = System.currentTimeMillis();
            long spawnTs = intaveSpawnTs.getOrDefault(entityPlayer, now - 999999);
            boolean recentlySpawned = now - spawnTs <= (long) intavePseudoRecentMs.getInput();
            boolean invisible = entityPlayer.isInvisible() && !entityPlayer.isPotionActive(Potion.invisibility);
            boolean close = mc.thePlayer != null && mc.thePlayer.getDistanceToEntity(entityPlayer) <= intavePseudoRange.getInput();
            boolean lowMotion = Math.abs(entityPlayer.motionX) + Math.abs(entityPlayer.motionZ) < 0.01 && Math.abs(entityPlayer.motionY) < 0.05;
            if (recentlySpawned && invisible && close && lowMotion) {
                return true;
            }
        }
        
        return false;
    }

    private static @NotNull List<String> getTablist() {
        return Raven.mc.getNetHandler().getPlayerInfoMap().stream()
                .map(NetworkPlayerInfo::getGameProfile)
                .filter(profile -> profile.getId() != Raven.mc.thePlayer.getUniqueID())
                .map(GameProfile::getName)
                .collect(Collectors.toList());
    }
    
    private static float normalizeYaw(float yaw) {
        yaw %= 360.0f;
        if (yaw >= 180.0f) {
            yaw -= 360.0f;
        }
        if (yaw < -180.0f) {
            yaw += 360.0f;
        }
        return yaw;
    }
    
    private static boolean checkAngleMimicking(EntityPlayer player, long currentTime) {
        LinkedList<AngleRecord> playerHistory = playerAngleHistory.get(player);
        if (playerHistory == null || playerHistory.isEmpty() || localPlayerAngleHistory.isEmpty()) {
            if (debug.isToggled()) {
                Utils.sendMessage("§c[AngleMimicking] §7" + player.getName() + ": No history data (playerHistory=" + 
                    (playerHistory == null ? "null" : playerHistory.size()) + ", localHistory=" + localPlayerAngleHistory.size() + ")");
            }
            return false;
        }
        
        // Calculate delay based on mode
        long delayMs;
        if ((int)angleMimickingMode.getInput() == 0) {
            // Latency mode: use player's ping (round trip time)
            try {
                NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
                if (playerInfo != null) {
                    int ping = playerInfo.getResponseTime();
                    // Latency = round trip time (ping already includes both directions)
                    delayMs = (long)ping;
                    if (debug.isToggled()) {
                        Utils.sendMessage("§b[AngleMimicking] §7" + player.getName() + ": Using latency mode, ping=" + ping + "ms, delay=" + delayMs + "ms");
                    }
                } else {
                    delayMs = 200L; // Default fallback
                    if (debug.isToggled()) {
                        Utils.sendMessage("§e[AngleMimicking] §7" + player.getName() + ": PlayerInfo null, using fallback delay=" + delayMs + "ms");
                    }
                }
            } catch (Exception e) {
                delayMs = 200L; // Default fallback
                if (debug.isToggled()) {
                    Utils.sendMessage("§e[AngleMimicking] §7" + player.getName() + ": Exception getting ping, using fallback delay=" + delayMs + "ms");
                }
            }
        } else {
            // Fixed time mode
            delayMs = (long)angleMimickingFixedTime.getInput();
            if (debug.isToggled()) {
                Utils.sendMessage("§b[AngleMimicking] §7" + player.getName() + ": Using fixed time mode, delay=" + delayMs + "ms");
            }
        }
        
        int totalChecks = 0;
        int matchesFound = 0;
        float bestMatchYawDiff = Float.MAX_VALUE;
        long bestMatchTimeDiff = 0;
        
        // Check if ANY piece of player's rotation data matches local player's rotation data with delay
        for (AngleRecord playerRecord : playerHistory) {
            float playerYaw = normalizeYaw(playerRecord.yaw);
            long playerRecordTime = playerRecord.timestamp;
            
            // Find matching local player angle accounting for delay
            for (AngleRecord localRecord : localPlayerAngleHistory) {
                long localRecordTime = localRecord.timestamp;
                totalChecks++;
                
                // Calculate time difference: player record should be delayed by delayMs from local record
                long timeDiff = playerRecordTime - localRecordTime;
                
                // Check if delay matches (within 100ms tolerance for network variations)
                if (Math.abs(timeDiff - delayMs) <= 100) {
                    float localYaw = normalizeYaw(localRecord.yaw);
                    
                    // Check if yaws match highly (allowing 1.0 degree tolerance)
                    float yawDiff = Math.abs(playerYaw - localYaw);
                    // Handle wrap-around case (e.g., -179 and 179 are only 2 degrees apart)
                    if (yawDiff > 180.0f) {
                        yawDiff = 360.0f - yawDiff;
                    }
                    
                    if (yawDiff < bestMatchYawDiff) {
                        bestMatchYawDiff = yawDiff;
                        bestMatchTimeDiff = timeDiff;
                    }
                    
                    if (yawDiff < 1.0f) {
                        matchesFound++;
                        if (debug.isToggled()) {
                            Utils.sendMessage("§a[AngleMimicking] §7" + player.getName() + ": MATCH found! PlayerYaw=" + 
                                String.format("%.2f", playerYaw) + "°, LocalYaw=" + String.format("%.2f", localYaw) + 
                                "°, YawDiff=" + String.format("%.2f", yawDiff) + "°, TimeDiff=" + timeDiff + "ms (delay=" + delayMs + "ms)");
                        }
                        return true; // Bot detected: matching angle with correct delay
                    }
                }
            }
        }
        
        if (debug.isToggled() && totalChecks > 0) {
            if (matchesFound == 0) {
                Utils.sendMessage("§c[AngleMimicking] §7" + player.getName() + ": No match. Checks=" + totalChecks + 
                    ", BestYawDiff=" + String.format("%.2f", bestMatchYawDiff) + "°, BestTimeDiff=" + bestMatchTimeDiff + 
                    "ms, ExpectedDelay=" + delayMs + "ms, PlayerHistorySize=" + playerHistory.size() + 
                    ", LocalHistorySize=" + localPlayerAngleHistory.size());
            }
        }
        
        return false;
    }
}

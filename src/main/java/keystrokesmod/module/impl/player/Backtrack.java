package keystrokesmod.module.impl.player;

import java.awt.Color;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import keystrokesmod.event.PreTickEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.WorldChangeEvent;
import keystrokesmod.mixins.impl.network.S14PacketEntityAccessor;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.TimerUtil;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.backtrack.BacktrackData;
import keystrokesmod.utility.backtrack.QueueData;
import keystrokesmod.utility.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.*;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

public class Backtrack extends Module {
    public static final Color color = new Color(0, 255, 0);

    // Mode settings
    private final ModeSetting mode = new ModeSetting("Mode", new String[]{"Legacy", "Modern"}, 1);
    
    // Modern mode settings
    private final SliderSetting nextBacktrackDelay = new SliderSetting("NextBacktrackDelay", 0, 0, 2000, 10);
    private final SliderSetting maxDelay = new SliderSetting("MaxDelay", 80, 0, 2000, 10);
    private final SliderSetting minDelay = new SliderSetting("MinDelay", 80, 0, 2000, 10);
    private final ModeSetting style = new ModeSetting("Style", new String[]{"Pulse", "Smooth"}, 1);
    private final SliderSetting distanceMin = new SliderSetting("Distance Min", 2.0, 0.0, 6.0, 0.1);
    private final SliderSetting distanceMax = new SliderSetting("Distance Max", 3.0, 0.0, 6.0, 0.1);
    private final ButtonSetting smart = new ButtonSetting("Smart", true);
    private final ModeSetting espMode = new ModeSetting("ESP-Mode", new String[]{"None", "Box", "Model", "Wireframe"}, 1);
    private final SliderSetting wireframeWidth = new SliderSetting("WireFrame-Width", 1.0, 0.5, 5.0, 0.1);
    private final SliderSetting espColorRed = new SliderSetting("ESP Color Red", 0, 0, 255, 1);
    private final SliderSetting espColorGreen = new SliderSetting("ESP Color Green", 255, 0, 255, 1);
    private final SliderSetting espColorBlue = new SliderSetting("ESP Color Blue", 0, 0, 255, 1);
    
    // Legacy mode settings
    private final ModeSetting legacyPos = new ModeSetting("Caching mode", new String[]{"ClientPos", "ServerPos"}, 0);
    private final SliderSetting maximumCachedPositions = new SliderSetting("MaxCachedPositions", 10, 1, 20, 1);

    private final Queue<QueueData> packetQueue = new ConcurrentLinkedQueue<>();
    private final Queue<PositionData> positions = new ConcurrentLinkedQueue<>();
    private final Map<UUID, List<BacktrackData>> backtrackedPlayer = new ConcurrentHashMap<>();

    private EntityLivingBase target;
    private TimerUtil globalTimer = new TimerUtil();
    private boolean shouldRender = true;
    private boolean ignoreWholeTick = false;
    private long delayForNextBacktrack = 0L;
    private DelayPair modernDelay = new DelayPair(Utils.randomizeInt((int) minDelay.getInput(), (int) maxDelay.getInput()), false);

    private static class PositionData {
        final net.minecraft.util.Vec3 vec;
        final long time;

        PositionData(net.minecraft.util.Vec3 vec, long time) {
            this.vec = vec;
            this.time = time;
        }
    }

    private static class DelayPair {
        final int delay;
        final boolean changed;

        DelayPair(int delay, boolean changed) {
            this.delay = delay;
            this.changed = changed;
        }
    }

    public Backtrack() {
        super("Backtrack", category.player);
        this.registerSetting(new DescriptionSetting("Allows you to hit past opponents."));
        this.registerSetting(mode);
        this.registerSetting(nextBacktrackDelay);
        this.registerSetting(maxDelay);
        this.registerSetting(minDelay);
        this.registerSetting(style);
        this.registerSetting(distanceMin);
        this.registerSetting(distanceMax);
        this.registerSetting(smart);
        this.registerSetting(espMode);
        this.registerSetting(wireframeWidth);
        this.registerSetting(espColorRed);
        this.registerSetting(espColorGreen);
        this.registerSetting(espColorBlue);
        this.registerSetting(legacyPos);
        this.registerSetting(maximumCachedPositions);
    }

    @Override
    public String getInfo() {
        return String.valueOf(getSupposedDelay());
    }

    @Override
    public void guiUpdate() {
        Utils.correctValue(minDelay, maxDelay);
        Utils.correctValue(distanceMin, distanceMax);
        if (maxDelay.getInput() < minDelay.getInput()) {
            maxDelay.setValue(minDelay.getInput());
        }
        if (minDelay.getInput() > maxDelay.getInput()) {
            minDelay.setValue(maxDelay.getInput());
        }
    }

    @Override
    public void onEnable() {
        clearPackets();
        backtrackedPlayer.clear();
        reset();
    }

    @Override
    public void onDisable() {
        clearPackets();
        backtrackedPlayer.clear();
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (mode.getInput() == 0) { // Legacy
            backtrackedPlayer.forEach((key, backtrackData) -> {
                backtrackData.removeIf(data -> data.getTime() + getSupposedDelay() < System.currentTimeMillis());
                if (backtrackData.isEmpty()) {
                    backtrackedPlayer.remove(key);
                }
            });
        }

        if (mode.getInput() == 1) { // Modern
            if (shouldBacktrack() && target != null) {
                if (!Blink.isBlinking() && target.posX != 0 && target.posY != 0 && target.posZ != 0) {
                    double trueDist = mc.thePlayer.getDistance(target.posX, target.posY, target.posZ);
                    double dist = mc.thePlayer.getDistance(target.posX, target.posY, target.posZ);

                    if (trueDist <= 6.0 && (!smart.isToggled() || trueDist >= dist) && 
                        (style.getInput() == 1 || !globalTimer.hasTimeElapsed(getSupposedDelay()))) {
                        shouldRender = true;

                        double distance = mc.thePlayer.getDistanceToEntity(target);
                        if (distance >= distanceMin.getInput() && distance <= distanceMax.getInput()) {
                            handlePackets();
                        } else {
                            handlePacketsRange();
                        }
                    } else {
                        clear();
                    }
                }
            } else {
                clear();
            }
        }

        ignoreWholeTick = false;
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent e) {
        if (mode.getInput() == 1) { // Modern
            boolean shouldChangeDelay = packetQueue.isEmpty() && PacketUtils.skipReceiveEvent.isEmpty();

            if (!shouldChangeDelay) {
                modernDelay = new DelayPair(modernDelay.delay, false);
            }

            if (shouldChangeDelay && !modernDelay.changed && !shouldBacktrack()) {
                delayForNextBacktrack = System.currentTimeMillis() + (long) nextBacktrackDelay.getInput();
                modernDelay = new DelayPair(Utils.randomizeInt((int) minDelay.getInput(), (int) maxDelay.getInput()), true);
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(@NotNull ReceivePacketEvent e) {
        if (!Utils.nullCheck()) return;
        Packet<?> packet = e.getPacket();

        if (mode.getInput() == 0) { // Legacy
            if (packet instanceof S0CPacketSpawnPlayer) {
                S0CPacketSpawnPlayer spawnPacket = (S0CPacketSpawnPlayer) packet;
                Entity entity = mc.theWorld.getEntityByID(spawnPacket.getEntityID());
                if (entity instanceof EntityPlayer) {
                    addBacktrackData(spawnPacket.getPlayer(), entity.posX, entity.posY, entity.posZ, System.currentTimeMillis());
                }
            } else if (packet instanceof S14PacketEntity || packet instanceof S18PacketEntityTeleport) {
                if (legacyPos.getInput() == 1) { // ServerPos
                    int id;
                    if (packet instanceof S14PacketEntity) {
                        id = ((S14PacketEntityAccessor) packet).getEntityId();
                    } else {
                        id = ((S18PacketEntityTeleport) packet).getEntityId();
                    }

                    Entity entity = mc.theWorld.getEntityByID(id);
                    if (entity instanceof EntityPlayer) {
                        addBacktrackData(entity.getUniqueID(), entity.posX, entity.posY, entity.posZ, System.currentTimeMillis());
                    }
                }
            }
            return;
        }

        // Modern mode
        if (mc.isSingleplayer() || mc.getCurrentServerData() == null) {
            clearPackets();
            return;
        }

        if (packetQueue.isEmpty() && PacketUtils.skipReceiveEvent.isEmpty() && !shouldBacktrack()) {
            return;
        }

        // Ignore server related packets
        if (packet instanceof C00Handshake || packet instanceof C00PacketServerQuery || 
            packet instanceof S02PacketChat || packet instanceof S01PacketPong) {
            return;
        }

        if (packet instanceof S29PacketSoundEffect) {
            S29PacketSoundEffect soundPacket = (S29PacketSoundEffect) packet;
            String soundName = soundPacket.getSoundName();
            if (soundName.contains("game.player.hurt") || soundName.contains("game.player.die")) {
                return;
            }
        }

        // Flush on own death
        if (packet instanceof S06PacketUpdateHealth) {
            S06PacketUpdateHealth healthPacket = (S06PacketUpdateHealth) packet;
            if (healthPacket.getHealth() <= 0) {
                clearPackets();
                return;
            }
        }

        if (packet instanceof S13PacketDestroyEntities) {
            S13PacketDestroyEntities destroyPacket = (S13PacketDestroyEntities) packet;
            if (target != null) {
                for (int entityId : destroyPacket.getEntityIDs()) {
                    if (entityId == target.getEntityId()) {
                        clearPackets();
                        reset();
                        return;
                    }
                }
            }
        }

        if (packet instanceof S1CPacketEntityMetadata) {
            S1CPacketEntityMetadata metadataPacket = (S1CPacketEntityMetadata) packet;
            if (target != null && target.getEntityId() == metadataPacket.getEntityId()) {
                // Check for health metadata
                // Simplified - would need to check metadata properly
                return;
            }
        }

        if (packet instanceof S19PacketEntityStatus && target != null) {
            S19PacketEntityStatus statusPacket = (S19PacketEntityStatus) packet;
            if (statusPacket.getEntity(mc.theWorld) == target) {
                return;
            }
        }

        // Cancel every received packet
        if (e.isCanceled()) {
            return;
        }

                    if (packet instanceof S14PacketEntity) {
                        S14PacketEntity entityPacket = (S14PacketEntity) packet;
                        if (target != null && ((S14PacketEntityAccessor) entityPacket).getEntityId() == target.getEntityId()) {
                            positions.add(new PositionData(new net.minecraft.util.Vec3(target.posX, target.posY, target.posZ), System.currentTimeMillis()));
                        }
                    }

                    if (packet instanceof S18PacketEntityTeleport) {
                        S18PacketEntityTeleport teleportPacket = (S18PacketEntityTeleport) packet;
                        if (target != null && teleportPacket.getEntityId() == target.getEntityId()) {
                            positions.add(new PositionData(new net.minecraft.util.Vec3(target.posX, target.posY, target.posZ), System.currentTimeMillis()));
                        }
                    }

        e.setCanceled(true);
        packetQueue.add(new QueueData(packet, System.currentTimeMillis()));
    }

    @SubscribeEvent
    public void onAttack(@NotNull AttackEntityEvent e) {
        if (!isSelected(e.target, true)) return;

        // Clear all packets, start again on enemy change
        if (target != e.target) {
            clearPackets();
            reset();
        }

        if (e.target instanceof EntityLivingBase) {
            target = (EntityLivingBase) e.target;
        }
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        if (mode.getInput() == 0) { // Legacy
            Color renderColor = Color.RED;
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityPlayer) {
                    glPushMatrix();
                    glDisable(GL_TEXTURE_2D);
                    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                    glEnable(GL_LINE_SMOOTH);
                    glEnable(GL_BLEND);
                    glDisable(GL_DEPTH_TEST);

                    mc.entityRenderer.disableLightmap();

                    glBegin(GL_LINE_STRIP);
                    GL11.glColor4f(renderColor.getRed() / 255.0f, renderColor.getGreen() / 255.0f, renderColor.getBlue() / 255.0f, 1.0f);

                    loopThroughBacktrackData(entity, () -> {
                        double renderPosX = mc.getRenderManager().viewerPosX;
                        double renderPosY = mc.getRenderManager().viewerPosY;
                        double renderPosZ = mc.getRenderManager().viewerPosZ;
                        glVertex3d(entity.posX - renderPosX, entity.posY - renderPosY, entity.posZ - renderPosZ);
                        return false;
                    });

                    glColor4d(1.0, 1.0, 1.0, 1.0);
                    glEnd();
                    glEnable(GL_DEPTH_TEST);
                    glDisable(GL_LINE_SMOOTH);
                    glDisable(GL_BLEND);
                    glEnable(GL_TEXTURE_2D);
                    glPopMatrix();
                }
            }
        } else if (mode.getInput() == 1) { // Modern
            if (!shouldBacktrack() || !shouldRender) return;

            if (target != null) {
                double renderPosX = mc.getRenderManager().viewerPosX;
                double renderPosY = mc.getRenderManager().viewerPosY;
                double renderPosZ = mc.getRenderManager().viewerPosZ;
                double x = target.posX - renderPosX;
                double y = target.posY - renderPosY;
                double z = target.posZ - renderPosZ;

                if (target.posX != 0 && target.posY != 0 && target.posZ != 0) {
                    int espModeValue = (int) espMode.getInput();
                    Color espColor = new Color(
                        (int) espColorRed.getInput(),
                        (int) espColorGreen.getInput(),
                        (int) espColorBlue.getInput()
                    );

                    if (espModeValue == 1) { // Box
                        AxisAlignedBB axisAlignedBB = target.getEntityBoundingBox().offset(-target.posX + x, -target.posY + y, -target.posZ + z);
                        RenderUtils.renderBox(
                            (int) axisAlignedBB.minX, (int) axisAlignedBB.minY, (int) axisAlignedBB.minZ,
                            axisAlignedBB.maxX - axisAlignedBB.minX,
                            axisAlignedBB.maxY - axisAlignedBB.minY,
                            axisAlignedBB.maxZ - axisAlignedBB.minZ,
                            espColor.getRGB(), true, false
                        );
                    } else if (espModeValue == 2) { // Model
                        glPushMatrix();
                        glPushAttrib(GL_ALL_ATTRIB_BITS);

                        GlStateManager.color(0.6f, 0.6f, 0.6f, 1f);
                        mc.getRenderManager().doRenderEntity(
                            target,
                            x, y, z,
                            target.prevRotationYaw + (target.rotationYaw - target.prevRotationYaw) * e.partialTicks,
                            e.partialTicks,
                            true
                        );

                        glPopAttrib();
                        glPopMatrix();
                    } else if (espModeValue == 3) { // Wireframe
                        glPushMatrix();
                        glPushAttrib(GL_ALL_ATTRIB_BITS);

                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                        glDisable(GL_TEXTURE_2D);
                        glDisable(GL_LIGHTING);
                        glDisable(GL_DEPTH_TEST);
                        glEnable(GL_LINE_SMOOTH);

                        glEnable(GL_BLEND);
                        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                        glLineWidth((float) wireframeWidth.getInput());

                        float r = espColor.getRed() / 255.0f;
                        float g = espColor.getGreen() / 255.0f;
                        float b = espColor.getBlue() / 255.0f;
                        glColor4f(r, g, b, 1.0f);
                        mc.getRenderManager().doRenderEntity(
                            target,
                            x, y, z,
                            target.prevRotationYaw + (target.rotationYaw - target.prevRotationYaw) * e.partialTicks,
                            e.partialTicks,
                            true
                        );

                        glPopAttrib();
                        glPopMatrix();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent e) {
        if (mode.getInput() == 0 && legacyPos.getInput() == 0) { // Legacy ClientPos
            Entity entity = e.entity;
            if (entity instanceof EntityPlayer) {
                addBacktrackData(entity.getUniqueID(), entity.posX, entity.posY, entity.posZ, System.currentTimeMillis());
            }
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldChangeEvent e) {
        if (mode.getInput() == 1) { // Modern
            if (mc.theWorld == null) {
                clearPackets(false);
            }
            target = null;
        }
    }

    private void handlePackets() {
        long supposedDelay = getSupposedDelay();
        packetQueue.removeIf(queueData -> {
            if (queueData.getTime() <= System.currentTimeMillis() - supposedDelay) {
                Packet<?> packet = queueData.getPacket();
                if (packet instanceof Packet) {
                    try {
                        PacketUtils.receivePacket((Packet<INetHandlerPlayClient>) packet);
                    } catch (Exception ignored) {
                    }
                }
                return true;
            }
            return false;
        });

        positions.removeIf(posData -> posData.time < System.currentTimeMillis() - supposedDelay);
    }

    private void handlePacketsRange() {
        long time = getRangeTime();
        if (time == -1L) {
            clearPackets();
            return;
        }

        packetQueue.removeIf(queueData -> {
            if (queueData.getTime() <= time) {
                Packet<?> packet = queueData.getPacket();
                if (packet instanceof Packet) {
                    try {
                        PacketUtils.receivePacket((Packet<INetHandlerPlayClient>) packet);
                    } catch (Exception ignored) {
                    }
                }
                return true;
            }
            return false;
        });

        positions.removeIf(posData -> posData.time < time);
    }

    private long getRangeTime() {
        if (target == null) return 0L;

        long time = 0L;
        boolean found = false;

        for (PositionData data : positions) {
            time = data.time;
            net.minecraft.util.Vec3 targetPos = new net.minecraft.util.Vec3(target.posX, target.posY, target.posZ);
            AxisAlignedBB targetBox = target.getEntityBoundingBox().offset(
                data.vec.xCoord - targetPos.xCoord,
                data.vec.yCoord - targetPos.yCoord,
                data.vec.zCoord - targetPos.zCoord
            );

            // Calculate distance to box manually
            double dx = Math.max(0, Math.max(mc.thePlayer.getEntityBoundingBox().minX - targetBox.maxX, targetBox.minX - mc.thePlayer.getEntityBoundingBox().maxX));
            double dy = Math.max(0, Math.max(mc.thePlayer.getEntityBoundingBox().minY - targetBox.maxY, targetBox.minY - mc.thePlayer.getEntityBoundingBox().maxY));
            double dz = Math.max(0, Math.max(mc.thePlayer.getEntityBoundingBox().minZ - targetBox.maxZ, targetBox.minZ - mc.thePlayer.getEntityBoundingBox().maxZ));
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance >= distanceMin.getInput() && distance <= distanceMax.getInput()) {
                found = true;
                break;
            }
        }

        return found ? time : -1L;
    }

    private void clearPackets() {
        clearPackets(true, true);
    }

    private void clearPackets(boolean handlePackets) {
        clearPackets(handlePackets, true);
    }

    private void clearPackets(boolean handlePackets, boolean stopRendering) {
        if (handlePackets) {
            packetQueue.removeIf(queueData -> {
                Packet<?> packet = queueData.getPacket();
                if (packet instanceof Packet) {
                    try {
                        PacketUtils.receivePacket((Packet<INetHandlerPlayClient>) packet);
                    } catch (Exception ignored) {
                    }
                }
                return true;
            });
        } else {
            packetQueue.clear();
        }

        positions.clear();

        if (stopRendering) {
            shouldRender = false;
            ignoreWholeTick = true;
        }
    }

    private void addBacktrackData(UUID id, double x, double y, double z, long time) {
        List<BacktrackData> backtrackData = backtrackedPlayer.get(id);

        if (backtrackData != null) {
            if (backtrackData.size() >= (int) maximumCachedPositions.getInput()) {
                backtrackData.remove(0);
            }
            backtrackData.add(new BacktrackData(x, y, z, time));
        } else {
            backtrackedPlayer.put(id, new ArrayList<>(Collections.singletonList(new BacktrackData(x, y, z, time))));
        }
    }

    private List<BacktrackData> getBacktrackData(UUID id) {
        return backtrackedPlayer.get(id);
    }

    private void removeBacktrackData(UUID id) {
        backtrackedPlayer.remove(id);
    }

    public double getNearestTrackedDistance(Entity entity) {
        final double[] nearestRange = {0.0};

        loopThroughBacktrackData(entity, () -> {
            double range = entity.getDistanceToEntity(mc.thePlayer);
            if (range < nearestRange[0] || nearestRange[0] == 0.0) {
                nearestRange[0] = range;
            }
            return false;
        });

        return nearestRange[0];
    }

    public void loopThroughBacktrackData(Entity entity, BacktrackAction action) {
        if (!isEnabled() || !(entity instanceof EntityPlayer) || mode.getInput() == 1) return;

        List<BacktrackData> backtrackDataArray = getBacktrackData(entity.getUniqueID());
        if (backtrackDataArray == null) return;

        net.minecraft.util.Vec3 currPos = new net.minecraft.util.Vec3(entity.posX, entity.posY, entity.posZ);
        net.minecraft.util.Vec3 prevPos = new net.minecraft.util.Vec3(entity.prevPosX, entity.prevPosY, entity.prevPosZ);

        // Loop through data from newest to oldest
        List<BacktrackData> reversed = new ArrayList<>(backtrackDataArray);
        Collections.reverse(reversed);

        for (BacktrackData data : reversed) {
            entity.setPosition(data.getX(), data.getY(), data.getZ());
            if (action.execute()) break;
        }

        // Reset position
        entity.setPosition(currPos.xCoord, currPos.yCoord, currPos.zCoord);
        entity.prevPosX = prevPos.xCoord;
        entity.prevPosY = prevPos.yCoord;
        entity.prevPosZ = prevPos.zCoord;
    }

    @FunctionalInterface
    private interface BacktrackAction {
        boolean execute();
    }

    private int getSupposedDelay() {
        return mode.getInput() == 1 ? modernDelay.delay : (int) maxDelay.getInput();
    }

    private boolean shouldBacktrack() {
        return mc.thePlayer != null && mc.theWorld != null && target != null && 
               mc.thePlayer.getHealth() > 0 && 
               (target.getHealth() > 0 || Double.isNaN(target.getHealth())) &&
               mc.playerController.getCurrentGameType() != WorldSettings.GameType.SPECTATOR &&
               System.currentTimeMillis() >= delayForNextBacktrack &&
               isSelected(target, true) &&
               (mc.thePlayer.ticksExisted > 20) &&
               !ignoreWholeTick;
    }

    private boolean isSelected(Entity entity, boolean playersOnly) {
        if (entity == null || entity == mc.thePlayer) return false;
        if (playersOnly && !(entity instanceof EntityPlayer)) return false;
        if (entity.isDead) return false;
        return true;
    }

    private void reset() {
        target = null;
        globalTimer.reset();
    }

    private void clear() {
        clearPackets();
        globalTimer.reset();
    }

    public boolean isPacketQueueEmpty() {
        return packetQueue.isEmpty();
    }

    public EntityLivingBase getTarget() {
        return target;
    }
}

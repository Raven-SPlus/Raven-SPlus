package keystrokesmod.module.impl.combat.killaura;

import keystrokesmod.Raven;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.other.SlotHandler;
import keystrokesmod.module.impl.player.Blink;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Reflection;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.util.BlockPos;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.minecraft.util.EnumFacing.DOWN;

public class KillAuraAutoBlock {
    private final KillAura parent;
    private final Minecraft mc = Minecraft.getMinecraft();
    
    public AtomicBoolean block = new AtomicBoolean();
    public boolean blocking;
    public boolean blinking;
    public boolean lag;
    public boolean swapped;
    public int hypixelAutoblockState;
    public int blockingTime = 0;
    
    public final ConcurrentLinkedQueue<Packet<?>> blinkedPackets = new ConcurrentLinkedQueue<>();

    public KillAuraAutoBlock(KillAura parent) {
        this.parent = parent;
    }

    public void onEnable() {
        // No specific enable logic needed yet
    }

    public void onDisable() {
        resetBlinkState(true);
        block.set(false);
        blockingTime = 0;
        hypixelAutoblockState = 0;
        
        // Stop using Blink silently for Hypixel autoblock
        if (parent.autoBlockMode.getInput() == 9 && ModuleManager.blink != null) {
            ModuleManager.blink.stopUsingSilently(parent);
        }
    }
    
    public void updateBlockState(EntityLivingBase target, double distance) {
        if (target != null && distance <= parent.blockRange.getInput() && parent.autoBlockMode.getInput() > 0) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            block.set(true);
        } else {
            block.set(false);
        }
    }

    public void block(EntityLivingBase target) {
        if (!block.get() && !blocking) {
            return;
        }
        if (parent.manualBlock.isToggled() && !parent.rmbDown) {
            block.set(false);
        }
        if (!Utils.holdingSword()) {
            block.set(false);
        }
        
        switch ((int) parent.autoBlockMode.getInput()) {
            case 0:  // manual
                setBlockState(false, false, true);
                break;
            case 8:
            case 1: // vanilla
                setBlockState(block.get(), true, true);
                break;
            case 2: // post
                setBlockState(block.get(), false, true);
                break;
            case 3: // interact
            case 4:
            case 5:
                setBlockState(block.get(), false, false);
                break;
            case 6: // fake
                setBlockState(block.get(), false, false);
                break;
            case 7: // partial
                boolean down = (target == null || target.hurtTime >= 5) && block.get();
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), down);
                Reflection.setButton(1, down);
                blocking = down;
                break;
            case 9: // Hypixel - uses Blink silently
                setBlockState(block.get(), false, false);
                // Use Blink silently when blocking starts
                if (block.get() && ModuleManager.blink != null) {
                    if (!blinking) {
                        boolean blinkWasEnabled = ModuleManager.blink.isEnabled();
                        if (!blinkWasEnabled) {
                            ModuleManager.blink.useSilently(parent);
                        }
                        blinking = true;
                    }
                } else if (!block.get() && blinking && ModuleManager.blink != null) {
                    // Stop using Blink silently when blocking ends (if not enabled normally)
                    if (!ModuleManager.blink.isEnabled()) {
                        ModuleManager.blink.stopUsingSilently(parent);
                    }
                    blinking = false;
                }
                break;
        }
        if (block.get()) {
            blockingTime++;
        } else {
            blockingTime = 0;
        }
    }

    private void setBlockState(boolean state, boolean sendBlock, boolean sendUnBlock) {
        if (Utils.holdingSword()) {
            if (sendBlock && !blocking && state && Utils.holdingSword() && !Raven.badPacketsHandler.C07) {
                sendBlock();
            } else if (sendUnBlock && blocking && !state) {
                unBlock();
            }
        }
        blocking = Reflection.setBlocking(state);
    }
    
    public void sendBlock() {
        mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(SlotHandler.getHeldItem()));
    }

    public void unBlock() {
        if (!Utils.holdingSword()) {
            return;
        }
        mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, DOWN));
        blockingTime = 0;
    }

    public void resetBlinkState(boolean unblock) {
        if (!Utils.nullCheck()) return;
        releasePackets();
        blocking = false;
        
        // Handle Hypixel autoblock reset
        if (parent.autoBlockMode.getInput() == 9) {
            if (hypixelAutoblockState == 0 && blocking) {
                mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 9));
                unBlock();
                swapped = true;
                hypixelAutoblockState++;
            } else {
                hypixelAutoblockState = 0;
            }
            
            // Stop using Blink silently when resetting
            if (blinking && ModuleManager.blink != null && !ModuleManager.blink.isEnabled()) {
                ModuleManager.blink.stopUsingSilently(parent);
                blinking = false;
            }
        }
        
        if (Raven.badPacketsHandler.playerSlot != mc.thePlayer.inventory.currentItem && swapped) {
            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            Raven.badPacketsHandler.playerSlot = mc.thePlayer.inventory.currentItem;
            swapped = false;
        }
        if (lag && unblock) {
            unBlock();
        }
        lag = false;
    }

    public void releasePackets() {
        try {
            // For Hypixel autoblock, use Blink's packet queue if available
            if (parent.autoBlockMode.getInput() == 9 && ModuleManager.blink != null && ModuleManager.blink.getMode().getSelected() instanceof keystrokesmod.module.impl.player.blink.NormalBlink) {
                keystrokesmod.module.impl.player.blink.NormalBlink normalBlink = (keystrokesmod.module.impl.player.blink.NormalBlink) ModuleManager.blink.getMode().getSelected();
                synchronized (normalBlink.blinkedPackets) {
                    for (Packet<?> packet : normalBlink.blinkedPackets) {
                        if (packet instanceof C09PacketHeldItemChange) {
                            Raven.badPacketsHandler.playerSlot = ((C09PacketHeldItemChange) packet).getSlotId();
                        }
                        PacketUtils.sendPacketNoEvent(packet);
                    }
                    normalBlink.blinkedPackets.clear();
                }
            } else {
                // Fallback to local packet queue
                synchronized (blinkedPackets) {
                    for (Packet<?> packet : blinkedPackets) {
                        if (packet instanceof C09PacketHeldItemChange) {
                            Raven.badPacketsHandler.playerSlot = ((C09PacketHeldItemChange) packet).getSlotId();
                        }
                        PacketUtils.sendPacketNoEvent(packet);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendModuleMessage(parent, "&cThere was an error releasing blinked packets");
        }
        blinkedPackets.clear();
        blinking = false;
    }
    
    public void handlePacketReceive(ReceivePacketEvent e) {
        if (!parent.fixSlotReset.isToggled()) return;
        
        if (Utils.holdingSword() && (mc.thePlayer.isBlocking() || block.get())) {
            if (e.getPacket() instanceof S2FPacketSetSlot) {
                if (mc.thePlayer.inventory.currentItem == ((S2FPacketSetSlot) e.getPacket()).func_149173_d() - 36 && mc.currentScreen == null) {
                    if (((S2FPacketSetSlot) e.getPacket()).func_149174_e() == null || (((S2FPacketSetSlot) e.getPacket()).func_149174_e().getItem() != mc.thePlayer.getHeldItem().getItem())) {
                        return;
                    }
                    e.setCanceled(true);
                }
            }
        }
    }
}

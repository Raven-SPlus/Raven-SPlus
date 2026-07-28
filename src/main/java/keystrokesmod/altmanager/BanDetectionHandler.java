package keystrokesmod.altmanager;

import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BanDetectionHandler {
    private static boolean initialized = false;

    public static void init() {
        if (!initialized) {
            MinecraftForge.EVENT_BUS.register(new BanDetectionHandler());
            initialized = true;
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof S40PacketDisconnect) {
            S40PacketDisconnect disconnectPacket = (S40PacketDisconnect) event.getPacket();
            
            try {
                // Get the disconnect reason/message
                IChatComponent reason = disconnectPacket.getReason();
                if (reason != null) {
                    String message = reason.getUnformattedText().toLowerCase();
                    
                    // Check if the message contains ban-related keywords
                    if (message.contains("suspended") || message.contains("banned") || message.contains("kicked")) {
                        // Get the current account name
                        String currentUsername = Utils.mc.getSession().getUsername();
                        
                        if (currentUsername != null && !currentUsername.isEmpty()) {
                            // Mark the account as banned
                            markAccountAsBanned(currentUsername);
                            Utils.sendMessage("&cAccount &b" + currentUsername + " &chas been detected as banned/kicked/suspended");
                        }
                    }
                }
            } catch (Exception e) {
                // Silently handle any errors (reflection issues, etc.)
            }
        }
    }

    private void markAccountAsBanned(String username) {
        // Find and mark the account in the alt list
        for (Alt alt : AltManagerGui.alts) {
            if (alt.getName().equalsIgnoreCase(username)) {
                alt.setBanned(true);
                // Save the updated alt list
                keystrokesmod.altmanager.util.AltJsonHandler.saveAlts();
                break;
            }
        }
        
        // Also update AccountData if needed
        AccountData.setBanned(username);
    }
}

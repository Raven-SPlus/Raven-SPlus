package keystrokesmod.helper;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

public class ScaffoldBlockCountHelper {
    private final Minecraft mc;
    private Timer fadeTimer;
    private Timer fadeInTimer;
    private float previousAlpha;

    public ScaffoldBlockCountHelper(Minecraft mc) {
        this.mc = mc;
        this.fadeTimer = null;
        (this.fadeInTimer = new Timer(150)).start();
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) {
            return;
        }
        
        boolean shouldShow = Utils.nullCheck() && ModuleManager.scaffold != null && 
                           ModuleManager.scaffold.showBlockCount.isToggled() && 
                           ModuleManager.scaffold.isEnabled();
        
        if (!shouldShow) {
            // Start fade out if module is disabled or setting is off
            if (fadeTimer == null && fadeInTimer == null && previousAlpha > 10) {
                beginFade();
            }
            // Continue fading if already started
            if (fadeTimer != null) {
                float alpha = 255 - fadeTimer.getValueInt(0, 255, 1);
                previousAlpha = alpha;
                if (alpha <= 10) {
                    onDisable();
                    return;
                }
                // Still render with fading alpha
                if (mc.currentScreen == null) {
                    final ScaledResolution scaledResolution = new ScaledResolution(mc);
                    int blocks = ModuleManager.scaffold != null ? ModuleManager.scaffold.totalBlocks() : 0;
                    String color = "§";
                    if (blocks <= 5) {
                        color += "c";
                    }
                    else if (blocks <= 15) {
                        color += "6";
                    }
                    else if (blocks <= 25) {
                        color += "e";
                    }
                    else {
                        color = "";
                    }
                    int colorAlpha = -1 | ((int) previousAlpha << 24);
                    GL11.glPushMatrix();
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    mc.fontRendererObj.drawStringWithShadow(color + blocks + " §rblock" + (blocks == 1 ? "" : "s"), scaledResolution.getScaledWidth()/2 + 8, scaledResolution.getScaledHeight()/2 + 4, colorAlpha);
                    GL11.glDisable(GL11.GL_BLEND);
                    GL11.glPopMatrix();
                }
            }
            return;
        }
        
        // Reset fade if module is re-enabled
        if (fadeTimer != null) {
            fadeTimer = null;
            (this.fadeInTimer = new Timer(150)).start();
        }
        
        if (mc.currentScreen != null) {
            return;
        }
        
        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        int blocks = ModuleManager.scaffold.totalBlocks();
        String color = "§";
        if (blocks <= 5) {
            color += "c";
        }
        else if (blocks <= 15) {
            color += "6";
        }
        else if (blocks <= 25) {
            color += "e";
        }
        else {
            color = "";
        }
        float alpha = fadeTimer == null ? 255 : (255 - fadeTimer.getValueInt(0, 255, 1));
        if (fadeInTimer != null) {
            alpha = fadeInTimer.getValueFloat(10, 255, 1);
            if (alpha == 255) {
                fadeInTimer = null;
            }
        }
        previousAlpha = alpha;
        int colorAlpha = -1 | ((int) previousAlpha << 24);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mc.fontRendererObj.drawStringWithShadow(color + blocks + " §rblock" + (blocks == 1 ? "" : "s"), scaledResolution.getScaledWidth()/2 + 8, scaledResolution.getScaledHeight()/2 + 4, colorAlpha);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    public void beginFade() {
        (this.fadeTimer = new Timer(150)).start();
        this.fadeInTimer = null;
    }

    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        fadeInTimer = null;
        fadeTimer = null;
    }
}

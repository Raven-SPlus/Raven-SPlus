package keystrokesmod.utility.render.blur;

import keystrokesmod.utility.render.ColorUtils;
import keystrokesmod.utility.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.settings.GameSettings;
import org.jetbrains.annotations.Range;
import java.lang.reflect.Field;

import static keystrokesmod.Raven.mc;
import static keystrokesmod.utility.render.blur.StencilUtil.checkSetupFBO;
import static org.lwjgl.opengl.GL11.*;

/**
 * Global high-efficiency blur manager that batches blur operations
 * and uses downsampling for optimal performance.
 * 
 * Usage:
 * 1. Call startBlur() once at the start of rendering
 * 2. Draw all elements that need blur (they will be added to stencil)
 * 3. Call endBlur() once at the end to apply blur to all elements
 */
public class GlobalBlurManager {

    private static final KawaseBlur kawaseBlur = new KawaseBlur();
    
    private static boolean blurActive = false;
    private static int currentBlurRadius = 0;
    private static float currentCompression = 0;
    
    /**
     * Start blur collection. Call this once before drawing elements that need blur.
     */
    public static void startBlur() {
        if (blurActive || shouldSkipBlur()) {
            return; // Already active, don't reset
        }
        if (mc.displayWidth <= 0 || mc.displayHeight <= 0) {
            return; // Minimized or invalid viewport; skip to avoid GL errors
        }
        
        mc.mcProfiler.startSection("GlobalBlur-Pre");
        mc.getFramebuffer().bindFramebuffer(false);
        checkSetupFBO(mc.getFramebuffer());
        glClear(GL_STENCIL_BUFFER_BIT);
        glEnable(GL_STENCIL_TEST);
        
        glStencilFunc(GL_ALWAYS, 1, 1);
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
        glColorMask(false, false, false, false);
        
        blurActive = true;
        mc.mcProfiler.endSection();
    }
    
    /**
     * End blur and apply it to all collected stencil areas.
     * This performs a single efficient blur operation for all elements.
     * The main performance gain comes from batching all blur operations into one pass.
     * 
     * @param radius Blur radius (0-64)
     * @param compression Compression factor (typically radius / 4.0f)
     */
    public static void endBlur(@Range(from = 0, to = 64) int radius, float compression) {
        if (!blurActive) {
            return;
        }
        if (shouldSkipBlur() || mc.displayWidth <= 0 || mc.displayHeight <= 0) {
            StencilUtil.uninitStencilBuffer();
            ColorUtils.resetColor();
            GlStateManager.bindTexture(0);
            blurActive = false;
            return;
        }
        
        mc.mcProfiler.startSection("GlobalBlur-Post");
        
        // Read stencil buffer - this sets up stencil test to only render where stencil = 1
        StencilUtil.readStencilBuffer(1);

        currentBlurRadius = radius;
        currentCompression = compression;

        if (radius <= 0 || compression <= 0) {
            StencilUtil.uninitStencilBuffer();
            ColorUtils.resetColor();
            GlStateManager.bindTexture(0);
            blurActive = false;
            mc.mcProfiler.endSection();
            return;
        }

        int iterations = Math.max(1, Math.min(6, radius / 4));
        int offset = (int) compression;

        KawaseBlur.renderBlur(mc.getFramebuffer().framebufferTexture, iterations, offset);

        StencilUtil.uninitStencilBuffer();
        ColorUtils.resetColor();
        GlStateManager.bindTexture(0);

        blurActive = false;
        mc.mcProfiler.endSection();
    }
    
    /**
     * Check if blur is currently active
     */
    public static boolean isBlurActive() {
        return blurActive;
    }

    /**
     * Detect OptiFine Fast Render or invalid viewport to avoid crashing GL calls.
     */
    private static boolean shouldSkipBlur() {
        try {
            GameSettings settings = mc.gameSettings;
            if (settings != null) {
                Field fastRender = GameSettings.class.getDeclaredField("ofFastRender");
                fastRender.setAccessible(true);
                Object value = fastRender.get(settings);
                if (value instanceof Boolean && (Boolean) value) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            // Field not present (vanilla or other forks) - treat as not enabled
        }
        return false;
    }
    
    /**
     * Force end blur (cleanup in case of errors)
     */
    public static void forceEndBlur() {
        if (blurActive) {
            StencilUtil.uninitStencilBuffer();
            ColorUtils.resetColor();
            GlStateManager.bindTexture(0);
            blurActive = false;
        }
    }
    
    /**
     * Cleanup framebuffers (call on game shutdown)
     */
    public static void cleanup() {
        KawaseBlur.cleanup();
    }
}


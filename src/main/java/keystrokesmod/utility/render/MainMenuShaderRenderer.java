package keystrokesmod.utility.render;

import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.utility.ShaderUtils;
import keystrokesmod.utility.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renders the main menu background using shaders
 * Based on Rise client's shader system
 */
public class MainMenuShaderRenderer {
    private static ShaderUtils shader;
    private static int currentShaderMode = -1; // Track current shader mode
    private static long startTime = System.currentTimeMillis();
    private static long pausedTime = 0; // Accumulated paused time
    private static long lastFrameTime = System.currentTimeMillis(); // Track last frame time
    private static boolean wasFocused = true; // Track focus state
    private static long lastUpdateTime = System.currentTimeMillis(); // Track last successful update
    private static long focusRegainTime = 0; // Track when focus was regained
    private static final long FOCUS_REGAIN_DELAY = 200; // Wait 200ms after focus regain before rendering
    private static Framebuffer opacityFramebuffer = new Framebuffer(1, 1, false); // Framebuffer for opacity rendering
    private static long lastUnfocusedRenderTime = 0; // Track last render time when unfocused
    private static final long UNFOCUSED_FRAME_INTERVAL = 100; // Minimum 100ms between frames when unfocused (~10 FPS)

    /**
     * Renders the shader-based background to the screen
     * @param gui The GuiScreen to render to
     */
    public static void renderBackground(@NotNull net.minecraft.client.gui.GuiScreen gui) {
        final int width = gui.width;
        final int height = gui.height;
        
        // Early validation - check if dimensions are valid
        if (width <= 0 || height <= 0) {
            return;
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        
        // Check if window is focused/active
        boolean isFocused = true;
        boolean contextValid = false;
        try {
            if (org.lwjgl.opengl.Display.isCreated()) {
                isFocused = org.lwjgl.opengl.Display.isActive();
                // If Display is created and active, assume context is valid
                // We'll validate it by trying a safe OpenGL call if needed
                contextValid = isFocused;
            } else {
                contextValid = false;
            }
        } catch (Exception e) {
            // If Display check fails, try fallback
            try {
                isFocused = mc.inGameHasFocus;
                contextValid = isFocused; // Assume valid if window has focus
            } catch (Exception e2) {
                // If everything fails, don't render
                return;
            }
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Handle window focus changes - improved for Windows 11 minimize/restore
        if (!isFocused && wasFocused) {
            // Window just lost focus - invalidate shader and record pause time
            lastFrameTime = currentTime;
            invalidateShader(); // Invalidate shader when losing focus to prevent context issues
        } else if (isFocused && !wasFocused) {
            // Window just regained focus after being minimized
            focusRegainTime = currentTime; // Record when focus was regained
            
            // Calculate how long we were paused (before updating lastFrameTime)
            long elapsedWhileMinimized = currentTime - lastFrameTime;
            
            // Add paused time to our accumulator to maintain smooth animation
            if (elapsedWhileMinimized > 100) {
                pausedTime += elapsedWhileMinimized;
            }
            
            // Update tracking times
            lastFrameTime = currentTime;
            lastUpdateTime = currentTime;
        }
        
        // Update focus state
        wasFocused = isFocused;
        
        // If context is invalid, don't render (but still update tracking)
        if (!contextValid) {
            lastFrameTime = currentTime;
            return;
        }
        
        // When unfocused/minimized, reduce framerate instead of stopping
        if (!isFocused) {
            // Check if enough time has passed since last render (throttle to ~10 FPS)
            if (lastUnfocusedRenderTime > 0 && (currentTime - lastUnfocusedRenderTime) < UNFOCUSED_FRAME_INTERVAL) {
                lastFrameTime = currentTime;
                return; // Skip this frame, but continue rendering next time
            }
            lastUnfocusedRenderTime = currentTime; // Update last render time
        } else {
            // When focused, reset unfocused render time and allow normal framerate
            lastUnfocusedRenderTime = 0;
            
            // Wait a bit after focus regain before rendering to ensure context is stable
            if (focusRegainTime > 0 && (currentTime - focusRegainTime) < FOCUS_REGAIN_DELAY) {
                return;
            }
            focusRegainTime = 0; // Reset after delay period
        }
        
        // Get current shader mode from settings
        int shaderMode = 3; // Default to Aurora
        if (Settings.backgroundMode != null) {
            shaderMode = (int) Settings.backgroundMode.getInput();
        }
        
        // Load or reload shader if mode changed or shader is null
        if (shader == null || currentShaderMode != shaderMode) {
            // Clean up old shader if it exists
            if (shader != null) {
                try {
                    shader.unload();
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
                shader = null;
            }
            
            // Load the appropriate shader based on mode
            String shaderPath;
            switch (shaderMode) {
                case 0: // Flow
                    shaderPath = "keystrokesmod:shaders/main_menu_background_flow.frag";
                    break;
                case 1: // Rise
                    shaderPath = "keystrokesmod:shaders/main_menu_background_rise.frag";
                    break;
                case 2: // Nexus
                    shaderPath = "keystrokesmod:shaders/main_menu_background_nexus.frag";
                    break;
                case 3: // Aurora
                    shaderPath = "keystrokesmod:shaders/main_menu_background_aurora.frag";
                    break;
                default:
                    shaderPath = "keystrokesmod:shaders/main_menu_background_rise.frag";
                    break;
            }
            
            try {
                shader = new ShaderUtils(shaderPath);
                currentShaderMode = shaderMode;
            } catch (Exception e) {
                e.printStackTrace();
                shader = null;
                currentShaderMode = -1;
                // Fallback to default background if shader fails
                return;
            }
        }
        
        // Double-check shader is valid before using
        if (shader == null) {
            return;
        }
        
        // Update last update time for next frame
        lastUpdateTime = currentTime;
        
        // Calculate time in seconds with proper pause handling
        long effectiveElapsed = currentTime - startTime - pausedTime;
        float time = Math.min(effectiveElapsed / 1000.0f, 3600.0f); // Cap at 1 hour
        
        // Wrap all OpenGL operations in try-catch to prevent deadlocks
        try {
            // Setup OpenGL state using GlStateManager for better compatibility
            GlStateManager.disableAlpha();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            
            // Activate shader
            shader.init();
            
            // Set uniforms - use actual display resolution for gl_FragCoord
            shader.setUniformf("resolution", (double) mc.displayWidth, (double) mc.displayHeight);
            shader.setUniformf("time", time);
            
            // Draw fullscreen quad using ShaderUtils helper
            ShaderUtils.drawQuads(0, 0, width, height);
            
            // Cleanup shader first
            shader.unload();
            
            // Restore OpenGL state
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.resetColor();
        } catch (Exception e) {
            // If any OpenGL operation fails, invalidate shader and return
            e.printStackTrace();
            invalidateShader();
            // Try to restore OpenGL state even on error
            try {
                GlStateManager.resetColor();
                GlStateManager.enableTexture2D();
                GlStateManager.enableAlpha();
            } catch (Exception e2) {
                // Ignore cleanup errors
            }
            return;
        }
    }

    /**
     * Renders the shader-based background to the screen with opacity
     * @param gui The GuiScreen to render to
     * @param opacity Opacity value from 0.0 to 1.0
     */
    public static void renderBackgroundWithOpacity(@NotNull net.minecraft.client.gui.GuiScreen gui, float opacity) {
        final int width = gui.width;
        final int height = gui.height;
        
        // Early validation - check if dimensions are valid
        if (width <= 0 || height <= 0 || opacity <= 0.0f) {
            return;
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        
        // Check if window is focused/active
        boolean isFocused = true;
        boolean contextValid = false;
        try {
            if (org.lwjgl.opengl.Display.isCreated()) {
                isFocused = org.lwjgl.opengl.Display.isActive();
                contextValid = isFocused;
            } else {
                contextValid = false;
            }
        } catch (Exception e) {
            try {
                isFocused = mc.inGameHasFocus;
                contextValid = isFocused;
            } catch (Exception e2) {
                return;
            }
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Handle window focus changes
        if (!isFocused && wasFocused) {
            lastFrameTime = currentTime;
            invalidateShader();
        } else if (isFocused && !wasFocused) {
            focusRegainTime = currentTime;
            long elapsedWhileMinimized = currentTime - lastFrameTime;
            if (elapsedWhileMinimized > 100) {
                pausedTime += elapsedWhileMinimized;
            }
            lastFrameTime = currentTime;
            lastUpdateTime = currentTime;
        }
        
        wasFocused = isFocused;
        
        // If context is invalid, don't render (but still update tracking)
        if (!contextValid) {
            lastFrameTime = currentTime;
            return;
        }
        
        // When unfocused/minimized, reduce framerate instead of stopping
        if (!isFocused) {
            // Check if enough time has passed since last render (throttle to ~10 FPS)
            if (lastUnfocusedRenderTime > 0 && (currentTime - lastUnfocusedRenderTime) < UNFOCUSED_FRAME_INTERVAL) {
                lastFrameTime = currentTime;
                return; // Skip this frame, but continue rendering next time
            }
            lastUnfocusedRenderTime = currentTime; // Update last render time
        } else {
            // When focused, reset unfocused render time and allow normal framerate
            lastUnfocusedRenderTime = 0;
            
            // Wait a bit after focus regain before rendering to ensure context is stable
            if (focusRegainTime > 0 && (currentTime - focusRegainTime) < FOCUS_REGAIN_DELAY) {
                return;
            }
            focusRegainTime = 0; // Reset after delay period
        }
        
        // Get current shader mode from settings
        int shaderMode = 3; // Default to Aurora
        if (Settings.backgroundMode != null) {
            shaderMode = (int) Settings.backgroundMode.getInput();
        }
        
        // Load or reload shader if mode changed or shader is null
        if (shader == null || currentShaderMode != shaderMode) {
            if (shader != null) {
                try {
                    shader.unload();
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
                shader = null;
            }
            
            // Load the appropriate shader based on mode
            String shaderPath;
            switch (shaderMode) {
                case 0: // Flow
                    shaderPath = "keystrokesmod:shaders/main_menu_background_flow.frag";
                    break;
                case 1: // Rise
                    shaderPath = "keystrokesmod:shaders/main_menu_background_rise.frag";
                    break;
                case 2: // Nexus
                    shaderPath = "keystrokesmod:shaders/main_menu_background_nexus.frag";
                    break;
                case 3: // Aurora
                    shaderPath = "keystrokesmod:shaders/main_menu_background_aurora.frag";
                    break;
                default:
                    shaderPath = "keystrokesmod:shaders/main_menu_background_rise.frag";
                    break;
            }
            
            try {
                shader = new ShaderUtils(shaderPath);
                currentShaderMode = shaderMode;
            } catch (Exception e) {
                e.printStackTrace();
                shader = null;
                currentShaderMode = -1;
                return;
            }
        }
        
        if (shader == null) {
            return;
        }
        
        lastUpdateTime = currentTime;
        
        long effectiveElapsed = currentTime - startTime - pausedTime;
        float time = Math.min(effectiveElapsed / 1000.0f, 3600.0f);
        
        try {
            // Create/update framebuffer for rendering shader with opacity
            opacityFramebuffer = RenderUtils.createFrameBuffer(opacityFramebuffer);
            
            // Render shader to framebuffer
            opacityFramebuffer.framebufferClear();
            opacityFramebuffer.bindFramebuffer(false);
            
            // Setup OpenGL state for shader rendering
            GlStateManager.disableAlpha();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            
            // Activate shader
            shader.init();
            
            // Set uniforms - use actual display resolution
            shader.setUniformf("resolution", (double) mc.displayWidth, (double) mc.displayHeight);
            shader.setUniformf("time", time);
            
            // Draw fullscreen quad to framebuffer using display dimensions
            ShaderUtils.drawQuads(0, 0, mc.displayWidth, mc.displayHeight);
            
            // Cleanup shader
            shader.unload();
            
            // Unbind framebuffer
            opacityFramebuffer.unbindFramebuffer();
            
            // Restore OpenGL state
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            
            // Now draw the framebuffer texture with opacity to the GUI
            mc.getFramebuffer().bindFramebuffer(false);
            int alpha = (int) (opacity * 255.0f);
            int color = (alpha << 24) | 0xFFFFFF; // White with alpha
            RenderUtils.drawImage(opacityFramebuffer.framebufferTexture, 0, 0, width, height, color);
            
            GlStateManager.resetColor();
        } catch (Exception e) {
            e.printStackTrace();
            invalidateShader();
            try {
                GlStateManager.resetColor();
                GlStateManager.enableTexture2D();
                GlStateManager.enableAlpha();
                mc.getFramebuffer().bindFramebuffer(false);
            } catch (Exception e2) {
                // Ignore cleanup errors
            }
            return;
        }
    }

    /**
     * Resets the start time for the shader animation
     */
    public static void resetTime() {
        startTime = System.currentTimeMillis();
        pausedTime = 0;
        lastFrameTime = System.currentTimeMillis();
        lastUpdateTime = System.currentTimeMillis();
        wasFocused = true;
        focusRegainTime = 0;
        lastUnfocusedRenderTime = 0;
    }
    
    /**
     * Forces shader reload on next render (useful when settings change)
     */
    public static void invalidateShader() {
        if (shader != null) {
            try {
                shader.unload();
            } catch (Exception e) {
                // Ignore cleanup errors - context might be invalid
            }
            shader = null;
        }
        currentShaderMode = -1;
        focusRegainTime = 0; // Reset focus regain time
    }
}


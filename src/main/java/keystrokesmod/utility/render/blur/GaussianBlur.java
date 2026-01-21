package keystrokesmod.utility.render.blur;

import keystrokesmod.utility.render.ColorUtils;
import net.minecraft.client.renderer.GlStateManager;
import org.jetbrains.annotations.Range;

import static keystrokesmod.Raven.mc;
import static keystrokesmod.utility.render.blur.StencilUtil.checkSetupFBO;
import static org.lwjgl.opengl.GL11.*;

public class GaussianBlur {

    public static void startBlur(){
        mc.mcProfiler.startSection("Pre-blur");
        mc.getFramebuffer().bindFramebuffer(false);
        checkSetupFBO(mc.getFramebuffer());
        glClear(GL_STENCIL_BUFFER_BIT);
        glEnable(GL_STENCIL_TEST);

        glStencilFunc(GL_ALWAYS, 1, 1);
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
        glColorMask(false, false, false, false);
        mc.mcProfiler.endSection();
    }

    public static void endBlur(@Range(from = 0, to = 64) int radius, float compression) {
        mc.mcProfiler.startSection("Post-blur");
        StencilUtil.readStencilBuffer(1);

        if (radius <= 0 || compression <= 0) {
            StencilUtil.uninitStencilBuffer();
            ColorUtils.resetColor();
            GlStateManager.bindTexture(0);
            mc.mcProfiler.endSection();
            return;
        }

        int iterations = (int) Math.max(1, radius / 4f);
        int offset = (int) compression;

        KawaseBlur.renderBlur(mc.getFramebuffer().framebufferTexture, iterations, offset);

        StencilUtil.uninitStencilBuffer();
        ColorUtils.resetColor();
        GlStateManager.bindTexture(0);

        mc.mcProfiler.endSection();
    }

}
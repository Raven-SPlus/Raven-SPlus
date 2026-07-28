package keystrokesmod.render.glide.blur;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;

public class BlurUtils {

    private static Minecraft mc = Minecraft.getMinecraft();

    private static ShaderGroup blurShader;
    private static Framebuffer buffer;

    private static float lastScale = 0;
    private static float lastScaleWidth = 0;
    private static float lastScaleHeight = 0;

    private static void reinitShader() {
        try {
            buffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
            buffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            blurShader = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), new ResourceLocation("shaders/post/blurArea.json"));
            blurShader.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void drawBlurScreen(float radius) {
        ScaledResolution sr = new ScaledResolution(mc);

        int factor = sr.getScaleFactor();
        int factor2 = sr.getScaledWidth();
        int factor3 = sr.getScaledHeight();
        int x = 0;
        int y = 0;
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();

        if (lastScale != factor || lastScaleWidth != factor2 || lastScaleHeight != factor3) {
            reinitShader();
        }

        lastScale = factor;
        lastScaleWidth = factor2;
        lastScaleHeight = factor3;

        // These casts require IMixinShaderGroup accessor for listShaders.
        // If the mixin is not available in Raven S+, this will need adaptation.
        try {
            Object shaderGroupMixin = blurShader;
            java.lang.reflect.Method getListShaders = null;
            for (Class<?> iface : shaderGroupMixin.getClass().getInterfaces()) {
                try {
                    getListShaders = iface.getMethod("getListShaders");
                    break;
                } catch (NoSuchMethodException ignored) {}
            }
            if (getListShaders == null) {
                getListShaders = shaderGroupMixin.getClass().getMethod("getListShaders");
            }
            @SuppressWarnings("unchecked")
            java.util.List<net.minecraft.client.shader.Shader> shaders = (java.util.List<net.minecraft.client.shader.Shader>) getListShaders.invoke(shaderGroupMixin);

            shaders.get(0).getShaderManager().getShaderUniform("BlurXY").set(x * (sr.getScaleFactor() / 2.0F), (factor3 - height) * (sr.getScaleFactor() / 2.0F));
            shaders.get(1).getShaderManager().getShaderUniform("BlurXY").set(x * (sr.getScaleFactor() / 2.0F), (factor3 - height) * (sr.getScaleFactor() / 2.0F));
            shaders.get(0).getShaderManager().getShaderUniform("BlurCoord").set((width - x) * (sr.getScaleFactor() / 2.0F), (height - y) * (sr.getScaleFactor() / 2.0F));
            shaders.get(1).getShaderManager().getShaderUniform("BlurCoord").set((width - x) * (sr.getScaleFactor() / 2.0F), (height - y) * (sr.getScaleFactor() / 2.0F));
            shaders.get(0).getShaderManager().getShaderUniform("Radius").set(radius);
            shaders.get(1).getShaderManager().getShaderUniform("Radius").set(radius);

            java.lang.reflect.Method getTimer = null;
            for (Class<?> iface : mc.getClass().getInterfaces()) {
                try {
                    getTimer = iface.getMethod("getTimer");
                    break;
                } catch (NoSuchMethodException ignored) {}
            }
            if (getTimer == null) {
                getTimer = mc.getClass().getMethod("getTimer");
            }
            Object timer = getTimer.invoke(mc);
            java.lang.reflect.Field partialTicks = timer.getClass().getField("renderPartialTicks");
            blurShader.loadShaderGroup(partialTicks.getFloat(timer));
        } catch (Exception e) {
            e.printStackTrace();
        }

        mc.getFramebuffer().bindFramebuffer(true);
    }
}

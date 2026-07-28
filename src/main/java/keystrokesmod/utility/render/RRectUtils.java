package keystrokesmod.utility.render;

import keystrokesmod.utility.ShaderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class RRectUtils {
    public static final ShaderUtils roundedShader = new ShaderUtils("keystrokesmod:shaders/rrect.frag");
    public static final ShaderUtils roundedOutlineShader = new ShaderUtils("keystrokesmod:shaders/rrectOutline.frag");
    private static final ShaderUtils roundedGradientShader = new ShaderUtils("keystrokesmod:shaders/rrectGradient.frag");
    private static final ShaderUtils roundedOutlineGradientShader = new ShaderUtils("keystrokesmod:shaders/rrectOutlineGradient.frag");
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static void drawGradientRoundCorner(double x, double y, double width, double height, double radius) {
        ColorUtils.resetColor();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        roundedGradientShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedGradientShader);
        roundedGradientShader.setUniformf("color1", ColorUtils.getBackgroundColor(1).getRed() / 255f, ColorUtils.getBackgroundColor(1).getGreen() / 255f, ColorUtils.getBackgroundColor(1).getBlue() / 255f, ColorUtils.getBackgroundColor(1).getAlpha() / 255f);
        roundedGradientShader.setUniformf("color2", ColorUtils.getBackgroundColor(2).getRed() / 255f, ColorUtils.getBackgroundColor(2).getGreen() / 255f, ColorUtils.getBackgroundColor(2).getBlue() / 255f, ColorUtils.getBackgroundColor(2).getAlpha() / 255f);
        roundedGradientShader.setUniformf("color3", ColorUtils.getBackgroundColor(2).getRed() / 255f, ColorUtils.getBackgroundColor(2).getGreen() / 255f, ColorUtils.getBackgroundColor(2).getBlue() / 255f, ColorUtils.getBackgroundColor(2).getAlpha() / 255f);
        roundedGradientShader.setUniformf("color4", ColorUtils.getBackgroundColor(1).getRed() / 255f, ColorUtils.getBackgroundColor(1).getGreen() / 255f, ColorUtils.getBackgroundColor(1).getBlue() / 255f, ColorUtils.getBackgroundColor(1).getAlpha() / 255f);
        ShaderUtils.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedGradientShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawRound(double x, double y, double width, double height, double radius, Color color) {
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        roundedShader.init();

        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        ShaderUtils.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedShader.unload();
        GlStateManager.disableBlend();
    }

    /**
     * Shader-based rounded rect with 4-corner gradient (GPU-side).
     * Colors are in ARGB int format (like Minecraft).
     */
    public static void drawRoundGradient(double x, double y, double width, double height, double radius,
                                         int color1, int color2, int color3, int color4) {
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        roundedGradientShader.init();

        setupRoundedRectUniforms(x, y, width, height, radius, roundedGradientShader);
        setUniformColor4(roundedGradientShader, "color1", color1);
        setUniformColor4(roundedGradientShader, "color2", color2);
        setUniformColor4(roundedGradientShader, "color3", color3);
        setUniformColor4(roundedGradientShader, "color4", color4);

        ShaderUtils.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedGradientShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawRoundOutline(double x, double y, double width, double height, double radius, double outlineThickness, Color color, Color outlineColor) {
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        roundedOutlineShader.init();

        ScaledResolution sr = new ScaledResolution(mc);
        setupRoundedRectUniforms(x, y, width, height, radius, roundedOutlineShader);
        roundedOutlineShader.setUniformf("outlineThickness", outlineThickness * sr.getScaleFactor());
        roundedOutlineShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        roundedOutlineShader.setUniformf("outlineColor", outlineColor.getRed() / 255f, outlineColor.getGreen() / 255f, outlineColor.getBlue() / 255f, outlineColor.getAlpha() / 255f);

        ShaderUtils.drawQuads(x - (2 + outlineThickness), y - (2 + outlineThickness), width + (4 + outlineThickness * 2), height + (4 + outlineThickness * 2));
        roundedOutlineShader.unload();
        GlStateManager.disableBlend();
    }

    /**
     * Shader-based rounded rect with a left->right gradient outline.
     * Outline thickness is in screen pixels (pre-scale).
     */
    public static void drawRoundOutlineGradient(double x, double y, double width, double height, double radius,
                                                double outlineThickness, int fillColor, int outlineColor1, int outlineColor2) {
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        roundedOutlineGradientShader.init();

        ScaledResolution sr = new ScaledResolution(mc);
        setupRoundedRectUniforms(x, y, width, height, radius, roundedOutlineGradientShader);
        // Thickness in screen pixels (glLineWidth style), scale for shader
        roundedOutlineGradientShader.setUniformf("outlineThickness", (float) (outlineThickness * sr.getScaleFactor()));
        setUniformColor4(roundedOutlineGradientShader, "color", fillColor);
        setUniformColor4(roundedOutlineGradientShader, "outlineColor1", outlineColor1);
        setUniformColor4(roundedOutlineGradientShader, "outlineColor2", outlineColor2);

        // Expand quad to fit outline (similar to regular outline shader)
        double pad = 2.0 + outlineThickness;
        ShaderUtils.drawQuads(x - pad, y - pad, width + pad * 2, height + pad * 2);
        roundedOutlineGradientShader.unload();
        GlStateManager.disableBlend();
    }

    static void setupRoundedRectUniforms(double x, double y, double width, double height, double radius, ShaderUtils roundedTexturedShader) {
        ScaledResolution sr = new ScaledResolution(mc);
        roundedTexturedShader.setUniformf("location", x * sr.getScaleFactor(), (Minecraft.getMinecraft().displayHeight - (height * sr.getScaleFactor())) - (y * sr.getScaleFactor()));
        roundedTexturedShader.setUniformf("rectSize", width * sr.getScaleFactor(), height * sr.getScaleFactor());
        roundedTexturedShader.setUniformf("radius", radius * sr.getScaleFactor());
    }

    private static void setUniformColor4(ShaderUtils shader, String uniform, int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        shader.setUniformf(uniform, r, g, b, a);
    }

    protected static float zLevel;

    public static void drawGradientRect(double left, double top, double right, double bottom, int startColor, int endColor) {
        float f = (float) (startColor >> 24 & 255) / 255.0F;
        float f1 = (float) (startColor >> 16 & 255) / 255.0F;
        float f2 = (float) (startColor >> 8 & 255) / 255.0F;
        float f3 = (float) (startColor & 255) / 255.0F;
        float f4 = (float) (endColor >> 24 & 255) / 255.0F;
        float f5 = (float) (endColor >> 16 & 255) / 255.0F;
        float f6 = (float) (endColor >> 8 & 255) / 255.0F;
        float f7 = (float) (endColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(right, top, zLevel).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, top, zLevel).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, bottom, zLevel).color(f5, f6, f7, f4).endVertex();
        worldrenderer.pos(right, bottom, zLevel).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
}
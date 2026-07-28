package keystrokesmod.module.impl.render.targetvisual.targetesp;

import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.render.TargetESP;
import keystrokesmod.module.impl.render.targetvisual.ITargetVisual;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class JelloTargetESP extends SubMode<TargetESP> implements ITargetVisual {
    private final SliderSetting red;
    private final SliderSetting green;
    private final SliderSetting blue;
    private final SliderSetting alpha;

    private Color color = new Color(255, 255, 255, 128);

    public JelloTargetESP(String name, @NotNull TargetESP parent) {
        super(name, parent);
        this.registerSetting(red = new SliderSetting("Red", 255, 0, 255, 1));
        this.registerSetting(green = new SliderSetting("Green", 255, 0, 255, 1));
        this.registerSetting(blue = new SliderSetting("Blue", 255, 0, 255, 1));
        this.registerSetting(alpha = new SliderSetting("Alpha", 128, 0, 255, 1));
    }

    @Override
    public void onUpdate() {
        color = new Color((int) red.getInput(), (int) green.getInput(), (int) blue.getInput(), (int) alpha.getInput());
    }

    @Override
    public void render(@NotNull EntityLivingBase target) {
        int drawTime = (int) (System.currentTimeMillis() % 2000);
        boolean drawMode = drawTime > 1000;
        float drawPercent = drawTime / 1000f;

        if (!drawMode) {
            drawPercent = 1 - drawPercent;
        } else {
            drawPercent -= 1;
        }

        drawPercent = drawPercent * 2;

        if (drawPercent < 1) {
            drawPercent = 0.5f * drawPercent * drawPercent * drawPercent;
        } else {
            float f = drawPercent - 2;
            drawPercent = 0.5f * (f * f * f + 2);
        }

        Minecraft mc = Minecraft.getMinecraft();
        mc.entityRenderer.disableLightmap();
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        mc.entityRenderer.disableLightmap();

        double radius = target.width;
        double height = target.height + 0.1;
        double x = target.lastTickPosX + (target.posX - target.lastTickPosX) * (double) Utils.getTimer().renderPartialTicks - mc.getRenderManager().viewerPosX;
        double y = target.lastTickPosY + (target.posY - target.lastTickPosY) * (double) Utils.getTimer().renderPartialTicks - mc.getRenderManager().viewerPosY + height * drawPercent;
        double z = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * (double) Utils.getTimer().renderPartialTicks - mc.getRenderManager().viewerPosZ;
        double eased = (height / 3) * ((drawPercent > 0.5) ? 1 - drawPercent : drawPercent) * ((drawMode) ? -1 : 1);

        // Optional GPU-friendly path: batch all quads into one Tessellator draw
        try {
            if (Settings.gpu3DRendering != null && Settings.gpu3DRendering.isToggled()) {
                float r = color.getRed() / 255.0f;
                float g = color.getGreen() / 255.0f;
                float b = color.getBlue() / 255.0f;
                float a = color.getAlpha() / 255.0f;

                Tessellator tessellator = Tessellator.getInstance();
                WorldRenderer worldrenderer = tessellator.getWorldRenderer();
                
                // Batch all quads
                worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                for (int segments = 0; segments < 360; segments += 5) {
                    double x1 = x - Math.sin(segments * Math.PI / 180F) * radius;
                    double z1 = z + Math.cos(segments * Math.PI / 180F) * radius;
                    double x2 = x - Math.sin((segments - 5) * Math.PI / 180F) * radius;
                    double z2 = z + Math.cos((segments - 5) * Math.PI / 180F) * radius;

                    // Top vertices (transparent)
                    worldrenderer.pos(x1, y + eased, z1).color(r, g, b, 0.0f).endVertex();
                    worldrenderer.pos(x2, y + eased, z2).color(r, g, b, 0.0f).endVertex();
                    // Bottom vertices (opaque)
                    worldrenderer.pos(x2, y, z2).color(r, g, b, a).endVertex();
                    worldrenderer.pos(x1, y, z1).color(r, g, b, a).endVertex();
                }
                tessellator.draw();

                // Batch all lines
                worldrenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
                for (int segments = 0; segments < 360; segments += 5) {
                    double x1 = x - Math.sin(segments * Math.PI / 180F) * radius;
                    double z1 = z + Math.cos(segments * Math.PI / 180F) * radius;
                    double x2 = x - Math.sin((segments - 5) * Math.PI / 180F) * radius;
                    double z2 = z + Math.cos((segments - 5) * Math.PI / 180F) * radius;

                    worldrenderer.pos(x2, y, z2).color(r, g, b, a).endVertex();
                    worldrenderer.pos(x1, y, z1).color(r, g, b, a).endVertex();
                }
                tessellator.draw();
            } else {
                // Legacy immediate-mode path
                for (int segments = 0; segments < 360; segments += 5) {
                    double x1 = x - Math.sin(segments * Math.PI / 180F) * radius;
                    double z1 = z + Math.cos(segments * Math.PI / 180F) * radius;
                    double x2 = x - Math.sin((segments - 5) * Math.PI / 180F) * radius;
                    double z2 = z + Math.cos((segments - 5) * Math.PI / 180F) * radius;

                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 0.0f);
                    GL11.glVertex3d(x1, y + eased, z1);
                    GL11.glVertex3d(x2, y + eased, z2);
                    GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
                    GL11.glVertex3d(x2, y, z2);
                    GL11.glVertex3d(x1, y, z1);
                    GL11.glEnd();
                    GL11.glBegin(GL11.GL_LINE_LOOP);
                    GL11.glVertex3d(x2, y, z2);
                    GL11.glVertex3d(x1, y, z1);
                    GL11.glEnd();
                }
            }
        } catch (Throwable ignored) {
            // Fallback to legacy path on error
            for (int segments = 0; segments < 360; segments += 5) {
                double x1 = x - Math.sin(segments * Math.PI / 180F) * radius;
                double z1 = z + Math.cos(segments * Math.PI / 180F) * radius;
                double x2 = x - Math.sin((segments - 5) * Math.PI / 180F) * radius;
                double z2 = z + Math.cos((segments - 5) * Math.PI / 180F) * radius;

                GL11.glBegin(GL11.GL_QUADS);
                GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 0.0f);
                GL11.glVertex3d(x1, y + eased, z1);
                GL11.glVertex3d(x2, y + eased, z2);
                GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
                GL11.glVertex3d(x2, y, z2);
                GL11.glVertex3d(x1, y, z1);
                GL11.glEnd();
                GL11.glBegin(GL11.GL_LINE_LOOP);
                GL11.glVertex3d(x2, y, z2);
                GL11.glVertex3d(x1, y, z1);
                GL11.glEnd();
            }
        }

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }
}

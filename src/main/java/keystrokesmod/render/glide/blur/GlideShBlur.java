package keystrokesmod.render.glide.blur;

import keystrokesmod.render.glide.GlideContext;
import keystrokesmod.render.glide.blur.uniform.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.Util;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.io.IOException;
import java.nio.FloatBuffer;

public class GlideShBlur {
    private static final GlideShBlur instance = new GlideShBlur();

    public static GlideShBlur getInstance() {
        return instance;
    }

    private int nvgImage = -1;
    private final Minecraft mc = Minecraft.getMinecraft();
    private Framebuffer framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
    private Framebuffer framebuffer1 = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
    private Framebuffer framebuffer2 = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
    private Framebuffer framebuffer3 = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
    private FloatBuffer weightBuffer = BufferUtils.createFloatBuffer(128);
    private UIShader shader = new UIShader("soar/shaders/vertex.vert", "soar/shaders/blur.frag");
    private long lastUpdate = System.currentTimeMillis();
    private float radius = 4f;

    private boolean enabled = true;
    private boolean skipOSX = true;

    public void init() {
        framebuffer.createFramebuffer(mc.displayWidth / 2, mc.displayHeight / 2);
        framebuffer1.createFramebuffer(mc.displayWidth / 2, mc.displayHeight / 2);
        framebuffer2.createFramebuffer(mc.displayWidth / 6, mc.displayHeight / 6);
        framebuffer3.createFramebuffer(mc.displayWidth / 6, mc.displayHeight / 6);

        framebuffer3.setFramebufferFilter(GL11.GL_LINEAR);

        cacheRadius(radius);
        try {
            shader.init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void cacheRadius(float radius) {
        weightBuffer = BufferUtils.createFloatBuffer(128);
        for (int i = 0; i < radius; i++) {
            weightBuffer.put(gauss((float) i, radius / 2f));
        }
        weightBuffer.rewind();
    }

    public void resize() {
        framebuffer.deleteFramebuffer();
        framebuffer = new Framebuffer(mc.displayWidth / 2, mc.displayHeight / 2, false);
        framebuffer1.deleteFramebuffer();
        framebuffer1 = new Framebuffer(mc.displayWidth / 2, mc.displayHeight / 2, false);
        framebuffer2.deleteFramebuffer();
        framebuffer2 = new Framebuffer(mc.displayWidth / 6, mc.displayHeight / 6, false);
        framebuffer3.deleteFramebuffer();
        framebuffer3 = new Framebuffer(mc.displayWidth / 6, mc.displayHeight / 6, false);

        framebuffer3.setFramebufferFilter(GL11.GL_LINEAR);
    }

    private int nvgImageFromHandle(int texture, int width, int height) {
        return NanoVGGL2.nvglCreateImageFromHandle(
                GlideContext.getInstance().getNvgManager().getContext(),
                texture, width, height, NanoVG.NVG_IMAGE_FLIPY);
    }

    public void render() {
        if (!enabled) return;
        if (skipOSX && Util.getOSType() == Util.EnumOS.OSX) return;
        if (shader == null) return;
        if (nvgImage == -1) {
            nvgImage = nvgImageFromHandle(framebuffer3.framebufferTexture, mc.displayWidth, mc.displayHeight);
        }
        ScaledResolution sr = new ScaledResolution(mc);
        if (System.currentTimeMillis() - lastUpdate > 15) {
            try {
                lastUpdate = System.currentTimeMillis();
                framebuffer.bindFramebuffer(true);
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

                shader.attach();
                bindTexture(mc.getFramebuffer().framebufferTexture, 10);
                shader.uniform(Uniform.makeInt("texture", 10));
                shader.uniform(Uniform.makeVec2("direction", 1f, 0f));
                shader.uniform(Uniform.makeVec2("texelSize", 1f / mc.getFramebuffer().framebufferWidth, 1f / mc.getFramebuffer().framebufferHeight));
                shader.uniform(Uniform.makeFloatBuffer("kernels", weightBuffer));
                shader.uniform(Uniform.makeInt("ignoreAlpha", 1));
                shader.rect(0f, 0f, sr.getScaledWidth(), sr.getScaledHeight());

                framebuffer1.bindFramebuffer(true);
                bindTexture(framebuffer.framebufferTexture, 10);
                shader.uniform(Uniform.makeInt("texture", 10));
                shader.uniform(Uniform.makeVec2("direction", 0f, 1f));
                shader.uniform(Uniform.makeVec2("texelSize", 1f / framebuffer1.framebufferWidth, 1f / framebuffer1.framebufferHeight));
                shader.rect(0f, 0f, sr.getScaledWidth(), sr.getScaledHeight());

                framebuffer2.bindFramebuffer(true);
                bindTexture(framebuffer1.framebufferTexture, 10);
                shader.uniform(Uniform.makeInt("texture", 10));
                shader.uniform(Uniform.makeVec2("direction", 1f, 0f));
                shader.uniform(Uniform.makeVec2("texelSize", 1f / framebuffer2.framebufferWidth, 1f / framebuffer2.framebufferHeight));
                shader.rect(0f, 0f, sr.getScaledWidth(), sr.getScaledHeight());

                framebuffer3.bindFramebuffer(true);
                bindTexture(framebuffer2.framebufferTexture, 10);
                shader.uniform(Uniform.makeInt("texture", 10));
                shader.uniform(Uniform.makeVec2("direction", 0f, 1f));
                shader.uniform(Uniform.makeVec2("texelSize", 1f / framebuffer3.framebufferWidth, 1f / framebuffer3.framebufferHeight));
                shader.rect(0f, 0f, sr.getScaledWidth(), sr.getScaledHeight());
            } catch (Throwable ignored) {
                nvgImage = -1;
                return;
            } finally {
                try {
                    shader.detach();
                } catch (Throwable ignored) {
                }
                try {
                    GL11.glPopAttrib();
                } catch (Throwable ignored) {
                }
            }
        }
        mc.getFramebuffer().bindFramebuffer(true);
    }

    private void bindTexture(int texture, int id) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + id);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    private float gauss(float x, float sigma) {
        double PI = 3.141592653;
        double output = 1.0 / Math.sqrt(2.0 * PI * (sigma * sigma));
        return (float) (output * Math.exp(-(x * x) / (2.0 * (sigma * sigma))));
    }

    public void drawBlur(float x, float y, float w, float h, float radius) {
        if (!enabled) return;
        if (skipOSX && Util.getOSType() == Util.EnumOS.OSX) return;
        if (nvgImage == -1) return;
        long ctx = GlideContext.getInstance().getNvgManager().getContext();
        ScaledResolution sr = new ScaledResolution(mc);

        NVGPaint paint = NVGPaint.calloc();

        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRoundedRect(ctx, x, y, w, h, radius);
        NanoVG.nvgImagePattern(ctx, 0f, 0f, sr.getScaledWidth(), sr.getScaledHeight(), 0f, nvgImage, 1f, paint);
        NanoVG.nvgFillPaint(ctx, paint);
        NanoVG.nvgFill(ctx);
        NanoVG.nvgClosePath(ctx);

        paint.free();
    }

    public void drawBlur(Runnable r) {
        if (!enabled) return;
        if (skipOSX && Util.getOSType() == Util.EnumOS.OSX) return;
        if (nvgImage == -1) return;
        long ctx = GlideContext.getInstance().getNvgManager().getContext();
        ScaledResolution sr = new ScaledResolution(mc);
        NVGPaint paint = NVGPaint.calloc();
        NanoVG.nvgBeginPath(ctx);
        r.run();
        NanoVG.nvgImagePattern(ctx, 0f, 0f, sr.getScaledWidth(), sr.getScaledHeight(), 0f, nvgImage, 1f, paint);
        NanoVG.nvgFillPaint(ctx, paint);
        NanoVG.nvgFill(ctx);
        NanoVG.nvgClosePath(ctx);

        paint.free();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSkipOSX() {
        return skipOSX;
    }

    public void setSkipOSX(boolean skipOSX) {
        this.skipOSX = skipOSX;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = Math.max(0.0F, radius);
    }
}

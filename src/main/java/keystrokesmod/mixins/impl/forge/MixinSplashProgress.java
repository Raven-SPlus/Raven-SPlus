package keystrokesmod.mixins.impl.forge;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.ProgressManager.ProgressBar;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.SharedDrawable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;

import static org.lwjgl.opengl.GL11.*;

/**
 * Replaces Forge's legacy splash renderer (Mojang logo + multiple bars)
 * with a minimalist dark background + single centered progress bar.
 *
 * Target: Forge 1.8.9 {@code net.minecraftforge.fml.client.SplashProgress}.
 */
@Mixin(value = net.minecraftforge.fml.client.SplashProgress.class, remap = false)
public abstract class MixinSplashProgress {

    @Shadow private static Drawable d;
    @Shadow private static volatile boolean pause;
    @Shadow private static volatile boolean done;
    @Shadow private static Thread thread;
    @Shadow private static volatile Throwable threadError;

    @Shadow private static boolean enabled;

    @Shadow @Final private static Lock lock;
    @Shadow public static Semaphore mutex;

    @Shadow private static void checkThreadState() { }

    private static final int BG_RGB = 0x0B0B0B;
    private static final int BAR_BORDER_RGB = 0xD0D0D0;
    private static final int BAR_TRACK_RGB = 0x222222;
    private static final int BAR_FILL_RGB = 0xFFFFFF;

    @Overwrite
    public static void start() {
        // Match Forge defaults: disabled on mac by default, and also gate for OptiFine compatibility.
        final boolean defaultEnabled = !System.getProperty("os.name").toLowerCase().contains("mac");
        enabled = defaultEnabled && ((!FMLClientHandler.instance().hasOptifine()) || Launch.blackboard.containsKey("optifine.ForgeSplashCompatible"));
        if (!enabled) return;

        try {
            d = new SharedDrawable(Display.getDrawable());
            Display.getDrawable().releaseContext();
            d.makeCurrent();
        } catch (LWJGLException e) {
            // If we can't get a shared drawable, fall back to vanilla behavior.
            enabled = false;
            return;
        }

        thread = new Thread(() -> {
            float smoothProgress = 0.0F;
            float lastTargetProgress = 0.0F;
            setGL();
            try {
                while (!done) {
                    // Use ONLY the outermost/first bar as "global progress".
                    // Summing nested bars causes progress to go backwards when Forge pushes sub-tasks.
                    ProgressBar first = null;
                    Iterator<ProgressBar> it = ProgressManager.barIterator();
                    if (it.hasNext()) first = it.next();

                    float targetProgress = lastTargetProgress;
                    if (first != null) {
                        // Forge uses (step+1)/(steps+1) to avoid 0-width on step 0.
                        targetProgress = (first.getStep() + 1) / (float) (first.getSteps() + 1);
                    }
                    // Make it monotonic even if Forge ever replaces the outer bar.
                    if (targetProgress < lastTargetProgress) targetProgress = lastTargetProgress;
                    lastTargetProgress = targetProgress;
                    // Smooth out step-changes so the bar "moves" continuously.
                    smoothProgress += (targetProgress - smoothProgress) * 0.12F;
                    smoothProgress = clamp01(smoothProgress);

                    glClear(GL_COLOR_BUFFER_BIT);

                    int w = Display.getWidth();
                    int h = Display.getHeight();
                    glViewport(0, 0, w, h);
                    glMatrixMode(GL_PROJECTION);
                    glLoadIdentity();
                    glOrtho(0, w, h, 0, -1, 1);
                    glMatrixMode(GL_MODELVIEW);
                    glLoadIdentity();

                    // Single centered progress bar.
                    final int barW = 240;
                    final int barH = 6;
                    final int x1 = (w - barW) / 2;
                    final int y1 = (h - barH) / 2;
                    drawProgressBar(x1, y1, barW, barH, smoothProgress);

                    // See Forge comments about mutex + Display.update.
                    mutex.acquireUninterruptibly();
                    Display.update();
                    mutex.release();

                    if (pause) {
                        clearGL();
                        setGL();
                    }
                    Display.sync(100);
                }
            } finally {
                clearGL();
            }
        }, "RavenS+ Splash");

        thread.setUncaughtExceptionHandler((t, e) -> threadError = e);
        thread.start();
        checkThreadState();
    }

    @Overwrite
    public static void finish() {
        if (!enabled) return;
        try {
            checkThreadState();
            done = true;
            thread.join();
            d.releaseContext();
            Display.getDrawable().makeCurrent();
        } catch (Exception ignored) {
            // If the splash is broken, just let startup proceed.
            enabled = false;
        }
    }

    private static void setGL() {
        lock.lock();
        try {
            Display.getDrawable().makeCurrent();
        } catch (LWJGLException e) {
            throw new RuntimeException(e);
        }

        float r = ((BG_RGB >> 16) & 0xFF) / 255.0F;
        float g = ((BG_RGB >> 8) & 0xFF) / 255.0F;
        float b = (BG_RGB & 0xFF) / 255.0F;
        glClearColor(r, g, b, 1.0F);

        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    private static void clearGL() {
        // Restore a sane-ish GL state and return context to the main drawable.
        glClearColor(1, 1, 1, 1);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, .1f);
        try {
            Display.getDrawable().releaseContext();
        } catch (LWJGLException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private static void drawProgressBar(int x, int y, int w, int h, float progress01) {
        // Border
        setRGB(BAR_BORDER_RGB);
        drawQuad(x - 1, y - 1, w + 2, h + 2);

        // Track
        setRGB(BAR_TRACK_RGB);
        drawQuad(x, y, w, h);

        // Fill
        int fillW = (int) (w * clamp01(progress01));
        if (fillW > 0) {
            setRGB(BAR_FILL_RGB);
            drawQuad(x, y, fillW, h);
        }
    }

    private static void drawQuad(int x, int y, int w, int h) {
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x, y + h);
        glVertex2f(x + w, y + h);
        glVertex2f(x + w, y);
        glEnd();
    }

    private static void setRGB(int rgb) {
        glColor3ub((byte) ((rgb >> 16) & 0xFF), (byte) ((rgb >> 8) & 0xFF), (byte) (rgb & 0xFF));
    }

    private static float clamp01(float v) {
        if (v < 0.0F) return 0.0F;
        return Math.min(1.0F, v);
    }
}


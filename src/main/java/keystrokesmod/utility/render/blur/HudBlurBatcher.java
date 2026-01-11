package keystrokesmod.utility.render.blur;

import keystrokesmod.utility.Utils;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.render.HUD;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Batches multiple HUD blur requests into a single GlobalBlurManager pass per frame.
 *
 * Why: many HUD elements (notifications, target HUD, arraylist background, etc.) previously ran
 * their own fullscreen blur passes, which scales badly when several are on-screen.
 *
 * How: modules enqueue stencil writers + foreground render jobs.
 * - preBlur: things drawn before blur (will be blurred if inside the stencil area)
 * - blurStencil: shapes written into stencil (color writes are disabled during startBlur)
 * - afterBlur: foreground drawing that must remain sharp (drawn after the blur pass)
 */
public final class HudBlurBatcher {
    private static final List<Runnable> PRE_BLUR = new ArrayList<>();
    private static final List<Runnable> BLUR_STENCIL = new ArrayList<>();
    private static final List<Runnable> AFTER_BLUR = new ArrayList<>();

    private static boolean requestedBlur = false;

    public static void addPreBlur(Runnable job) {
        if (job != null) PRE_BLUR.add(job);
    }

    public static void addBlurStencil(int radius, Runnable stencilWriter) {
        if (stencilWriter == null) return;
        // Blur strength is shared and controlled by the HUD module.
        requestedBlur = true;
        BLUR_STENCIL.add(stencilWriter);
    }

    public static void addAfterBlur(Runnable job) {
        if (job != null) AFTER_BLUR.add(job);
    }

    private static void resetFrame() {
        PRE_BLUR.clear();
        BLUR_STENCIL.clear();
        AFTER_BLUR.clear();
        requestedBlur = false;
    }

    private static int getSharedHudBlurRadius() {
        try {
            if (ModuleManager.hud != null && HUD.blurStrength != null) {
                return Math.max(0, Math.min(64, (int) HUD.blurStrength.getInput()));
            }
        } catch (Throwable ignored) {
        }
        return 8; // safe fallback
    }

    private static void flushFrame() {
        if (!Utils.nullCheck()) {
            resetFrame();
            return;
        }

        // Always draw queued jobs, even if blur is skipped (OptiFine fast render, etc.)
        for (Runnable job : PRE_BLUR) {
            try {
                job.run();
            } catch (Throwable ignored) {
            }
        }

        if (requestedBlur && !BLUR_STENCIL.isEmpty()) {
            final int radius = getSharedHudBlurRadius();
            if (radius > 0) {
            GlobalBlurManager.startBlur();
            if (GlobalBlurManager.isBlurActive()) {
                for (Runnable stencil : BLUR_STENCIL) {
                    try {
                        stencil.run();
                    } catch (Throwable ignored) {
                    }
                }
                GlobalBlurManager.endBlur(radius, radius / 4.0f);
            }
            }
        }

        for (Runnable job : AFTER_BLUR) {
            try {
                job.run();
            } catch (Throwable ignored) {
            }
        }

        resetFrame();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void ravenAPlus$renderTickBegin(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        resetFrame();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void ravenAPlus$renderTickEnd(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        flushFrame();
    }
}


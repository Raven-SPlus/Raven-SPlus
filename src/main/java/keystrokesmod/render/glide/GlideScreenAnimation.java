package keystrokesmod.render.glide;

public final class GlideScreenAnimation {
    public void wrap(Runnable glRender, Runnable task, float x, float y, float width, float height, float animationProgress, float alphaProgress, boolean stencil) {
        if (task != null) {
            task.run();
        }
        if (glRender != null) {
            glRender.run();
        }
    }

    public void wrap(Runnable task, float x, float y, float width, float height, float animationProgress, float alphaProgress) {
        wrap(null, task, x, y, width, height, animationProgress, alphaProgress, false);
    }
}

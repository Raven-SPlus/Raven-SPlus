package keystrokesmod.render.glide;

public final class GlideShBlur {
    private float radius = 4.0F;

    public void setRadius(float radius) {
        this.radius = Math.max(0.0F, radius);
    }

    public float getRadius() {
        return radius;
    }

    public void drawBlur(Runnable stencilTask) {
        if (stencilTask != null) {
            stencilTask.run();
        }
    }
}

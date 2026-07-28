package keystrokesmod.render.glide.blur.uniform;

public class UVec4 extends Uniform {
    private float x, y, z, w;

    public UVec4(String name, float x, float y, float z, float w) {
        super(name);
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float z() {
        return z;
    }

    public float w() {
        return w;
    }
}

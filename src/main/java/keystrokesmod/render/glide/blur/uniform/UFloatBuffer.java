package keystrokesmod.render.glide.blur.uniform;

import java.nio.FloatBuffer;

public class UFloatBuffer extends Uniform {
    private FloatBuffer buffer;

    public UFloatBuffer(String name, FloatBuffer buffer) {
        super(name);
        this.buffer = buffer;
    }

    public FloatBuffer buffer() {
        return buffer;
    }
}

package keystrokesmod.render.glide.blur.uniform;

public class UFloat extends Uniform {
    private float value;

    public UFloat(String name, float value) {
        super(name);
        this.value = value;
    }

    public float value() {
        return value;
    }
}

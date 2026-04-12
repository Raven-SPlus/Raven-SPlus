package keystrokesmod.render.glide.blur.uniform;

public class UInt extends Uniform {
    private int value;

    public UInt(String name, int value) {
        super(name);
        this.value = value;
    }

    public int value() {
        return value;
    }
}

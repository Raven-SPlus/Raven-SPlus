package keystrokesmod.utility.backtrack;

public class BacktrackData {
    private final double x;
    private final double y;
    private final double z;
    private final long time;

    public BacktrackData(double x, double y, double z, long time) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.time = time;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public long getTime() {
        return time;
    }
}


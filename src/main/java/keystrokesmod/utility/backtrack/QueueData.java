package keystrokesmod.utility.backtrack;

import net.minecraft.network.Packet;

public class QueueData {
    private final Packet<?> packet;
    private final long time;

    public QueueData(Packet<?> packet, long time) {
        this.packet = packet;
        this.time = time;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public long getTime() {
        return time;
    }
}


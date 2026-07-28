package keystrokesmod.event;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Event for slot updates (copied from reference for Scaffold compatibility)
 */
@Cancelable
public class SlotUpdateEvent extends Event {
    public int slot;

    public SlotUpdateEvent(int slot) {
        this.slot = slot;
    }
}

package keystrokesmod.module.impl.world;

import keystrokesmod.event.RotationEvent;
import keystrokesmod.utility.aim.RotationData;

interface ScaffoldRotationMode {
    RotationData onRotation(float placeYaw, float placePitch, boolean forceStrict, RotationEvent event);

    default boolean onPreSchedulePlace() {
        return true;
    }
}

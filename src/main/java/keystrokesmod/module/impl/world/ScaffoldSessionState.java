package keystrokesmod.module.impl.world;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

final class ScaffoldSessionState {
    int blockSlot = -1;
    boolean hasPlaced;

    Scaffold.PlaceData currentPlacement;
    Scaffold.PlaceData lastPlacement;
    EnumFacing lastPlacedFacing;
    Vec3 targetBlock;
    Vec3 hitVec;
    Vec3 lookVec;
    float[] blockRotations;

    float scaffoldYaw;
    float scaffoldPitch;
    float blockYaw;
    float yawOffset;

    boolean rotateForward;
    double startYPos = -1;
    boolean fastScaffoldKeepY;
    boolean firstKeepYPlace;
    boolean rotatingForward;
    int keepYTicks;
    boolean lowhop;
    int rotationDelay;

    boolean floatJumped;
    boolean floatStarted;
    boolean floatWasEnabled;
    boolean floatKeepY;

    float minOffset;
    float minPitch = 80F;
    float edge = -999999929F;
    long firstStroke;
    float lastEdge2;
    float yawAngle;
    float theYaw;
    boolean set2;
    boolean was451;
    boolean was452;

    int scaffoldTicks;
    boolean enabledOffGround;
    boolean canBlockFade;

    void resetForEnable(float clientYaw, boolean offGround) {
        edge = -999999929F;
        minPitch = 80F;
        enabledOffGround = offGround;
        rotationDelay = offGround ? 2 : 0;
        lastEdge2 = clientYaw;
        hasPlaced = false;
        currentPlacement = null;
        lastPlacement = null;
        lastPlacedFacing = null;
        targetBlock = null;
        hitVec = null;
        lookVec = null;
        blockRotations = null;
        rotateForward = false;
        startYPos = -1;
        fastScaffoldKeepY = false;
        firstKeepYPlace = false;
        rotatingForward = false;
        keepYTicks = 0;
        lowhop = false;
        floatJumped = false;
        floatStarted = false;
        floatWasEnabled = false;
        floatKeepY = false;
        minOffset = 0F;
        firstStroke = 0L;
        yawAngle = 0F;
        theYaw = 0F;
        set2 = false;
        was451 = false;
        was452 = false;
        scaffoldTicks = 0;
        canBlockFade = false;
    }

    void resetAfterDisable() {
        hasPlaced = false;
        currentPlacement = null;
        lastPlacement = null;
        lastPlacedFacing = null;
        targetBlock = null;
        hitVec = null;
        lookVec = null;
        blockRotations = null;
        fastScaffoldKeepY = false;
        firstKeepYPlace = false;
        rotateForward = false;
        rotatingForward = false;
        floatStarted = false;
        floatJumped = false;
        floatWasEnabled = false;
        floatKeepY = false;
        was451 = false;
        was452 = false;
        enabledOffGround = false;
        rotationDelay = 0;
        keepYTicks = 0;
        scaffoldTicks = 0;
        firstStroke = 0L;
        startYPos = -1;
        blockSlot = -1;
        lowhop = false;
    }
}

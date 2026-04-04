package keystrokesmod.module.impl.world;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

final class ScaffoldSessionState {
    int blockSlot = -1;
    boolean hasPlaced;
    boolean lastPlaceSuccessful;

    Scaffold.PlaceData currentPlacement;
    Scaffold.PlaceData lastPlacement;
    EnumFacing lastPlacedFacing;
    Vec3 targetBlock;
    Vec3 hitVec;
    Vec3 lookVec;
    float[] blockRotations;

    float scaffoldYaw;
    float scaffoldPitch;
    float targetYaw;
    float targetPitch;
    float serverYaw;
    float serverPitch;
    float yawVelocity;
    float pitchVelocity;
    boolean hasTargetRotation;
    boolean hasServerRotation;
    boolean rotationReady;
    boolean raytraceReady;
    int placeCooldownTicks;
    int ticksSincePlace;
    int keepYJitterTicks;

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

    int scaffoldTicks;
    boolean enabledOffGround;
    boolean canBlockFade;

    void resetForEnable(float clientYaw, boolean offGround) {
        enabledOffGround = offGround;
        rotationDelay = offGround ? 2 : 0;
        hasPlaced = false;
        lastPlaceSuccessful = false;
        currentPlacement = null;
        lastPlacement = null;
        lastPlacedFacing = null;
        targetBlock = null;
        hitVec = null;
        lookVec = null;
        blockRotations = null;
        scaffoldYaw = clientYaw;
        scaffoldPitch = 80F;
        targetYaw = clientYaw;
        targetPitch = 80F;
        serverYaw = clientYaw;
        serverPitch = 80F;
        yawVelocity = 0F;
        pitchVelocity = 0F;
        hasTargetRotation = false;
        hasServerRotation = false;
        rotationReady = false;
        raytraceReady = false;
        placeCooldownTicks = 0;
        ticksSincePlace = Integer.MAX_VALUE;
        keepYJitterTicks = -1;
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
        scaffoldTicks = 0;
        canBlockFade = false;
    }

    void resetAfterDisable() {
        hasPlaced = false;
        lastPlaceSuccessful = false;
        currentPlacement = null;
        lastPlacement = null;
        lastPlacedFacing = null;
        targetBlock = null;
        hitVec = null;
        lookVec = null;
        blockRotations = null;
        hasTargetRotation = false;
        hasServerRotation = false;
        rotationReady = false;
        raytraceReady = false;
        placeCooldownTicks = 0;
        ticksSincePlace = Integer.MAX_VALUE;
        keepYJitterTicks = -1;
        yawVelocity = 0F;
        pitchVelocity = 0F;
        fastScaffoldKeepY = false;
        firstKeepYPlace = false;
        rotateForward = false;
        rotatingForward = false;
        floatStarted = false;
        floatJumped = false;
        floatWasEnabled = false;
        floatKeepY = false;
        enabledOffGround = false;
        rotationDelay = 0;
        keepYTicks = 0;
        scaffoldTicks = 0;
        startYPos = -1;
        blockSlot = -1;
        lowhop = false;
    }
}

package keystrokesmod.module.impl.world;

import keystrokesmod.event.ScaffoldPlaceEvent;
import keystrokesmod.utility.ContainerUtils;
import keystrokesmod.utility.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;

final class ScaffoldPlacementExecutor {
    private final Scaffold scaffold;
    private final ScaffoldSessionState state;
    private final Minecraft mc = Minecraft.getMinecraft();

    ScaffoldPlacementExecutor(Scaffold scaffold, ScaffoldSessionState state) {
        this.scaffold = scaffold;
        this.state = state;
    }

    void place(Scaffold.PlaceData placeData, boolean extraPlacement) {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)
                || !ContainerUtils.canBePlaced((ItemBlock) heldItem.getItem())) {
            state.lastPlaceSuccessful = false;
            return;
        }

        MovingObjectPosition raycast = RotationUtils.rayTraceCustom(
                mc.playerController.getBlockReachDistance(),
                RotationUtils.serverRotations[0],
                RotationUtils.serverRotations[1]
        );

        scaffold.rayCasted = raycast;
        scaffold.placeYaw = RotationUtils.serverRotations[0];
        scaffold.placePitch = RotationUtils.serverRotations[1];
        boolean liveRaytraceMatches = raycast != null
                && raycast.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && raycast.getBlockPos().equals(placeData.blockPos)
                && raycast.sideHit == placeData.enumFacing;

        MovingObjectPosition hitResult;
        if (liveRaytraceMatches) {
            hitResult = raycast;
        } else {
            if (scaffold.rotation.getInput() >= 3) {
                state.lastPlaceSuccessful = false;
                state.raytraceReady = false;
                return;
            }
            Vec3 hitVec = placeData.hitVec != null
                    ? placeData.hitVec
                    : new Vec3(placeData.blockPos.getX() + 0.5D, placeData.blockPos.getY() + 0.5D, placeData.blockPos.getZ() + 0.5D);
            hitResult = new MovingObjectPosition(hitVec, placeData.enumFacing, placeData.blockPos);
        }

        ScaffoldPlaceEvent event = new ScaffoldPlaceEvent(hitResult, extraPlacement);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            state.lastPlaceSuccessful = false;
            return;
        }

        hitResult = event.getHitResult();
        scaffold.placeBlock = hitResult;
        scaffold.place = true;

        Scaffold.PlaceData placement = new Scaffold.PlaceData(hitResult.getBlockPos(), hitResult.sideHit);
        placement.hitVec = hitResult.hitVec;

        if (mc.playerController.onPlayerRightClick(
                mc.thePlayer,
                mc.theWorld,
                heldItem,
                placement.blockPos,
                placement.enumFacing,
                placement.hitVec)) {
            state.lastPlacement = placement;
            if (scaffold.silentSwingEnabled()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            } else {
                mc.thePlayer.swingItem();
                mc.getItemRenderer().resetEquippedProgress();
            }
            scaffold.highlight.put(placement.blockPos.offset(placement.enumFacing), null);
            state.hasPlaced = true;
            state.lastPlaceSuccessful = true;
            state.ticksSincePlace = 0;
            state.placeCooldownTicks = scaffold.isGrimLegitMotion() ? (extraPlacement ? 3 : 2) : (extraPlacement ? 1 : 0);
            state.raytraceReady = liveRaytraceMatches;
        } else {
            state.lastPlaceSuccessful = false;
            state.placeCooldownTicks = Math.max(state.placeCooldownTicks, scaffold.isGrimLegitMotion() ? 2 : 1);
        }
    }
}

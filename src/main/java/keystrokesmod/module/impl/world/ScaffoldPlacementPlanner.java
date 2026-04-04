package keystrokesmod.module.impl.world;

import keystrokesmod.module.impl.other.RotationHandler;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.scaffold.ScaffoldUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.List;

final class ScaffoldPlacementPlanner {
    private final Scaffold scaffold;
    private final ScaffoldSessionState state;
    private final Minecraft mc = Minecraft.getMinecraft();

    ScaffoldPlacementPlanner(Scaffold scaffold, ScaffoldSessionState state) {
        this.scaffold = scaffold;
        this.state = state;
    }

    Scaffold.PlaceData preparePlacement(int yOffset, int xOffset) {
        List<Scaffold.PlaceData> blocks = findBlocks(yOffset, xOffset);
        if (blocks == null || blocks.isEmpty()) {
            state.currentPlacement = null;
            state.targetBlock = null;
            state.lookVec = null;
            state.hitVec = null;
            state.blockRotations = null;
            return null;
        }

        double sumX = 0;
        double sumY = mc.thePlayer.onGround ? blocks.get(0).blockPos.getY() : 0;
        double sumZ = 0;
        int index = 0;

        for (Scaffold.PlaceData placeData : blocks) {
            if (index > 1 || (!ScaffoldUtils.scaffoldDiagonal(false) && index > 0 && mc.thePlayer.onGround)) {
                break;
            }
            sumX += placeData.blockPos.getX();
            if (!mc.thePlayer.onGround) {
                sumY += placeData.blockPos.getY();
            }
            sumZ += placeData.blockPos.getZ();
            index++;
        }

        if (index == 0) {
            return null;
        }

        double avgX = sumX / index;
        double avgY = mc.thePlayer.onGround ? blocks.get(0).blockPos.getY() : sumY / index;
        double avgZ = sumZ / index;

        state.targetBlock = new Vec3(avgX, avgY, avgZ);

        Scaffold.PlaceData placeData = blocks.get(0);
        EnumFacing blockFacing = placeData.enumFacing;
        state.lastPlacedFacing = blockFacing;

        Vec3 hitVec = createPlacementHitVec(placeData.blockPos, blockFacing);
        placeData.hitVec = hitVec;
        state.hitVec = hitVec;
        state.lookVec = new Vec3(
                hitVec.xCoord - placeData.blockPos.getX(),
                hitVec.yCoord - placeData.blockPos.getY(),
                hitVec.zCoord - placeData.blockPos.getZ()
        );
        state.currentPlacement = placeData;
        updateBlockRotations();
        return placeData;
    }

    void updateBlockRotations() {
        if (state.currentPlacement == null || state.hitVec == null) {
            state.blockRotations = null;
            return;
        }

        state.blockRotations = RotationUtils.getRotationsToVec(state.hitVec);
    }

    Vec3 createPlacementHitVec(BlockPos blockPos, EnumFacing blockFacing) {
        List<Vec3> samples = getPlacementSamples(blockPos, blockFacing);
        if (samples.isEmpty()) {
            double hitX = blockPos.getX() + 0.5D + getCoord(blockFacing.getOpposite(), "x") * 0.5D;
            double hitY = blockPos.getY() + 0.5D + getCoord(blockFacing.getOpposite(), "y") * 0.5D;
            double hitZ = blockPos.getZ() + 0.5D + getCoord(blockFacing.getOpposite(), "z") * 0.5D;
            return new Vec3(hitX, hitY, hitZ);
        }

        float referenceYaw = state.hasServerRotation ? state.serverYaw : RotationHandler.getRotationYaw();
        float referencePitch = state.hasServerRotation ? state.serverPitch : RotationHandler.getRotationPitch();
        Vec3 bestSample = samples.get(0);
        double bestScore = Double.MAX_VALUE;

        for (Vec3 sample : samples) {
            float[] rotations = RotationUtils.getRotationsToVec(sample);
            float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(rotations[0] - referenceYaw));
            float pitchDiff = Math.abs(rotations[1] - referencePitch);
            double eyeDistance = mc.thePlayer.getPositionEyes(1.0F).distanceTo(sample);
            double score = yawDiff + pitchDiff * 1.35D + eyeDistance * 0.15D;
            if (score < bestScore) {
                bestScore = score;
                bestSample = sample;
            }
        }

        return bestSample;
    }

    private List<Vec3> getPlacementSamples(BlockPos blockPos, EnumFacing blockFacing) {
        double centerX = blockPos.getX() + 0.5D + getCoord(blockFacing.getOpposite(), "x") * 0.5D;
        double centerY = blockPos.getY() + 0.5D + getCoord(blockFacing.getOpposite(), "y") * 0.5D;
        double centerZ = blockPos.getZ() + 0.5D + getCoord(blockFacing.getOpposite(), "z") * 0.5D;

        double[][] offsets = {
                {0.0D, 0.0D},
                {0.16D, 0.0D},
                {-0.16D, 0.0D},
                {0.0D, 0.16D},
                {0.0D, -0.16D}
        };

        List<Vec3> samples = new ArrayList<>(offsets.length);
        for (double[] offset : offsets) {
            double hitX = centerX;
            double hitY = centerY;
            double hitZ = centerZ;

            switch (blockFacing) {
                case UP:
                case DOWN:
                    hitX = clampAxis(centerX + offset[0], blockPos.getX());
                    hitZ = clampAxis(centerZ + offset[1], blockPos.getZ());
                    break;
                case EAST:
                case WEST:
                    hitY = clampAxis(centerY + offset[0], blockPos.getY());
                    hitZ = clampAxis(centerZ + offset[1], blockPos.getZ());
                    break;
                case NORTH:
                case SOUTH:
                    hitX = clampAxis(centerX + offset[0], blockPos.getX());
                    hitY = clampAxis(centerY + offset[1], blockPos.getY());
                    break;
                default:
                    break;
            }

            samples.add(new Vec3(hitX, hitY, hitZ));
        }
        return samples;
    }

    private double clampAxis(double value, int blockAxis) {
        return Math.max(blockAxis + 0.08D, Math.min(blockAxis + 0.92D, value));
    }

    private double getCoord(EnumFacing facing, String axis) {
        switch (axis) {
            case "x":
                return facing == EnumFacing.WEST ? -0.5D : facing == EnumFacing.EAST ? 0.5D : 0D;
            case "y":
                return facing == EnumFacing.DOWN ? -0.5D : facing == EnumFacing.UP ? 0.5D : 0D;
            case "z":
                return facing == EnumFacing.NORTH ? -0.5D : facing == EnumFacing.SOUTH ? 0.5D : 0D;
            default:
                return 0D;
        }
    }

    private List<Scaffold.PlaceData> findBlocks(int yOffset, int xOffset) {
        int x = (int) Math.floor(mc.thePlayer.posX + xOffset);
        int y = (int) Math.floor((state.startYPos != -1 ? state.startYPos : mc.thePlayer.posY) + yOffset);
        int z = (int) Math.floor(mc.thePlayer.posZ);

        BlockPos base = new BlockPos(x, y - 1, z);
        if (!BlockUtils.replaceable(base)) {
            return null;
        }

        EnumFacing[] allFacings = getFacingsSorted();
        List<EnumFacing> validFacings = new ArrayList<>(5);
        for (EnumFacing facing : allFacings) {
            if (facing != EnumFacing.UP && placeConditions(facing, yOffset, xOffset)) {
                validFacings.add(facing);
            }
        }

        List<Scaffold.PlaceData> possibleBlocks = new ArrayList<>();
        int maxYLayer = 2;
        for (int dy = 1; dy <= maxYLayer; dy++) {
            BlockPos layerBase = new BlockPos(x, y - dy, z);
            if (dy == 1) {
                for (EnumFacing facing : validFacings) {
                    BlockPos neighbor = layerBase.offset(facing);
                    if (!BlockUtils.replaceable(neighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(neighbor))) {
                        possibleBlocks.add(new Scaffold.PlaceData(neighbor, facing.getOpposite()));
                    }
                }
            }
            for (EnumFacing facing : validFacings) {
                BlockPos adjacent = layerBase.offset(facing);
                if (BlockUtils.replaceable(adjacent)) {
                    for (EnumFacing nestedFacing : validFacings) {
                        BlockPos nestedNeighbor = adjacent.offset(nestedFacing);
                        if (!BlockUtils.replaceable(nestedNeighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(nestedNeighbor))) {
                            possibleBlocks.add(new Scaffold.PlaceData(nestedNeighbor, nestedFacing.getOpposite()));
                        }
                    }
                }
            }
            for (EnumFacing facing : validFacings) {
                BlockPos adjacent = layerBase.offset(facing);
                if (BlockUtils.replaceable(adjacent)) {
                    for (EnumFacing nestedFacing : validFacings) {
                        BlockPos nestedNeighbor = adjacent.offset(nestedFacing);
                        if (BlockUtils.replaceable(nestedNeighbor)) {
                            for (EnumFacing thirdFacing : validFacings) {
                                BlockPos thirdNeighbor = nestedNeighbor.offset(thirdFacing);
                                if (!BlockUtils.replaceable(thirdNeighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(thirdNeighbor))) {
                                    possibleBlocks.add(new Scaffold.PlaceData(thirdNeighbor, thirdFacing.getOpposite()));
                                }
                            }
                        }
                    }
                }
            }
        }

        return possibleBlocks.isEmpty() ? null : possibleBlocks;
    }

    private EnumFacing[] getFacingsSorted() {
        float clientYaw = RotationHandler.getRotationYaw();
        EnumFacing lastFacing = EnumFacing.getHorizontal(MathHelper.floor_double((clientYaw * 4.0F / 360.0F) + 0.5D) & 3);
        EnumFacing clockwise = lastFacing.rotateY();
        EnumFacing counterClockwise = lastFacing.rotateYCCW();
        EnumFacing opposite = lastFacing.getOpposite();

        float yaw = clientYaw % 360F;
        if (yaw > 180F) {
            yaw -= 360F;
        } else if (yaw < -180F) {
            yaw += 360F;
        }

        float diffClockwise = Math.abs(MathHelper.wrapAngleTo180_float(yaw - getFacingAngle(clockwise)));
        float diffCounterClockwise = Math.abs(MathHelper.wrapAngleTo180_float(yaw - getFacingAngle(counterClockwise)));

        EnumFacing firstPerp = diffClockwise <= diffCounterClockwise ? clockwise : counterClockwise;
        EnumFacing secondPerp = diffClockwise <= diffCounterClockwise ? counterClockwise : clockwise;
        return new EnumFacing[]{EnumFacing.UP, EnumFacing.DOWN, lastFacing, firstPerp, secondPerp, opposite};
    }

    private float getFacingAngle(EnumFacing facing) {
        switch (facing) {
            case WEST:
                return 90F;
            case NORTH:
                return 180F;
            case EAST:
                return -90F;
            default:
                return 0F;
        }
    }

    private boolean placeConditions(EnumFacing facing, int yOffset, int xOffset) {
        if (xOffset == -1) {
            return facing == EnumFacing.EAST;
        }
        if (yOffset == 1) {
            return facing == EnumFacing.DOWN;
        }
        return true;
    }
}

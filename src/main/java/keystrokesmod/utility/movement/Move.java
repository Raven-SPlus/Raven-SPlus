package keystrokesmod.utility.movement;

import keystrokesmod.utility.RotationUtils;
import lombok.Getter;
import org.jetbrains.annotations.Contract;

@Getter
public enum Move {
    FORWARD(0, 0.98f, 0f, Direction.POSITIVE_Z),
    FORWARD_RIGHT(45, 0.98f, -0.98f, Direction.POSITIVE_Z),
    RIGHT(90, 0f, -0.98f, Direction.NEGATIVE_X),
    BACKWARD_RIGHT(135, -0.98f, -0.98f, Direction.NEGATIVE_X),
    BACKWARD(180, -0.98f, 0f, Direction.NEGATIVE_Z),
    BACKWARD_LEFT(225, -0.98f, 0.98f, Direction.NEGATIVE_Z),
    LEFT(270, 0f, 0.98f, Direction.POSITIVE_X),
    FORWARD_LEFT(315, 0.98f, 0.98f, Direction.POSITIVE_X);

    private final float deltaYaw;
    private final float forward;
    private final float strafing;
    private final Direction direction;

    Move(float deltaYaw, float forward, float strafing, Direction direction) {
        this.deltaYaw = deltaYaw;
        this.forward = forward;
        this.strafing = strafing;
        this.direction = direction;
    }

    @Contract(pure = true)
    public static Move fromMovement(float forward, float strafing) {
        if (forward > 0)
            if (strafing > 0)
                return FORWARD_LEFT;
            else if (strafing < 0)
                return FORWARD_RIGHT;
            else
                return FORWARD;
        else if (forward < 0)
            if (strafing > 0)
                return BACKWARD_LEFT;
            else if (strafing < 0)
                return BACKWARD_RIGHT;
            else
                return BACKWARD;
        else
        if (strafing > 0)
            return LEFT;
        else if (strafing < 0)
            return RIGHT;
        else
            return FORWARD;
    }

    /**
     * Find the move direction closest to the yaw delta between view and rotation.
     * @param yaw yaw delta in [0, 360)
     * @return closest move direction for the given yaw delta
     */
    @Contract(pure = true)
    public static Move fromDeltaYaw(float yaw) {
        yaw = RotationUtils.normalize(yaw, 0, 360);

        Move bestMove = FORWARD;
        float bestDeltaYaw = Math.abs(yaw - bestMove.getDeltaYaw());

        for (Move move : values()) {
            if (move != bestMove) {
                float deltaYaw = Math.abs(yaw - move.getDeltaYaw());
                if (deltaYaw < bestDeltaYaw) {
                    bestMove = move;
                    bestDeltaYaw = deltaYaw;
                }
            }
        }

        return bestMove;
    }

    public Move reserve() {
        switch (this) {
            case FORWARD:
                return BACKWARD;
            case FORWARD_RIGHT:
                return BACKWARD_LEFT;
            case RIGHT:
                return LEFT;
            case BACKWARD_RIGHT:
                return FORWARD_LEFT;
            default:
            case BACKWARD:
                return FORWARD;
            case BACKWARD_LEFT:
                return FORWARD_RIGHT;
            case LEFT:
                return RIGHT;
            case FORWARD_LEFT:
                return BACKWARD_RIGHT;
        }
    }
}
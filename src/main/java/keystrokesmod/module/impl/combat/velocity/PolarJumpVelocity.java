package keystrokesmod.module.impl.combat.velocity;

import keystrokesmod.event.PreVelocityEvent;
import keystrokesmod.module.impl.combat.Velocity;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * PolarJump velocity bypass - ported from FireBounce.
 * Jumps when hurtTime matches polarhurtTime to bypass Polar anticheat.
 */
public class PolarJumpVelocity extends SubMode<Velocity> {

    private final ButtonSetting forceChangehurtTime;
    private final SliderSetting forceChangehurtTimeCount;
    private final ButtonSetting debug;

    private int polarhurtTime = 0;
    private int polarhurtCount = 0;
    private boolean hasReceivedVelocity = false;
    private final Random random = new Random();

    public PolarJumpVelocity(String name, @NotNull Velocity parent) {
        super(name, parent);
        this.registerSetting(forceChangehurtTime = new ButtonSetting("Force change hurt time", true));
        this.registerSetting(forceChangehurtTimeCount = new SliderSetting("Hurt count", 5, 1, 10, 1, forceChangehurtTime::isToggled));
        this.registerSetting(debug = new ButtonSetting("Debug", false));
    }

    @Override
    public void onEnable() {
        polarhurtTime = 7 + random.nextInt(3);
        polarhurtCount = 0;
        hasReceivedVelocity = false;
    }

    @SubscribeEvent
    public void onPreVelocity(PreVelocityEvent event) {
        hasReceivedVelocity = true;
        polarhurtTime = 7 + random.nextInt(3);
        polarhurtCount++;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Utils.nullCheck()) return;
        if (!hasReceivedVelocity) return;

        if (polarhurtTime == mc.thePlayer.hurtTime && mc.thePlayer.onGround) {
            mc.thePlayer.jump();
            if (debug.isToggled()) Utils.sendMessage("[PolarJump] Jumped");
            polarhurtTime = 7 + random.nextInt(3);
            if (debug.isToggled()) Utils.sendMessage("[PolarJump] NextJumpHurtTime: " + polarhurtTime);
        }
        if (forceChangehurtTime.isToggled() && polarhurtCount >= forceChangehurtTimeCount.getInput()) {
            polarhurtCount = 0;
            polarhurtTime = 7 + random.nextInt(3);
            if (debug.isToggled()) Utils.sendMessage("[PolarJump] ForceChange-NextJumpHurtTime: " + polarhurtTime);
        }
    }
}

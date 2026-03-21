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

/**
 * Intave14 velocity bypass - ported from FireBounce.
 * Reduces XZ velocity when hurtTime >= 9 (Intave14 style).
 */
public class Intave14Velocity extends SubMode<Velocity> {

    private final SliderSetting horizontal;
    private final SliderSetting vertical;
    private final SliderSetting chance;
    private final ButtonSetting debug;

    public Intave14Velocity(String name, @NotNull Velocity parent) {
        super(name, parent);
        this.registerSetting(horizontal = new SliderSetting("Horizontal", 0.0, 0, 1, 0.01));
        this.registerSetting(vertical = new SliderSetting("Vertical", 100.0, 0, 100, 1));
        this.registerSetting(chance = new SliderSetting("Chance", 100, 0, 100, 1, "%"));
        this.registerSetting(debug = new ButtonSetting("Debug", false));
    }

    @SubscribeEvent
    public void onPreVelocity(PreVelocityEvent event) {
        if (Utils.nullCheck()) return;
        if (chance.getInput() != 100 && Math.random() * 100 > chance.getInput()) return;

        double xzFactor = 1.0 - horizontal.getInput();
        double yFactor = vertical.getInput() / 100.0;

        if (mc.thePlayer.hurtTime >= 9) {
            event.setMotionX((int) (event.getMotionX() * xzFactor));
            event.setMotionY((int) (event.getMotionY() * yFactor));
            event.setMotionZ((int) (event.getMotionZ() * xzFactor));
            if (debug.isToggled()) Utils.sendMessage("[Intave14] Reduced");
        }
    }
}

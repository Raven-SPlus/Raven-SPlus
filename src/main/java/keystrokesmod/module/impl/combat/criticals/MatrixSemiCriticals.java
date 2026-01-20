package keystrokesmod.module.impl.combat.criticals;

import keystrokesmod.module.impl.combat.Criticals;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.PacketUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class MatrixSemiCriticals extends SubMode<Criticals> {
    private final SliderSetting delay;
    private final SliderSetting hurtTime;

    private long lastAttackTime = 0;
    private int attacks = 0;

    @Override
    public void onEnable() {
        attacks = 0;
    }

    public MatrixSemiCriticals(String name, @NotNull Criticals parent) {
        super(name, parent);
        this.registerSetting(delay = new SliderSetting("Delay", 0, 0, 500, 10, "ms"));
        this.registerSetting(hurtTime = new SliderSetting("HurtTime", 10, 0, 10, 1));
    }

    @SubscribeEvent
    public void onAttack(@NotNull AttackEntityEvent event) {
        if (event.target instanceof EntityLivingBase) {
            EntityLivingBase entity = (EntityLivingBase) event.target;
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastAttackTime < delay.getInput()) return;
            if (entity.hurtTime > hurtTime.getInput()) return;

            double x = mc.thePlayer.posX;
            double y = mc.thePlayer.posY;
            double z = mc.thePlayer.posZ;

            attacks++;
            if (attacks > 3) {
                PacketUtils.sendPacketNoEvent(new C04PacketPlayerPosition(x, y + 0.0825080378093, z, false));
                PacketUtils.sendPacketNoEvent(new C04PacketPlayerPosition(x, y + 0.023243243674, z, false));
                PacketUtils.sendPacketNoEvent(new C04PacketPlayerPosition(x, y + 0.0215634532004, z, false));
                PacketUtils.sendPacketNoEvent(new C04PacketPlayerPosition(x, y + 0.00150000001304, z, false));
                attacks = 0;
            }

            lastAttackTime = currentTime;
        }
    }
}

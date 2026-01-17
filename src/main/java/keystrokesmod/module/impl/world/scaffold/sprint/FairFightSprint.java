package keystrokesmod.module.impl.world.scaffold.sprint;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.mixins.impl.client.KeyBindingAccessor;
import keystrokesmod.module.impl.world.Scaffold;
import keystrokesmod.module.impl.world.scaffold.IScaffoldSprint;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.MoveUtil;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/**
 * FairFight Anti-Cheat Scaffold Bypass
 * 
 * Exploits:
 * 1. ScaffoldD: Sneak toggle every 20 ticks disables the placement speed check
 *    Code: "if (!data.isBridging() || !wrapper.isPlacedBlock() || data.getTick() - lastSneak <= 20) return;"
 * 
 * 2. ScaffoldE (SafeWalk): Sneaking exempts from the check entirely
 *    Code: "boolean exempt = !data.isBridging() || !data.isClientGround() || data.isSneaking();"
 * 
 * 3. ScaffoldG (Success ratio): Sneaking also exempts this check
 *    Code: "if (packet instanceof CPacketBlockPlace && data.isBridging() && !data.isSneaking() ..."
 * 
 * By toggling sneak every ~15-19 ticks, we bypass most scaffold checks while maintaining speed.
 */
public class FairFightSprint extends IScaffoldSprint {
    private final DescriptionSetting desc = new DescriptionSetting("Bypasses FairFight scaffold checks");
    
    private final SliderSetting sneakInterval;
    private final SliderSetting sneakDuration;
    private final ButtonSetting randomizeInterval;
    private final SliderSetting randomRange;
    private final ButtonSetting sprintBetweenSneaks;
    private final ButtonSetting jumpOnSneak;
    
    // State tracking
    private int ticksSinceSneak = 0;
    private int sneakingTicks = 0;
    private boolean isSneaking = false;
    private int currentInterval = 15;
    
    public FairFightSprint(String name, Scaffold parent) {
        super(name, parent);
        this.registerSetting(desc);
        this.registerSetting(sneakInterval = new SliderSetting("Sneak interval", 15, 10, 19, 1,
                "Ticks between sneaks (FF checks <=20)"));
        this.registerSetting(sneakDuration = new SliderSetting("Sneak duration", 2, 1, 5, 1,
                "How many ticks to hold sneak"));
        this.registerSetting(randomizeInterval = new ButtonSetting("Randomize interval", true,
                "Vary timing to look more legit"));
        this.registerSetting(randomRange = new SliderSetting("Random range", 3, 1, 5, 1,
                randomizeInterval::isToggled));
        this.registerSetting(sprintBetweenSneaks = new ButtonSetting("Sprint between sneaks", true,
                "Sprint when not sneaking"));
        this.registerSetting(jumpOnSneak = new ButtonSetting("Jump on sneak", false,
                "Jump when starting sneak (bypass ScaffoldH tower check)"));
    }
    
    @Override
    public void onEnable() {
        ticksSinceSneak = 0;
        sneakingTicks = 0;
        isSneaking = false;
        calculateNextInterval();
    }
    
    @Override
    public void onDisable() {
        if (isSneaking) {
            ((KeyBindingAccessor) mc.gameSettings.keyBindSneak).setPressed(
                    Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));
            isSneaking = false;
        }
    }
    
    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck() || !MoveUtil.isMoving()) {
            if (isSneaking) {
                stopSneak();
            }
            return;
        }
        
        ticksSinceSneak++;
        
        if (isSneaking) {
            sneakingTicks++;
            
            // Stop sneaking after duration
            if (sneakingTicks >= sneakDuration.getInput()) {
                stopSneak();
                calculateNextInterval();
            }
        } else {
            // Start sneaking at interval
            if (ticksSinceSneak >= currentInterval) {
                startSneak();
            }
        }
    }
    
    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (!Utils.nullCheck()) return;
        
        // Sprint between sneaks
        if (sprintBetweenSneaks.isToggled() && !isSneaking && MoveUtil.isMoving()) {
            if (!mc.thePlayer.isSprinting()) {
                mc.thePlayer.setSprinting(true);
            }
        }
    }
    
    private void startSneak() {
        isSneaking = true;
        sneakingTicks = 0;
        ticksSinceSneak = 0;
        
        // Set both key binding state and pressed state for sneak to work properly
        net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
        ((KeyBindingAccessor) mc.gameSettings.keyBindSneak).setPressed(true);
        
        // Optional: jump when starting sneak to bypass tower check
        if (jumpOnSneak.isToggled() && mc.thePlayer.onGround) {
            mc.thePlayer.jump();
        }
    }
    
    private void stopSneak() {
        isSneaking = false;
        boolean wasHoldingSneak = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
        // Restore both states
        net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), wasHoldingSneak);
        ((KeyBindingAccessor) mc.gameSettings.keyBindSneak).setPressed(wasHoldingSneak);
    }
    
    private void calculateNextInterval() {
        int base = (int) sneakInterval.getInput();
        
        if (randomizeInterval.isToggled()) {
            int range = (int) randomRange.getInput();
            currentInterval = base + Utils.randomizeInt(-range, range);
            // Ensure we stay under 20 ticks (FF's check window)
            currentInterval = Math.min(Math.max(currentInterval, 8), 19);
        } else {
            currentInterval = base;
        }
    }
    
    @Override
    public boolean isSprint() {
        return sprintBetweenSneaks.isToggled() && !isSneaking;
    }
    
    @Override
    public boolean onPreSchedulePlace() {
        return true;
    }
}


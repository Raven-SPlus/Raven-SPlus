package keystrokesmod.module.impl.client;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ModeValue;
import keystrokesmod.module.setting.impl.SubMode;

public class MemoryFix extends Module {
    public final ModeValue gcMode;
    public final ButtonSetting scalePackIcons;
    public final ButtonSetting fixCapeLeak;

    // GC mode constants
    public static final int GC_DISABLED = 0;  // Block all explicit GC (default, safe)
    public static final int GC_FORCE = 1;     // Allow explicit GC (original Minecraft behavior)

    public MemoryFix() {
        super("MemoryFix", category.client);
        this.registerSetting(
                gcMode = new ModeValue("GC mode", this,
                        new SubMode<Module>("Disabled", this) {},   // 0 - safe default
                        new SubMode<Module>("Force GC", this) {}    // 1 - classic behavior
                ),
                scalePackIcons = new ButtonSetting("ScalePackIcons", true),
                fixCapeLeak = new ButtonSetting("FixCapeLeak", true)
        );
    }
}

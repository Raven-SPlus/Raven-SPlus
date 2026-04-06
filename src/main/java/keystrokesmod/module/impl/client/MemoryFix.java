package keystrokesmod.module.impl.client;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;

public class MemoryFix extends Module {
    public final ButtonSetting disableExplicitGc;
    public final ButtonSetting scalePackIcons;
    public final ButtonSetting fixCapeLeak;

    public MemoryFix() {
        super("MemoryFix", category.client);
        this.registerSetting(
                disableExplicitGc = new ButtonSetting("DisableExplicitGC", true),
                scalePackIcons = new ButtonSetting("ScalePackIcons", true),
                fixCapeLeak = new ButtonSetting("FixCapeLeak", true)
        );
    }
}

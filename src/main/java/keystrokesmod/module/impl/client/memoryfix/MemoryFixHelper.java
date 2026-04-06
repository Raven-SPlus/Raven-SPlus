package keystrokesmod.module.impl.client.memoryfix;

import keystrokesmod.module.ModuleManager;

public final class MemoryFixHelper {
    private MemoryFixHelper() {
    }

    public static boolean isMemoryFixEnabled() {
        return ModuleManager.memoryFix != null && ModuleManager.memoryFix.isEnabled();
    }

    public static boolean shouldDisableExplicitGc() {
        return isMemoryFixEnabled() && ModuleManager.memoryFix.disableExplicitGc.isToggled();
    }

    public static boolean shouldScalePackIcons() {
        return isMemoryFixEnabled() && ModuleManager.memoryFix.scalePackIcons.isToggled();
    }

    public static boolean shouldFixCapeLeak() {
        return isMemoryFixEnabled() && ModuleManager.memoryFix.fixCapeLeak.isToggled();
    }
}

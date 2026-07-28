package keystrokesmod.module.impl.client.memoryfix;

import keystrokesmod.module.ModuleManager;

public final class MemoryFixHelper {
    private MemoryFixHelper() {
    }

    public static boolean isMemoryFixEnabled() {
        return ModuleManager.memoryFix != null && ModuleManager.memoryFix.isEnabled();
    }

    /**
     * Returns true when explicit GC should be BLOCKED.
     * The default mode (GC_DISABLED / index 0) blocks all explicit GC calls.
     * Force GC mode (index 1) allows them through as vanilla Minecraft does.
     */
    public static boolean shouldDisableExplicitGc() {
        if (!isMemoryFixEnabled()) return false;
        return ModuleManager.memoryFix.gcMode.getInput() == 0; // GC_DISABLED
    }

    public static boolean shouldScalePackIcons() {
        return isMemoryFixEnabled() && ModuleManager.memoryFix.scalePackIcons.isToggled();
    }

    public static boolean shouldFixCapeLeak() {
        return isMemoryFixEnabled() && ModuleManager.memoryFix.fixCapeLeak.isToggled();
    }
}

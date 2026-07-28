package keystrokesmod.module;

import keystrokesmod.Raven;
import keystrokesmod.module.impl.client.ClickGUI;
import keystrokesmod.module.impl.client.Notifications;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.script.Script;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.i18n.I18nModule;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class Module {
    private @Nullable I18nModule i18nObject = null;

    protected final ArrayList<Setting> settings;
    private final HashSet<Setting> settingsWeak;
    private final String moduleName;
    private String prettyName;
    private String prettyInfo = "";
    private final Module.category moduleCategory;
    private boolean enabled;
    private int keycode;
    private final @Nullable String toolTip;
    protected static Minecraft mc;
    private boolean isToggled = false;
    public boolean canBeEnabled = true;
    public boolean ignoreOnSave = false;
    public boolean hidden = false;
    public Script script = null;
    
    // Cached formatted pretty name - invalidated when underlying name changes
    private String cachedPrettyName = null;
    private String cachedPrettyNameSource = null;
    
    // Silent usage tracking - allows modules to use this module without enabling it
    private final java.util.Set<Module> silentUsers = new java.util.HashSet<>();

    public Module(String moduleName, Module.category moduleCategory, int keycode) {
        this(moduleName, moduleCategory, keycode, null);
    }

    public Module(String moduleName, Module.category moduleCategory, int keycode, @Nullable String toolTip) {
        this.moduleName = moduleName;
        this.prettyName = Utils.formatModuleName(moduleName);
        this.moduleCategory = moduleCategory;
        this.keycode = keycode;
        this.toolTip = toolTip;
        this.enabled = false;
        mc = Minecraft.getMinecraft();
        this.settings = new ArrayList<>();
        this.settingsWeak = new HashSet<>();
        if (!(this instanceof SubMode))
            Raven.moduleCounter++;
    }

    public static @Nullable Module getModule(Class<? extends Module> a) {
        Iterator<Module> var1 = ModuleManager.modules.iterator();

        Module module;
        do {
            if (!var1.hasNext()) {
                return null;
            }

            module = var1.next();
        } while (module.getClass() != a);

        return module;
    }

    public Module(String name, Module.category moduleCategory) {
        this(name, moduleCategory, null);
    }

    public Module(String name, Module.category moduleCategory, String toolTip) {
        this(name, moduleCategory, 0, toolTip);
    }

    public Module(@NotNull Script script) {
        this(script.name, category.scripts);
        this.script = script;
    }

    // Explicit getters/setters to avoid Lombok dependency at compile time
    public @Nullable I18nModule getI18nObject() {
        return this.i18nObject;
    }

    public void setI18nObject(@Nullable I18nModule i18nObject) {
        this.i18nObject = i18nObject;
        this.cachedPrettyName = null;
        this.cachedPrettyNameSource = null;
    }

    public ArrayList<Setting> getSettings() {
        return this.settings;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getKeycode() {
        return this.keycode;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void keybind() {
        if (this.keycode != 0) {
            try {
                if (!this.isToggled && (this.keycode >= 1000 ? Mouse.isButtonDown(this.keycode - 1000) : Keyboard.isKeyDown(this.keycode))) {
                    this.toggle();
                    this.isToggled = true;
                } else if ((this.keycode >= 1000 ? !Mouse.isButtonDown(this.keycode - 1000) : !Keyboard.isKeyDown(this.keycode))) {
                    this.isToggled = false;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                Utils.sendMessage("&cFailed to check keybinding. Setting to none");
                this.keycode = 0;
            }
        }
    }

    public final boolean canBeEnabled() {
        if (this.script != null && script.error) {
            return false;
        }
        return this.canBeEnabled;
    }

    private boolean hasSilentUsers() {
        synchronized (silentUsers) {
            return !silentUsers.isEmpty();
        }
    }

    private void activateRuntime() {
        if (this.script != null) {
            Raven.scriptManager.onEnable(script);
            return;
        }

        try {
            FMLCommonHandler.instance().bus().register(this);
            this.onEnable();
        } catch (Throwable ignored) {
        }
    }

    private void deactivateRuntime() {
        if (this.script != null) {
            Raven.scriptManager.onDisable(script);
            return;
        }

        try {
            FMLCommonHandler.instance().bus().unregister(this);
            this.onDisable();
        } catch (Throwable ignored) {
        }
    }

    private void updateRuntimeState(boolean wasActive) {
        boolean isActive = this.isActive();
        if (wasActive == isActive) {
            return;
        }

        if (isActive) {
            activateRuntime();
        } else {
            deactivateRuntime();
        }
    }

    public final void enable() {
        if (!this.canBeEnabled() || this.isEnabled()) {
            return;
        }
        boolean wasActive = this.isActive();
        this.setEnabled(true);
        ModuleManager.organizedModules.add(this);
        if (ModuleManager.hud != null && ModuleManager.hud.isEnabled()) {
            ModuleManager.sort();
        }
        updateRuntimeState(wasActive);
    }

    public final void disable() {
        if (!this.isEnabled()) {
            return;
        }
        boolean wasActive = this.isActive();
        this.setEnabled(false);
        ModuleManager.organizedModules.remove(this);
        updateRuntimeState(wasActive);
    }
    
    /**
     * Use this module silently without enabling it. The module's functionality will be active
     * but it won't appear as enabled in the GUI or module list.
     * @param user The module that wants to use this module silently
     */
    public void useSilently(Module user) {
        if (user == null) return;
        boolean shouldActivate = false;
        synchronized (silentUsers) {
            boolean wasActive = this.enabled || !silentUsers.isEmpty();
            silentUsers.add(user);
            shouldActivate = !wasActive && (this.enabled || !silentUsers.isEmpty());
        }

        if (shouldActivate) {
            activateRuntime();
        }
    }
    
    /**
     * Stop using this module silently.
     * @param user The module that was using this module silently
     */
    public void stopUsingSilently(Module user) {
        if (user == null) return;
        boolean shouldDeactivate = false;
        synchronized (silentUsers) {
            boolean wasActive = this.enabled || !silentUsers.isEmpty();
            silentUsers.remove(user);
            shouldDeactivate = wasActive && !this.enabled && silentUsers.isEmpty();
        }

        if (shouldDeactivate) {
            deactivateRuntime();
        }
    }
    
    /**
     * Check if this module is being used silently (by other modules without being enabled).
     * @return true if being used silently
     */
    public boolean isUsedSilently() {
        return hasSilentUsers();
    }
    
    /**
     * Check if this module is active (either enabled or being used silently).
     * @return true if the module is active
     */
    public boolean isActive() {
        return this.isEnabled() || hasSilentUsers();
    }

    public String getInfo() {
        return "";
    }

    public String getPrettyInfo() {
        return ModuleManager.customName.isEnabled() && ModuleManager.customName.info.isToggled()
                ? getRawPrettyInfo()
                : getInfo();
    }

    public String getName() {
        return this.moduleName;
    }

    public String getPrettyName() {
        String name = ModuleManager.customName.isEnabled()
                ? getRawPrettyName()
                : i18nObject != null ? i18nObject.getName() : getName();
        // Cache: formatModuleName is expensive, and names rarely change at runtime.
        if (cachedPrettyName == null || !name.equals(cachedPrettyNameSource)) {
            cachedPrettyNameSource = name;
            cachedPrettyName = Utils.formatModuleName(name);
        }
        return cachedPrettyName;
    }

    public @Nullable String getToolTip() {
        return toolTip;
    }

    public @Nullable String getPrettyToolTip() {
        return i18nObject != null ? i18nObject.getToolTip() : getToolTip();
    }

    public String getRawPrettyName() {
        return prettyName;
    }

    public String getRawPrettyInfo() {
        return prettyInfo.isEmpty() ? getInfo() : prettyInfo;
    }

    public final void setPrettyName(String name) {
        this.prettyName = name;
        this.cachedPrettyName = null;
        this.cachedPrettyNameSource = null;
        ModuleManager.sort();
    }

    public final void setPrettyInfo(String name) {
        this.prettyInfo = name;
        ModuleManager.sort();
    }

    public void registerSetting(Setting setting) {
        synchronized (settings) {
            if (settingsWeak.contains(setting))
                throw new RuntimeException("Setting '" + setting.getName() + "' is already registered in module '" + this.getName() + "'!");

            this.settingsWeak.add(setting);
            this.settings.add(setting);
            setting.setParent(this);
        }
    }

    public void registerSetting(Setting @NotNull ... setting) {
        for (Setting set : setting) {
            registerSetting(set);
        }
    }

    public void registerSetting(@NotNull Iterable<Setting> setting) {
        for (Setting set : setting) {
            registerSetting(set);
        }
    }

    public void unregisterSetting(@NotNull Setting setting) {
        synchronized (settings) {
            this.settings.remove(setting);
            this.settingsWeak.remove(setting);
        }
    }

    public final Module.category moduleCategory() {
        return this.moduleCategory;
    }

    public void onEnable() throws Throwable {
    }

    public void onDisable() throws Throwable {
    }

    public void toggle() {
        boolean wasEnabled = this.isEnabled();
        if (wasEnabled) {
            this.disable();
            if (Settings.toggleSound.getInput() != 0) mc.thePlayer.playSound(Settings.getToggleSound(false), 1, 1);
            // Only show notification if module was actually disabled (state changed)
            if (Notifications.moduleToggled.isToggled() && !(this instanceof ClickGUI) && this.isEnabled() != wasEnabled)
                Notifications.sendModuleToggleNotification(Notifications.NotificationTypes.INFO, "§4Disabled " + this.getPrettyName(), this.getPrettyName());
        } else {
            this.enable();
            if (Settings.toggleSound.getInput() != 0) mc.thePlayer.playSound(Settings.getToggleSound(true), 1, 1);
            // Only show notification if module was actually enabled (state changed)
            if (Notifications.moduleToggled.isToggled() && !(this instanceof ClickGUI) && this.isEnabled() != wasEnabled)
                Notifications.sendModuleToggleNotification(Notifications.NotificationTypes.INFO, "§2Enabled " + this.getPrettyName(), this.getPrettyName());
        }

    }

    public void onUpdate() throws Throwable {
    }

    public void guiUpdate() throws Throwable {
    }

    public void guiButtonToggled(ButtonSetting b) throws Exception {
    }

    public void setBind(int keybind) {
        this.keycode = keybind;
    }


    public enum category {
        combat,
        movement,
        player,
        world,
        render,
        minigames,
        fun,
        other,
        client,
        profiles,
        scripts,
        exploit,
        experimental
    }
}
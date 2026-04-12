package keystrokesmod.render.glide.adapter;

import keystrokesmod.Raven;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;

import java.util.ArrayList;
import java.util.List;

public final class RavenModAdapter {

    public enum SettingType {
        BUTTON,
        SLIDER,
        MODE,
        DESCRIPTION
    }

    public static final class SettingEntry {
        private final Setting setting;
        private final SettingType type;

        SettingEntry(Setting setting, SettingType type) {
            this.setting = setting;
            this.type = type;
        }

        public String getName() {
            return setting.getName();
        }

        public SettingType getType() {
            return type;
        }

        public boolean isVisible() {
            return setting.isVisible();
        }

        public Setting getRaw() {
            return setting;
        }

        public boolean getBooleanValue() {
            if (setting instanceof ButtonSetting) {
                return ((ButtonSetting) setting).isToggled();
            }
            return false;
        }

        public void setBooleanValue(boolean value) {
            if (setting instanceof ButtonSetting) {
                ButtonSetting buttonSetting = (ButtonSetting) setting;
                if (buttonSetting.isMethodButton) {
                    return;
                }
                if (buttonSetting.isToggled() != value) {
                    buttonSetting.toggle();
                    Module parent = setting.getParent();
                    if (parent != null) {
                        try {
                            parent.guiButtonToggled(buttonSetting);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        public boolean isActionButton() {
            return setting instanceof ButtonSetting && ((ButtonSetting) setting).isMethodButton;
        }

        public void runAction() {
            if (setting instanceof ButtonSetting) {
                ButtonSetting buttonSetting = (ButtonSetting) setting;
                buttonSetting.runMethod();
                Module parent = setting.getParent();
                if (parent != null) {
                    try {
                        parent.guiButtonToggled(buttonSetting);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        public double getDoubleValue() {
            if (setting instanceof SliderSetting) {
                return ((SliderSetting) setting).getInput();
            }
            if (setting instanceof ModeSetting) {
                return ((ModeSetting) setting).getInput();
            }
            return 0;
        }

        public void setDoubleValue(double value) {
            if (setting instanceof SliderSetting) {
                ((SliderSetting) setting).setValue(value);
            } else if (setting instanceof ModeSetting) {
                ((ModeSetting) setting).setValue(value);
            }
        }

        public double getMin() {
            if (setting instanceof SliderSetting) {
                return ((SliderSetting) setting).getMin();
            }
            if (setting instanceof ModeSetting) {
                return ((ModeSetting) setting).getMin();
            }
            return 0;
        }

        public double getMax() {
            if (setting instanceof SliderSetting) {
                return ((SliderSetting) setting).getMax();
            }
            if (setting instanceof ModeSetting) {
                return ((ModeSetting) setting).getMax();
            }
            return 0;
        }

        public double getInterval() {
            if (setting instanceof SliderSetting) {
                return ((SliderSetting) setting).getIntervals();
            }
            return 1;
        }

        public String[] getModeOptions() {
            if (setting instanceof ModeSetting) {
                return ((ModeSetting) setting).getPrettyOptions();
            }
            if (setting instanceof SliderSetting && ((SliderSetting) setting).getOptions() != null) {
                return ((SliderSetting) setting).getOptions();
            }
            return new String[0];
        }

        public String getDescription() {
            if (setting instanceof DescriptionSetting) {
                return ((DescriptionSetting) setting).getDesc();
            }
            return "";
        }
    }

    public static final class ModEntry {
        private final Module module;

        ModEntry(Module module) {
            this.module = module;
        }

        public String getName() {
            return module.getPrettyName();
        }

        public String getRawName() {
            return module.getName();
        }

        public boolean isToggled() {
            return module.isEnabled();
        }

        public void setToggled(boolean state) {
            if (state && !module.isEnabled()) {
                module.enable();
            } else if (!state && module.isEnabled()) {
                module.disable();
            }
        }

        public String getCategory() {
            return module.moduleCategory().name();
        }

        public List<SettingEntry> getSettings() {
            List<SettingEntry> entries = new ArrayList<SettingEntry>();
            for (Setting s : module.getSettings()) {
                SettingType type;
                if (s instanceof DescriptionSetting) {
                    // Compact Glide settings scene cannot lay out long descriptions cleanly.
                    continue;
                } else if (s instanceof ButtonSetting) {
                    type = SettingType.BUTTON;
                } else if (s instanceof SliderSetting && ((SliderSetting) s).getOptions() != null) {
                    type = SettingType.MODE;
                } else if (s instanceof SliderSetting) {
                    type = SettingType.SLIDER;
                } else if (s instanceof ModeSetting) {
                    type = SettingType.MODE;
                } else {
                    continue;
                }
                entries.add(new SettingEntry(s, type));
            }
            return entries;
        }

        public int getKeyBind() {
            return module.getKeycode();
        }

        public boolean isHidden() {
            return module.isHidden();
        }

        public Module getRaw() {
            return module;
        }
    }

    public List<ModEntry> getMods() {
        ModuleManager mgr = Raven.getModuleManager();
        if (mgr == null) {
            return new ArrayList<ModEntry>();
        }
        List<Module> modules = mgr.getModules();
        List<ModEntry> entries = new ArrayList<ModEntry>(modules.size());
        for (Module m : modules) {
            if (m instanceof SubMode) {
                continue;
            }
            entries.add(new ModEntry(m));
        }
        return entries;
    }

    public List<ModEntry> getModsByCategory(String category) {
        List<ModEntry> result = new ArrayList<ModEntry>();
        ModuleManager mgr = Raven.getModuleManager();
        if (mgr == null) {
            return result;
        }
        for (Module m : mgr.getModules()) {
            if (m instanceof SubMode) {
                continue;
            }
            if (m.moduleCategory().name().equalsIgnoreCase(category)) {
                result.add(new ModEntry(m));
            }
        }
        return result;
    }
}

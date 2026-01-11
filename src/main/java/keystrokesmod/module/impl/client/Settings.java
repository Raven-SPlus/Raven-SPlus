package keystrokesmod.module.impl.client;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import org.jetbrains.annotations.NotNull;

public class Settings extends Module {
    public static ButtonSetting weaponSword;
    public static ButtonSetting weaponAxe;
    public static ButtonSetting weaponRod;
    public static ButtonSetting weaponStick;
    public static ButtonSetting smartAnimatedTextures;
    public static ButtonSetting gpuUiRoundedRects;
    public static ButtonSetting gpu3DRendering;
    public static SliderSetting offset;
    public static SliderSetting timeMultiplier;
    public static ModeSetting toggleSound;
    public static ButtonSetting sendMessage;
    public static ModeSetting backgroundMode;
    
    // Target filter settings
    public static ButtonSetting targetPlayers;
    public static ButtonSetting targetPassive;
    public static ButtonSetting targetNeutral;
    public static ButtonSetting targetHostile;
    public static ButtonSetting targetGolems;
    public static ButtonSetting targetVillagers;
    public static ButtonSetting targetOthers;

    public Settings() {
        super("Settings", category.client, 0);
        this.registerSetting(new DescriptionSetting("General"));
        this.registerSetting(weaponSword = new ButtonSetting("Set sword as weapon", true));
        this.registerSetting(weaponAxe = new ButtonSetting("Set axe as weapon", false));
        this.registerSetting(weaponRod = new ButtonSetting("Set rod as weapon", false));
        this.registerSetting(weaponStick = new ButtonSetting("Set stick as weapon", false));
        this.registerSetting(new DescriptionSetting("Performance"));
        this.registerSetting(smartAnimatedTextures = new ButtonSetting("Smart animated textures", true));
        this.registerSetting(gpuUiRoundedRects = new ButtonSetting("GPU UI rounded rects", true));
        this.registerSetting(gpu3DRendering = new ButtonSetting("GPU 3D rendering", true));
        this.registerSetting(new DescriptionSetting("Profiles"));
        this.registerSetting(sendMessage = new ButtonSetting("Send message on enable", true));
        this.registerSetting(new DescriptionSetting("Theme colors"));
        this.registerSetting(offset = new SliderSetting("Offset", 0.5, -3.0, 3.0, 0.1));
        this.registerSetting(timeMultiplier = new SliderSetting("Time multiplier", 0.5, 0.1, 4.0, 0.1));
        this.registerSetting(toggleSound = new ModeSetting("Toggle sound", new String[]{"None", "Rise", "Sigma", "QuickMacro"}, 1));
        this.registerSetting(new DescriptionSetting("Client background"));
        this.registerSetting(backgroundMode = new ModeSetting("Custom client background", new String[]{"Flow", "Rise", "Nexus", "Aurora"}, 3));
        
        // Target filter settings
        this.registerSetting(new DescriptionSetting("Target Filter"));
        this.registerSetting(targetPlayers = new ButtonSetting("Target players", true));
        this.registerSetting(targetPassive = new ButtonSetting("Target passive", false));
        this.registerSetting(targetNeutral = new ButtonSetting("Target neutral", false));
        this.registerSetting(targetHostile = new ButtonSetting("Target hostile", false));
        this.registerSetting(targetGolems = new ButtonSetting("Target golems", false));
        this.registerSetting(targetVillagers = new ButtonSetting("Target villagers", false));
        this.registerSetting(targetOthers = new ButtonSetting("Target others", false));
        
        this.canBeEnabled = false;
    }

    public static @NotNull String getToggleSound(boolean enable) {
        final String startSuffix = "keystrokesmod:toggle.";
        final String endSuffix = enable ? ".enable" : ".disable";

        final String middleSuffix;
        switch ((int) toggleSound.getInput()) {
            default:
            case 0:
                return "";
            case 1:
                middleSuffix = "rise";
                break;
            case 2:
                middleSuffix = "sigma";
                break;
            case 3:
                middleSuffix = "quickmacro";
                break;
        }
        return startSuffix + middleSuffix + endSuffix;
    }
}
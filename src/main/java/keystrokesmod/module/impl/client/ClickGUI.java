package keystrokesmod.module.impl.client;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.utility.Utils;

public class ClickGUI extends Module {
    public static ButtonSetting removePlayerModel, resetPosition, translucentBackground, removeWatermark, rainBowOutlines, toolTip;
    public static ButtonSetting blurBackground;
    public static SliderSetting blurStrength;
    public static ButtonSetting blurButtons;
    public static SliderSetting buttonBlurStrength;
    public static ModeSetting font;
    public static ModeSetting backgroundOverlay;
    public static SliderSetting backgroundDarkness;
    public static SliderSetting shaderOpacity;
    public static ModeSetting mode;

    public ClickGUI() {
        super("ClickGUI", Module.category.client, 54);
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Dropdown", "Panel"}, 0));
        this.registerSetting(rainBowOutlines = new ButtonSetting("Rainbow outlines", true));
        this.registerSetting(removePlayerModel = new ButtonSetting("Remove player model", false));
        // Remove watermark by default in the ClickGUI
        this.registerSetting(removeWatermark = new ButtonSetting("Remove watermark", true));
        this.registerSetting(translucentBackground = new ButtonSetting("Translucent background", true));
        this.registerSetting(toolTip = new ButtonSetting("Tool tip", true));
        this.registerSetting(blurBackground = new ButtonSetting("Blur background", false));
        this.registerSetting(blurStrength = new SliderSetting("Blur strength", 20, 1, 64, 1, blurBackground::isToggled));
        this.registerSetting(blurButtons = new ButtonSetting("Blur buttons", false));
        this.registerSetting(buttonBlurStrength = new SliderSetting("Button blur strength", 20, 1, 64, 1, blurButtons::isToggled));
        this.registerSetting(new keystrokesmod.module.setting.impl.DescriptionSetting("Background overlay"));
        this.registerSetting(backgroundOverlay = new ModeSetting("Background overlay", new String[]{"None", "Black", "Shader"}, 0));
        this.registerSetting(backgroundDarkness = new SliderSetting("Background darkness", 128, 0, 255, 1, () -> backgroundOverlay.getInput() == 1));
        this.registerSetting(shaderOpacity = new SliderSetting("Shader opacity", 128, 0, 255, 1, () -> backgroundOverlay.getInput() == 2));
        this.registerSetting(resetPosition = new ButtonSetting("Reset position", ClickGui::resetPosition));
        this.registerSetting(font = new ModeSetting("Font", new String[]{"Minecraft", "Product Sans", "Tenacity"}, 0));
    }

    public void onEnable() {
        if (Utils.nullCheck() && mc.currentScreen != Raven.clickGui) {
            mc.displayGuiScreen(Raven.clickGui);
            Raven.clickGui.initMain();
        }
        this.disable();
    }
}


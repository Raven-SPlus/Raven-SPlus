package keystrokesmod.render.glide.gui.category;

import java.awt.Color;
import java.util.ArrayList;

import org.lwjgl.input.Keyboard;

import keystrokesmod.Raven;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.render.RenderFeatureFlags;
import keystrokesmod.render.RenderMode;
import keystrokesmod.render.glide.GlideContext;
import keystrokesmod.render.glide.adapter.RavenModAdapter;
import keystrokesmod.render.glide.adapter.RavenModAdapter.ModEntry;
import keystrokesmod.render.glide.adapter.RavenModAdapter.SettingEntry;
import keystrokesmod.render.glide.animation.Animation;
import keystrokesmod.render.glide.animation.Direction;
import keystrokesmod.render.glide.animation.SimpleAnimation;
import keystrokesmod.render.glide.animation.SmoothStepAnimation;
import keystrokesmod.render.glide.color.AccentColor;
import keystrokesmod.render.glide.color.ColorPalette;
import keystrokesmod.render.glide.color.ColorType;
import keystrokesmod.render.glide.color.GlideColorManager;
import keystrokesmod.render.glide.color.GlideTheme;
import keystrokesmod.render.glide.nanovg.NvgColorUtils;
import keystrokesmod.render.glide.nanovg.NvgManager;
import keystrokesmod.render.glide.nanovg.font.NvgFonts;
import keystrokesmod.render.glide.util.MathUtils;
import keystrokesmod.render.glide.util.MouseUtils;
import keystrokesmod.render.glide.util.Scroll;
import keystrokesmod.utility.profile.ProfileModule;

public class GlideSettingCategory {
    private static final String ICON_SETTINGS = "3";
    private static final String ICON_MONITOR = "i";
    private static final String ICON_CHEVRON_RIGHT = "\u00C8";
    private static final String ICON_CHEVRON_LEFT = "\u00C7";

    private enum Scene {
        APPEARANCE,
        SETTINGS
    }

    private final String name = "Settings";
    private final String icon = ICON_SETTINGS;

    private int x, y, width, height;
    private boolean canClose = true;

    private final Scroll themeScroll = new Scroll();
    private final Scroll accentScroll = new Scroll();
    private final Scroll settingScroll = new Scroll();

    private Animation sceneAnimation;
    private Scene currentScene;
    private ModEntry settingsModule;
    private final ArrayList<ModuleSetting> comps = new ArrayList<ModuleSetting>();
    private ModuleSetting sliderDragging;

    public void initGui() {
        resetState();
    }

    public void initCategory() {
        resetState();
    }

    private void resetState() {
        themeScroll.resetAll();
        accentScroll.resetAll();
        settingScroll.resetAll();
        currentScene = null;
        sliderDragging = null;
        comps.clear();
        sceneAnimation = new SmoothStepAnimation(260, 1.0);
        sceneAnimation.setValue(1.0);
        canClose = true;
        GlideContext.getInstance().getColorManager().syncFromSettings();
        settingsModule = findSettingsModule();
    }

    private ModEntry findSettingsModule() {
        RavenModAdapter adapter = GlideContext.getInstance().getModAdapter();
        for (ModEntry mod : adapter.getMods()) {
            if ("Settings".equalsIgnoreCase(mod.getRawName())) {
                return mod;
            }
        }
        return null;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlideContext ctx = GlideContext.getInstance();
        NvgManager nvg = ctx.getNvgManager();
        GlideColorManager colorManager = ctx.getColorManager();
        colorManager.syncFromSettings();

        ColorPalette palette = colorManager.getPalette();
        AccentColor accentColor = colorManager.getCurrentColor();

        sceneAnimation.setDirection(currentScene != null ? Direction.BACKWARDS : Direction.FORWARDS);

        if (sceneAnimation.isDone(Direction.FORWARDS)) {
            canClose = true;
            currentScene = null;
            sliderDragging = null;
        }

        nvg.save();
        nvg.translate((float) -(600 - (sceneAnimation.getValue() * 600)), 0);
        drawMainScene(nvg, palette, accentColor);
        nvg.restore();

        nvg.save();
        nvg.translate((float) (sceneAnimation.getValue() * 600), 0);
        if (currentScene == Scene.APPEARANCE) {
            drawAppearanceScene(nvg, colorManager, palette, accentColor, mouseX, mouseY);
        } else if (currentScene == Scene.SETTINGS) {
            drawSettingsModuleScene(nvg, palette, accentColor, mouseX, mouseY, partialTicks);
        }
        nvg.restore();
    }

    private void drawMainScene(NvgManager nvg, ColorPalette palette, AccentColor accentColor) {
        drawMenuItem(nvg, palette, accentColor, x + 15, y + 15, width - 30, 40,
                ICON_MONITOR, "Appearance", "Theme, accent color and blur");
        drawMenuItem(nvg, palette, accentColor, x + 15, y + 65, width - 30, 40,
                ICON_SETTINGS, "Settings", "Client, renderer and profile settings");
    }

    private void drawMenuItem(NvgManager nvg, ColorPalette palette, AccentColor accentColor,
                              float itemX, float itemY, float itemW, float itemH,
                              String itemIcon, String title, String description) {
        nvg.drawRoundedRect(itemX, itemY, itemW, itemH, 8, palette.getBackgroundColor(ColorType.DARK));
        nvg.drawGradientRoundedRect(itemX + 8, itemY + 6, 24, 28, 7,
                NvgColorUtils.applyAlpha(accentColor.getColor1(), 210),
                NvgColorUtils.applyAlpha(accentColor.getColor2(), 210));
        nvg.drawText(itemIcon, itemX + 14, itemY + 14, Color.WHITE, 14, NvgFonts.LEGACYICON);
        nvg.drawText(title, itemX + 42, itemY + 9, palette.getFontColor(ColorType.DARK), 12.5F, NvgFonts.MEDIUM);
        nvg.drawText(description, itemX + 42, itemY + 23, palette.getFontColor(ColorType.NORMAL), 7.5F, NvgFonts.REGULAR);
        nvg.drawText(ICON_CHEVRON_RIGHT, itemX + itemW - 17, itemY + 15, palette.getFontColor(ColorType.NORMAL), 10, NvgFonts.LEGACYICON);
    }

    private void drawAppearanceScene(NvgManager nvg, GlideColorManager colorManager,
                                     ColorPalette palette, AccentColor currentAccent,
                                     int mouseX, int mouseY) {
        float sceneX = x + 15;
        float sceneY = y + 15;
        float sceneW = width - 30;

        nvg.drawRoundedRect(sceneX, sceneY, sceneW, 137, 6, palette.getBackgroundColor(ColorType.DARK));
        nvg.drawText("Theme", sceneX + 8, sceneY + 8, palette.getFontColor(ColorType.DARK), 13, NvgFonts.MEDIUM);

        nvg.save();
        nvg.scissor(sceneX, sceneY + 24, sceneW, 72);
        nvg.translate(themeScroll.getValue(), 0);

        if (MouseUtils.isInside(mouseX, mouseY, sceneX, sceneY + 18, sceneW, 50)) {
            themeScroll.onScroll();
            themeScroll.onAnimation();
        }

        float offsetX = 0;
        int themeCount = 1;
        for (GlideTheme theme : GlideTheme.values()) {
            int alpha = (int) (theme.getAnimation().getValue() * 255);
            nvg.drawRoundedRect(sceneX + offsetX + 12, sceneY + 28, 36, 36, 6, theme.getNormalBackgroundColor(255));

            theme.getAnimation().setAnimation(theme.equals(colorManager.getTheme()) ? 1.0F : 0.0F, 16);
            nvg.drawGradientOutlineRoundedRect(sceneX + offsetX + 12, sceneY + 28, 36, 36, 6,
                    1.4F * theme.getAnimation().getValue(),
                    NvgColorUtils.applyAlpha(currentAccent.getColor1(), alpha),
                    NvgColorUtils.applyAlpha(currentAccent.getColor2(), alpha));

            offsetX += 46;
            themeCount++;
        }
        themeScroll.setMaxScroll((themeCount - 9.1F) * 44F);
        nvg.restore();

        offsetX = 0;
        nvg.drawText("Accent Color", sceneX + 8, sceneY + 81, palette.getFontColor(ColorType.DARK), 13, NvgFonts.MEDIUM);

        nvg.save();
        nvg.scissor(sceneX, sceneY + 89, sceneW, 72);
        nvg.translate(accentScroll.getValue(), 0);

        if (MouseUtils.isInside(mouseX, mouseY, sceneX, sceneY + 89, sceneW, 50)) {
            accentScroll.onScroll();
            accentScroll.onAnimation();
        }

        int accentCount = 1;
        for (AccentColor color : colorManager.getColors()) {
            nvg.drawGradientRoundedRect(sceneX + offsetX + 12, sceneY + 95, 32, 32, 6, color.getColor1(), color.getColor2());
            color.getAnimation().setAnimation(color.equals(currentAccent) ? 1.0F : 0.0F, 16);
            nvg.drawCenteredText("I", sceneX + offsetX + 28, sceneY + 103,
                    new Color(255, 255, 255, (int) (color.getAnimation().getValue() * 255)),
                    16, NvgFonts.LEGACYICON);
            offsetX += 40F;
            accentCount++;
        }
        accentScroll.setMaxScroll((accentCount - 10.3F) * 40F);
        nvg.restore();

        float controlRowY = sceneY + 152;
        nvg.drawRoundedRect(sceneX, controlRowY, sceneW, 41, 6, palette.getBackgroundColor(ColorType.DARK));
        nvg.drawText("Render Mode", sceneX + 8, controlRowY + 11.5F, palette.getFontColor(ColorType.DARK), 13, NvgFonts.MEDIUM);
        nvg.drawText(Settings.getConfiguredRenderMode() == RenderMode.GLIDE ? "Glide" : "Legacy",
                sceneX + sceneW - 72, controlRowY + 14, palette.getFontColor(ColorType.NORMAL), 9, NvgFonts.REGULAR);
        drawToggle(nvg, palette, currentAccent, sceneX + sceneW - 44, controlRowY + 14, Settings.getConfiguredRenderMode() == RenderMode.GLIDE);

        float blurRowY = sceneY + 202;
        nvg.drawRoundedRect(sceneX, blurRowY, sceneW, 41, 6, palette.getBackgroundColor(ColorType.DARK));
        nvg.drawText("UI Blur", sceneX + 8, blurRowY + 11.5F, palette.getFontColor(ColorType.DARK), 13, NvgFonts.MEDIUM);
        boolean blurEnabled = ModuleManager.clientTheme != null
                && ModuleManager.clientTheme.buttonBlur != null
                && ModuleManager.clientTheme.buttonBlur.isToggled();
        drawToggle(nvg, palette, currentAccent, sceneX + sceneW - 44, blurRowY + 14, blurEnabled);
    }

    private void drawToggle(NvgManager nvg, ColorPalette palette, AccentColor accentColor, float x, float y, boolean enabled) {
        float width = 26;
        float height = 13;
        float anim = enabled ? 1.0F : 0.0F;

        nvg.drawRoundedRect(x, y, width, height, height / 2, palette.getBackgroundColor(ColorType.NORMAL));
        nvg.drawGradientRoundedRect(x, y, width, height, height / 2,
                NvgColorUtils.applyAlpha(accentColor.getColor1(), (int) (255 * anim)),
                NvgColorUtils.applyAlpha(accentColor.getColor2(), (int) (255 * anim)));

        float knobRadius = (height - 4) / 2;
        float knobX = x + 2 + knobRadius + (width - 4 - knobRadius * 2) * anim;
        nvg.drawCircle(knobX, y + height / 2, knobRadius, Color.WHITE);
    }

    private void drawSettingsModuleScene(NvgManager nvg, ColorPalette palette, AccentColor accentColor,
                                         int mouseX, int mouseY, float partialTicks) {
        if (settingsModule == null) {
            return;
        }

        if (sliderDragging != null) {
            handleSliderDrag(mouseX);
        }

        if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height)) {
            settingScroll.onScroll();
            settingScroll.onAnimation();
        }

        float offsetY = 15;
        int offsetX = 0;

        nvg.drawRoundedRect(x + 15, y + offsetY, width - 30, height - 30, 10, palette.getBackgroundColor(ColorType.DARK));
        nvg.drawText(ICON_CHEVRON_LEFT, x + 25, y + offsetY + 8, palette.getFontColor(ColorType.DARK), 13, NvgFonts.LEGACYICON);
        nvg.drawText(settingsModule.getName(), x + 42, y + offsetY + 9, palette.getFontColor(ColorType.DARK), 13, NvgFonts.MEDIUM);

        offsetY = 44;

        nvg.save();
        nvg.scissor(x + 15, y + offsetY, width - 30, height - 59);
        nvg.translate(0, settingScroll.getValue());

        int setIndex = 0;
        for (ModuleSetting s : comps) {
            s.openAnimation.setAnimation(s.openY, 16);

            String labelText = nvg.getLimitText(s.name, 10, NvgFonts.MEDIUM, 92);
            nvg.drawText(labelText, x + offsetX + 26, y + offsetY + 15F + s.openAnimation.getValue(),
                    palette.getFontColor(ColorType.DARK), 10, NvgFonts.MEDIUM);

            float compX = x + offsetX;
            float compY = y + offsetY + s.openAnimation.getValue();
            drawSettingComp(nvg, palette, accentColor, s, compX, compY);

            offsetX += 194;
            setIndex++;
            if (setIndex % 2 == 0) {
                offsetY += 29;
                offsetX = 0;
            }
        }

        nvg.restore();
        settingScroll.setMaxScroll(getModuleSettingHeight());
    }

    private void drawSettingComp(NvgManager nvg, ColorPalette palette, AccentColor accentColor,
                                 ModuleSetting setting, float compX, float compY) {
        SettingEntry entry = setting.entry;
        float settingX = compX + 122;

        switch (entry.getType()) {
            case BUTTON:
                if (entry.isActionButton()) {
                    nvg.drawRoundedRect(settingX, compY + 11, 75, 16, 6, palette.getBackgroundColor(ColorType.NORMAL));
                    nvg.drawText("Run", settingX + 8, compY + 14, palette.getFontColor(ColorType.DARK), 9, NvgFonts.MEDIUM);
                    nvg.drawText(">", settingX + 65, compY + 14.5F, palette.getFontColor(ColorType.NORMAL), 9, NvgFonts.REGULAR);
                    break;
                }

                boolean value = entry.getBooleanValue();
                setting.toggleAnim.setAnimation(value ? 1.0F : 0.0F, 16);

                float toggleX = compX + 168;
                float toggleY = compY + 12;
                float toggleW = 26;
                float toggleH = 13;
                nvg.drawRoundedRect(toggleX, toggleY, toggleW, toggleH, toggleH / 2, palette.getBackgroundColor(ColorType.NORMAL));
                nvg.drawGradientRoundedRect(toggleX, toggleY, toggleW, toggleH, toggleH / 2,
                        NvgColorUtils.applyAlpha(accentColor.getColor1(), (int) (setting.toggleAnim.getValue() * 255)),
                        NvgColorUtils.applyAlpha(accentColor.getColor2(), (int) (setting.toggleAnim.getValue() * 255)));
                float knobRadius = (toggleH - 4) / 2;
                float knobX = toggleX + 2 + knobRadius + (toggleW - 4 - knobRadius * 2) * setting.toggleAnim.getValue();
                nvg.drawCircle(knobX, toggleY + toggleH / 2, knobRadius, Color.WHITE);
                break;
            case SLIDER:
                float sliderW = 75;
                float sliderY = compY + 17;
                float barH = 4;
                double min = entry.getMin();
                double max = entry.getMax();
                double cur = entry.getDoubleValue();
                float pct = (float) ((cur - min) / (max - min));
                nvg.drawRoundedRect(settingX, sliderY, sliderW, barH, 2, palette.getBackgroundColor(ColorType.NORMAL));
                nvg.drawGradientRoundedRect(settingX, sliderY, sliderW * pct, barH, 2, accentColor.getColor1(), accentColor.getColor2());
                nvg.drawCircle(settingX + sliderW * pct, sliderY + barH / 2, 4, Color.WHITE);
                String valueText = formatSliderValue(cur, entry.getInterval());
                float valueWidth = nvg.getTextWidth(valueText, 7, NvgFonts.REGULAR);
                float valueX = Math.max(settingX, Math.min(settingX + sliderW - valueWidth, settingX + sliderW * pct - (valueWidth / 2)));
                nvg.drawText(valueText, valueX, compY + 2, palette.getFontColor(ColorType.NORMAL), 7, NvgFonts.REGULAR);
                break;
            case MODE:
                String[] options = entry.getModeOptions();
                int selectedIndex = (int) entry.getDoubleValue();
                String selectedLabel = (selectedIndex >= 0 && selectedIndex < options.length) ? options[selectedIndex] : "?";
                selectedLabel = nvg.getLimitText(selectedLabel, 8, NvgFonts.REGULAR, 43);
                nvg.drawRoundedRect(settingX, compY + 11, 75, 16, 6, palette.getBackgroundColor(ColorType.NORMAL));
                nvg.drawText("<", settingX + 5, compY + 14.5F, palette.getFontColor(ColorType.NORMAL), 9, NvgFonts.REGULAR);
                nvg.drawCenteredText(selectedLabel, settingX + 37.5F, compY + 14, palette.getFontColor(ColorType.DARK), 8, NvgFonts.REGULAR);
                nvg.drawText(">", settingX + 65, compY + 14.5F, palette.getFontColor(ColorType.NORMAL), 9, NvgFonts.REGULAR);
                break;
            default:
                break;
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (currentScene == null) {
            if (MouseUtils.isInside(mouseX, mouseY, x + 15, y + 15, width - 30, 40) && mouseButton == 0) {
                currentScene = Scene.APPEARANCE;
                canClose = false;
                return;
            }

            if (MouseUtils.isInside(mouseX, mouseY, x + 15, y + 65, width - 30, 40) && mouseButton == 0 && settingsModule != null) {
                openSettingsModule();
                return;
            }
            return;
        }

        if (currentScene == Scene.APPEARANCE && sceneAnimation.isDone(Direction.BACKWARDS)) {
            handleAppearanceClicks(mouseX, mouseY, mouseButton);
        } else if (currentScene == Scene.SETTINGS && sceneAnimation.isDone(Direction.BACKWARDS)) {
            if (MouseUtils.isInside(mouseX, mouseY, x + 22, y + 20, 18, 18) && mouseButton == 0) {
                closeScene();
                return;
            }
            handleSettingClicks(mouseX, mouseY, mouseButton);
        }

        if (!MouseUtils.isInside(mouseX, mouseY, x, y, width, height) && mouseButton == 0) {
            closeScene();
        }

        if (mouseButton == 3) {
            closeScene();
        }
    }

    private void handleAppearanceClicks(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }

        GlideContext ctx = GlideContext.getInstance();
        GlideColorManager colorManager = ctx.getColorManager();

        float themeOffsetX = themeScroll.getValue();
        for (GlideTheme theme : GlideTheme.values()) {
            if (MouseUtils.isInside(mouseX, mouseY, x + 27 + themeOffsetX, y + 43, 36, 36)) {
                colorManager.setTheme(theme);
                if (Settings.glideTheme != null) {
                    Settings.glideTheme.setValue(theme.getId());
                }
                markCurrentProfileDirty();
            }
            themeOffsetX += 46;
        }

        float accentOffsetX = accentScroll.getValue();
        for (AccentColor color : colorManager.getColors()) {
            if (MouseUtils.isInside(mouseX, mouseY, x + 27 + accentOffsetX, y + 110, 32, 32)) {
                colorManager.setCurrentColor(color);
                if (Settings.glideAccent != null) {
                    String[] options = Settings.glideAccent.getOptions();
                    for (int i = 0; i < options.length; i++) {
                        if (options[i].equals(color.getName())) {
                            Settings.glideAccent.setValue(i);
                            break;
                        }
                    }
                }
                markCurrentProfileDirty();
            }
            accentOffsetX += 40F;
        }

        if (MouseUtils.isInside(mouseX, mouseY, x + width - 59, y + 181, 26, 13)) {
            if (Settings.renderMode != null) {
                Settings.renderMode.setValue(Settings.getConfiguredRenderMode() == RenderMode.GLIDE ? 0 : 1);
            }
            RenderFeatureFlags.setRenderMode(Settings.getConfiguredRenderMode());
            markCurrentProfileDirty();
        }

        if (MouseUtils.isInside(mouseX, mouseY, x + width - 59, y + 231, 26, 13)
                && ModuleManager.clientTheme != null
                && ModuleManager.clientTheme.buttonBlur != null) {
            ModuleManager.clientTheme.buttonBlur.toggle();
            markCurrentProfileDirty();
        }
    }

    private void openSettingsModule() {
        comps.clear();
        for (SettingEntry entry : settingsModule.getSettings()) {
            if (!entry.isVisible()) {
                continue;
            }
            comps.add(new ModuleSetting(entry));
        }
        settingScroll.resetAll();
        sliderDragging = null;
        currentScene = Scene.SETTINGS;
        canClose = false;
    }

    private void closeScene() {
        currentScene = null;
        sliderDragging = null;
    }

    private void handleSettingClicks(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || !MouseUtils.isInside(mouseX, mouseY, x, y, width, height)) {
            return;
        }

        int adjustedMouseY = (int) (mouseY - settingScroll.getValue());
        int offsetX = 0;
        float offsetY = 44;
        int setIndex = 0;

        for (ModuleSetting setting : comps) {
            float compX = x + offsetX;
            float compY = y + offsetY + setting.openAnimation.getValue();

            switch (setting.entry.getType()) {
                case BUTTON:
                    if (setting.entry.isActionButton()) {
                        if (MouseUtils.isInside(mouseX, adjustedMouseY, compX + 122, compY + 11, 75, 16)) {
                            setting.entry.runAction();
                            markCurrentProfileDirty();
                        }
                    } else if (MouseUtils.isInside(mouseX, adjustedMouseY, compX + 168, compY + 12, 26, 13)) {
                        setting.entry.setBooleanValue(!setting.entry.getBooleanValue());
                        markCurrentProfileDirty();
                    }
                    break;
                case SLIDER:
                    if (MouseUtils.isInside(mouseX, adjustedMouseY, compX + 120, compY + 13, 79, 12)) {
                        sliderDragging = setting;
                        handleSliderDrag(mouseX);
                        markCurrentProfileDirty();
                    }
                    break;
                case MODE:
                    float bx = compX + 122;
                    float by = compY + 11;
                    String[] options = setting.entry.getModeOptions();
                    if (options.length > 0) {
                        if (MouseUtils.isInside(mouseX, adjustedMouseY, bx, by, 16, 16)) {
                            int cur = (int) setting.entry.getDoubleValue();
                            setting.entry.setDoubleValue(cur > 0 ? (cur - 1) : (options.length - 1));
                            markCurrentProfileDirty();
                        } else if (MouseUtils.isInside(mouseX, adjustedMouseY, bx + 59, by, 16, 16)) {
                            int cur = (int) setting.entry.getDoubleValue();
                            setting.entry.setDoubleValue((cur + 1) % options.length);
                            markCurrentProfileDirty();
                        }
                    }
                    break;
                default:
                    break;
            }

            offsetX += 194;
            setIndex++;
            if (setIndex % 2 == 0) {
                offsetY += 29;
                offsetX = 0;
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (sliderDragging != null) {
            sliderDragging = null;
        }
    }

    private void handleSliderDrag(int mouseX) {
        if (sliderDragging == null) {
            return;
        }

        SettingEntry entry = sliderDragging.entry;
        int offsetX = 0;
        int setIndex = 0;

        for (ModuleSetting setting : comps) {
            if (setting == sliderDragging) {
                float settingX = x + offsetX + 122;
                float pct = MathUtils.clamp((mouseX - settingX) / 75F);
                double raw = entry.getMin() + (entry.getMax() - entry.getMin()) * pct;
                double interval = entry.getInterval();
                if (interval > 0) {
                    raw = Math.round(raw / interval) * interval;
                }
                entry.setDoubleValue(Math.max(entry.getMin(), Math.min(entry.getMax(), raw)));
                markCurrentProfileDirty();
                return;
            }

            offsetX += 194;
            setIndex++;
            if (setIndex % 2 == 0) {
                offsetX = 0;
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (currentScene != null && keyCode == Keyboard.KEY_ESCAPE) {
            closeScene();
        }
    }

    private String formatSliderValue(double value, double interval) {
        if (interval >= 1.0) {
            return String.valueOf((int) value);
        }
        return String.valueOf(MathUtils.roundToPlace(value, 2));
    }

    private int getModuleSettingHeight() {
        int oddOutput = 0;
        int evenOutput = 0;

        for (int i = 0; i < comps.size(); i++) {
            if (MathUtils.isOdd(i + 1)) {
                oddOutput += 29;
            } else {
                evenOutput += 29;
            }
        }

        return Math.max(0, Math.max(oddOutput, evenOutput) - (height - 72));
    }

    private void markCurrentProfileDirty() {
        if (Raven.currentProfile != null && Raven.currentProfile.getModule() instanceof ProfileModule) {
            ((ProfileModule) Raven.currentProfile.getModule()).saved = false;
        }
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public boolean canClose() {
        return canClose;
    }

    public void setCanClose(boolean canClose) {
        this.canClose = canClose;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    private static final class ModuleSetting {
        private final SettingEntry entry;
        private final String name;
        private final SimpleAnimation openAnimation = new SimpleAnimation();
        private final SimpleAnimation toggleAnim = new SimpleAnimation();
        private float openY;

        private ModuleSetting(SettingEntry entry) {
            this.entry = entry;
            this.name = entry.getName();
            this.openY = 0;
        }
    }
}

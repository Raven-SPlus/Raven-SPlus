package keystrokesmod.render.glide.gui.category;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import keystrokesmod.Raven;
import keystrokesmod.render.glide.GlideContext;
import keystrokesmod.render.glide.adapter.RavenModAdapter;
import keystrokesmod.render.glide.adapter.RavenModAdapter.ModEntry;
import keystrokesmod.render.glide.adapter.RavenModAdapter.SettingEntry;
import keystrokesmod.render.glide.adapter.RavenModAdapter.SettingType;
import keystrokesmod.render.glide.animation.Animation;
import keystrokesmod.render.glide.animation.ColorAnimation;
import keystrokesmod.render.glide.animation.Direction;
import keystrokesmod.render.glide.animation.SimpleAnimation;
import keystrokesmod.render.glide.animation.SmoothStepAnimation;
import keystrokesmod.render.glide.color.AccentColor;
import keystrokesmod.render.glide.color.ColorPalette;
import keystrokesmod.render.glide.color.ColorType;
import keystrokesmod.render.glide.color.GlideColorManager;
import keystrokesmod.render.glide.gui.GlideGuiModMenu;
import keystrokesmod.render.glide.nanovg.NvgManager;
import keystrokesmod.render.glide.nanovg.NvgColorUtils;
import keystrokesmod.render.glide.nanovg.font.NvgFonts;
import keystrokesmod.render.glide.util.MathUtils;
import keystrokesmod.render.glide.util.MouseUtils;
import keystrokesmod.render.glide.util.Scroll;
import keystrokesmod.utility.profile.ProfileModule;
import org.lwjgl.input.Keyboard;

public class GlideModuleCategory extends GlideCategory {

    private static final String ICON_ARCHIVE = "C";
    private static final String ICON_SETTINGS = "3";
    private static final String ICON_CHEVRON_LEFT = "\u00C7";
    private static final String ICON_BIND = "_";

    private static final String CAT_ALL = "ALL";

    private String currentCatFilter = CAT_ALL;

    private Scroll settingScroll = new Scroll();
    private boolean openSetting;
    private Animation settingAnimation;
    private ModEntry currentMod;
    private Color noColour = new Color(0, 0, 0, 0);

    private ArrayList<ModuleSetting> comps = new ArrayList<ModuleSetting>();

    private List<CatButton> catButtons = new ArrayList<CatButton>();
    private SimpleAnimation[] modAnimations;
    private boolean bindingKey;

    public GlideModuleCategory(GlideGuiModMenu parent) {
        super(parent, "Modules", ICON_ARCHIVE, true, true);
    }

    @Override
    public void initGui() {
        currentCatFilter = CAT_ALL;
        openSetting = false;
        bindingKey = false;
        settingAnimation = new SmoothStepAnimation(260, 1.0);
        settingAnimation.setValue(1.0);
        buildCatButtons();
    }

    @Override
    public void initCategory() {
        scroll.resetAll();
        openSetting = false;
        bindingKey = false;
        settingAnimation = new SmoothStepAnimation(260, 1.0);
        settingAnimation.setValue(1.0);
        buildCatButtons();
    }

    private void buildCatButtons() {
        catButtons.clear();
        LinkedHashMap<String, Object> seen = new LinkedHashMap<String, Object>();
        seen.put(CAT_ALL, null);

        RavenModAdapter adapter = GlideContext.getInstance().getModAdapter();
        for (ModEntry m : adapter.getMods()) {
            if (!m.isHidden()) {
                seen.put(m.getCategory(), null);
            }
        }

        for (String name : seen.keySet()) {
            catButtons.add(new CatButton(name));
        }
    }

    private String prettyCategory(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        return raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlideContext ctx = GlideContext.getInstance();
        NvgManager nvg = ctx.getNvgManager();
        RavenModAdapter adapter = ctx.getModAdapter();
        GlideColorManager colorManager = ctx.getColorManager();
        ColorPalette palette = colorManager.getPalette();
        AccentColor accentColor = colorManager.getCurrentColor();
        float sceneShift = Math.max(this.getWidth() + 180F, getCategoryStripWidth(nvg) + 80F);

        int offsetX = 0;
        float offsetY = 13;
        int index = 1;
        float scrollValue = scroll.getValue();

        settingAnimation.setDirection(openSetting ? Direction.BACKWARDS : Direction.FORWARDS);

        if (settingAnimation.isDone(Direction.FORWARDS)) {
            this.setCanClose(true);
            currentMod = null;
        }

        nvg.save();
        nvg.translate((float) (-(sceneShift - (settingAnimation.getValue() * sceneShift))), 0);

        // -------- Module list scene --------

        nvg.save();
        nvg.translate(0, scrollValue);

        for (CatButton cb : catButtons) {
            String label = prettyCategory(cb.name);
            float textWidth = nvg.getTextWidth(label, 9, NvgFonts.MEDIUM);
            boolean isCurrent = cb.name.equals(currentCatFilter);

            cb.bgAnimation.setAnimation(isCurrent ? 1.0F : 0.0F, 16);

            Color defaultColor = palette.getBackgroundColor(ColorType.DARK);
            Color color1 = NvgColorUtils.applyAlpha(accentColor.getColor1(), (int) (cb.bgAnimation.getValue() * 255));
            Color color2 = NvgColorUtils.applyAlpha(accentColor.getColor2(), (int) (cb.bgAnimation.getValue() * 255));
            Color textColor = cb.textColorAnim.getColor(isCurrent ? Color.WHITE : palette.getFontColor(ColorType.DARK), 20);

            nvg.drawRoundedRect(this.getX() + 15 + offsetX, this.getY() + offsetY - 3, textWidth + 20, 16, 6, defaultColor);
            nvg.drawGradientRoundedRect(this.getX() + 15 + offsetX, this.getY() + offsetY - 3, textWidth + 20, 16, 6, color1, color2);

            nvg.drawText(label, this.getX() + 15 + offsetX + ((textWidth + 20) - textWidth) / 2,
                    this.getY() + offsetY + 1.5F, textColor, 9, NvgFonts.MEDIUM);

            offsetX += (int) textWidth + 28;
        }

        offsetY = offsetY + 23;

        List<ModEntry> mods = adapter.getMods();

        for (ModEntry m : mods) {
            if (filterMod(m)) {
                continue;
            }

            if (offsetY + scrollValue + 45 > 0 && offsetY + scrollValue < this.getHeight()) {
                nvg.drawRoundedRect(this.getX() + 15, this.getY() + offsetY, this.getWidth() - 30, 40, 8, palette.getBackgroundColor(ColorType.DARK));
                nvg.drawRoundedRect(this.getX() + 21, this.getY() + offsetY + 6, 28, 28, 6, palette.getBackgroundColor(ColorType.NORMAL));

                nvg.drawText(m.getName(), this.getX() + 56, this.getY() + offsetY + 15F,
                        palette.getFontColor(ColorType.DARK), 13, NvgFonts.MEDIUM);

                SimpleAnimation toggleAnim = getModAnimation(m);
                toggleAnim.setAnimation(m.isToggled() ? 1.0F : 0.0F, 16);

                nvg.save();
                nvg.scale(this.getX() + 21, this.getY() + offsetY + 6, 28, 28, toggleAnim.getValue());

                nvg.drawGradientRoundedRect(this.getX() + 21, this.getY() + offsetY + 6, 28, 28, 6,
                        NvgColorUtils.applyAlpha(accentColor.getColor1(), (int) (toggleAnim.getValue() * 255)),
                        NvgColorUtils.applyAlpha(accentColor.getColor2(), (int) (toggleAnim.getValue() * 255)));

                nvg.restore();

                nvg.drawText(ICON_SETTINGS, this.getX() + this.getWidth() - 39, this.getY() + offsetY + 13.5F,
                        palette.getFontColor(ColorType.NORMAL), 13, NvgFonts.LEGACYICON);
            }

            index++;
            offsetY += 50;
        }

        nvg.restore();
        nvg.drawVerticalGradientRect(getX() + 15, this.getY(), getWidth() - 30, 12, palette.getBackgroundColor(ColorType.NORMAL), noColour);
        nvg.drawVerticalGradientRect(getX() + 15, this.getY() + this.getHeight() - 12, getWidth() - 30, 12, noColour, palette.getBackgroundColor(ColorType.NORMAL));
        nvg.restore();

        // -------- Settings scene --------

        nvg.save();
        nvg.translate((float) (settingAnimation.getValue() * sceneShift), 0);

        if (currentMod != null) {
            if (sliderDragging != null) {
                handleSliderDrag(mouseX);
            }

            if (MouseUtils.isInside(mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight())) {
                settingScroll.onScroll();
                settingScroll.onAnimation();
            }

            offsetY = 15;
            offsetX = 0;

            nvg.save();

            nvg.drawRoundedRect(this.getX() + 15, this.getY() + offsetY, this.getWidth() - 30, this.getHeight() - 30, 10, palette.getBackgroundColor(ColorType.DARK));
            nvg.drawText(ICON_CHEVRON_LEFT, this.getX() + 25, this.getY() + offsetY + 8, palette.getFontColor(ColorType.DARK), 13, NvgFonts.LEGACYICON);
            nvg.drawText(currentMod.getName(), this.getX() + 42, this.getY() + offsetY + 9, palette.getFontColor(ColorType.DARK), 13, NvgFonts.MEDIUM);
            float bindBoxW = 76;
            float bindBoxH = 16;
            float bindBoxX = this.getX() + this.getWidth() - bindBoxW - 24;
            float bindBoxY = this.getY() + offsetY + 7;
            nvg.drawRoundedRect(bindBoxX, bindBoxY, bindBoxW, bindBoxH, 4, palette.getBackgroundColor(ColorType.NORMAL));
            nvg.drawText(ICON_BIND, bindBoxX + 6, bindBoxY + 4.5F, palette.getFontColor(ColorType.NORMAL), 10, NvgFonts.LEGACYICON);
            String bindLabel = bindingKey ? "Binding..." : Keyboard.getKeyName(currentMod.getKeyBind());
            bindLabel = nvg.getLimitText(bindLabel, 8, NvgFonts.REGULAR, bindBoxW - 24);
            nvg.drawText(bindLabel, bindBoxX + 18, bindBoxY + 4.5F, palette.getFontColor(ColorType.DARK), 8, NvgFonts.REGULAR);

            offsetY = 44;

            nvg.scissor(this.getX() + 15, this.getY() + offsetY, this.getWidth() - 30, this.getHeight() - 59);
            nvg.translate(0, settingScroll.getValue());

            int setIndex = 0;

            for (ModuleSetting s : comps) {
                s.openAnimation.setAnimation(s.openY, 16);

                String labelText = nvg.getLimitText(s.name, 10, NvgFonts.MEDIUM, 92);
                nvg.drawText(labelText, this.getX() + offsetX + 26, this.getY() + offsetY + 15F + s.openAnimation.getValue(),
                        palette.getFontColor(ColorType.DARK), 10, NvgFonts.MEDIUM);

                float compX = this.getX() + offsetX;
                float compY = this.getY() + offsetY + s.openAnimation.getValue();

                drawSettingComp(nvg, palette, accentColor, s, compX, compY, mouseX, (int) (mouseY - settingScroll.getValue()), partialTicks);

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

        nvg.restore();

        scroll.setMaxScroll((index - (index > 5 ? 5.18F : index)) * 50);
    }

    private void drawSettingComp(NvgManager nvg, ColorPalette palette, AccentColor accentColor,
                                 ModuleSetting s, float compX, float compY,
                                 int mouseX, int mouseY, float partialTicks) {
        SettingEntry entry = s.entry;
        float settingX = compX + 122;
        float settingY = compY + 12;

        switch (entry.getType()) {
            case BUTTON: {
                float bw = 75;
                float bh = 16;
                float bx = compX + 122;
                float by = compY + 11;

                if (entry.isActionButton()) {
                    nvg.drawRoundedRect(bx, by, bw, bh, 6, palette.getBackgroundColor(ColorType.DARK));
                    nvg.drawText("Run", bx + 8, compY + 14, palette.getFontColor(ColorType.DARK), 9, NvgFonts.MEDIUM);
                    nvg.drawText(">", bx + bw - 10, compY + 14.5F, palette.getFontColor(ColorType.NORMAL), 9, NvgFonts.REGULAR);
                    break;
                }

                boolean val = entry.getBooleanValue();
                s.toggleAnim.setAnimation(val ? 1.0F : 0.0F, 16);

                float sw = 26;
                float sh = 13;
                float tx = compX + 168;
                float ty = compY + 12;

                nvg.drawRoundedRect(tx, ty, sw, sh, sh / 2, palette.getBackgroundColor(ColorType.DARK));

                Color trackC1 = NvgColorUtils.applyAlpha(accentColor.getColor1(), (int) (s.toggleAnim.getValue() * 255));
                Color trackC2 = NvgColorUtils.applyAlpha(accentColor.getColor2(), (int) (s.toggleAnim.getValue() * 255));
                nvg.drawGradientRoundedRect(tx, ty, sw, sh, sh / 2, trackC1, trackC2);

                float knobR = (sh - 4) / 2;
                float knobX = tx + 2 + knobR + (sw - 4 - knobR * 2) * s.toggleAnim.getValue();
                float knobY = ty + sh / 2;
                nvg.drawCircle(knobX, knobY, knobR, Color.WHITE);
                break;
            }
            case SLIDER: {
                float sw = 75;
                float sy = compY + 17;
                float barH = 4;

                double min = entry.getMin();
                double max = entry.getMax();
                double cur = entry.getDoubleValue();
                float pct = (float) ((cur - min) / (max - min));

                nvg.drawRoundedRect(settingX, sy, sw, barH, 2, palette.getBackgroundColor(ColorType.DARK));
                nvg.drawGradientRoundedRect(settingX, sy, sw * pct, barH, 2, accentColor.getColor1(), accentColor.getColor2());
                nvg.drawCircle(settingX + sw * pct, sy + barH / 2, 4, Color.WHITE);

                String valText = formatSliderValue(cur, entry.getInterval());
                float valTextW = nvg.getTextWidth(valText, 7, NvgFonts.REGULAR);
                float valueX = Math.max(settingX, Math.min(settingX + sw - valTextW, settingX + sw * pct - (valTextW / 2)));
                nvg.drawText(valText, valueX, compY + 2, palette.getFontColor(ColorType.NORMAL), 7, NvgFonts.REGULAR);
                break;
            }
            case MODE: {
                String[] options = entry.getModeOptions();
                int selectedIndex = (int) entry.getDoubleValue();
                String selectedLabel = (selectedIndex >= 0 && selectedIndex < options.length) ? options[selectedIndex] : "?";
                selectedLabel = nvg.getLimitText(selectedLabel, 8, NvgFonts.REGULAR, 43);

                float bw = 75;
                float bh = 16;
                nvg.drawRoundedRect(settingX, compY + 11, bw, bh, 6, palette.getBackgroundColor(ColorType.DARK));
                nvg.drawText("<", settingX + 5, compY + 14.5F, palette.getFontColor(ColorType.NORMAL), 9, NvgFonts.REGULAR);
                nvg.drawCenteredText(selectedLabel, settingX + (bw / 2F), compY + 14, palette.getFontColor(ColorType.DARK), 8, NvgFonts.REGULAR);
                nvg.drawText(">", settingX + bw - 10, compY + 14.5F, palette.getFontColor(ColorType.NORMAL), 9, NvgFonts.REGULAR);
                break;
            }
            case DESCRIPTION: {
                nvg.drawText(entry.getDescription(), compX + 26, compY + 15F,
                        palette.getFontColor(ColorType.NORMAL), 8, NvgFonts.REGULAR);
                break;
            }
        }
    }

    private String formatSliderValue(double value, double interval) {
        if (interval >= 1.0) {
            return String.valueOf((int) value);
        }
        return String.valueOf(MathUtils.roundToPlace(value, 2));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        GlideContext ctx = GlideContext.getInstance();
        NvgManager nvg = ctx.getNvgManager();
        RavenModAdapter adapter = ctx.getModAdapter();

        int offsetX = 0;
        float offsetY = 13 + scroll.getValue();

        if (!openSetting) {
            for (CatButton cb : catButtons) {
                String label = prettyCategory(cb.name);
                float textWidth = nvg.getTextWidth(label, 9, NvgFonts.MEDIUM);

                if (MouseUtils.isInside(mouseX, mouseY, this.getX() + 15 + offsetX, this.getY() + offsetY - 3, textWidth + 20, 16) && mouseButton == 0) {
                    currentCatFilter = cb.name;
                    scroll.reset();
                }
                offsetX += (int) textWidth + 28;
            }

            offsetY = offsetY + 23;

            for (ModEntry m : adapter.getMods()) {
                if (filterMod(m)) {
                    continue;
                }

                if (MouseUtils.isInside(mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight()) && mouseButton == 0) {
                    if (MouseUtils.isInside(mouseX, mouseY, this.getX() + 15, this.getY() + offsetY, this.getWidth() - 60, 40)) {
                        m.setToggled(!m.isToggled());
                        markCurrentProfileDirty();
                    }

                    if (MouseUtils.isInside(mouseX, mouseY, this.getX() + this.getWidth() - 44, this.getY() + offsetY + 9, 22, 22) && !openSetting) {
                        List<SettingEntry> settings = m.getSettings();
                        comps.clear();
                        for (SettingEntry se : settings) {
                            if (!se.isVisible()) continue;
                            comps.add(new ModuleSetting(se));
                        }
                        settingScroll.resetAll();
                        currentMod = m;
                        bindingKey = false;
                        openSetting = true;
                        this.setCanClose(false);
                    }
                }

                offsetY += 50;
            }
        }

        if (openSetting && settingAnimation.isDone(Direction.BACKWARDS)) {
            if (MouseUtils.isInside(mouseX, mouseY, this.getX() + 22, this.getY() + 20, 18, 18) && mouseButton == 0) {
                openSetting = false;
                bindingKey = false;
            }

            int bx = getX() - 32, by = getY() - 31, bw = getWidth() + 32, bh = getHeight() + 31;
            if (!MouseUtils.isInside(mouseX, mouseY, bx - 5, by - 5, bw + 10, bh + 10) && mouseButton == 0) {
                openSetting = false;
                bindingKey = false;
            }

            handleSettingClicks(mouseX, mouseY, mouseButton);

            float bindBoxW = 76;
            float bindBoxH = 16;
            float bindBoxX = this.getX() + this.getWidth() - bindBoxW - 24;
            float bindBoxY = this.getY() + 22;
            if (MouseUtils.isInside(mouseX, mouseY, bindBoxX, bindBoxY, bindBoxW, bindBoxH) && mouseButton == 0) {
                bindingKey = !bindingKey;
            }
        }

        if (openSetting && mouseButton == 3) {
            openSetting = false;
            bindingKey = false;
        }
    }

    private void handleSettingClicks(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;
        if (!MouseUtils.isInside(mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight())) return;

        int adjustedMouseY = (int) (mouseY - settingScroll.getValue());
        int offsetX = 0;
        float offsetY = 44;
        int setIndex = 0;

        for (ModuleSetting ms : comps) {
            float compX = this.getX() + offsetX;
            float compY = this.getY() + offsetY + ms.openAnimation.getValue();

            switch (ms.entry.getType()) {
                case BUTTON: {
                    if (ms.entry.isActionButton()) {
                        float bx = compX + 122;
                        float by = compY + 11;
                        if (MouseUtils.isInside(mouseX, adjustedMouseY, bx, by, 75, 16)) {
                            ms.entry.runAction();
                            markCurrentProfileDirty();
                        }
                    } else {
                        float bx = compX + 168;
                        float by = compY + 12;
                        if (MouseUtils.isInside(mouseX, adjustedMouseY, bx, by, 26, 13)) {
                            ms.entry.setBooleanValue(!ms.entry.getBooleanValue());
                            markCurrentProfileDirty();
                        }
                    }
                    break;
                }
                case SLIDER: {
                    float sx = compX + 122;
                    float sy = compY + 13;
                    if (MouseUtils.isInside(mouseX, adjustedMouseY, sx - 2, sy, 79, 12)) {
                        sliderDragging = ms;
                        handleSliderDrag(mouseX);
                        markCurrentProfileDirty();
                    }
                    break;
                }
                case MODE: {
                    float bx = compX + 122;
                    float by = compY + 11;
                    if (MouseUtils.isInside(mouseX, adjustedMouseY, bx, by, 16, 16)) {
                        String[] options = ms.entry.getModeOptions();
                        if (options.length > 0) {
                            int cur = (int) ms.entry.getDoubleValue();
                            ms.entry.setDoubleValue(cur > 0 ? (cur - 1) : (options.length - 1));
                            markCurrentProfileDirty();
                        }
                    } else if (MouseUtils.isInside(mouseX, adjustedMouseY, bx + 59, by, 16, 16)) {
                        String[] options = ms.entry.getModeOptions();
                        if (options.length > 0) {
                            int cur = (int) ms.entry.getDoubleValue();
                            ms.entry.setDoubleValue((cur + 1) % options.length);
                            markCurrentProfileDirty();
                        }
                    }
                    break;
                }
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

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (openSetting && sliderDragging != null) {
            sliderDragging = null;
        }
    }

    private ModuleSetting sliderDragging = null;

    private void handleSliderDrag(int mouseX) {
        if (sliderDragging == null) return;
        SettingEntry entry = sliderDragging.entry;
        int offsetX = 0;
        float offsetY = 44;
        int setIndex = 0;

        for (ModuleSetting ms : comps) {
            if (ms == sliderDragging) {
                float compX = this.getX() + offsetX;
                float settingX = compX + 122;
                float sw = 75;

                float pct = MathUtils.clamp((mouseX - settingX) / sw);
                double min = entry.getMin();
                double max = entry.getMax();
                double interval = entry.getInterval();
                double raw = min + (max - min) * pct;

                if (interval > 0) {
                    raw = Math.round(raw / interval) * interval;
                }
                raw = Math.max(min, Math.min(max, raw));
                entry.setDoubleValue(raw);
                markCurrentProfileDirty();
                return;
            }
            offsetX += 194;
            setIndex++;
            if (setIndex % 2 == 0) {
                offsetY += 29;
                offsetX = 0;
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (bindingKey && currentMod != null) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) {
                currentMod.getRaw().setBind(0);
            } else {
                currentMod.getRaw().setBind(keyCode);
            }
            bindingKey = false;
            markCurrentProfileDirty();
            return;
        }
        if (openSetting && keyCode == Keyboard.KEY_ESCAPE) {
            openSetting = false;
            bindingKey = false;
        }
        if (!openSetting) {
            scroll.onKey(keyCode);
            if (keyCode != Keyboard.KEY_DOWN && keyCode != Keyboard.KEY_UP && keyCode != Keyboard.KEY_ESCAPE) {
                this.getSearchBox().setFocused(true);
            }
        }
    }

    private boolean filterMod(ModEntry m) {
        if (m.isHidden()) {
            return true;
        }

        if (!currentCatFilter.equals(CAT_ALL) && !m.getCategory().equalsIgnoreCase(currentCatFilter)) {
            return true;
        }

        String search = this.getSearchBox().getText();
        if (!search.isEmpty()) {
            return !m.getName().toLowerCase().contains(search.toLowerCase())
                    && !m.getRawName().toLowerCase().contains(search.toLowerCase());
        }

        return false;
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

        int output = Math.max(oddOutput, evenOutput);
        return Math.max(0, output - (this.getHeight() - 72));
    }

    private float getCategoryStripWidth(NvgManager nvg) {
        float width = 0;

        for (CatButton cb : catButtons) {
            width += nvg.getTextWidth(prettyCategory(cb.name), 9, NvgFonts.MEDIUM) + 28F;
        }

        return width;
    }

    private Map<String, SimpleAnimation> modAnimCache = new LinkedHashMap<String, SimpleAnimation>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SimpleAnimation> eldest) {
            return size() > 256;
        }
    };

    private SimpleAnimation getModAnimation(ModEntry m) {
        String key = m.getRawName();
        SimpleAnimation anim = modAnimCache.get(key);
        if (anim == null) {
            anim = new SimpleAnimation();
            modAnimCache.put(key, anim);
        }
        return anim;
    }

    private static class CatButton {
        final String name;
        final SimpleAnimation bgAnimation = new SimpleAnimation();
        final ColorAnimation textColorAnim = new ColorAnimation();

        CatButton(String name) {
            this.name = name;
        }
    }

    private static class ModuleSetting {
        final SettingEntry entry;
        final String name;
        final SimpleAnimation openAnimation = new SimpleAnimation();
        final SimpleAnimation toggleAnim = new SimpleAnimation();
        float openY;

        ModuleSetting(SettingEntry entry) {
            this.entry = entry;
            this.name = entry.getName();
            this.openY = 0;
        }
    }

    private void markCurrentProfileDirty() {
        if (Raven.currentProfile != null && Raven.currentProfile.getModule() instanceof ProfileModule) {
            ((ProfileModule) Raven.currentProfile.getModule()).saved = false;
        }
    }
}

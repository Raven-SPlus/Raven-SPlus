package keystrokesmod.module.impl.render;

import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.player.ChestStealer;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.module.setting.utils.ModeOnly;
import keystrokesmod.utility.font.FontManager;
import keystrokesmod.utility.font.IFont;
import keystrokesmod.utility.render.ColorUtils;
import keystrokesmod.utility.render.RenderUtils;
import keystrokesmod.utility.render.blur.HudBlurBatcher;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class HUD extends Module {
    private static final double ROW_TOP_SPACING = 2.0;
    private static final double ROW_HORIZONTAL_PADDING = 6.0;
    private static final double ROW_VERTICAL_PADDING = 2.0;
    private static final double ROW_GAP = 2.0;

    public static ModeSetting theme;
    public static ModeSetting font;
    public static ButtonSetting dropShadow;
    private final ButtonSetting background;
    private final SliderSetting backgroundOpacity;
    private final ButtonSetting sidebar;
    private final ButtonSetting blurBackground;
    public static SliderSetting blurStrength;
    public static ButtonSetting alphabeticalSort;
    private static ButtonSetting alignRight;
    public static ButtonSetting lowercase;
    public static ButtonSetting showInfo;
    private static ModeSetting categoryMode;
    private static SliderSetting textOffset;
    private static SliderSetting verticalTextOffset;
    private static ButtonSetting combat;
    private static ButtonSetting movement;
    private static ButtonSetting player;
    private static ButtonSetting world;
    private static ButtonSetting render;
    private static ButtonSetting minigames;
    private static ButtonSetting fun;
    private static ButtonSetting other;
    private static ButtonSetting client;
    private static ButtonSetting scripts;
    private static ButtonSetting exploit;
    private static ButtonSetting experimental;
    public static int hudX = -1; // Will be set to upper right in constructor
    public static int hudY = 5;
    private boolean isAlphabeticalSort;
    private boolean canShowInfo;

    public HUD() {
        super("HUD", Module.category.render);
        this.registerSetting(new DescriptionSetting("Right click bind to hide modules."));
        this.registerSetting(theme = new ModeSetting("Theme", Theme.themes, 0));
        this.registerSetting(font = new ModeSetting("Font", new String[]{"Minecraft", "Product Sans", "Regular", "Tenacity"}, 0));
        this.registerSetting(new ButtonSetting("Edit position", () -> {
            final EditScreen screen = new EditScreen();
            FMLCommonHandler.instance().bus().register(screen);
            mc.displayGuiScreen(screen);
        }));
        this.registerSetting(alignRight = new ButtonSetting("Align right", true));
        this.registerSetting(alphabeticalSort = new ButtonSetting("Alphabetical sort", false));
        this.registerSetting(dropShadow = new ButtonSetting("Drop shadow", true));
        this.registerSetting(background = new ButtonSetting("Background", false));
        this.registerSetting(backgroundOpacity = new SliderSetting("Background opacity", 100.0, 0.0, 255.0, 1.0, background::isToggled));
        this.registerSetting(blurBackground = new ButtonSetting("Blur background", false, background::isToggled));
        // Global blur strength used by HUD + any HUD overlays that request blur.
        this.registerSetting(blurStrength = new SliderSetting("Blur strength", 15.0, 1.0, 64.0, 1.0));
        this.registerSetting(sidebar = new ButtonSetting("Sidebar", false));
        this.registerSetting(textOffset = new SliderSetting("Horizontal text offset", 0.0, -10.0, 10.0, 0.1));
        this.registerSetting(verticalTextOffset = new SliderSetting("Vertical text offset", 0.0, -10.0, 10.0, 0.1));
        this.registerSetting(lowercase = new ButtonSetting("Lowercase", false));
        this.registerSetting(showInfo = new ButtonSetting("Show module info", true));

        this.registerSetting(new DescriptionSetting("Categories"));
        this.registerSetting(categoryMode = new ModeSetting("Category mode", new String[]{"All", "Exclude render", "Custom"}, 2));
        // Hide category toggles when preset is not "Custom" (mode 2)
        ModeOnly customMode = new ModeOnly(categoryMode, 2);
        this.registerSetting(combat = new ButtonSetting("Combat", true, customMode));
        this.registerSetting(movement = new ButtonSetting("Movement", true, customMode));
        this.registerSetting(player = new ButtonSetting("Player", true, customMode));
        this.registerSetting(world = new ButtonSetting("World", true, customMode));
        this.registerSetting(render = new ButtonSetting("Render", true, customMode));
        this.registerSetting(minigames = new ButtonSetting("Minigames", true, customMode));
        this.registerSetting(fun = new ButtonSetting("Fun", true, customMode));
        this.registerSetting(other = new ButtonSetting("Other", true, customMode));
        this.registerSetting(client = new ButtonSetting("Client", true, customMode));
        this.registerSetting(scripts = new ButtonSetting("Scripts", true, customMode));
        this.registerSetting(exploit = new ButtonSetting("Exploit", true, customMode));
        this.registerSetting(experimental = new ButtonSetting("Experimental", true, customMode));
    }

    public void onEnable() {
        ModuleManager.sort();
    }

    public void guiButtonToggled(ButtonSetting b) {
        if (b == alphabeticalSort || b == showInfo) {
            ModuleManager.sort();
        }
    }

    @SubscribeEvent
    public void onRenderTick(@NotNull RenderTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END || !Utils.nullCheck()) {
            return;
        }
        if (isAlphabeticalSort != alphabeticalSort.isToggled()) {
            isAlphabeticalSort = alphabeticalSort.isToggled();
            ModuleManager.sort();
        }
        if (canShowInfo != showInfo.isToggled()) {
            canShowInfo = showInfo.isToggled();
            ModuleManager.sort();
        }
        if (mc.currentScreen != null && !(mc.currentScreen instanceof GuiChest && ChestStealer.noChestRender()) && !(mc.currentScreen instanceof GuiChat) || mc.gameSettings.showDebugInfo) {
            return;
        }
        // Initialize hudX if not set (default to upper right corner with spacing)
        if (hudX == -1 && mc != null) {
            ScaledResolution res = new ScaledResolution(mc);
            hudX = res.getScaledWidth() - 5;
        }
        
        try {
            List<String> texts = getDrawTexts();

            if (texts.isEmpty()) {
                return;
            }

            boolean shouldBlur = background.isToggled() && blurBackground.isToggled() && blurStrength != null && blurStrength.getInput() > 0;
            double bgOpacity = backgroundOpacity != null ? backgroundOpacity.getInput() : 100.0;
            List<HudRow> rows = buildHudRows(texts);

            if (shouldBlur) {
                final int blurRadius = (int) blurStrength.getInput();
                HudBlurBatcher.addBlurStencil(blurRadius, () -> {
                    for (HudRow row : rows) {
                        RenderUtils.drawRoundedRectangle(
                                (float) row.backgroundX,
                                (float) row.backgroundY,
                                (float) (row.backgroundX + row.backgroundWidth),
                                (float) (row.backgroundY + row.backgroundHeight),
                                getHudRowRadius(row),
                                -1
                        );
                    }
                });

                HudBlurBatcher.addAfterBlur(() -> {
                    if (bgOpacity > 0) {
                        drawHudRows(rows, bgOpacity, false);
                    }
                    drawHudRowForeground(rows);
                });
                return;
            }

            if (background.isToggled() && bgOpacity > 0) {
                drawHudRows(rows, bgOpacity, true);
            }
            drawHudRowForeground(rows);
        }
        catch (Exception exception) {
            Utils.sendMessage("&cAn error occurred rendering HUD. check your logs");
            Utils.sendDebugMessage(Arrays.toString(exception.getStackTrace()));
            Utils.log.error(exception);
        }
    }

    private @NotNull List<HudRow> buildHudRows(@NotNull List<String> texts) {
        IFont fontRenderer = getFontRenderer();
        List<HudRow> rows = new ArrayList<>(texts.size());
        double currentY = hudY + ROW_TOP_SPACING;
        double horizontalOffset = textOffset != null ? textOffset.getInput() : 0.0;
        double verticalOffset = verticalTextOffset != null ? verticalTextOffset.getInput() : 0.0;
        double colorOffset = 0.0;
        double backgroundHeight = Math.round(fontRenderer.height() + ROW_VERTICAL_PADDING * 2.0);

        for (String text : texts) {
            double textWidth = fontRenderer.width(text);
            double textX = alignRight.isToggled() ? hudX - textWidth + horizontalOffset : hudX - horizontalOffset;
            double backgroundX = textX - ROW_HORIZONTAL_PADDING;
            double backgroundWidth = textWidth + ROW_HORIZONTAL_PADDING * 2.0;
            double backgroundY = currentY;
            double textY = backgroundY + ROW_VERTICAL_PADDING - verticalOffset;
            double sidebarX = alignRight.isToggled() ? backgroundX + backgroundWidth + 1.5 : backgroundX - 3.0;

            rows.add(new HudRow(text, backgroundX, backgroundY, backgroundWidth, backgroundHeight, textX, textY, sidebarX, colorOffset));

            colorOffset += theme.getInput() == 0 ? -120.0 : -12.0;
            currentY += backgroundHeight + ROW_GAP;
        }

        return rows;
    }

    private void drawHudRows(@NotNull List<HudRow> rows, double bgOpacity, boolean drawShadow) {
        for (HudRow row : rows) {
            if (drawShadow) {
                int shadowAlpha = clampAlpha((bgOpacity * 0.45) + 10.0);
                RenderUtils.drawBloomShadow(
                        (float) row.backgroundX,
                        (float) row.backgroundY,
                        (float) row.backgroundWidth,
                        (float) row.backgroundHeight,
                        10,
                        (int) getHudRowRadius(row),
                        new Color(0, 0, 0, shadowAlpha).getRGB(),
                        true
                );
            }

            int[] fillColors = getHudBackgroundColors(row.colorOffset, bgOpacity);
            RenderUtils.drawRoundedGradientRect(
                    (float) row.backgroundX,
                    (float) row.backgroundY,
                    (float) (row.backgroundX + row.backgroundWidth),
                    (float) (row.backgroundY + row.backgroundHeight),
                    getHudRowRadius(row),
                    fillColors[0],
                    fillColors[1],
                    fillColors[1],
                    fillColors[0]
            );
        }
    }

    private void drawHudRowForeground(@NotNull List<HudRow> rows) {
        IFont fontRenderer = getFontRenderer();

        for (HudRow row : rows) {
            int textColor = Theme.getGradient((int) theme.getInput(), row.colorOffset);

            if (sidebar.isToggled()) {
                Color accent = new Color(textColor, true);
                int sidebarColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 210).getRGB();
                RenderUtils.drawRoundedRectangle(
                        (float) row.sidebarX,
                        (float) (row.backgroundY + 1.5),
                        (float) (row.sidebarX + 2.0),
                        (float) (row.backgroundY + row.backgroundHeight - 1.5),
                        1.5f,
                        sidebarColor
                );
            }

            fontRenderer.drawString(row.text, row.textX, row.textY, textColor, dropShadow.isToggled());
        }
    }

    private int[] getHudBackgroundColors(double colorOffset, double bgOpacity) {
        Color leftTheme = new Color(Theme.getGradient((int) theme.getInput(), colorOffset), true);
        Color rightTheme = new Color(Theme.getGradient((int) theme.getInput(), colorOffset - 18.0), true);
        Color leftFill = ColorUtils.blend(leftTheme, new Color(24, 27, 36), 0.25);
        Color rightFill = ColorUtils.blend(rightTheme, new Color(12, 15, 22), 0.22);
        int alpha = clampAlpha(bgOpacity);

        return new int[]{
                new Color(leftFill.getRed(), leftFill.getGreen(), leftFill.getBlue(), alpha).getRGB(),
                new Color(rightFill.getRed(), rightFill.getGreen(), rightFill.getBlue(), alpha).getRGB()
        };
    }

    private static float getHudRowRadius(@NotNull HudRow row) {
        return (float) Math.max(4.0, Math.min(10.0, row.backgroundHeight / 2.0));
    }

    private static int clampAlpha(double alpha) {
        return Math.max(0, Math.min(255, (int) Math.round(alpha)));
    }

    private static final class HudRow {
        private final String text;
        private final double backgroundX;
        private final double backgroundY;
        private final double backgroundWidth;
        private final double backgroundHeight;
        private final double textX;
        private final double textY;
        private final double sidebarX;
        private final double colorOffset;

        private HudRow(String text, double backgroundX, double backgroundY, double backgroundWidth, double backgroundHeight, double textX, double textY, double sidebarX, double colorOffset) {
            this.text = text;
            this.backgroundX = backgroundX;
            this.backgroundY = backgroundY;
            this.backgroundWidth = backgroundWidth;
            this.backgroundHeight = backgroundHeight;
            this.textX = textX;
            this.textY = textY;
            this.sidebarX = sidebarX;
            this.colorOffset = colorOffset;
        }
    }

    @NotNull
    private List<String> getDrawTexts() {
        List<Module> modules = ModuleManager.organizedModules;
        List<String> texts = new ArrayList<>(modules.size());

        for (Module module : modules) {
            if (isIgnored(module)) continue;

            String text = module.getPrettyName();
            if (showInfo.isToggled() && !module.getPrettyInfo().isEmpty()) {
                text += " §7" + module.getPrettyInfo();
            }
            if (lowercase.isToggled()) {
                text = text.toLowerCase();
            }
            texts.add(text);
        }
        return texts;
    }

    public static double getLongestModule(IFont fr) {
        double length = 0;

        for (Module module : ModuleManager.organizedModules) {
            if (module.isEnabled()) {
                String moduleName = module.getPrettyName();
                if (showInfo.isToggled() && !module.getInfo().isEmpty()) {
                    moduleName += " §7" + module.getInfo();
                }
                if (lowercase.isToggled()) {
                    moduleName = moduleName.toLowerCase();
                }
                if (fr.width(moduleName) > length) {
                    length = fr.width(moduleName);
                }
            }
        }
        return length;
    }

    static class EditScreen extends GuiScreen {
        final String example = "This is an-Example-HUD";
        GuiButtonExt resetPosition;
        boolean hoverHUD = false;
        boolean hoverTargetHUD = false;
        boolean hoverWatermark = false;
        int miX = 0;
        int miY = 0;
        double maX = 0;
        double maY = 0;
        int curHudX = -1;
        int curHudY = 5;
        int laX = 0;
        int laY = 0;
        int lmX = 0;
        int lmY = 0;
        double clickMinX = 0;

        public void initGui() {
            super.initGui();
            ScaledResolution res = new ScaledResolution(this.mc);
            // Position button at bottom center with spacing (5 pixels from bottom)
            int buttonWidth = 85;
            int buttonHeight = 20;
            int buttonX = res.getScaledWidth() / 2 - buttonWidth / 2;
            int buttonY = res.getScaledHeight() - buttonHeight - 5;
            this.buttonList.add(this.resetPosition = new GuiButtonExt(1, buttonX, buttonY, buttonWidth, buttonHeight, "Reset position"));
            this.curHudX = HUD.hudX == -1 ? res.getScaledWidth() - 5 : HUD.hudX;
            this.curHudY = HUD.hudY;
        }

        @Override
        public void onGuiClosed() {
            FMLCommonHandler.instance().bus().unregister(this);
        }

        public void drawScreen(int mX, int mY, float pt) {
            drawRect(0, 0, this.width, this.height, -1308622848);
            int miX = this.curHudX;
            int miY = this.curHudY;
            int maX = miX + 50;
            int maY = miY + 32;
            double[] clickPos = this.d(getFontRenderer(), this.example);
            this.miX = miX;
            this.miY = miY;
            if (clickPos == null) {
                this.maX = maX;
                this.maY = maY;
                this.clickMinX = miX;
            }
            else {
                this.maX = clickPos[0];
                this.maY = clickPos[1];
                this.clickMinX = clickPos[2];
            }
            HUD.hudX = miX;
            HUD.hudY = miY;
            ScaledResolution res = new ScaledResolution(this.mc);
            int x = res.getScaledWidth() / 2 - 84;
            int y = res.getScaledHeight() / 2 - 20;
            RenderUtils.dct("Edit the HUD position by dragging.", '-', x, y, 2L, 0L, true, getFontRenderer());

            try {
                this.handleInput();
            } catch (IOException ignored) {
            }

            super.drawScreen(mX, mY, pt);
        }

        @SubscribeEvent
        public void onRenderTick(RenderTickEvent event) {
            TargetHUD.renderExample();
            ModuleManager.watermark.render();
        }

        private double @Nullable [] d(IFont fr, String t) {
            if (empty()) {
                double x = this.miX;
                double y = this.miY;
                String[] var5 = t.split("-");

                for (String s : var5) {
                    if (HUD.alignRight.isToggled()) {
                        x += getFontRenderer().width(var5[0]) - getFontRenderer().width(s);
                    }
                    fr.drawString(s, (float) x, (float) y, Color.white.getRGB(), HUD.dropShadow.isToggled());
                    y += Math.round(fr.height() + 2);
                }
            }
            else {
                double longestModule = getLongestModule(getFontRenderer());
                double n = this.miY;
                double n2 = 0.0;
                for (Module module : ModuleManager.organizedModules) {
                    if (isIgnored(module)) continue;

                    String moduleName = module.getPrettyName();
                    if (showInfo.isToggled() && !module.getInfo().isEmpty()) {
                        moduleName += " §7" + module.getInfo();
                    }
                    if (lowercase.isToggled()) {
                        moduleName = moduleName.toLowerCase();
                    }
                    int e = Theme.getGradient((int) theme.getInput(), n2);
                    if (theme.getInput() == 0) {
                        n2 -= 120;
                    }
                    else {
                        n2 -= 12;
                    }
                    double n3 = this.miX;
                    if (alignRight.isToggled()) {
                        n3 -= getFontRenderer().width(moduleName);
                    }
                    getFontRenderer().drawString(moduleName, n3, (float) n, e, dropShadow.isToggled());
                    n += Math.round(getFontRenderer().height() + 2);
                }
                return new double[]{this.miX + longestModule, n, this.miX - longestModule};
            }
            return null;
        }

        protected void mouseClickMove(int mX, int mY, int b, long t) {
            super.mouseClickMove(mX, mY, b, t);
            if (b == 0) {
                if (this.hoverHUD) {
                    this.curHudX = this.laX + (mX - this.lmX);
                    this.curHudY = this.laY + (mY - this.lmY);
                } else if (this.hoverTargetHUD) {
                    TargetHUD.posX = this.laX + (mX - this.lmX);
                    TargetHUD.posY = this.laY + (mY - this.lmY);
                } else if (this.hoverWatermark) {
                    Watermark.posX = this.laX + (mX - this.lmX);
                    Watermark.posY = this.laY + (mY - this.lmY);
                } else if (mX > this.clickMinX && mX < this.maX && mY > this.miY && mY < this.maY) {
                    this.hoverHUD = true;
                    this.lmX = mX;
                    this.lmY = mY;
                    this.laX = this.curHudX;
                    this.laY = this.curHudY;
                } else if (mX > TargetHUD.current$minX && mX < TargetHUD.current$maxX && mY > TargetHUD.current$minY && mY < TargetHUD.current$maxY) {
                    this.hoverTargetHUD = true;
                    this.lmX = mX;
                    this.lmY = mY;
                    this.laX = TargetHUD.posX;
                    this.laY = TargetHUD.posY;
                } else if (mX > Watermark.current$minX && mX < Watermark.current$maxX && mY > Watermark.current$minY && mY < Watermark.current$maxY) {
                    this.hoverWatermark = true;
                    this.lmX = mX;
                    this.lmY = mY;
                    this.laX = Watermark.posX;
                    this.laY = Watermark.posY;
                }

            }
        }

        protected void mouseReleased(int mX, int mY, int s) {
            super.mouseReleased(mX, mY, s);
            if (s == 0) {
                this.hoverHUD = false;
                this.hoverTargetHUD = false;
                this.hoverWatermark = false;
            }

        }

        public void actionPerformed(GuiButton b) {
            if (b == this.resetPosition) {
                ScaledResolution res = new ScaledResolution(this.mc);
                // Reset to upper right corner with spacing
                this.curHudX = HUD.hudX = res.getScaledWidth() - 5;
                this.curHudY = HUD.hudY = 5;
                TargetHUD.posX = 70;
                TargetHUD.posY = 30;
                Watermark.posX = 5;
                Watermark.posY = 5;
            }

        }

        public boolean doesGuiPauseGame() {
            return false;
        }

        private boolean empty() {
            for (Module module : ModuleManager.organizedModules) {
                if (module.isEnabled() && !module.getName().equals("HUD")) {
                    if (module.isHidden()) {
                        continue;
                    }
                    if (module == ModuleManager.commandLine) {
                        continue;
                    }
                    return false;
                }
            }
            return true;
        }
    }

    private static boolean isIgnored(@NotNull Module module) {
        if (!module.isEnabled() || module.getName().equals("HUD"))
            return true;
        if (module instanceof SubMode)
            return true;
        
        // Check category mode presets
        if (categoryMode != null) {
            int mode = (int) categoryMode.getInput();
            if (mode == 0) { // All
                // Show all modules except hidden ones and command line
                if (module.isHidden()) {
                    return true;
                }
                return module == ModuleManager.commandLine;
            } else if (mode == 1) { // Exclude render
                // Show all except render category
                if (module.moduleCategory() == category.render) {
                    return true;
                }
                if (module.isHidden()) {
                    return true;
                }
                return module == ModuleManager.commandLine;
            }
            // mode == 2 is Custom, fall through to custom category checks
        }

        // Custom mode - check individual category toggles
        if (module.moduleCategory() == category.combat && !combat.isToggled()) return true;
        if (module.moduleCategory() == category.movement && !movement.isToggled()) return true;
        if (module.moduleCategory() == category.player && !player.isToggled()) return true;
        if (module.moduleCategory() == category.world && !world.isToggled()) return true;
        if (module.moduleCategory() == category.render && !render.isToggled()) return true;
        if (module.moduleCategory() == category.minigames && !minigames.isToggled()) return true;
        if (module.moduleCategory() == category.fun && !fun.isToggled()) return true;
        if (module.moduleCategory() == category.other && !other.isToggled()) return true;
        if (module.moduleCategory() == category.client && !client.isToggled()) return true;
        if (module.moduleCategory() == category.scripts && !scripts.isToggled()) return true;
        if (module.moduleCategory() == category.exploit && !exploit.isToggled()) return true;
        if (module.moduleCategory() == category.experimental && !experimental.isToggled()) return true;

        if (module.isHidden()) {
            return true;
        }
        return module == ModuleManager.commandLine;
    }

    public static IFont getFontRenderer() {
        switch ((int) font.getInput()) {
            default:
            case 0:
                return FontManager.getMinecraft();
            case 1:
                return FontManager.productSans20;
            case 2:
                return FontManager.regular22;
            case 3:
                return FontManager.tenacity20;
        }
    }
}

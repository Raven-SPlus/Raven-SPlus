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
        
        // Add spacing at the top of array list
        double topSpacing = 2.0;
        int n = (int) (hudY + topSpacing);
        double n2 = 0.0;
        try {
            List<String> texts = getDrawTexts();
            
            if (texts.isEmpty()) {
                return;
            }
            
            // Calculate background bounds (backgrounds stay at original position)
            double offset = textOffset != null ? textOffset.getInput() : 0.0;
            double leftExtend = 3.0; // Extend background more to the left for each row
            double rowHeight = Math.round(getFontRenderer().height() + 2);
            double bgOpacity = backgroundOpacity != null ? backgroundOpacity.getInput() : 100.0;
            int bgColor = new Color(0, 0, 0, (int) bgOpacity).getRGB();
            
            // Check if blur is enabled
            boolean shouldBlur = background.isToggled() && blurBackground.isToggled() && blurStrength != null && blurStrength.getInput() > 0;

            if (shouldBlur) {
                final int blurRadius = (int) blurStrength.getInput();

                // Precompute rects once (used for stencil + overlay)
                final java.util.ArrayList<double[]> backgroundRects = new java.util.ArrayList<>();
                int currentY = (int) (hudY + topSpacing);
                for (String text : texts) {
                    double width = getFontRenderer().width(text);
                    double backgroundX;
                    double backgroundWidth;
                    if (alignRight.isToggled()) {
                        backgroundX = hudX - width - offset - leftExtend;
                        backgroundWidth = width + offset + leftExtend;
                    } else {
                        backgroundX = hudX - leftExtend;
                        backgroundWidth = width + offset + leftExtend;
                    }
                    double bgY = currentY - 1;
                    double bgHeight = rowHeight + 1; // connected rows for blur
                    backgroundRects.add(new double[]{backgroundX, bgY, backgroundWidth, bgHeight});
                    currentY += rowHeight;
                }

                // 1) Blur stencil (batched)
                HudBlurBatcher.addBlurStencil(blurRadius, () -> {
                    if (!backgroundRects.isEmpty()) {
                        for (double[] rect : backgroundRects) {
                            RenderUtils.drawRect(rect[0], rect[1], rect[0] + rect[2], rect[1] + rect[3], -1);
                        }
                    }
                });

                // 2) Foreground draw after blur (keep text sharp)
                HudBlurBatcher.addAfterBlur(() -> {
                    // Draw darkness overlay on top of blurred area (just draw the same rects)
                    if (bgOpacity > 0 && !backgroundRects.isEmpty()) {
                        for (double[] rect : backgroundRects) {
                            RenderUtils.drawRect(rect[0], rect[1], rect[0] + rect[2], rect[1] + rect[3], bgColor);
                        }
                    }

                    // Draw all text and sidebars after blur is complete
                    double verticalOffset = verticalTextOffset != null ? verticalTextOffset.getInput() : 0.0;
                    int baseY = (int) (hudY + topSpacing);
                    int textY = (int) (hudY + topSpacing - verticalOffset);
                    double gradOffset = 0.0;

                    int yRow = baseY;
                    for (String text : texts) {
                        int e = Theme.getGradient((int) theme.getInput(), gradOffset);
                        if (theme.getInput() == 0) {
                            gradOffset -= 120;
                        } else {
                            gradOffset -= 12;
                        }

                        double xText = hudX;
                        double width = getFontRenderer().width(text);

                        // Base text position (without offset) for sidebar
                        double baseTextX = hudX;
                        if (alignRight.isToggled()) {
                            baseTextX -= width;
                        }

                        // Apply horizontal text offset
                        if (alignRight.isToggled()) {
                            xText -= width;
                            xText += offset;
                        } else {
                            xText -= offset;
                        }

                        // Sidebar stays at original position (no offset)
                        if (sidebar.isToggled()) {
                            double sidebarX = alignRight.isToggled() ? baseTextX + width : baseTextX - 2;
                            RenderUtils.drawRect(sidebarX, yRow - 1, sidebarX + 1, yRow + Math.round(getFontRenderer().height() + 1), new Color(255, 255, 255, 200).getRGB());
                        }

                        getFontRenderer().drawString(text, xText, textY, e, dropShadow.isToggled());
                        textY += rowHeight;
                        yRow += rowHeight;
                    }
                });
                return;
            }

            // Non-blur path: draw immediately (existing behavior)
            if (background.isToggled()) {
                int currentY = (int) (hudY + topSpacing);
                for (String text : texts) {
                    double width = getFontRenderer().width(text);
                    double backgroundX;
                    double backgroundWidth;
                    if (alignRight.isToggled()) {
                        backgroundX = hudX - width - offset - leftExtend;
                        backgroundWidth = width + offset + leftExtend;
                    } else {
                        backgroundX = hudX - leftExtend;
                        backgroundWidth = width + offset + leftExtend;
                    }
                    double bgY = currentY - 1;
                    double bgHeight = Math.round(getFontRenderer().height() + 1);
                    RenderUtils.drawRect(backgroundX, bgY, backgroundX + backgroundWidth, bgY + bgHeight, bgColor);
                    currentY += rowHeight;
                }
            }

            double verticalOffset = verticalTextOffset != null ? verticalTextOffset.getInput() : 0.0;
            int baseY = (int) (hudY + topSpacing);
            n = (int) (hudY + topSpacing - verticalOffset);
            n2 = 0.0;
            for (String text : texts) {
                int e = Theme.getGradient((int) theme.getInput(), n2);
                if (theme.getInput() == 0) {
                    n2 -= 120;
                } else {
                    n2 -= 12;
                }
                double n3 = hudX;
                double width = getFontRenderer().width(text);

                double baseTextX = hudX;
                if (alignRight.isToggled()) {
                    baseTextX -= width;
                }

                if (alignRight.isToggled()) {
                    n3 -= width;
                    n3 += offset;
                } else {
                    n3 -= offset;
                }

                if (sidebar.isToggled()) {
                    double sidebarX = alignRight.isToggled() ? baseTextX + width : baseTextX - 2;
                    RenderUtils.drawRect(sidebarX, baseY - 1, sidebarX + 1, baseY + Math.round(getFontRenderer().height() + 1), new Color(255, 255, 255, 200).getRGB());
                }
                getFontRenderer().drawString(text, n3, n, e, dropShadow.isToggled());
                n += rowHeight;
                baseY += rowHeight;
            }
        }
        catch (Exception exception) {
            Utils.sendMessage("&cAn error occurred rendering HUD. check your logs");
            Utils.sendDebugMessage(Arrays.toString(exception.getStackTrace()));
            Utils.log.error(exception);
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

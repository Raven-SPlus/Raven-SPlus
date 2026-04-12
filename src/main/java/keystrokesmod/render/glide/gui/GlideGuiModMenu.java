package keystrokesmod.render.glide.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.render.glide.GlideContext;
import keystrokesmod.render.glide.animation.Animation;
import keystrokesmod.render.glide.animation.Direction;
import keystrokesmod.render.glide.animation.EaseBackIn;
import keystrokesmod.render.glide.animation.SimpleAnimation;
import keystrokesmod.render.glide.color.AccentColor;
import keystrokesmod.render.glide.color.ColorPalette;
import keystrokesmod.render.glide.color.ColorType;
import keystrokesmod.render.glide.color.GlideColorManager;
import keystrokesmod.render.glide.gui.category.GlideCategory;
import keystrokesmod.render.glide.gui.category.GlideModuleCategory;
import keystrokesmod.render.glide.gui.category.GlideProfileCategory;
import keystrokesmod.render.glide.gui.category.GlideSettingCategory;
import keystrokesmod.render.glide.gui.comp.CompSearchBox;
import keystrokesmod.render.glide.nanovg.NvgManager;
import keystrokesmod.render.glide.nanovg.ScreenAnimation;
import keystrokesmod.render.glide.nanovg.font.NvgFonts;
import keystrokesmod.render.glide.util.MathUtils;
import keystrokesmod.render.glide.util.MouseUtils;
import keystrokesmod.render.glide.util.Scroll;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

public class GlideGuiModMenu extends GuiScreen {

    private static final String ICON_ARCHIVE = "C";
    private static final String ICON_USER    = "{";
    private static final String ICON_SETTINGS = "3";
    private static final String ICON_GLIDE   = "A";
    private static final String ICON_LAYOUT  = "?";

    private Animation introAnimation;
    private int x, y, width, height;

    private ArrayList<GlideCategory> categories = new ArrayList<GlideCategory>();
    private GlideCategory currentCategory;

    private SimpleAnimation moveAnimation = new SimpleAnimation();

    private ScreenAnimation screenAnimation = new ScreenAnimation();

    private Scroll scroll = new Scroll();

    private boolean toEditHUD;
    private boolean canClose;

    private CompSearchBox searchBox = new CompSearchBox();

    public GlideGuiModMenu() {
        categories.add(new GlideModuleCategory(this));
        categories.add(createProfileCategory());
        categories.add(createSettingCategory());

        currentCategory = categories.get(0);
    }

    @Override
    public void initGui() {
        ScaledResolution sr = new ScaledResolution(mc);
        GlideContext.getInstance().getColorManager().syncFromSettings();

        int addX = 225;
        int addY = 140;

        x = (sr.getScaledWidth() / 2) - addX;
        y = (sr.getScaledHeight() / 2) - addY;
        width = addX * 2;
        height = addY * 2;

        introAnimation = new EaseBackIn(320, 1.0F, 2.0F);
        introAnimation.setDirection(Direction.FORWARDS);

        for (GlideCategory c : categories) {
            c.initGui();
        }

        scroll.resetAll();
        toEditHUD = false;
        canClose = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlideContext ctx = GlideContext.getInstance();
        NvgManager nvg = ctx.getNvgManager();

        screenAnimation.wrap(new Runnable() {
            @Override
            public void run() {
                nvg.drawShadow(x, y, width, height, 12);
            }
        }, 2 - introAnimation.getValueFloat(), Math.min(introAnimation.getValueFloat(), 1));

        screenAnimation.wrap(new Runnable() {
            @Override
            public void run() {
                drawNanoVG(mouseX, mouseY, partialTicks);
            }
        }, x, y, width, height, 2 - introAnimation.getValueFloat(), Math.min(introAnimation.getValueFloat(), 1), true);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawNanoVG(int mouseX, int mouseY, float partialTicks) {
        GlideContext ctx = GlideContext.getInstance();
        NvgManager nvg = ctx.getNvgManager();
        GlideColorManager colorManager = ctx.getColorManager();
        ColorPalette palette = colorManager.getPalette();
        AccentColor currentColor = colorManager.getCurrentColor();

        if (introAnimation.isDone(Direction.BACKWARDS)) {
            mc.displayGuiScreen(toEditHUD ? new GlideGuiEditHUD(true) : null);
        }

        nvg.drawRoundedRect(x, y, width, height, 12, palette.getBackgroundColor(ColorType.NORMAL));
        boolean blurEnabled = ModuleManager.clientTheme != null
                && ModuleManager.clientTheme.buttonBlur != null
                && ModuleManager.clientTheme.buttonBlur.isToggled();
        if (blurEnabled) {
            ctx.getRenderCore().getBlur().drawBlur(new Runnable() {
                @Override
                public void run() {
                    nvg.drawRoundedRectVarying(x, y, 32, height, 12, 0, 12, 0, palette.getBackgroundColor(ColorType.DARK));
                }
            });
            Color sidebarColor = palette.getBackgroundColor(ColorType.DARK);
            nvg.drawRoundedRectVarying(x, y, 32, height, 12, 0, 12, 0,
                    new Color(sidebarColor.getRed(), sidebarColor.getGreen(), sidebarColor.getBlue(), 210));
        } else {
            nvg.drawRoundedRectVarying(x, y, 32, height, 12, 0, 12, 0, palette.getBackgroundColor(ColorType.DARK));
        }

        nvg.drawGradientRoundedRect(x + 5, y + 7, 22, 22, 11, currentColor.getColor1(), currentColor.getColor2());
        nvg.drawText(ICON_GLIDE, x + 8, y + 10, Color.WHITE, 16, NvgFonts.ICON_FILLED);

        if (currentCategory.isShowTitle()) {
            nvg.save();
            nvg.translate(currentCategory.getTextAnimation().getValue() * 15, 0);
            nvg.drawText(currentCategory.getName(), x + 32, y + 10,
                    palette.getFontColor(ColorType.DARK, (int) (currentCategory.getTextAnimation().getValue() * 255)),
                    15, NvgFonts.SEMIBOLD);
            nvg.restore();
        }

        int offsetY = 0;

        moveAnimation.setAnimation(categories.indexOf(currentCategory) * 30, 18);

        nvg.save();

        nvg.drawGradientRoundedRect(x + 5.5F, y + 38.5F + moveAnimation.getValue(), 21, 21, 5, currentColor.getColor1(), currentColor.getColor2());

        for (GlideCategory c : categories) {
            Color textColor = c.getTextColorAnimation().getColor(
                    MathUtils.isInRange(moveAnimation.getValue(), offsetY - 8, offsetY + 8)
                            ? Color.WHITE
                            : palette.getFontColor(ColorType.NORMAL),
                    18);

            c.getTextAnimation().setAnimation(c.equals(currentCategory) ? 1.0F : 0.0F, 14);

            nvg.drawText(c.getIcon(), x + 9F, y + 42 + offsetY, textColor, 14, NvgFonts.LEGACYICON);

            offsetY += 30;
        }

        nvg.restore();

        for (GlideCategory c : categories) {
            c.getCategoryAnimation().setAnimation(c.equals(currentCategory) ? 1.0F : 0.0F, 16);

            if (c.equals(currentCategory)) {
                nvg.save();

                if (!c.isInitialized()) {
                    c.setInitialized(true);
                    c.initCategory();
                    searchBox.setText("");
                    c.setCanClose(true);
                }

                if (c.isShowSearchBox()) {
                    searchBox.setPosition(x + width - 175, y + 6.5F, 160, 18);
                    searchBox.draw(mouseX, mouseY, partialTicks);
                }

                int yOff = currentCategory.isShowTitle() ? 31 : 0;
                nvg.scissor(x + 32, y + yOff, width - 32, height - yOff);
                nvg.translate(0, 50 - (c.getCategoryAnimation().getValue() * 50));

                c.drawScreen(mouseX, mouseY, partialTicks);

                nvg.restore();
            } else if (c.isInitialized()) {
                c.setInitialized(false);
            }
        }

        nvg.drawGradientRoundedRect(x + 5.5F, y + height - 30, 21, 21, 6, currentColor.getColor1(), currentColor.getColor2());
        nvg.drawText(ICON_LAYOUT, x + 9, y + height - 26.5F, Color.WHITE, 14, NvgFonts.LEGACYICON);

        if (MouseUtils.isInside(mouseX, mouseY, x + 32, y + 31, width - 32, height - 31)) {
            scroll.onScroll();
        }

        scroll.onAnimation();

        if (currentCategory.isShowSearchBox() && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_F)) {
            currentCategory.getSearchBox().setFocused(true);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int offsetY = 0;

        if (!MouseUtils.isInside(mouseX, mouseY, x - 5, y - 5, width + 10, height + 10) && mouseButton == 0 && canClose) {
            introAnimation.setDirection(Direction.BACKWARDS);
        }

        for (GlideCategory c : categories) {
            if (MouseUtils.isInside(mouseX, mouseY, x + 5.5F, y + 38.5F + offsetY, 21, 21) && mouseButton == 0) {
                currentCategory = c;
            }
            offsetY += 30;
        }

        if (MouseUtils.isInside(mouseX, mouseY, x + 5.5F, y + height - 30, 21, 21) && mouseButton == 0) {
            toEditHUD = true;
            introAnimation.setDirection(Direction.BACKWARDS);
        }

        currentCategory.mouseClicked(mouseX, mouseY, mouseButton);
        searchBox.mouseClicked(mouseX, mouseY, mouseButton);

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        currentCategory.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        currentCategory.keyTyped(typedChar, keyCode);
        searchBox.keyTyped(typedChar, keyCode);

        if (currentCategory.isShowSearchBox() && canClose) {
            if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
                if (!searchBox.getText().isEmpty()) {
                    searchBox.setText("");
                    searchBox.setFocused(false);
                    return;
                }

                if (searchBox.isFocused()) {
                    searchBox.setFocused(false);
                    return;
                }
            }
        }

        if (keyCode == Keyboard.KEY_ESCAPE && canClose) {
            introAnimation.setDirection(Direction.BACKWARDS);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ArrayList<GlideCategory> getCategories() {
        return categories;
    }

    public GlideCategory getCategoryByClass(Class<?> clazz) {
        for (GlideCategory c : categories) {
            if (c.getClass().equals(clazz)) {
                return c;
            }
        }
        return null;
    }

    public Scroll getScroll() {
        return scroll;
    }

    public CompSearchBox getSearchBox() {
        return searchBox;
    }

    public boolean isCanClose() {
        return canClose;
    }

    public void setCanClose(boolean canClose) {
        this.canClose = canClose;
    }

    private GlideCategory createProfileCategory() {
        GlideProfileCategory category = new GlideProfileCategory();
        category.setX(getX() + 32);
        category.setY(getY() + 31);
        category.setWidth(getWidth() - 32);
        category.setHeight(getHeight() - 31);
        return new GlideCategory(this, "Profiles", ICON_USER, false, true) {
            @Override
            public void initGui() {
                category.setX(getX());
                category.setY(getY());
                category.setWidth(getWidth());
                category.setHeight(getHeight());
                category.initGui();
                setCanClose(category.canClose());
            }

            @Override
            public void initCategory() {
                category.setX(getX());
                category.setY(getY());
                category.setWidth(getWidth());
                category.setHeight(getHeight());
                category.initCategory();
                setCanClose(category.canClose());
            }

            @Override
            public void drawScreen(int mouseX, int mouseY, float partialTicks) {
                category.setX(getX());
                category.setY(getY());
                category.setWidth(getWidth());
                category.setHeight(getHeight());
                category.drawScreen(mouseX, mouseY, partialTicks);
                setCanClose(category.canClose());
            }

            @Override
            public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
                category.mouseClicked(mouseX, mouseY, mouseButton);
                setCanClose(category.canClose());
            }

            @Override
            public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
                category.mouseReleased(mouseX, mouseY, mouseButton);
            }

            @Override
            public void keyTyped(char typedChar, int keyCode) {
                category.keyTyped(typedChar, keyCode);
                setCanClose(category.canClose());
            }
        };
    }

    private GlideCategory createSettingCategory() {
        GlideSettingCategory category = new GlideSettingCategory();
        category.setX(getX() + 32);
        category.setY(getY() + 31);
        category.setWidth(getWidth() - 32);
        category.setHeight(getHeight() - 31);
        return new GlideCategory(this, "Settings", ICON_SETTINGS, false, true) {
            @Override
            public void initGui() {
                category.setX(getX());
                category.setY(getY());
                category.setWidth(getWidth());
                category.setHeight(getHeight());
                category.initGui();
                setCanClose(category.canClose());
            }

            @Override
            public void initCategory() {
                category.setX(getX());
                category.setY(getY());
                category.setWidth(getWidth());
                category.setHeight(getHeight());
                category.initCategory();
                setCanClose(category.canClose());
            }

            @Override
            public void drawScreen(int mouseX, int mouseY, float partialTicks) {
                category.setX(getX());
                category.setY(getY());
                category.setWidth(getWidth());
                category.setHeight(getHeight());
                category.drawScreen(mouseX, mouseY, partialTicks);
                setCanClose(category.canClose());
            }

            @Override
            public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
                category.mouseClicked(mouseX, mouseY, mouseButton);
                setCanClose(category.canClose());
            }

            @Override
            public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
                category.mouseReleased(mouseX, mouseY, mouseButton);
            }

            @Override
            public void keyTyped(char typedChar, int keyCode) {
                category.keyTyped(typedChar, keyCode);
                setCanClose(category.canClose());
            }
        };
    }
}

package keystrokesmod.render.glide.gui.category;

import java.awt.Color;
import java.util.List;

import org.lwjgl.input.Keyboard;

import keystrokesmod.render.glide.GlideContext;
import keystrokesmod.render.glide.adapter.RavenProfileAdapter;
import keystrokesmod.render.glide.animation.Animation;
import keystrokesmod.render.glide.animation.ColorAnimation;
import keystrokesmod.render.glide.animation.Direction;
import keystrokesmod.render.glide.animation.SimpleAnimation;
import keystrokesmod.render.glide.animation.SmoothStepAnimation;
import keystrokesmod.render.glide.color.AccentColor;
import keystrokesmod.render.glide.color.ColorPalette;
import keystrokesmod.render.glide.color.ColorType;
import keystrokesmod.render.glide.color.GlideColorManager;
import keystrokesmod.render.glide.nanovg.NvgColorUtils;
import keystrokesmod.render.glide.nanovg.NvgManager;
import keystrokesmod.render.glide.nanovg.font.NvgFonts;
import keystrokesmod.render.glide.util.MouseUtils;
import keystrokesmod.render.glide.util.Scroll;

/**
 * Profile management category for the Glide click-gui.
 * Lists Raven profiles with create / load / delete actions.
 */
public class GlideProfileCategory extends Object {

    private static final String ICON_EDIT   = "U";
    private static final String ICON_PLUS   = "n";
    private static final String ICON_TRASH  = "6";
    private static final String ICON_CHECK  = "I";
    private static final String ICON_BACK   = "[";

    private final String name = "Profile";
    private final String icon = ICON_EDIT;

    private int x, y, width, height;
    private boolean canClose = true;

    private Animation panelAnimation;
    private boolean showCreatePanel;

    private final Scroll scroll = new Scroll();

    private String createNameBuffer = "";
    private boolean nameFieldFocused;
    private int cursorBlink;

    private SimpleAnimation hoverAnimation = new SimpleAnimation();

    public GlideProfileCategory() {
    }

    public void initGui() {
        showCreatePanel = false;
        canClose = true;
        panelAnimation = new SmoothStepAnimation(260, 1.0);
        panelAnimation.setValue(1.0);
        createNameBuffer = "";
        nameFieldFocused = false;
    }

    public void initCategory() {
        scroll.resetAll();
        showCreatePanel = false;
        canClose = true;
        panelAnimation = new SmoothStepAnimation(260, 1.0);
        panelAnimation.setValue(1.0);
        createNameBuffer = "";
        nameFieldFocused = false;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlideContext ctx = GlideContext.getInstance();
        NvgManager nvg = ctx.getNvgManager();
        GlideColorManager colorManager = ctx.getColorManager();
        AccentColor accentColor = colorManager.getCurrentColor();
        ColorPalette palette = colorManager.getPalette();
        RavenProfileAdapter profileAdapter = ctx.getProfileAdapter();

        List<String> profiles = profileAdapter.getProfiles();
        String currentProfile = profileAdapter.getCurrentProfileName();

        panelAnimation.setDirection(showCreatePanel ? Direction.BACKWARDS : Direction.FORWARDS);

        if (panelAnimation.isDone(Direction.FORWARDS)) {
            createNameBuffer = "";
            nameFieldFocused = false;
            canClose = true;
        }

        cursorBlink++;
        if (cursorBlink > 40) {
            cursorBlink = 0;
        }

        // ---- Profile list scene ----
        nvg.save();
        nvg.translate((float) -(600 - (panelAnimation.getValue() * 600)), 0);

        float offsetY = 13;
        float cardWidth = 170;
        float cardHeight = 52;
        float cardGap = 10;
        int columns = Math.max(1, (int) ((width - 30) / (cardWidth + cardGap)));

        int col = 0;
        float listStartY = offsetY;

        nvg.save();
        nvg.scissor(x, y, width, height);
        nvg.translate(0, scroll.getValue());

        if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height)) {
            scroll.onScroll();
            scroll.onAnimation();
        }

        for (int i = 0; i < profiles.size(); i++) {
            String profileName = profiles.get(i);
            float cardX = x + 15 + col * (cardWidth + cardGap);
            float cardY = y + listStartY;
            boolean isCurrent = profileName.equals(currentProfile);

            nvg.drawRoundedRect(cardX, cardY, cardWidth, cardHeight, 8, palette.getBackgroundColor(ColorType.DARK));

            if (isCurrent) {
                nvg.drawGradientOutlineRoundedRect(cardX, cardY, cardWidth, cardHeight, 8,
                        1.6F, accentColor.getColor1(), accentColor.getColor2());
            }

            float iconAreaSize = 34;
            float iconX = cardX + 9;
            float iconY = cardY + (cardHeight - iconAreaSize) / 2;

            nvg.drawRoundedRect(iconX, iconY, iconAreaSize, iconAreaSize, 6,
                    palette.getBackgroundColor(ColorType.NORMAL));
            String initial = profileName.isEmpty() ? "?" : profileName.substring(0, 1).toUpperCase();
            nvg.drawCenteredText(initial, iconX + iconAreaSize / 2, iconY + 10,
                    palette.getFontColor(ColorType.DARK), 16, NvgFonts.SEMIBOLD);

            float textX = cardX + 9 + iconAreaSize + 8;
            String displayName = nvg.getLimitText(profileName, 11, NvgFonts.MEDIUM, cardWidth - iconAreaSize - 50);
            nvg.drawText(displayName, textX, cardY + 12,
                    palette.getFontColor(ColorType.DARK), 11, NvgFonts.MEDIUM);

            if (isCurrent) {
                nvg.drawText(ICON_CHECK, textX, cardY + 28,
                        accentColor.getInterpolateColor(), 11, NvgFonts.LEGACYICON);
                nvg.drawText("Active", textX + 14, cardY + 29,
                        palette.getFontColor(ColorType.NORMAL), 8, NvgFonts.REGULAR);
            } else {
                nvg.drawText(ICON_TRASH, cardX + cardWidth - 22, cardY + 19,
                        palette.getMaterialRed(), 12, NvgFonts.LEGACYICON);
            }

            col++;
            if (col >= columns) {
                col = 0;
                listStartY += cardHeight + cardGap;
            }
        }

        // "Add profile" card
        float addCardX = x + 15 + col * (cardWidth + cardGap);
        float addCardY = y + listStartY;
        nvg.drawRoundedRect(addCardX, addCardY, cardWidth, cardHeight, 8,
                palette.getBackgroundColor(ColorType.DARK));
        nvg.drawCenteredText(ICON_PLUS, addCardX + cardWidth / 2, addCardY + 15,
                palette.getFontColor(ColorType.DARK), 20, NvgFonts.LEGACYICON);

        float totalContentHeight = listStartY + cardHeight + cardGap;
        scroll.setMaxScroll(Math.max(0, totalContentHeight - height + 20));

        nvg.restore();
        nvg.restore();

        // ---- Create profile panel ----
        nvg.save();
        nvg.translate((float) (panelAnimation.getValue() * 600), 0);

        float panelX = x + 15;
        float panelY = y + 15;
        float panelW = width - 30;
        float panelH = height - 30;

        nvg.drawRoundedRect(panelX, panelY, panelW, panelH, 10,
                palette.getBackgroundColor(ColorType.DARK));

        nvg.drawRect(panelX, panelY + 32, panelW, 1,
                palette.getBackgroundColor(ColorType.NORMAL));

        nvg.drawText(ICON_BACK, panelX + 10, panelY + 10,
                palette.getFontColor(ColorType.NORMAL), 12, NvgFonts.LEGACYICON);
        nvg.drawText("Create Profile", panelX + 28, panelY + 9,
                palette.getFontColor(ColorType.DARK), 13, NvgFonts.MEDIUM);

        nvg.drawText("Name", panelX + 16, panelY + 46,
                palette.getFontColor(ColorType.DARK), 12, NvgFonts.MEDIUM);

        float fieldX = panelX + 16;
        float fieldY = panelY + 65;
        float fieldW = panelW - 32;
        float fieldH = 24;

        Color fieldBg = nameFieldFocused
                ? palette.getBackgroundColor(ColorType.NORMAL)
                : palette.getBackgroundColor(ColorType.NORMAL);
        nvg.drawRoundedRect(fieldX, fieldY, fieldW, fieldH, 6, fieldBg);

        if (nameFieldFocused) {
            nvg.drawGradientOutlineRoundedRect(fieldX, fieldY, fieldW, fieldH, 6, 1.2F,
                    accentColor.getColor1(), accentColor.getColor2());
        }

        if (createNameBuffer.isEmpty() && !nameFieldFocused) {
            nvg.drawText("Enter profile name...", fieldX + 8, fieldY + 6,
                    palette.getFontColor(ColorType.NORMAL), 10, NvgFonts.REGULAR);
        } else {
            String cursor = (nameFieldFocused && cursorBlink < 20) ? "|" : "";
            nvg.drawText(createNameBuffer + cursor, fieldX + 8, fieldY + 6,
                    palette.getFontColor(ColorType.DARK), 10, NvgFonts.REGULAR);
        }

        float btnW = 100;
        float btnH = 26;
        float btnX = panelX + panelW - btnW - 16;
        float btnY = panelY + panelH - btnH - 16;

        boolean canCreate = !createNameBuffer.trim().isEmpty();
        Color btnColor1 = canCreate ? accentColor.getColor1() : palette.getBackgroundColor(ColorType.NORMAL);
        Color btnColor2 = canCreate ? accentColor.getColor2() : palette.getBackgroundColor(ColorType.NORMAL);
        nvg.drawGradientRoundedRect(btnX, btnY, btnW, btnH, 6, btnColor1, btnColor2);

        Color btnTextColor = canCreate ? Color.WHITE : palette.getFontColor(ColorType.NORMAL);
        nvg.drawCenteredText("Create", btnX + btnW / 2, btnY + 7,
                btnTextColor, 11, NvgFonts.MEDIUM);

        nvg.restore();
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        GlideContext ctx = GlideContext.getInstance();
        NvgManager nvg = ctx.getNvgManager();
        RavenProfileAdapter profileAdapter = ctx.getProfileAdapter();

        if (showCreatePanel && panelAnimation.isDone(Direction.BACKWARDS)) {
            float panelX = x + 15;
            float panelY = y + 15;
            float panelW = width - 30;
            float panelH = height - 30;

            if (MouseUtils.isInside(mouseX, mouseY, panelX + 10, panelY + 8, 16, 16) && mouseButton == 0) {
                showCreatePanel = false;
                return;
            }

            float fieldX = panelX + 16;
            float fieldY = panelY + 65;
            float fieldW = panelW - 32;
            float fieldH = 24;

            nameFieldFocused = MouseUtils.isInside(mouseX, mouseY, fieldX, fieldY, fieldW, fieldH);

            float btnW = 100;
            float btnH = 26;
            float btnX = panelX + panelW - btnW - 16;
            float btnY = panelY + panelH - btnH - 16;

            if (MouseUtils.isInside(mouseX, mouseY, btnX, btnY, btnW, btnH) && mouseButton == 0) {
                String name = createNameBuffer.trim();
                if (!name.isEmpty()) {
                    profileAdapter.saveProfile(name);
                    showCreatePanel = false;
                }
            }
            return;
        }

        if (mouseButton == 3) {
            showCreatePanel = false;
            return;
        }

        if (mouseButton != 0) {
            return;
        }

        List<String> profiles = profileAdapter.getProfiles();
        String currentProfile = profileAdapter.getCurrentProfileName();

        float cardWidth = 170;
        float cardHeight = 52;
        float cardGap = 10;
        int columns = Math.max(1, (int) ((width - 30) / (cardWidth + cardGap)));

        float offsetY = 13;
        int col = 0;
        float listStartY = offsetY;

        float scrollOffset = scroll.getValue();

        for (int i = 0; i < profiles.size(); i++) {
            String profileName = profiles.get(i);
            boolean isCurrent = profileName.equals(currentProfile);
            float cardX = x + 15 + col * (cardWidth + cardGap);
            float cardY = y + listStartY + scrollOffset;

            if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height)) {
                if (!isCurrent) {
                    float trashX = cardX + cardWidth - 22;
                    float trashY = cardY + 19;
                    if (MouseUtils.isInside(mouseX, mouseY, trashX - 2, trashY - 2, 16, 16)) {
                        profileAdapter.deleteProfile(profileName);
                        return;
                    }
                }

                if (MouseUtils.isInside(mouseX, mouseY, cardX, cardY, cardWidth, cardHeight)) {
                    if (!isCurrent) {
                        profileAdapter.loadProfile(profileName);
                    }
                    return;
                }
            }

            col++;
            if (col >= columns) {
                col = 0;
                listStartY += cardHeight + cardGap;
            }
        }

        float addCardX = x + 15 + col * (cardWidth + cardGap);
        float addCardY = y + listStartY + scrollOffset;
        if (MouseUtils.isInside(mouseX, mouseY, addCardX, addCardY, cardWidth, cardHeight)) {
            showCreatePanel = true;
            canClose = false;
            nameFieldFocused = true;
            panelAnimation.setDirection(Direction.BACKWARDS);
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (showCreatePanel) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                showCreatePanel = false;
                return;
            }

            if (nameFieldFocused) {
                if (keyCode == Keyboard.KEY_BACK) {
                    if (!createNameBuffer.isEmpty()) {
                        createNameBuffer = createNameBuffer.substring(0, createNameBuffer.length() - 1);
                    }
                } else if (keyCode == Keyboard.KEY_RETURN) {
                    String name = createNameBuffer.trim();
                    if (!name.isEmpty()) {
                        GlideContext.getInstance().getProfileAdapter().saveProfile(name);
                        showCreatePanel = false;
                    }
                } else if (isAllowedChar(typedChar)) {
                    if (createNameBuffer.length() < 24) {
                        createNameBuffer += typedChar;
                    }
                }
            }
        }
    }

    private boolean isAllowedChar(char c) {
        return c >= ' ' && c != '\u007f';
    }

    // ---- Accessors matching GlideCategory contract ----

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
}

package keystrokesmod.render.glide.gui.comp;

import java.awt.Color;

import keystrokesmod.render.glide.GlideContext;
import keystrokesmod.render.glide.animation.SimpleAnimation;
import keystrokesmod.render.glide.color.AccentColor;
import keystrokesmod.render.glide.color.ColorPalette;
import keystrokesmod.render.glide.color.ColorType;
import keystrokesmod.render.glide.nanovg.NvgManager;
import keystrokesmod.render.glide.nanovg.font.NvgFonts;
import keystrokesmod.render.glide.util.MouseUtils;

public class GlideComboBox {

    private float x, y, width, height;
    private String[] options;
    private int selectedIndex;
    private boolean expanded;
    private final SimpleAnimation expandAnimation;
    private final SimpleAnimation hoverAnimation;
    private final SimpleAnimation[] optionHoverAnimations;

    public GlideComboBox(float x, float y, float width, float height, String[] options, int selectedIndex) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.options = options;
        this.selectedIndex = Math.max(0, Math.min(selectedIndex, options.length - 1));
        this.expanded = false;
        this.expandAnimation = new SimpleAnimation();
        this.hoverAnimation = new SimpleAnimation();
        this.optionHoverAnimations = new SimpleAnimation[options.length];
        for (int i = 0; i < options.length; i++) {
            optionHoverAnimations[i] = new SimpleAnimation();
        }
    }

    public GlideComboBox(float x, float y, float width, float height, String[] options) {
        this(x, y, width, height, options, 0);
    }

    public void draw(int mouseX, int mouseY, float partialTicks) {
        GlideContext ctx = GlideContext.getInstance();
        NvgManager nvg = ctx.getNvgManager();
        ColorPalette palette = ctx.getColorManager().getPalette();
        AccentColor accent = ctx.getColorManager().getCurrentColor();

        boolean isInsideHeader = MouseUtils.isInside(mouseX, mouseY, x, y, width, height);
        hoverAnimation.setAnimation(isInsideHeader ? 1.0F : 0.0F, 16);
        expandAnimation.setAnimation(expanded ? 1.0F : 0.0F, 16);

        Color headerBg = palette.getBackgroundColor(ColorType.DARK);
        nvg.drawRoundedRect(x, y, width, height, 6, headerBg);

        String selectedText = (selectedIndex >= 0 && selectedIndex < options.length)
                ? options[selectedIndex] : "";
        float fontSize = height * 0.45F;
        float textH = nvg.getTextHeight(selectedText.isEmpty() ? "A" : selectedText, fontSize, NvgFonts.REGULAR);
        float textY = y + (height / 2F) - (textH / 2F);

        nvg.drawText(selectedText, x + 8, textY,
                palette.getFontColor(ColorType.NORMAL), fontSize, NvgFonts.REGULAR);

        float arrowSize = 4F;
        float arrowX = x + width - 14;
        float arrowY = y + (height / 2F);
        float arrowAngle = expanded ? 270F : 90F;
        nvg.drawArrow(arrowX, arrowY, arrowSize, arrowAngle,
                palette.getFontColor(ColorType.NORMAL));

        if (expandAnimation.getValue() > 0.01F) {
            float dropdownHeight = options.length * height;
            float animatedHeight = dropdownHeight * expandAnimation.getValue();
            float dropdownY = y + height + 2;

            nvg.save();
            nvg.scissor(x, dropdownY, width, animatedHeight);

            nvg.drawRoundedRect(x, dropdownY, width, dropdownHeight, 6,
                    palette.getBackgroundColor(ColorType.NORMAL));

            for (int i = 0; i < options.length; i++) {
                float optionY = dropdownY + (i * height);
                boolean isOverOption = MouseUtils.isInside(mouseX, mouseY, x, optionY, width, height) && expanded;

                if (i < optionHoverAnimations.length) {
                    optionHoverAnimations[i].setAnimation(isOverOption ? 1.0F : 0.0F, 16);
                }

                if (i == selectedIndex) {
                    Color accentColor = accent.getInterpolateColor();
                    nvg.drawRoundedRect(x + 2, optionY + 2, width - 4, height - 4, 4,
                            new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 60));
                } else if (i < optionHoverAnimations.length && optionHoverAnimations[i].getValue() > 0.01F) {
                    int alpha = (int) (optionHoverAnimations[i].getValue() * 30);
                    nvg.drawRoundedRect(x + 2, optionY + 2, width - 4, height - 4, 4,
                            new Color(255, 255, 255, Math.min(255, alpha)));
                }

                float optTextH = nvg.getTextHeight(options[i], fontSize, NvgFonts.REGULAR);
                nvg.drawText(options[i], x + 8, optionY + (height / 2F) - (optTextH / 2F),
                        palette.getFontColor(ColorType.NORMAL), fontSize, NvgFonts.REGULAR);
            }

            nvg.restore();
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }

        boolean isInsideHeader = MouseUtils.isInside(mouseX, mouseY, x, y, width, height);

        if (isInsideHeader) {
            expanded = !expanded;
            return;
        }

        if (expanded) {
            float dropdownY = y + height + 2;
            for (int i = 0; i < options.length; i++) {
                float optionY = dropdownY + (i * height);
                if (MouseUtils.isInside(mouseX, mouseY, x, optionY, width, height)) {
                    selectedIndex = i;
                    expanded = false;
                    return;
                }
            }
            expanded = false;
        }
    }

    public String getSelectedOption() {
        if (selectedIndex >= 0 && selectedIndex < options.length) {
            return options[selectedIndex];
        }
        return "";
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = Math.max(0, Math.min(selectedIndex, options.length - 1));
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
        if (selectedIndex >= options.length) {
            selectedIndex = 0;
        }
        SimpleAnimation[] newAnims = new SimpleAnimation[options.length];
        for (int i = 0; i < options.length; i++) {
            newAnims[i] = (i < optionHoverAnimations.length) ? optionHoverAnimations[i] : new SimpleAnimation();
        }
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }
}

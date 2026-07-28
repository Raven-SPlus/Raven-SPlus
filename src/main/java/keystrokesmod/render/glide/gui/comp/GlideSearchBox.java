package keystrokesmod.render.glide.gui.comp;

import java.awt.Color;

import keystrokesmod.render.glide.GlideContext;
import keystrokesmod.render.glide.animation.SimpleAnimation;
import keystrokesmod.render.glide.color.ColorPalette;
import keystrokesmod.render.glide.color.ColorType;
import keystrokesmod.render.glide.nanovg.NvgManager;
import keystrokesmod.render.glide.nanovg.font.NvgFonts;
import keystrokesmod.render.glide.util.MouseUtils;
import keystrokesmod.render.glide.util.TimerUtils;
import net.minecraft.client.gui.GuiScreen;

public class GlideSearchBox {

    private float x, y, width, height;
    private String text;
    private boolean focused;
    private int cursorPosition;
    private int selectionEnd;
    private final TimerUtils cursorTimer;
    private final SimpleAnimation placeholderAnimation;

    public GlideSearchBox(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = "";
        this.focused = false;
        this.cursorPosition = 0;
        this.selectionEnd = 0;
        this.cursorTimer = new TimerUtils();
        this.placeholderAnimation = new SimpleAnimation(1.0F);
    }

    public void draw(int mouseX, int mouseY, float partialTicks) {
        GlideContext ctx = GlideContext.getInstance();
        NvgManager nvg = ctx.getNvgManager();
        ColorPalette palette = ctx.getColorManager().getPalette();

        float halfHeight = height / 2F;
        float fontSize = halfHeight;

        float addX = 0;
        int outTextSize = 0;
        String resultText = "";

        for (int i = 0; i < text.length(); i++) {
            resultText = resultText + text.charAt(i);
            if (nvg.getTextWidth(resultText, fontSize, NvgFonts.REGULAR) + halfHeight + 5 > width) {
                outTextSize++;
                addX = width - nvg.getTextWidth(resultText, fontSize, NvgFonts.REGULAR) - halfHeight - 5;
            }
        }

        if (selectionEnd < outTextSize) {
            StringBuilder reversed = new StringBuilder(text).reverse();
            addX = width - nvg.getTextWidth(reversed.toString().substring(outTextSize - selectionEnd), fontSize, NvgFonts.REGULAR) - halfHeight - 5;
        }

        nvg.drawRoundedRect(x, y, width, height, 6, palette.getBackgroundColor(ColorType.DARK));

        nvg.save();
        nvg.scissor(x + 1, y, width - 2, height);

        if (cursorPosition != selectionEnd) {
            int start = selectionEnd > cursorPosition ? cursorPosition : selectionEnd;
            int end = selectionEnd > cursorPosition ? selectionEnd : cursorPosition;

            String sub = safeSubstring(text, start, end);
            float selW = nvg.getTextWidth(sub, fontSize, NvgFonts.REGULAR);
            float offset = nvg.getTextWidth(safeSubstring(text, 0, start), fontSize, NvgFonts.REGULAR);

            if (selW != 0) {
                float textH = nvg.getTextHeight(text.isEmpty() ? "A" : text, fontSize, NvgFonts.REGULAR);
                nvg.drawRect(x + 10 + offset + addX,
                        y + (height / 2) - (textH / 2),
                        selW, textH, new Color(0, 135, 247));
            }
        }

        placeholderAnimation.setAnimation(!focused && text.isEmpty() ? 1.0F : 0.0F, 16);

        if (text.isEmpty()) {
            nvg.save();
            float textH = nvg.getTextHeight("A", fontSize, NvgFonts.REGULAR);
            nvg.translate((placeholderAnimation.getValue() * 8) - 8, 0);
            nvg.drawText("Search...", x + 10, y + (height / 2) - (textH / 2) + 1,
                    palette.getFontColor(ColorType.NORMAL, (int) (placeholderAnimation.getValue() * 200)),
                    fontSize, NvgFonts.REGULAR);
            nvg.restore();
        }

        float textH = nvg.getTextHeight(text.isEmpty() ? "A" : text, fontSize, NvgFonts.REGULAR);
        nvg.drawText(text, x + 10 + addX, y + (height / 2) - (textH / 2) + 1,
                palette.getFontColor(ColorType.NORMAL), fontSize, NvgFonts.REGULAR);

        if (cursorTimer.delay(600)) {
            float position = nvg.getTextWidth(text, fontSize, NvgFonts.REGULAR)
                    - nvg.getTextWidth(safeSubstring(text, cursorPosition, text.length()), fontSize, NvgFonts.REGULAR);

            if (focused && cursorPosition == selectionEnd) {
                nvg.drawRect(x + 10 + addX + position,
                        y + (height / 2) - (textH / 2) - 0.5F,
                        0.7F, textH + 1, palette.getFontColor(ColorType.DARK));
            }

            if (cursorTimer.delay(1200)) {
                cursorTimer.reset();
            }
        }

        nvg.restore();
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean inside = MouseUtils.isInside(mouseX, mouseY, x, y, width, height);
        setFocused(inside);

        if (inside && mouseButton == 1) {
            setText("");
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (!focused) {
            return;
        }

        boolean ctrl = GuiScreen.isCtrlKeyDown();

        if (ctrl && keyCode == 30) { // Ctrl+A
            selectionEnd = 0;
            cursorPosition = text.length();
            return;
        }

        if (ctrl && keyCode == 46) { // Ctrl+C
            if (cursorPosition != selectionEnd) {
                int start = Math.min(cursorPosition, selectionEnd);
                int end = Math.max(cursorPosition, selectionEnd);
                GuiScreen.setClipboardString(safeSubstring(text, start, end));
            }
            return;
        }

        if (ctrl && keyCode == 47) { // Ctrl+V
            String clip = GuiScreen.getClipboardString();
            if (clip != null) {
                deleteSelected();
                text = safeSubstring(text, 0, cursorPosition) + clip + safeSubstring(text, cursorPosition, text.length());
                cursorPosition += clip.length();
                selectionEnd = cursorPosition;
            }
            return;
        }

        if (ctrl && keyCode == 45) { // Ctrl+X
            if (cursorPosition != selectionEnd) {
                int start = Math.min(cursorPosition, selectionEnd);
                int end = Math.max(cursorPosition, selectionEnd);
                GuiScreen.setClipboardString(safeSubstring(text, start, end));
                deleteSelected();
            }
            return;
        }

        switch (keyCode) {
            case 14: // Backspace
                if (cursorPosition != selectionEnd) {
                    deleteSelected();
                } else if (cursorPosition > 0) {
                    text = safeSubstring(text, 0, cursorPosition - 1) + safeSubstring(text, cursorPosition, text.length());
                    cursorPosition--;
                    selectionEnd = cursorPosition;
                }
                break;

            case 211: // Delete
                if (cursorPosition != selectionEnd) {
                    deleteSelected();
                } else if (cursorPosition < text.length()) {
                    text = safeSubstring(text, 0, cursorPosition) + safeSubstring(text, cursorPosition + 1, text.length());
                }
                break;

            case 203: // Left
                if (cursorPosition > 0) {
                    cursorPosition--;
                }
                if (!GuiScreen.isShiftKeyDown()) {
                    selectionEnd = cursorPosition;
                }
                break;

            case 205: // Right
                if (cursorPosition < text.length()) {
                    cursorPosition++;
                }
                if (!GuiScreen.isShiftKeyDown()) {
                    selectionEnd = cursorPosition;
                }
                break;

            case 199: // Home
                cursorPosition = 0;
                if (!GuiScreen.isShiftKeyDown()) {
                    selectionEnd = cursorPosition;
                }
                break;

            case 207: // End
                cursorPosition = text.length();
                if (!GuiScreen.isShiftKeyDown()) {
                    selectionEnd = cursorPosition;
                }
                break;

            default:
                if (isPrintableChar(typedChar)) {
                    deleteSelected();
                    text = safeSubstring(text, 0, cursorPosition) + typedChar + safeSubstring(text, cursorPosition, text.length());
                    cursorPosition++;
                    selectionEnd = cursorPosition;
                }
                break;
        }
    }

    private void deleteSelected() {
        if (cursorPosition == selectionEnd) {
            return;
        }
        int start = Math.min(cursorPosition, selectionEnd);
        int end = Math.max(cursorPosition, selectionEnd);
        text = safeSubstring(text, 0, start) + safeSubstring(text, end, text.length());
        cursorPosition = start;
        selectionEnd = start;
    }

    private static boolean isPrintableChar(char c) {
        return c >= ' ' && c != '\u007f';
    }

    private static String safeSubstring(String s, int start, int end) {
        start = Math.max(0, Math.min(start, s.length()));
        end = Math.max(start, Math.min(end, s.length()));
        return s.substring(start, end);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        this.cursorPosition = text.length();
        this.selectionEnd = this.cursorPosition;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) {
            cursorTimer.reset();
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

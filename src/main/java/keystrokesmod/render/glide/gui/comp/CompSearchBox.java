package keystrokesmod.render.glide.gui.comp;

import java.awt.Color;

import keystrokesmod.render.glide.GlideContext;
import keystrokesmod.render.glide.color.ColorPalette;
import keystrokesmod.render.glide.color.ColorType;
import keystrokesmod.render.glide.nanovg.NvgManager;
import keystrokesmod.render.glide.nanovg.font.NvgFonts;
import keystrokesmod.render.glide.util.MouseUtils;
import org.lwjgl.input.Keyboard;

public class CompSearchBox {

    private String text = "";
    private boolean focused;
    private float x, y, width, height;

    public void setPosition(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void draw(int mouseX, int mouseY, float partialTicks) {
        NvgManager nvg = GlideContext.getInstance().getNvgManager();
        ColorPalette palette = GlideContext.getInstance().getColorManager().getPalette();

        nvg.drawRoundedRect(x, y, width, height, 6, palette.getBackgroundColor(ColorType.DARK));

        String display = text.isEmpty() && !focused ? "Search..." : text;
        Color textColor = text.isEmpty() && !focused
                ? palette.getFontColor(ColorType.NORMAL)
                : palette.getFontColor(ColorType.DARK);

        nvg.drawText(display, x + 6, y + (height / 2) - 4.5f, textColor, 9, NvgFonts.REGULAR);

        if (focused) {
            String caret = (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "";
            float textW = nvg.getTextWidth(text, 9, NvgFonts.REGULAR);
            nvg.drawText(caret, x + 6 + textW, y + (height / 2) - 4.5f, palette.getFontColor(ColorType.DARK), 9, NvgFonts.REGULAR);
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            focused = MouseUtils.isInside(mouseX, mouseY, x, y, width, height);
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (!focused) {
            return;
        }

        if (keyCode == Keyboard.KEY_BACK) {
            if (!text.isEmpty()) {
                text = text.substring(0, text.length() - 1);
            }
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            // handled by parent
        } else if (typedChar >= 32 && typedChar != 167) {
            text = text + typedChar;
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }
}

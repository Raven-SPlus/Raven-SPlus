package keystrokesmod.render.glide.nanovg.font;

import java.nio.ByteBuffer;

import org.lwjgl.nanovg.NanoVG;

import keystrokesmod.render.glide.nanovg.NvgIOUtils;

public class NvgFontManager {

    public void init(long nvg) {
        loadFont(nvg, NvgFonts.GLICONIC);
        loadFont(nvg, NvgFonts.ICON_FILLED);
        loadFont(nvg, NvgFonts.ICON_OUTLINE);
        loadFont(nvg, NvgFonts.UNIFONT);
        loadFont(nvg, NvgFonts.FALLBACK);
        loadFont(nvg, NvgFonts.REGULAR);
        loadFont(nvg, NvgFonts.MEDIUM);
        loadFont(nvg, NvgFonts.SEMIBOLD);
        loadFont(nvg, NvgFonts.LEGACYICON);
        loadFont(nvg, NvgFonts.MOJANGLES);
    }

    private void loadFont(long nvg, NvgFont font) {
        if (font.isLoaded()) {
            return;
        }

        int loaded = -1;

        try {
            ByteBuffer buffer = NvgIOUtils.resourceToByteBuffer(font.getResourceLocation());
            if (buffer == null) {
                throw new IllegalStateException("resource missing: " + font.getResourceLocation());
            }
            loaded = NanoVG.nvgCreateFontMem(nvg, font.getName(), buffer, false);
            font.setBuffer(buffer);
        } catch (Exception e) {
            System.err.println("[Glide] Failed to load font " + font.getName() + ": " + e.getMessage());
        }

        if (loaded == -1) {
            throw new RuntimeException("Failed to init font " + font.getName());
        } else {
            font.setLoaded(true);
            if (font == NvgFonts.MOJANGLES && NvgFonts.UNIFONT.isLoaded()) {
                NanoVG.nvgAddFallbackFont(nvg, font.getName(), NvgFonts.UNIFONT.getName());
                NanoVG.nvgAddFallbackFont(nvg, font.getName(), NvgFonts.REGULAR.getName());
                NanoVG.nvgAddFallbackFont(nvg, font.getName(), NvgFonts.FALLBACK.getName());
            } else if (NvgFonts.FALLBACK.isLoaded() && font != NvgFonts.FALLBACK) {
                NanoVG.nvgAddFallbackFont(nvg, font.getName(), NvgFonts.FALLBACK.getName());
                NanoVG.nvgAddFallbackFont(nvg, font.getName(), NvgFonts.UNIFONT.getName());
            }

            if (font == NvgFonts.ICON_OUTLINE && NvgFonts.GLICONIC.isLoaded()) {
                NanoVG.nvgAddFallbackFont(nvg, font.getName(), NvgFonts.GLICONIC.getName());
            }

            if (font == NvgFonts.ICON_FILLED && NvgFonts.GLICONIC.isLoaded()) {
                NanoVG.nvgAddFallbackFont(nvg, font.getName(), NvgFonts.GLICONIC.getName());
            }
        }
    }
}

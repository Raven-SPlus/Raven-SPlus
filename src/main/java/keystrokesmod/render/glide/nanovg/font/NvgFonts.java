package keystrokesmod.render.glide.nanovg.font;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.util.ResourceLocation;

public class NvgFonts {

    private static final String PATH = "soar/fonts/";

    public static final NvgFont FALLBACK = new NvgFont("fallback", new ResourceLocation(PATH + "fallback.ttf"));
    public static final NvgFont REGULAR = new NvgFont("regular", new ResourceLocation(PATH + "inter/Inter-Regular.ttf"));
    public static final NvgFont MEDIUM = new NvgFont("medium", new ResourceLocation(PATH + "inter/Inter-Medium.ttf"));
    public static final NvgFont SEMIBOLD = new NvgFont("semi-bold", new ResourceLocation(PATH + "inter/Inter-SemiBold.ttf"));
    public static final NvgFont LEGACYICON = new NvgFont("icon", new ResourceLocation(PATH + "Icon.ttf"));
    public static final NvgFont GLICONIC = new NvgFont("gliconic", new ResourceLocation(PATH + "Gliconic.ttf"));
    public static final NvgFont ICON_OUTLINE = new NvgFont("icon-outline", new ResourceLocation(PATH + "FluentSystemIcons-Regular.ttf"));
    public static final NvgFont ICON_FILLED = new NvgFont("icon-filled", new ResourceLocation(PATH + "FluentSystemIcons-Filled.ttf"));
    public static final NvgFont MOJANGLES = new NvgFont("mojangles", new ResourceLocation(PATH + "mojangles.ttf"));
    public static final NvgFont UNIFONT = new NvgFont("unifont", new ResourceLocation(PATH + "unifont/unifont.otf"));

    public static ArrayList<NvgFont> getFonts() {
        return new ArrayList<NvgFont>(Arrays.asList(MEDIUM, SEMIBOLD, REGULAR, LEGACYICON, MOJANGLES));
    }
}

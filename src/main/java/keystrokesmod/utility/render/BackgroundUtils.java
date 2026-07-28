package keystrokesmod.utility.render;

import keystrokesmod.utility.render.blur.GaussianBlur;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class BackgroundUtils {
    private static final int BLOOM_COLOR = new Color(255, 255, 255, 50).getRGB();
    private static int huoCheX = -99999;
    private static final ResourceLocation BG = new ResourceLocation("keystrokesmod:textures/backgrounds/bg.png");
    private static final ResourceLocation QI = new ResourceLocation("keystrokesmod:textures/backgrounds/qi.png");
    private static final ResourceLocation DIAN_XIAN = new ResourceLocation("keystrokesmod:textures/backgrounds/DianXian.png");
    private static final ResourceLocation HUO_CHE = new ResourceLocation("keystrokesmod:textures/backgrounds/huoChe.png");
    private static final ResourceLocation DIAN_XIAN_2 = new ResourceLocation("keystrokesmod:textures/backgrounds/DianXian2.png");
    private static final ResourceLocation CAO = new ResourceLocation("keystrokesmod:textures/backgrounds/cao.png");
    private static final ResourceLocation REN = new ResourceLocation("keystrokesmod:textures/backgrounds/ren.png");

    public static void renderBackground(@NotNull GuiScreen gui) {
        final int width = gui.width;
        final int height = gui.height;

        // Use shader-based background from Rise client
        try {
            MainMenuShaderRenderer.renderBackground(gui);
        } catch (Exception e) {
            // Fallback to original image-based background if shader fails
            e.printStackTrace();
            renderFallbackBackground(gui, width, height);
        }
    }

    private static void renderFallbackBackground(@NotNull GuiScreen gui, int width, int height) {
        if (huoCheX == -99999)
            huoCheX = -width;

        RenderUtils.drawImage(BG, 0, 0, width, height);
        RenderUtils.drawImage(QI, 0, 0, width, height);
        RenderUtils.drawImage(DIAN_XIAN, 0, 0, width, height);
        RenderUtils.drawImage(HUO_CHE, huoCheX, height / 3F, width * 2F, height / 3F);
        RenderUtils.drawImage(DIAN_XIAN_2, 0, 0, width, height);
        RenderUtils.drawImage(CAO, 0, 0, width, height);
        RenderUtils.drawBloomShadow(0, 0, width, height, 12, 6, BLOOM_COLOR, true);
        RenderUtils.drawImage(REN, 0, 0, width, height);
        if (huoCheX >= 0) {
            huoCheX = -width;
        }
        huoCheX++;
    }
}

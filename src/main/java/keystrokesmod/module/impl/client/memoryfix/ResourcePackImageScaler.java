package keystrokesmod.module.impl.client.memoryfix;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class ResourcePackImageScaler {
    public static final int SIZE = 64;

    private ResourcePackImageScaler() {
    }

    public static BufferedImage scalePackImage(BufferedImage image) {
        if (image == null) {
            return null;
        }

        if (image.getWidth() == SIZE && image.getHeight() == SIZE) {
            return image;
        }

        BufferedImage scaledImage = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaledImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(image, 0, 0, SIZE, SIZE, null);
        graphics.dispose();
        return scaledImage;
    }
}

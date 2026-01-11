package keystrokesmod.module.impl.client.notification;

import keystrokesmod.module.impl.client.Notifications;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.font.CenterMode;
import keystrokesmod.utility.font.FontManager;
import keystrokesmod.utility.font.IFont;
import keystrokesmod.utility.render.ColorUtils;
import keystrokesmod.utility.render.RRectUtils;
import keystrokesmod.utility.render.blur.HudBlurBatcher;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class DefaultNotification extends SubMode<Notifications> implements INotification {
    private final ModeSetting font;
    private final ButtonSetting blurBackground;

    public DefaultNotification(String name, @NotNull Notifications parent) {
        super(name, parent);
        this.registerSetting(font = new ModeSetting("Font", new String[]{"Minecraft", "Regular", "Product Sans", "Tenacity"}, 2));
        this.registerSetting(blurBackground = new ButtonSetting("Blur background", false));
    }

    private IFont getFont() {
        switch ((int) font.getInput()) {
            case 0:
                return FontManager.getMinecraft();
            case 1:
                return FontManager.regular16;
            default:
            case 2:
                return FontManager.productSans16;
            case 3:
                return FontManager.tenacity16;
        }
    }

    @Override
    public void render(Notifications.@NotNull Notification notification) {
        double notifX = notification.animationX.getValue();
        double notifY = notification.animationY.getValue();
        double notifWidth = 120;
        double notifHeight = 25;
        double centerY = notifY + notifHeight / 2.0;

        if (blurBackground.isToggled()) {
            HudBlurBatcher.addBlurStencil(1, () ->
                    RRectUtils.drawRound(notifX, notifY, notifWidth, notifHeight, 3, new Color(255, 255, 255, 255))
            );
            HudBlurBatcher.addAfterBlur(() -> {
                // Draw notification background
                RRectUtils.drawRound(notifX, notifY, notifWidth, notifHeight, 3, new Color(0, 0, 0, 128));

                // Calculate icon position (centered vertically, offset from left)
                double iconX = notifX + 12.5;
                double iconY = centerY;
                String iconChar = notification.type == Notifications.NotificationTypes.INFO ? "G" : "R";
                FontManager.icon20.drawString(iconChar, iconX, iconY, CenterMode.XY, false, ColorUtils.getFontColor(2).getRGB());

                // Calculate text position (centered vertically, after icon)
                String[] messageParts = notification.message.split("§");
                double textX = notifX + 30;
                double textY = centerY;

                if (messageParts.length == 1) {
                    getFont().drawString(notification.message, textX, textY, CenterMode.Y, false, Color.WHITE.getRGB());
                } else {
                    double currentX = textX;
                    for (String part : messageParts) {
                        if (part.isEmpty()) continue;
                        char colorCode = part.charAt(0);
                        String text = part.substring(1);
                        Color color = ColorUtils.getColorFromCode("§" + colorCode);
                        getFont().drawString(text, currentX, textY, CenterMode.Y, false, color.getRGB());
                        currentX += getFont().width(text);
                    }
                }
            });
            return;
        }

        // Non-blur path: draw immediately
        RRectUtils.drawRound(notifX, notifY, notifWidth, notifHeight, 3, new Color(0, 0, 0, 128));
        double iconX = notifX + 12.5;
        double iconY = centerY;
        String iconChar = notification.type == Notifications.NotificationTypes.INFO ? "G" : "R";
        FontManager.icon20.drawString(iconChar, iconX, iconY, CenterMode.XY, false, ColorUtils.getFontColor(2).getRGB());

        String[] messageParts = notification.message.split("§");
        double textX = notifX + 30;
        double textY = centerY;
        if (messageParts.length == 1) {
            getFont().drawString(notification.message, textX, textY, CenterMode.Y, false, Color.WHITE.getRGB());
        } else {
            double currentX = textX;
            for (String part : messageParts) {
                if (part.isEmpty()) continue;
                char colorCode = part.charAt(0);
                String text = part.substring(1);
                Color color = ColorUtils.getColorFromCode("§" + colorCode);
                getFont().drawString(text, currentX, textY, CenterMode.Y, false, color.getRGB());
                currentX += getFont().width(text);
            }
        }
    }
}

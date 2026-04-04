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
import keystrokesmod.utility.render.RenderUtils;
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
        double notifWidth = getNotificationWidth(notification.message);
        double notifHeight = 25;
        double centerY = notifY + notifHeight / 2.0;

        if (blurBackground.isToggled()) {
            HudBlurBatcher.addPreBlur(() ->
                    RenderUtils.drawBloomShadow(
                            (float) notifX - 3,
                            (float) notifY - 3,
                            (float) notifWidth + 6,
                            (float) notifHeight + 6,
                            10,
                            new Color(0, 0, 0, 28),
                            false
                    )
            );
            HudBlurBatcher.addBlurStencil(1, () ->
                    RRectUtils.drawRound(notifX, notifY, notifWidth, notifHeight, 3, new Color(255, 255, 255, 255))
            );
            HudBlurBatcher.addAfterBlur(() -> {
                drawNotification(notification, notifX, notifY, notifWidth, notifHeight, centerY);
            });
            return;
        }

        drawNotification(notification, notifX, notifY, notifWidth, notifHeight, centerY);
    }

    private double getNotificationWidth(@NotNull String message) {
        double textXOffset = 30.0;
        double rightPadding = 12.0;
        return Math.max(120.0, textXOffset + getMessageWidth(message) + rightPadding);
    }

    private double getMessageWidth(@NotNull String message) {
        String[] messageParts = message.split("§");
        if (messageParts.length == 1) {
            return getFont().width(message);
        }

        double width = 0.0;
        for (String part : messageParts) {
            if (part.isEmpty()) {
                continue;
            }

            String text = part.length() > 1 ? part.substring(1) : "";
            width += getFont().width(text);
        }

        return width;
    }

    private void drawNotification(Notifications.@NotNull Notification notification, double notifX, double notifY, double notifWidth, double notifHeight, double centerY) {
        RRectUtils.drawRound(notifX, notifY, notifWidth, notifHeight, 3, new Color(0, 0, 0, 128));
        double iconX = notifX + 12.5;
        double iconY = centerY;
        String iconChar = notification.type == Notifications.NotificationTypes.INFO ? "G" : "R";
        FontManager.icon20.drawString(iconChar, iconX, iconY, CenterMode.XY, false, ColorUtils.getFontColor(2).getRGB());

        double textX = notifX + 30;
        double textY = centerY;
        String[] messageParts = notification.message.split("§");
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

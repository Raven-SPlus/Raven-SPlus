package keystrokesmod.clickgui;

import keystrokesmod.clickgui.components.impl.CategoryComponent;
import keystrokesmod.clickgui.components.impl.ModuleComponent;
import keystrokesmod.module.Module;
import keystrokesmod.utility.render.RenderUtils;
import keystrokesmod.utility.font.FontManager;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class PanelClickGui {
    private static Module.category currentCategory = Module.category.combat;
    private static float scrollY = 0;
    private static float targetScrollY = 0;
    private static float maxScroll = 0;

    // Window state
    private static int x = 100;
    private static int y = 100;
    private static int width = 600;
    private static int height = 400;
    private static boolean dragging = false;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;
    private static boolean initialized = false;

    public static void init() {
        if (!initialized) {
            ScaledResolution sr = new ScaledResolution(keystrokesmod.Raven.mc);
            x = sr.getScaledWidth() / 2 - width / 2;
            y = sr.getScaledHeight() / 2 - height / 2;
            initialized = true;
        }
        currentCategory = Module.category.combat;
        scrollY = 0;
        targetScrollY = 0;
    }

    public static void drawScreen(int mouseX, int mouseY, float partialTicks, ClickGui parent) {
        final int headerHeight = 48;

        // Handle dragging
        if (dragging) {
            x = mouseX - dragOffsetX;
            y = mouseY - dragOffsetY;
        }

        // Draw Shadow
        RenderUtils.drawBloomShadow(x, y, width, height, 20, 10, new Color(0, 0, 0, 150).getRGB(), true);

        // Draw Panel Background
        RenderUtils.drawRoundedRectangle(x, y, x + width, y + height, 12, new Color(26, 26, 26, 235).getRGB());

        // Header bar
        RenderUtils.drawRoundedGradientRect(
                x, y, x + width, y + headerHeight,
                12,
                new Color(35, 35, 35, 255).getRGB(),
                new Color(30, 30, 30, 255).getRGB(),
                new Color(30, 30, 30, 255).getRGB(),
                new Color(26, 26, 26, 255).getRGB());

        // Sidebar Background
        int sidebarWidth = 120;
        RenderUtils.drawRoundedGradientRect(
                x + 6,
                y + headerHeight + 6,
                x + sidebarWidth,
                y + height - 6,
                10,
                new Color(24, 24, 24, 255).getRGB(),
                new Color(22, 22, 22, 255).getRGB(),
                new Color(22, 22, 22, 255).getRGB(),
                new Color(20, 20, 20, 255).getRGB());

        // Separator
        // RenderUtils.drawRect(x + sidebarWidth, y + 10, x + sidebarWidth + 1, y + height - 10, new Color(50, 50, 50).getRGB());

        // Draw Title (Header)
        FontManager.productSans20.drawString("Raven S+", x + 18, y + 18, new Color(255, 255, 255).getRGB());
        FontManager.tenacity16.drawString("Click GUI", x + 18, y + 32, new Color(180, 180, 180).getRGB());

        // Categories
        int catY = y + headerHeight + 18;
        for (Module.category category : Module.category.values()) {
            boolean selected = category == currentCategory;
            int color = selected ? new Color(24, 154, 255).getRGB() : new Color(180, 180, 180).getRGB();
            
            // Hover effect
            if (mouseX >= x && mouseX <= x + sidebarWidth && mouseY >= catY - 5 && mouseY <= catY + 15) {
                if (!selected) color = new Color(255, 255, 255).getRGB();
            }

            if (selected) {
                 RenderUtils.drawRoundedRectangle(x + 10, catY - 4, x + sidebarWidth - 10, catY + 14, 4, new Color(24, 154, 255, 30).getRGB());
                 RenderUtils.drawRect(x + 10, catY - 2, x + 12, catY + 12, new Color(24, 154, 255).getRGB());
            }

            FontManager.productSans20.drawString(category.name(), x + 25, catY, color);
            catY += 28;
        }

        // Content Area
        int contentX = x + sidebarWidth + 24;
        int contentY = y + headerHeight + 12;
        int contentWidth = width - sidebarWidth - 36;
        int contentHeight = height - headerHeight - 24;

        // Content background
        RenderUtils.drawRoundedRectangle(contentX - 6, contentY - 4, contentX + contentWidth + 6, contentY + contentHeight + 4, 10, new Color(18, 18, 18, 200).getRGB());

        // Scissor for content
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.scissor(contentX, contentY, contentWidth, contentHeight);

        CategoryComponent categoryComponent = ClickGui.categories.get(currentCategory);
        if (categoryComponent != null) {
            // Setup component for panel rendering
            categoryComponent.x(contentX);
            categoryComponent.width(contentWidth);
            categoryComponent.y(contentY + (int)scrollY);

            // Handle Scroll
            int dWheel = Mouse.getDWheel();
            if (dWheel != 0) {
                if (dWheel > 0) targetScrollY += 20;
                else targetScrollY -= 20;
            }
            
            // Clamp target scroll
            if (targetScrollY > 0) targetScrollY = 0;
            if (maxScroll > 0 && targetScrollY < -maxScroll) targetScrollY = -maxScroll;

            // Smooth scroll
            scrollY += (targetScrollY - scrollY) * 0.2f;

            // Render modules
            int moduleY = 0;
            for (ModuleComponent module : categoryComponent.getModules()) {
                module.so(moduleY); // Relative offset from category Y
                
                // Only render if visible
                float actualY = categoryComponent.getY() + moduleY;
                if (actualY + module.gh() > contentY - 8 && actualY < contentY + contentHeight + 8) { // Visibility check relative to content box
                    module.onDrawScreen(mouseX, mouseY);
                    module.render();
                }
                
                moduleY += module.gh() + 6; // Gap between modules
            }
            
            // Update max scroll (content height - visible height)
            maxScroll = Math.max(0, moduleY - contentHeight);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopMatrix();
    }

    public static void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // Window Header Dragging
        // Header drag zone
        final int headerHeight = 48;
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight) {
             dragging = true;
             dragOffsetX = mouseX - x;
             dragOffsetY = mouseY - y;
             return;
        }

        int sidebarWidth = 120;
        int catY = y + headerHeight + 18;
        
        // Sidebar clicks
        if (mouseX >= x + 6 && mouseX <= x + sidebarWidth && mouseY >= y + headerHeight + 6 && mouseY <= y + height - 6) {
            for (Module.category category : Module.category.values()) {
                if (mouseY >= catY - 5 && mouseY <= catY + 20) {
                    currentCategory = category;
                    targetScrollY = 0;
                    scrollY = 0;
                    return;
                }
                catY += 30;
            }
            return;
        }

        // Content clicks
        if (mouseX > x + sidebarWidth && mouseX < x + width && mouseY > y && mouseY < y + height) {
             CategoryComponent categoryComponent = ClickGui.categories.get(currentCategory);
             if (categoryComponent != null) {
                 for (ModuleComponent module : categoryComponent.getModules()) {
                     module.onClick(mouseX, mouseY, mouseButton);
                 }
             }
        }
    }

    public static void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        CategoryComponent categoryComponent = ClickGui.categories.get(currentCategory);
        if (categoryComponent != null) {
            for (ModuleComponent module : categoryComponent.getModules()) {
                module.mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    public static void keyTyped(char typedChar, int keyCode) {
        CategoryComponent categoryComponent = ClickGui.categories.get(currentCategory);
        if (categoryComponent != null) {
            for (ModuleComponent module : categoryComponent.getModules()) {
                module.keyTyped(typedChar, keyCode);
            }
        }
    }
    
    public static void handleMouseInput() {
        // Handled in drawScreen for now as DWheel access is static
    }
}

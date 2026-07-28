package keystrokesmod.clickgui.components.impl;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.profile.ProfileModule;
import keystrokesmod.utility.render.RenderUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderComponent extends Component {
    private final SliderSetting sliderSetting;
    private int o;
    private int x;
    private int y;
    private boolean dragging = false;
    private double width;
    private double targetValue;
    private double displayedValue;
    private static final double SLIDER_SPEED = 0.6;

    public SliderComponent(SliderSetting sliderSetting, ModuleComponent moduleComponent, int o) {
        super(moduleComponent);
        this.sliderSetting = sliderSetting;
        this.x = moduleComponent.categoryComponent.getX() + moduleComponent.categoryComponent.gw();
        this.y = moduleComponent.categoryComponent.getY() + moduleComponent.o;
        this.o = o;

        this.targetValue = sliderSetting.getInput();
        this.displayedValue = sliderSetting.getInput();
        this.width = computeWidth(sliderSetting.getInput());
    }

    @Override
    public Setting getSetting() {
        return sliderSetting;
    }

    @Override
    public void render() {
        RenderUtils.drawRoundedRectangle(
                this.parent.categoryComponent.getX() + 4,
                this.parent.categoryComponent.getY() + this.o + 11,
                this.parent.categoryComponent.getX() + 4 + this.parent.categoryComponent.gw() - 8,
                this.parent.categoryComponent.getY() + this.o + 15,
                4,
                -12302777);

        int left = this.parent.categoryComponent.getX() + 4;
        int right = (int) (left + this.width);
        if (right - left > 84) {
            right = left + 84;
        }

        RenderUtils.drawRoundedRectangle(
                left,
                this.parent.categoryComponent.getY() + this.o + 11,
                right,
                this.parent.categoryComponent.getY() + this.o + 15,
                4,
                Color.getHSBColor((float) (System.currentTimeMillis() % 11000L) / 11000.0F, 0.75F, 0.9F).getRGB());

        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);

        double input = this.sliderSetting.getInput();
        String info = this.sliderSetting.getPrettyInfo();
        String valueText;
        if (this.sliderSetting.isString) {
            int idx = (int) Math.round(input);
            idx = Math.max(0, Math.min(idx, this.sliderSetting.getOptions().length - 1));
            valueText = this.sliderSetting.getOptions()[idx];
        } else {
            valueText = Utils.isWholeNumber(input) ? String.valueOf((int) input) : String.valueOf(input);
        }

        getFont().drawString(
                this.sliderSetting.getName() + ": " + valueText + " " + info,
                (float) ((this.parent.categoryComponent.getX() + 4) * 2),
                (float) ((this.parent.categoryComponent.getY() + this.o + 3) * 2),
                color,
                true
        );
        GL11.glPopMatrix();
    }

    @Override
    public void so(int n) {
        this.o = n;
    }

    @Override
    public void onDrawScreen(int mouseX, int mouseY) {
        this.y = this.parent.categoryComponent.getY() + this.o;
        this.x = this.parent.categoryComponent.getX();

        double rangeWidth = this.parent.categoryComponent.gw() - 8;

        if (this.dragging) {
            double clamped = Math.min(rangeWidth, Math.max(0, mouseX - this.x));
            double raw = clamped / rangeWidth * (this.sliderSetting.getMax() - this.sliderSetting.getMin()) + this.sliderSetting.getMin();
            double stepped = Math.round((raw - this.sliderSetting.getMin()) / this.sliderSetting.getIntervals()) * this.sliderSetting.getIntervals() + this.sliderSetting.getMin();
            double newValue = roundToInterval(
                    Math.max(this.sliderSetting.getMin(), Math.min(this.sliderSetting.getMax(), stepped)),
                    4
            );
            targetValue = newValue;
            applyValue(newValue);
        } else {
            // Smooth towards external updates as well
            targetValue = this.sliderSetting.getInput();
        }

        displayedValue = displayedValue + (targetValue - displayedValue) * SLIDER_SPEED;
        width = computeWidth(displayedValue);
    }

    private double computeWidth(double value) {
        double range = (sliderSetting.getMax() - sliderSetting.getMin());
        if (range <= 0) return 0;
        double fraction = (value - sliderSetting.getMin()) / range;
        fraction = Math.max(0, Math.min(1, fraction));
        return (this.parent.categoryComponent.gw() - 8) * fraction;
    }

    private void applyValue(double newValue) {
        double current = this.sliderSetting.getInput();
        if (newValue != current) {
            this.sliderSetting.setValue(newValue);
            if (ModuleManager.hud != null && ModuleManager.hud.isEnabled() && !ModuleManager.organizedModules.isEmpty()) {
                ModuleManager.sort();
            }
            if (Raven.currentProfile != null) {
                ((ProfileModule) Raven.currentProfile.getModule()).saved = false;
            }
            parent.categoryComponent.render();
        }
    }

    private static double roundToInterval(double value, int places) {
        if (places < 0) {
            return 0.0D;
        }
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public void onClick(int mouseX, int mouseY, int button) {
        if (this.getSetting() != null && !this.getSetting().isVisible()) return;

        if ((u(mouseX, mouseY) || i(mouseX, mouseY)) && button == 0 && this.parent.po) {
            this.dragging = true;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        this.dragging = false;
    }

    public boolean u(int x, int y) {
        return x > this.x && x < this.x + this.parent.categoryComponent.gw() / 2 + 1 && y > this.y && y < this.y + 16;
    }

    public boolean i(int x, int y) {
        return x > this.x + this.parent.categoryComponent.gw() / 2 && x < this.x + this.parent.categoryComponent.gw() && y > this.y && y < this.y + 16;
    }

    @Override
    public void onGuiClosed() {
        this.dragging = false;
    }
}

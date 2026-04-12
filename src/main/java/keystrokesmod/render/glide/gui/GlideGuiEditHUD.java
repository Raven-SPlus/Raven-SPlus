package keystrokesmod.render.glide.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.nanovg.NanoVG;

import keystrokesmod.Raven;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.render.glide.GlideContext;
import keystrokesmod.render.glide.adapter.RavenHUDModAdapter;
import keystrokesmod.render.glide.adapter.RavenHUDModAdapter.HudElement;
import keystrokesmod.render.glide.animation.Animation;
import keystrokesmod.render.glide.animation.Direction;
import keystrokesmod.render.glide.animation.EaseBackIn;
import keystrokesmod.render.glide.animation.SimpleAnimation;
import keystrokesmod.render.glide.blur.BlurUtils;
import keystrokesmod.render.glide.color.ColorPalette;
import keystrokesmod.render.glide.color.ColorType;
import keystrokesmod.render.glide.nanovg.NvgManager;
import keystrokesmod.render.glide.nanovg.font.NvgFonts;
import keystrokesmod.render.glide.util.MathUtils;
import keystrokesmod.render.glide.util.MouseUtils;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.impl.render.TargetHUD;
import keystrokesmod.module.impl.render.Watermark;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

public class GlideGuiEditHUD extends GuiScreen {

    private Animation introAnimation;
    private final boolean fromModMenu;
    private boolean snapping;
    private boolean canSnap;
    private List<HudElement> elements;
    private SimpleAnimation[] hoverAnimations;
    private int localMouseX = -1;
    private int localMouseY = -1;

    public GlideGuiEditHUD(boolean fromModMenu) {
        this.fromModMenu = fromModMenu;

        GlideContext ctx = GlideContext.getInstance();
        RavenHUDModAdapter adapter = ctx.getHudModAdapter();
        adapter.refresh();

        this.elements = new ArrayList<HudElement>(adapter.getHudElements());
        Collections.reverse(this.elements);

        this.hoverAnimations = new SimpleAnimation[elements.size()];
        for (int i = 0; i < hoverAnimations.length; i++) {
            hoverAnimations[i] = new SimpleAnimation();
        }
    }

    @Override
    public void initGui() {
        refreshHudElements();
        for (HudElement el : elements) {
            el.setDragging(false);
        }

        introAnimation = new EaseBackIn(500, 1.0F, 0F);
        introAnimation.setDirection(Direction.FORWARDS);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        GlideContext ctx = GlideContext.getInstance();
        NvgManager nvg = ctx.getNvgManager();
        ColorPalette palette = ctx.getColorManager().getPalette();
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

        localMouseX = mouseX;
        localMouseY = mouseY;
        snapping = false;

        if (introAnimation.isDone(Direction.BACKWARDS)) {
            mc.displayGuiScreen(null);
            return;
        }

        BlurUtils.drawBlurScreen((float) (Math.min(introAnimation.getValue(), 1) * 20) + 1F);
        renderHudPreviews();
        refreshHudElements();

        nvg.setupAndDraw(new Runnable() {
            @Override
            public void run() {
                float introVal = (float) introAnimation.getValue();

                nvg.save();
                NanoVG.nvgGlobalAlpha(nvg.getContext(), introVal);
                nvg.restore();

                nvg.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(),
                        new Color(0, 0, 0, (int) (introVal * 100)));

                int halfW = sr.getScaledWidth() / 2;
                int halfH = sr.getScaledHeight() / 2;

                nvg.drawRect(0, halfH, sr.getScaledWidth(), 0.5F,
                        palette.getBackgroundColor(ColorType.DARK));
                nvg.drawRect(halfW, 0, 0.5F, sr.getScaledHeight(),
                        palette.getBackgroundColor(ColorType.DARK));

                nvg.drawCenteredText(
                        "Scroll to resize elements. Hold Shift for finer control.",
                        sr.getScaledWidth() / 2F, sr.getScaledHeight() - 15,
                        new Color(255, 255, 255, 200), 8F, NvgFonts.REGULAR);

                for (int i = 0; i < elements.size(); i++) {
                    HudElement el = elements.get(i);
                    if (!el.isEnabled()) {
                        continue;
                    }

                    float ex = el.getX();
                    float ey = el.getY();
                    float ew = el.getWidth();
                    float eh = el.getHeight();

                    boolean isInside = MouseUtils.isInside(mouseX, mouseY, ex, ey, ew, eh)
                            && isTopElement(mouseX, mouseY, el);

                    if (isInside) {
                        int dWheel = Mouse.getDWheel();
                        if (dWheel != 0) {
                            float scaleChange = shift ? 0.02F : 0.1F;
                            float newScale = el.getScale();
                            if (dWheel > 0) {
                                newScale += scaleChange;
                            }
                            if (dWheel < 0) {
                                newScale -= scaleChange;
                            }
                            el.setScale(Math.round(newScale * 100.0F) / 100.0F);
                        }
                    }

                    if (shift) {
                        canSnap = false;
                    }

                    hoverAnimations[i].setAnimation(isInside ? 1.0F : 0.0F, 14);

                    if (el.isDragging()) {
                        el.setX(mouseX + el.getDragOffsetX());
                        el.setY(mouseY + el.getDragOffsetY());
                        applyElementPosition(el);
                    }

                    float modX = el.getX();
                    float modY = el.getY();
                    float modW = el.getWidth();
                    float modH = el.getHeight();

                    el.setX(Math.max(0, Math.min(modX, sr.getScaledWidth() - modW)));
                    el.setY(Math.max(0, Math.min(modY, sr.getScaledHeight() - modH)));

                    int snapRange = 5;

                    if (canSnap) {
                        if (MathUtils.isInRange(el.getX() + (modW / 2f), halfW - snapRange, halfW + snapRange)) {
                            el.setX(halfW - (modW / 2));
                        }
                        if (MathUtils.isInRange(el.getY() + (modH / 2f), halfH - snapRange, halfH + snapRange)) {
                            el.setY(halfH - (modH / 2));
                        }
                    }

                    for (HudElement other : elements) {
                        if (!other.isEnabled() || !el.isDragging() || other == el || snapping || !canSnap) {
                            continue;
                        }

                        float ox = other.getX();
                        float oy = other.getY();
                        float ow = other.getWidth();
                        float oh = other.getHeight();
                        Color snapColor = new Color(217, 60, 255);

                        if (MathUtils.isInRange(ox, modX - snapRange, modX + snapRange)) {
                            nvg.drawRect(ox, 0, 0.5F, sr.getScaledHeight(), snapColor);
                            snapping = true;
                            el.setX(ox);
                        }
                        if (MathUtils.isInRange(oy, modY - snapRange, modY + snapRange)) {
                            nvg.drawRect(0, oy, sr.getScaledWidth(), 0.5F, snapColor);
                            snapping = true;
                            el.setY(oy);
                        }
                        if (MathUtils.isInRange(ox + ow, modX - snapRange, modX + snapRange)) {
                            nvg.drawRect(ox + ow, 0, 0.5F, sr.getScaledHeight(), snapColor);
                            snapping = true;
                            el.setX(ox + ow);
                        }
                        if (MathUtils.isInRange(oy + oh, modY - snapRange, modY + snapRange)) {
                            nvg.drawRect(0, oy + oh, sr.getScaledWidth(), 0.5F, snapColor);
                            snapping = true;
                            el.setY(oy + oh);
                        }
                        if (MathUtils.isInRange(ox, modX + modW - snapRange, modX + modW + snapRange)) {
                            nvg.drawRect(ox, 0, 0.5F, sr.getScaledHeight(), snapColor);
                            snapping = true;
                            el.setX(ox - modW);
                        }
                        if (MathUtils.isInRange(oy, modY + modH - snapRange, modY + modH + snapRange)) {
                            nvg.drawRect(0, oy, sr.getScaledWidth(), 0.5F, snapColor);
                            snapping = true;
                            el.setY(oy - modH);
                        }
                        if (MathUtils.isInRange(ox + ow, modX + modW - snapRange, modX + modW + snapRange)) {
                            nvg.drawRect(ox + ow, 0, 0.5F, sr.getScaledHeight(), snapColor);
                            snapping = true;
                            el.setX(ox + ow - modW);
                        }
                        if (MathUtils.isInRange(oy + oh, modY + modH - snapRange, modY + modH + snapRange)) {
                            nvg.drawRect(0, oy + oh, sr.getScaledWidth(), 0.5F, snapColor);
                            snapping = true;
                            el.setY(oy + oh - modH);
                        }
                    }

                    nvg.drawOutlineRoundedRect(
                            el.getX() - 2, el.getY() - 2,
                            el.getWidth() + 4, el.getHeight() + 4,
                            6.5F * el.getScale(), 2,
                            palette.getBackgroundColor(ColorType.DARK,
                                    (int) (hoverAnimations[i].getValue() * 255)));
                }
            }
        });
    }

    private boolean isTopElement(int mouseX, int mouseY, HudElement target) {
        for (HudElement el : elements) {
            if (!el.isEnabled()) {
                continue;
            }
            if (MouseUtils.isInside(mouseX, mouseY, el.getX(), el.getY(), el.getWidth(), el.getHeight())) {
                return el == target;
            }
        }
        return false;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for (HudElement el : elements) {
            if (!el.isEnabled()) {
                continue;
            }

            boolean isInside = MouseUtils.isInside(mouseX, mouseY, el.getX(), el.getY(), el.getWidth(), el.getHeight())
                    && isTopElement(mouseX, mouseY, el);

            if (mouseButton == 0) {
                canSnap = true;
            }

            if (mouseButton == 1 && isInside) {
                el.getRaw().toggle();
                refreshHudElements();
                initGui();
                return;
            }

            if (mouseButton == 2 && isInside) {
                el.setScale(1.0F);
            }

            if (isInside) {
                el.setDragging(true);
                el.setDragOffsetX(el.getX() - mouseX);
                el.setDragOffsetY(el.getY() - mouseY);
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        for (HudElement el : elements) {
            el.setDragging(false);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (fromModMenu) {
                mc.displayGuiScreen(new GlideGuiModMenu());
            } else {
                introAnimation.setDirection(Direction.BACKWARDS);
            }
            return;
        }

        for (HudElement el : elements) {
            if (!el.isEnabled()) {
                continue;
            }

            boolean isInside = MouseUtils.isInside(localMouseX, localMouseY,
                    el.getX(), el.getY(), el.getWidth(), el.getHeight())
                    && isTopElement(localMouseX, localMouseY, el);

            if ((keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) && isInside) {
                el.getRaw().toggle();
                refreshHudElements();
                initGui();
                return;
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void renderHudPreviews() {
        if (ModuleManager.hud != null && ModuleManager.hud.isEnabled()) {
            HUD.renderEditorPreview();
        }
        if (ModuleManager.targetHUD != null && ModuleManager.targetHUD.isEnabled()) {
            TargetHUD.renderExample();
        }
        if (ModuleManager.watermark != null && ModuleManager.watermark.isEnabled()) {
            ModuleManager.watermark.render();
        }
    }

    private void refreshHudElements() {
        RavenHUDModAdapter adapter = GlideContext.getInstance().getHudModAdapter();
        List<HudElement> previous = elements == null ? Collections.<HudElement>emptyList() : elements;
        adapter.refresh();

        List<HudElement> liveElements = new ArrayList<HudElement>(adapter.getHudElements());
        Collections.reverse(liveElements);

        List<HudElement> merged = new ArrayList<HudElement>(liveElements.size());
        for (HudElement live : liveElements) {
            HudElement existing = findElement(previous, live.getRawName());
            if (existing != null) {
                live.setDragging(existing.isDragging());
                live.setDragOffsetX(existing.getDragOffsetX());
                live.setDragOffsetY(existing.getDragOffsetY());
                live.setScale(existing.getScale());
            }
            merged.add(live);
        }

        this.elements = merged;
        this.hoverAnimations = resizeAnimations(this.hoverAnimations, this.elements.size());
        for (HudElement el : this.elements) {
            if (!el.isDragging()) {
                el.setDragging(false);
            }
        }
    }

    private HudElement findElement(List<HudElement> source, String rawName) {
        for (HudElement element : source) {
            if (element.getRawName().equalsIgnoreCase(rawName)) {
                return element;
            }
        }
        return null;
    }

    private SimpleAnimation[] resizeAnimations(SimpleAnimation[] existing, int size) {
        SimpleAnimation[] next = new SimpleAnimation[size];
        for (int i = 0; i < size; i++) {
            if (existing != null && i < existing.length && existing[i] != null) {
                next[i] = existing[i];
            } else {
                next[i] = new SimpleAnimation();
            }
        }
        return next;
    }

    private void applyElementPosition(HudElement element) {
        int anchorX = Math.round(element.getX() + element.getAnchorOffsetX());
        int anchorY = Math.round(element.getY() + element.getAnchorOffsetY());

        if (element.getRaw() == ModuleManager.hud) {
            HUD.hudX = anchorX;
            HUD.hudY = anchorY;
        } else if (element.getRaw() == ModuleManager.targetHUD) {
            TargetHUD.posX = anchorX;
            TargetHUD.posY = anchorY;
        } else if (element.getRaw() == ModuleManager.watermark) {
            Watermark.posX = anchorX;
            Watermark.posY = anchorY;
        }
    }
}

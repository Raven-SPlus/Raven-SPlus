package keystrokesmod.render.glide.adapter;

import keystrokesmod.Raven;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.impl.render.TargetHUD;
import keystrokesmod.module.impl.render.Watermark;

import java.util.ArrayList;
import java.util.List;

public final class RavenHUDModAdapter {

    public static final class HudElement {
        private final Module module;
        private float x;
        private float y;
        private float width;
        private float height;
        private float scale;
        private boolean dragging;
        private float dragOffsetX;
        private float dragOffsetY;
        private float anchorOffsetX;
        private float anchorOffsetY;

        HudElement(Module module) {
            this.module = module;
            this.x = 0;
            this.y = 0;
            this.width = 100;
            this.height = 20;
            this.scale = 1.0f;
            this.dragging = false;
        }

        public String getName() {
            return module.getPrettyName();
        }

        public String getRawName() {
            return module.getName();
        }

        public boolean isEnabled() {
            return module.isEnabled();
        }

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public float getWidth() {
            return width;
        }

        public void setWidth(float width) {
            this.width = width;
        }

        public float getHeight() {
            return height;
        }

        public void setHeight(float height) {
            this.height = height;
        }

        public float getScale() {
            return scale;
        }

        public void setScale(float scale) {
            this.scale = scale;
        }

        public boolean isDragging() {
            return dragging;
        }

        public void setDragging(boolean dragging) {
            this.dragging = dragging;
        }

        public float getDragOffsetX() {
            return dragOffsetX;
        }

        public void setDragOffsetX(float dragOffsetX) {
            this.dragOffsetX = dragOffsetX;
        }

        public float getDragOffsetY() {
            return dragOffsetY;
        }

        public void setDragOffsetY(float dragOffsetY) {
            this.dragOffsetY = dragOffsetY;
        }

        public float getAnchorOffsetX() {
            return anchorOffsetX;
        }

        public void setAnchorOffsetX(float anchorOffsetX) {
            this.anchorOffsetX = anchorOffsetX;
        }

        public float getAnchorOffsetY() {
            return anchorOffsetY;
        }

        public void setAnchorOffsetY(float anchorOffsetY) {
            this.anchorOffsetY = anchorOffsetY;
        }

        public Module getRaw() {
            return module;
        }
    }

    private final List<HudElement> elements = new ArrayList<HudElement>();

    public void refresh() {
        elements.clear();
        ModuleManager mgr = Raven.getModuleManager();
        if (mgr == null) {
            return;
        }
        if (ModuleManager.hud != null && ModuleManager.hud.isEnabled()) {
            HudElement arrayList = new HudElement(ModuleManager.hud);
            arrayList.setX(HUD.current$minX);
            arrayList.setY(HUD.current$minY);
            arrayList.setWidth(Math.max(1, HUD.current$maxX - HUD.current$minX));
            arrayList.setHeight(Math.max(1, HUD.current$maxY - HUD.current$minY));
            arrayList.setAnchorOffsetX(HUD.hudX - HUD.current$minX);
            arrayList.setAnchorOffsetY(HUD.hudY - HUD.current$minY);
            elements.add(arrayList);
        }

        if (ModuleManager.targetHUD != null && ModuleManager.targetHUD.isEnabled()) {
            HudElement targetHud = new HudElement(ModuleManager.targetHUD);
            targetHud.setX(TargetHUD.current$minX);
            targetHud.setY(TargetHUD.current$minY);
            targetHud.setWidth(Math.max(1, TargetHUD.current$maxX - TargetHUD.current$minX));
            targetHud.setHeight(Math.max(1, TargetHUD.current$maxY - TargetHUD.current$minY));
            targetHud.setAnchorOffsetX(TargetHUD.posX - TargetHUD.current$minX);
            targetHud.setAnchorOffsetY(TargetHUD.posY - TargetHUD.current$minY);
            elements.add(targetHud);
        }

        if (ModuleManager.watermark != null && ModuleManager.watermark.isEnabled()) {
            HudElement watermark = new HudElement(ModuleManager.watermark);
            watermark.setX(Watermark.current$minX);
            watermark.setY(Watermark.current$minY);
            watermark.setWidth(Math.max(1, (float) (Watermark.current$maxX - Watermark.current$minX)));
            watermark.setHeight(Math.max(1, Watermark.current$maxY - Watermark.current$minY));
            watermark.setAnchorOffsetX(Watermark.posX - Watermark.current$minX);
            watermark.setAnchorOffsetY(Watermark.posY - Watermark.current$minY);
            elements.add(watermark);
        }
    }

    public List<HudElement> getHudElements() {
        return elements;
    }

    public HudElement getElementByName(String name) {
        for (HudElement e : elements) {
            if (e.getRawName().equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }
}

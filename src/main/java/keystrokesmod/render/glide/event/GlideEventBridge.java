package keystrokesmod.render.glide.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GlideEventBridge {

    public interface Render2DListener {
        void onRender2D(float partialTicks);
    }

    public interface NotificationListener {
        void onRenderNotification();
    }

    private final List<Render2DListener> render2DListeners = new CopyOnWriteArrayList<Render2DListener>();
    private final List<NotificationListener> notificationListeners = new CopyOnWriteArrayList<NotificationListener>();

    public void registerRender2D(Render2DListener listener) {
        if (listener != null && !render2DListeners.contains(listener)) {
            render2DListeners.add(listener);
        }
    }

    public void unregisterRender2D(Render2DListener listener) {
        render2DListeners.remove(listener);
    }

    public void registerNotification(NotificationListener listener) {
        if (listener != null && !notificationListeners.contains(listener)) {
            notificationListeners.add(listener);
        }
    }

    public void unregisterNotification(NotificationListener listener) {
        notificationListeners.remove(listener);
    }

    public void fireRender2D(float partialTicks) {
        for (Render2DListener listener : render2DListeners) {
            try {
                listener.onRender2D(partialTicks);
            } catch (Throwable ignored) {
            }
        }
    }

    public void fireRenderNotification() {
        for (NotificationListener listener : notificationListeners) {
            try {
                listener.onRenderNotification();
            } catch (Throwable ignored) {
            }
        }
    }
}

package keystrokesmod.module.impl.client.memoryfix;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

public class MemoryFixCapeImageBuffer implements IImageBuffer {
    private static volatile Method parseCapeMethod;
    private static volatile Method setLocationOfCapeMethod;

    private final WeakReference<AbstractClientPlayer> playerRef;
    private final AbstractClientPlayer strongPlayerRef;
    private final ResourceLocation resourceLocation;

    public MemoryFixCapeImageBuffer(AbstractClientPlayer player, ResourceLocation resourceLocation) {
        this.playerRef = new WeakReference<>(player);
        this.strongPlayerRef = MemoryFixHelper.shouldFixCapeLeak() ? null : player;
        this.resourceLocation = resourceLocation;
    }

    @Override
    public BufferedImage parseUserSkin(BufferedImage image) {
        try {
            Method method = parseCapeMethod;
            if (method == null) {
                method = findMethod(Class.forName("CapeUtils"), "parseCape", BufferedImage.class);
                parseCapeMethod = method;
            }
            Object parsedImage = method.invoke(null, image);
            return parsedImage instanceof BufferedImage ? (BufferedImage) parsedImage : image;
        } catch (ReflectiveOperationException ignored) {
            return image;
        }
    }

    @Override
    public void skinAvailable() {
        AbstractClientPlayer player = strongPlayerRef != null ? strongPlayerRef : playerRef.get();
        if (player == null) {
            return;
        }

        try {
            Method method = setLocationOfCapeMethod;
            if (method == null) {
                method = findMethod(player.getClass(), "setLocationOfCape", ResourceLocation.class);
                setLocationOfCapeMethod = method;
            }
            method.invoke(player, resourceLocation);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Class<?> current = owner;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }

        throw new NoSuchMethodException(name);
    }
}

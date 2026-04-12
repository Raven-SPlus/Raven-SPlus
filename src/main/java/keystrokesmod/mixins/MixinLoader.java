package keystrokesmod.mixins;

import keystrokesmod.module.impl.client.memoryfix.MemoryFixCapeTransformer;
import keystrokesmod.render.glide.nanovg.LwjglNanoVGTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

@IFMLLoadingPlugin.MCVersion("1.8.9")
public class MixinLoader implements IFMLLoadingPlugin {
    public MixinLoader() {
        unlockLwjgl();
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.raven.json");
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
    }

    @NotNull
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                MemoryFixCapeTransformer.class.getName(),
                LwjglNanoVGTransformer.class.getName()
        };
    }

    @Nullable
    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Nullable
    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @SuppressWarnings("unchecked")
    private void unlockLwjgl() {
        try {
            Field field = LaunchClassLoader.class.getDeclaredField("classLoaderExceptions");
            field.setAccessible(true);
            ((Set<String>) field.get(Launch.classLoader)).remove("org.lwjgl.");
        } catch (Throwable ignored) {
        }
    }
}

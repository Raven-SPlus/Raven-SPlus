package keystrokesmod.render.glide.nanovg;

import org.lwjgl.opengl.GLContext;
import org.lwjgl.system.FunctionProvider;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class Lwjgl2FunctionProvider implements FunctionProvider {
    private final Method getFunctionAddress;

    public Lwjgl2FunctionProvider() {
        try {
            getFunctionAddress = GLContext.class.getDeclaredMethod("getFunctionAddress", String.class);
            getFunctionAddress.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getFunctionAddress(CharSequence functionName) {
        try {
            return ((Long) getFunctionAddress.invoke(null, functionName.toString())).longValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getFunctionAddress(ByteBuffer buffer) {
        throw new UnsupportedOperationException();
    }
}

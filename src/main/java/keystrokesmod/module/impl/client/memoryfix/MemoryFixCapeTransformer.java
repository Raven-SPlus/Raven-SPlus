package keystrokesmod.module.impl.client.memoryfix;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

public class MemoryFixCapeTransformer implements IClassTransformer {
    private static final String OPTIFINE_CAPE_BUFFER = "CapeUtils$1";
    private static final String REPLACEMENT_BUFFER = "keystrokesmod/module/impl/client/memoryfix/MemoryFixCapeImageBuffer";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !"CapeUtils".equals(name)) {
            return basicClass;
        }

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        RemappingClassAdapter adapter = new RemappingClassAdapter(classWriter, new Remapper() {
            @Override
            public String map(String typeName) {
                if (OPTIFINE_CAPE_BUFFER.equals(typeName)) {
                    return REPLACEMENT_BUFFER;
                }

                return typeName;
            }
        });

        ClassReader classReader = new ClassReader(basicClass);
        classReader.accept(adapter, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }
}

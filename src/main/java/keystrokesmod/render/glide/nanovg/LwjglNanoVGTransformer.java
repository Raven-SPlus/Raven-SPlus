package keystrokesmod.render.glide.nanovg;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class LwjglNanoVGTransformer implements IClassTransformer {
    private static final String TARGET_CLASS = "org.lwjgl.nanovg.NanoVGGLConfig";
    private static final String PROVIDER_OWNER = "keystrokesmod/render/glide/nanovg/Lwjgl2FunctionProvider";
    private static final String CONFIG_OWNER = "org/lwjgl/nanovg/NanoVGGLConfig";
    private static final String PROVIDER_DESC = "(JLorg/lwjgl/system/FunctionProvider;)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !TARGET_CLASS.equals(name)) {
            return basicClass;
        }

        ClassReader reader = new ClassReader(basicClass);
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.EXPAND_FRAMES);

        for (MethodNode method : node.methods) {
            if (!"configGL".equals(method.name)) {
                continue;
            }

            InsnList list = new InsnList();
            list.add(new VarInsnNode(Opcodes.LLOAD, 0));
            list.add(new TypeInsnNode(Opcodes.NEW, PROVIDER_OWNER));
            list.add(new InsnNode(Opcodes.DUP));
            list.add(new MethodInsnNode(
                    Opcodes.INVOKESPECIAL,
                    PROVIDER_OWNER,
                    "<init>",
                    "()V",
                    false
            ));
            list.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    CONFIG_OWNER,
                    "config",
                    PROVIDER_DESC,
                    false
            ));
            list.add(new InsnNode(Opcodes.RETURN));

            method.instructions.clear();
            method.instructions.insert(list);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);
        return writer.toByteArray();
    }
}

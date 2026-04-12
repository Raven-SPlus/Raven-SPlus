package keystrokesmod.render.glide.nanovg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

/**
 * Extracts and loads LWJGL3 natives from the embedded lwjgl-soar-natives.jar.
 *
 * With org.lwjgl.* unlocked in LaunchClassLoader, the LWJGL3 classes and these
 * natives now belong to the same classloader, so a simple extract + load path
 * is sufficient.
 */
public final class NvgNativeBootstrap {
    private static final String[][] NATIVE_LIBRARIES = new String[][] {
            { "", "lwjgl" },
            { "stb", "lwjgl_stb" },
            { "nanovg", "lwjgl_nanovg" }
    };

    private static volatile boolean attempted;
    private static volatile boolean loaded;
    private static volatile Throwable failure;

    private NvgNativeBootstrap() {
    }

    public static synchronized boolean ensureLoaded() {
        if (loaded) {
            return true;
        }
        if (attempted) {
            return false;
        }
        attempted = true;

        try {
            String platformDir = getPlatformDir();
            File extractRoot = new File(System.getProperty("java.io.tmpdir"), "raven-glide-natives");
            if (!extractRoot.exists() && !extractRoot.mkdirs()) {
                throw new IOException("failed to create native dir: " + extractRoot);
            }

            ClassLoader loader = NvgNativeBootstrap.class.getClassLoader();
            for (String[] entry : NATIVE_LIBRARIES) {
                String subdir = entry[0];
                String fileName = System.mapLibraryName(entry[1]);
                String resourcePath = resolveResourcePath(platformDir, subdir, fileName);
                File extracted = new File(extractRoot, fileName);
                if (!extracted.exists() || extracted.length() == 0) {
                    extractResource(loader, resourcePath, extracted);
                }
                System.load(extracted.getAbsolutePath());
            }

            String jlp = System.getProperty("java.library.path", "");
            System.setProperty("java.library.path", prependPath(extractRoot.getAbsolutePath(), jlp));
            resetJavaLibraryPath();

            loaded = true;
            return true;
        } catch (Throwable t) {
            failure = t;
            System.err.println("[Glide] NanoVG native bootstrap failed: " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }

    public static Throwable getFailure() {
        return failure;
    }

    private static void resetJavaLibraryPath() {
        try {
            Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            sysPathsField.set(null, null);
        } catch (Throwable t) {
            System.err.println("[Glide] Failed to reset java.library.path cache: " + t.getMessage());
        }
    }

    private static String prependPath(String dir, String existing) {
        if (existing == null || existing.isEmpty()) {
            return dir;
        }
        return dir + File.pathSeparator + existing;
    }

    private static String resolveResourcePath(String platformDir, String subdir, String fileName) {
        if (subdir.isEmpty()) {
            return platformDir + "/org/lwjgl/" + fileName;
        }
        return platformDir + "/org/lwjgl/" + subdir + "/" + fileName;
    }

    private static void extractResource(ClassLoader loader, String resourcePath, File destination) throws IOException {
        try (InputStream in = loader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("missing native resource stream: " + resourcePath);
            }
            try (FileOutputStream out = new FileOutputStream(destination)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        }
    }

    private static String getPlatformDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();
        String archDir;

        if (arch.contains("aarch64") || arch.contains("arm64")) {
            archDir = "arm64";
        } else if (arch.contains("64")) {
            archDir = "x64";
        } else {
            archDir = "x86";
        }

        if (os.contains("win")) {
            return "windows/" + archDir;
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return "macos/" + archDir;
        }
        return "linux/" + archDir;
    }
}

package keystrokesmod.utility;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Client build tag helper.
 *
 * This project uses tags like: b1, b2, b10 ...
 *
 * The local build tag is stored in the classpath resource: {@code raven.version}
 * The latest build tag comes from GitHub release {@code tag_name} (used as-is).
 */
public final class Version {
    private static final String VERSION_RESOURCE = "raven.version";
    private static volatile String cached;

    private Version() {
    }

    /**
     * @return current build tag (e.g. "b12"), or "DEV" if unknown.
     */
    public static @NotNull String current() {
        String v = cached;
        if (v != null) {
            return v;
        }

        // Optional override for dev/testing.
        String prop = System.getProperty("raven.version");
        if (prop != null) {
            prop = prop.trim();
            if (!prop.isEmpty()) {
                cached = prop;
                return prop;
            }
        }

        String fromResource = readVersionResource();
        if (fromResource != null) {
            cached = fromResource;
            return fromResource;
        }

        cached = "DEV";
        return cached;
    }

    /**
     * Compare two build tags like b1/b2/b10.
     *
     * @return -1 if a < b, 0 if equal, 1 if a > b
     */
    public static int compare(@NotNull String a, @NotNull String b) {
        Integer ai = parseBuildNumber(a);
        Integer bi = parseBuildNumber(b);

        if (ai != null && bi != null) {
            return Integer.compare(ai, bi);
        }

        // Fallback: stable equality check.
        return a.equals(b) ? 0 : a.compareToIgnoreCase(b);
    }

    private static String readVersionResource() {
        InputStream in = Version.class.getClassLoader().getResourceAsStream(VERSION_RESOURCE);
        if (in == null) {
            return null;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    return line;
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Parses "b123" -> 123. Returns null if not matching.
     */
    private static Integer parseBuildNumber(@NotNull String raw) {
        String s = raw.trim();
        if (s.length() < 2) {
            return null;
        }
        char c0 = s.charAt(0);
        if (c0 != 'b' && c0 != 'B') {
            return null;
        }

        int n = 0;
        for (int i = 1; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (!Character.isDigit(ch)) {
                return null;
            }
            n = (n * 10) + (ch - '0');
        }
        return n;
    }
}

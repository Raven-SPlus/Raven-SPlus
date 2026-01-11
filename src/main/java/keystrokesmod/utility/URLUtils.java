package keystrokesmod.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class URLUtils {
    public static String k = "";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    public static boolean isHypixelKeyValid(String ak) {
        String c = getTextFromURL("https://api.hypixel.net/key?key=" + ak);
        return !c.isEmpty() && !c.contains("Invalid");
    }

    public static String getTextFromURL(String _url) {
        HttpURLConnection con = null;

        try {
            URL url = new URL(_url);
            con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(CONNECT_TIMEOUT_MS);
            con.setReadTimeout(READ_TIMEOUT_MS);
            con.setUseCaches(false);
            con.setRequestProperty("User-Agent", "RavenS_PLUS");
            return getTextFromConnection(con);
        } catch (IOException ignored) {
            return "";
        } finally {
            if (con != null) {
                con.disconnect();
            }

        }
    }

    public static String getTextFromConnection(HttpURLConnection connection) {
        if (connection == null) return "";
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder stringBuilder = new StringBuilder();
            String input;
            while ((input = bufferedReader.readLine()) != null) {
                stringBuilder.append(input);
            }
            return stringBuilder.toString();
        } catch (Exception ignored) {
            return "";
        }
    }
}

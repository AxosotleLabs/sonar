package net.axolabs.sonar.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public final class SonarConfig {

    private static final Pattern HEX = Pattern.compile("#?[0-9a-fA-F]{6}");
    private static final String INVERT = "invert";

    public static final float[] WHITE = {1f, 1f, 1f};
    public static final float[] YELLOW = {1f, 0.85f, 0.2f};
    public static final float[] RED = {1f, 0.25f, 0.2f};

    private final WatchedFile file;

    private volatile boolean enabled = true;
    private volatile boolean requireEchoShard = true;
    private volatile boolean invertNeutral = true;
    private volatile float[] neutral = WHITE;
    private volatile float[] medium = YELLOW;
    private volatile float[] high = RED;

    SonarConfig(Path configDir) {
        this.file = new WatchedFile(configDir.resolve("sonar.json"), "/sonar/default_config.json");
    }

    void refresh() {
        if (!file.changed()) return;
        JsonObject root = read();
        enabled = bool(root, "enabled", true);
        requireEchoShard = bool(root, "requireEchoShard", true);
        String n = string(root, "neutralColor", INVERT);
        invertNeutral = n.equalsIgnoreCase(INVERT) || !HEX.matcher(n).matches();
        neutral = color(n, WHITE);
        medium = color(string(root, "mediumColor", "#FFD933"), YELLOW);
        high = color(string(root, "highColor", "#FF4033"), RED);
    }

    private JsonObject read() {
        try (Reader r = Files.newBufferedReader(file.path(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        } catch (Exception e) {
            Sonar.LOG.accept("Invalid " + file.path() + ", using defaults: " + e);
            try (InputStream in = WatchedFile.openDefault("/sonar/default_config.json");
                 Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(r).getAsJsonObject();
            } catch (Exception inner) {
                return new JsonObject();
            }
        }
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : def;
    }

    private static String string(JsonObject o, String key, String def) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString().trim() : def;
    }

    private static float[] color(String value, float[] def) {
        if (!HEX.matcher(value).matches()) return def;
        int c = Integer.parseInt(value.startsWith("#") ? value.substring(1) : value, 16);
        return new float[]{((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f};
    }

    public boolean enabled() { return enabled; }
    public boolean requireEchoShard() { return requireEchoShard; }
    boolean invertNeutral() { return invertNeutral; }
    float[] neutral() { return neutral; }
    float[] medium() { return medium; }
    float[] high() { return high; }
}

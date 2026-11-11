package net.axolabs.sonar.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class SoundRules {

    public record Rule(Danger danger, boolean ignoredWhenOwn) {}

    private record Table(List<Pattern> ownIgnored, List<Pattern> ignored, List<Pattern> high, List<Pattern> medium) {
        static final Table EMPTY = new Table(List.of(), List.of(), List.of(), List.of());
    }

    private final WatchedFile file;
    private volatile Table table = Table.EMPTY;
    private final Map<String, Rule> cache = new ConcurrentHashMap<>();

    SoundRules(Path configDir) {
        this.file = new WatchedFile(configDir.resolve("sonar_sounds.json"), "/sonar/default_sounds.json");
    }

    void refresh() {
        if (!file.changed()) return;
        try (Reader reader = Files.newBufferedReader(file.path(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            table = new Table(read(root, "own_ignored"), read(root, "ignored"), read(root, "high"), read(root, "medium"));
        } catch (Exception e) {
            Sonar.LOG.accept("Invalid " + file.path() + ": " + e);
        }
        cache.clear();
    }

    Rule lookup(String soundId) {
        return cache.computeIfAbsent(soundId, this::resolve);
    }

    private Rule resolve(String id) {
        Table t = table;
        if (matches(t.ignored, id)) return new Rule(null, true);
        Danger danger = matches(t.high, id) ? Danger.HIGH
                : matches(t.medium, id) ? Danger.MEDIUM
                : Danger.NEUTRAL;
        return new Rule(danger, matches(t.ownIgnored, id));
    }

    private static boolean matches(List<Pattern> patterns, String id) {
        for (Pattern p : patterns) {
            if (p.matcher(id).matches()) return true;
        }
        return false;
    }

    private static List<Pattern> read(JsonObject root, String key) {
        List<Pattern> out = new ArrayList<>();
        JsonElement array = root.get(key);
        if (array instanceof JsonArray a) {
            for (JsonElement e : a) out.add(compile(e.getAsString()));
        }
        return List.copyOf(out);
    }

    private static Pattern compile(String glob) {
        if (glob.indexOf(':') < 0) glob = "minecraft:" + glob;
        StringBuilder regex = new StringBuilder();
        for (String part : glob.split("\\*", -1)) {
            if (!regex.isEmpty()) regex.append(".*");
            regex.append(Pattern.quote(part));
        }
        return Pattern.compile(regex.toString());
    }
}

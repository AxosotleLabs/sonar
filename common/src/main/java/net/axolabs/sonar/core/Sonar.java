package net.axolabs.sonar.core;

import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class Sonar {

    public static final String MOD_ID = "sonar";
    public static final String TEXTURE = "textures/gui/sprites/hud/sonar.png";
    public static final int TEX_W = 13, TEX_H = 5;

    static Consumer<String> LOG = System.err::println;

    private static SonarConfig config;
    private static SoundRules rules;
    private static final SonarTracker TRACKER = new SonarTracker();
    private static final SonarHud HUD = new SonarHud(TRACKER);

    private Sonar() {}

    public static synchronized void init(Path configDir, Consumer<String> log) {
        if (config != null) return;
        LOG = log;
        config = new SonarConfig(configDir);
        rules = new SoundRules(configDir);
        config.refresh();
        rules.refresh();
    }

    public static SonarConfig config() {
        config.refresh();
        return config;
    }

    public static void onSound(String soundId, double x, double y, double z, float volume, BooleanSupplier isOwn) {
        if (rules == null) return;
        rules.refresh();
        SoundRules.Rule rule = rules.lookup(soundId);
        if (rule.danger() == null) return;
        if (rule.ignoredWhenOwn() && isOwn.getAsBoolean()) return;
        TRACKER.add(x, y, z, volume, rule.danger());
    }

    public static void render(Canvas canvas, boolean active, double camX, double camY, double camZ, float camYaw,
                              float centerX, float centerY) {
        if (config == null) return;
        HUD.render(canvas, config(), active, camX, camY, camZ, camYaw, centerX, centerY);
    }
}

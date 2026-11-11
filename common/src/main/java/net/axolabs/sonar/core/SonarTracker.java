package net.axolabs.sonar.core;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

final class SonarTracker {

    private static final long LIFETIME_MS = 1500;
    private static final float FADE_IN_MS = 120f;
    private static final float FADE_OUT_MS = 900f;
    private static final int MAX_PINGS = 256;
    private static final float STICKY_DEG = 30f;
    private static final float STICKY_BONUS = 0.3f;

    private record Ping(double x, double y, double z, float volume, Danger danger, long timeMs) {}

    record Target(float yaw, float level, Danger danger, float dy) {}

    private final Queue<Ping> pings = new ConcurrentLinkedQueue<>();

    void add(double x, double y, double z, float volume, Danger danger) {
        long now = System.currentTimeMillis();
        Ping head;
        while ((head = pings.peek()) != null
                && (now - head.timeMs() >= LIFETIME_MS || pings.size() >= MAX_PINGS)) {
            pings.poll();
        }
        pings.add(new Ping(x, y, z, volume, danger, now));
    }

    Target compute(double cx, double cy, double cz, float currentYaw) {
        long now = System.currentTimeMillis();
        Target best = null;
        float bestScore = -1f;

        for (Iterator<Ping> it = pings.iterator(); it.hasNext(); ) {
            Ping p = it.next();
            float age = now - p.timeMs();
            if (age >= LIFETIME_MS) {
                it.remove();
                continue;
            }
            float alpha = MathUtil.clamp(Math.min(age / FADE_IN_MS, (LIFETIME_MS - age) / FADE_OUT_MS), 0f, 1f);

            double dx = p.x() - cx;
            double dy = p.y() - cy;
            double dz = p.z() - cz;
            if (dx * dx + dz * dz < 0.01) continue;

            float range = 16f * Math.max(p.volume(), 1f);
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            float loud = MathUtil.clamp(1f - dist / range, 0f, 1f);
            if (loud <= 0f) continue;

            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float score = p.danger().ordinal() * 10f + loud * alpha;
            if (!Float.isNaN(currentYaw) && Math.abs(MathUtil.wrapDegrees(yaw - currentYaw)) <= STICKY_DEG) {
                score += STICKY_BONUS;
            }
            if (score > bestScore) {
                bestScore = score;
                best = new Target(yaw, loud * 3f, p.danger(), (float) dy);
            }
        }
        return best;
    }
}

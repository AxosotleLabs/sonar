package net.axolabs.sonar.core;

import java.util.Arrays;

final class SonarHud {

    private static final int TEX_W = Sonar.TEX_W, TEX_H = Sonar.TEX_H;

    private static final int[][][] RINGS = {
            {{5, 4, 3, 1}},
            {{4, 2, 5, 1}, {2, 3, 2, 1}, {9, 3, 2, 1}},
            {{3, 0, 7, 1}, {1, 1, 2, 1}, {10, 1, 2, 1}, {0, 2, 1, 1}, {12, 2, 1, 1}},
    };
    private static final float RADIUS = 6.5f;

    private static final float RING_IN_SEC = 0.12f;
    private static final float RING_OUT_SEC = 0.25f;
    private static final float COLOR_SEC = 0.3f;
    private static final float TURN_SPEED = 14f;
    private static final float JUMP_DEG = 60f;
    private static final float JUMP_OUT_SEC = 0.08f;
    private static final float WAVE_GATE = 0.35f;
    private static final float DRAW_SEC = 0.15f;
    private static final float ERASE_SEC = 0.2f;
    private static final float MIN_FILL = 0.1f;
    private static final float TEX_MID = TEX_W / 2f;
    private static final float[] RING_HALF = new float[RINGS.length];

    static {
        for (int r = 0; r < RINGS.length; r++) {
            for (int[] q : RINGS[r]) {
                RING_HALF[r] = Math.max(RING_HALF[r],
                        Math.max(Math.abs(q[0] - TEX_MID), Math.abs(q[0] + q[2] - TEX_MID)));
            }
        }
    }

    private static final float HEIGHT_DY = 3f;
    private static final float MARK_RADIUS = RADIUS + TEX_H + 3f;
    private static final int MARK_U = 6, MARK_V = 4;

    private final SonarTracker tracker;

    private final float[] ring = new float[3];
    private final float[] drawn = new float[3];
    private float markUp, markDown;
    private float yaw;
    private float tint;
    private float heat;
    private long lastFrameMs;

    private boolean invertNeutral;
    private float[] neutralRgb, mediumRgb, highRgb;

    SonarHud(SonarTracker tracker) {
        this.tracker = tracker;
    }

    void render(Canvas canvas, SonarConfig config, boolean active, double camX, double camY, double camZ,
                float camYaw, float cx, float cy) {
        if (!active || !config.enabled()) {
            Arrays.fill(ring, 0f);
            Arrays.fill(drawn, 0f);
            markUp = markDown = 0f;
            lastFrameMs = System.currentTimeMillis();
            return;
        }

        update(tracker.compute(camX, camY, camZ, isVisible() ? yaw : Float.NaN));
        if (!isVisible()) return;
        float angle = MathUtil.wrapDegrees(yaw - camYaw);

        invertNeutral = config.invertNeutral();
        neutralRgb = config.neutral();
        mediumRgb = config.medium();
        highRgb = config.high();

        if (invertNeutral) {
            canvas.begin(true);
            drawPass(canvas, cx, cy, angle, false);
            canvas.end();
        }
        canvas.begin(false);
        drawPass(canvas, cx, cy, angle, true);
        canvas.end();
    }

    private void drawPass(Canvas c, float cx, float cy, float angle, boolean colored) {
        c.pushArc(cx, cy, angle, -TEX_W / 2f, -RADIUS - TEX_H);
        drawRings(c, colored);
        c.pop();
        drawHeightMark(c, cx, cy, angle, colored);
    }

    private void drawHeightMark(Canvas c, float cx, float cy, float angle, boolean colored) {
        double rad = Math.toRadians(angle);
        int mx = (int) Math.floor(cx + Math.sin(rad) * MARK_RADIUS);
        int my = Math.round((float) (cy - Math.cos(rad) * MARK_RADIUS));

        float[] col = color(markUp, colored);
        if (col != null) {
            pixel(c, mx, my - 1, 1, col);
            pixel(c, mx - 1, my, 3, col);
        }
        col = color(markDown, colored);
        if (col != null) {
            pixel(c, mx - 1, my - 1, 3, col);
            pixel(c, mx, my, 1, col);
        }
    }

    private static void pixel(Canvas c, int x, int y, int width, float[] col) {
        c.blit(x, y, width, 1, MARK_U, MARK_V, 1, 1, col[0], col[1], col[2], col[3]);
    }

    private boolean isVisible() {
        for (float r : ring) {
            if (r > 0f) return true;
        }
        return false;
    }

    private void update(SonarTracker.Target target) {
        long now = System.currentTimeMillis();
        float dt = MathUtil.clamp((now - lastFrameMs) / 1000f, 0f, 0.1f);
        lastFrameMs = now;

        boolean jumping = false;
        if (target != null) {
            float tintTarget = target.danger() == Danger.NEUTRAL ? 0f : 1f;
            float heatTarget = target.danger() == Danger.HIGH ? 1f : 0f;
            float turn = MathUtil.wrapDegrees(target.yaw() - yaw);
            if (!isVisible()) {
                yaw = target.yaw();
                tint = tintTarget;
                heat = heatTarget;
            } else if (Math.abs(turn) > JUMP_DEG) {
                jumping = true;
            } else {
                float k = 1f - (float) Math.exp(-TURN_SPEED * dt);
                yaw = MathUtil.wrapDegrees(yaw + turn * k);
                tint = MathUtil.approach(tint, tintTarget, dt / COLOR_SEC, dt / COLOR_SEC);
                heat = tint < 0.01f ? heatTarget
                        : MathUtil.approach(heat, heatTarget, dt / COLOR_SEC, dt / COLOR_SEC);
            }
        }

        for (int r = 0; r < 3; r++) {
            float fill = target == null ? 0f : MathUtil.clamp(target.level() - r, 0f, 1f);
            boolean on = !jumping && fill > 0f;
            if (on) {
                if (r > 0 && drawn[r - 1] < WAVE_GATE) continue;
                drawn[r] = MathUtil.approach(drawn[r], Math.max(fill, MIN_FILL), dt / DRAW_SEC, dt / ERASE_SEC);
                ring[r] = MathUtil.approach(ring[r], 1f, dt / RING_IN_SEC, 0f);
            } else {
                if (!jumping && r < 2 && drawn[r + 1] > 1f - WAVE_GATE) continue;
                drawn[r] = Math.max(0f, drawn[r] - dt / (jumping ? JUMP_OUT_SEC : ERASE_SEC));
                if (drawn[r] <= 0f) ring[r] = 0f;
            }
        }

        float upGoal = 0f, downGoal = 0f;
        if (target != null && !jumping) {
            float inner = ring[0] * drawn[0];
            if (target.dy() > HEIGHT_DY) upGoal = inner;
            else if (target.dy() < -HEIGHT_DY) downGoal = inner;
        }
        float out = jumping ? dt / JUMP_OUT_SEC : dt / RING_OUT_SEC;
        markUp = MathUtil.approach(markUp, upGoal, dt / RING_IN_SEC, out);
        markDown = MathUtil.approach(markDown, downGoal, dt / RING_IN_SEC, out);
    }

    private void drawRings(Canvas c, boolean colored) {
        for (int r = 0; r < 3; r++) {
            float t = 1f - drawn[r];
            float reach = (1f - t * t) * RING_HALF[r];
            for (int[] q : RINGS[r]) {
                for (int y = q[1]; y < q[1] + q[3]; y++) {
                    for (int x = q[0]; x < q[0] + q[2]; x++) {
                        float dist = Math.abs(x + 0.5f - TEX_MID) - 0.5f;
                        float cover = MathUtil.clamp(reach - dist, 0f, 1f);
                        float[] col = color(ring[r] * cover, colored);
                        if (col == null) continue;
                        c.blit(x, y, 1, 1, x, y, 1, 1, col[0], col[1], col[2], col[3]);
                    }
                }
            }
        }
    }

    private float[] color(float visibility, boolean colored) {
        float share = !invertNeutral ? 1f : colored ? tint : 1f - tint;
        float weight = visibility * share;
        if (weight < 0.004f) return null;
        if (!colored) return new float[]{weight, weight, weight, 1f};
        float danger = invertNeutral ? 1f : tint;
        float[] out = new float[4];
        for (int i = 0; i < 3; i++) {
            out[i] = MathUtil.lerp(danger, neutralRgb[i], MathUtil.lerp(heat, mediumRgb[i], highRgb[i]));
        }
        out[3] = weight;
        return out;
    }
}

package net.axolabs.sonar.core;

final class MathUtil {

    private MathUtil() {}

    static float clamp(float v, float min, float max) {
        return v < min ? min : Math.min(v, max);
    }

    static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    static float wrapDegrees(float deg) {
        float d = deg % 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    static float approach(float value, float target, float maxUp, float maxDown) {
        return value + clamp(target - value, -maxDown, maxUp);
    }
}

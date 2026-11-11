package net.axolabs.sonar.core;

public interface Canvas {

    void begin(boolean invert);

    void pushArc(float centerX, float centerY, float degrees, float offsetX, float offsetY);

    void pop();

    void blit(int x, int y, int width, int height, int u, int v, int uWidth, int vHeight,
              float r, float g, float b, float a);

    void end();
}

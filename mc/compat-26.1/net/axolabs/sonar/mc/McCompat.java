package net.axolabs.sonar.mc;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

final class McCompat {

    private McCompat() {}

    static boolean hudHidden(Minecraft mc) {
        return mc.options.hideGui;
    }

    static Camera camera(Minecraft mc) {
        return mc.gameRenderer.getMainCamera();
    }
}

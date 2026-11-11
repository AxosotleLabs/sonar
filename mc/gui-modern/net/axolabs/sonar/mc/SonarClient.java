package net.axolabs.sonar.mc;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.axolabs.sonar.core.Canvas;
import net.axolabs.sonar.core.Sonar;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SonarClient {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Sonar.MOD_ID, Sonar.TEXTURE);

    private SonarClient() {}

    public static void onSound(SoundInstance s) {
        if (s == null || s.isRelative() || s.getAttenuation() == SoundInstance.Attenuation.NONE) return;
        double x = s.getX(), y = s.getY(), z = s.getZ();
        Sonar.onSound(s.getIdentifier().toString(), x, y, z, s.getVolume(), () -> isOwn(x, y, z));
    }

    private static boolean isOwn(double x, double y, double z) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        if (contains(player.getBoundingBox(), x, y, z)) return true;
        Entity vehicle = player.getVehicle();
        return vehicle != null && contains(vehicle.getBoundingBox(), x, y, z);
    }

    private static boolean contains(AABB box, double x, double y, double z) {
        return box.inflate(0.75).contains(x, y, z);
    }

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean active = !mc.options.hideGui
                && mc.options.getCameraType().isFirstPerson()
                && !player.isSpectator()
                && (!Sonar.config().requireEchoShard()
                || player.getInventory().hasAnyMatching(stack -> stack.is(Items.ECHO_SHARD)));

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 pos = camera.position();
        float cx = (g.guiWidth() - 15) / 2 + 7.5f;
        float cy = (g.guiHeight() - 15) / 2 + 7.5f;
        Sonar.render(new ModernCanvas(g), active, pos.x, pos.y, pos.z, camera.yRot(), cx, cy);
    }

    private static final class ModernCanvas implements Canvas {

        private final GuiGraphics g;
        private RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;

        ModernCanvas(GuiGraphics g) {
            this.g = g;
        }

        @Override
        public void begin(boolean invert) {
            pipeline = invert ? RenderPipelines.CROSSHAIR : RenderPipelines.GUI_TEXTURED;
        }

        @Override
        public void pushArc(float centerX, float centerY, float degrees, float offsetX, float offsetY) {
            g.pose().pushMatrix();
            g.pose().translate(centerX, centerY);
            g.pose().rotate((float) Math.toRadians(degrees));
            g.pose().translate(offsetX, offsetY);
        }

        @Override
        public void pop() {
            g.pose().popMatrix();
        }

        @Override
        public void blit(int x, int y, int width, int height, int u, int v, int uWidth, int vHeight,
                         float r, float gr, float b, float a) {
            int color = channel(a) << 24 | channel(r) << 16 | channel(gr) << 8 | channel(b);
            g.blit(pipeline, TEXTURE, x, y, u, v, width, height, uWidth, vHeight, Sonar.TEX_W, Sonar.TEX_H, color);
        }

        private static int channel(float f) {
            return Math.round(Math.max(0f, Math.min(1f, f)) * 255f);
        }

        @Override
        public void end() {
            pipeline = RenderPipelines.GUI_TEXTURED;
        }
    }
}

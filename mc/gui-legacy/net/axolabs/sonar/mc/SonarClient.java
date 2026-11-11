package net.axolabs.sonar.mc;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.axolabs.sonar.core.Canvas;
import net.axolabs.sonar.core.Sonar;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

public final class SonarClient {

    private static final ResourceLocation TEXTURE = ResourceLocation.tryParse(Sonar.MOD_ID + ":" + Sonar.TEXTURE);

    private SonarClient() {}

    public static void onSound(SoundInstance s) {
        if (s == null || s.isRelative() || s.getAttenuation() == SoundInstance.Attenuation.NONE) return;
        double x = s.getX(), y = s.getY(), z = s.getZ();
        Sonar.onSound(s.getLocation().toString(), x, y, z, s.getVolume(), () -> isOwn(x, y, z));
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
        float cx = (g.guiWidth() - 15) / 2 + 7.5f;
        float cy = (g.guiHeight() - 15) / 2 + 7.5f;
        Sonar.render(new LegacyCanvas(g), active, camera.getPosition().x, camera.getPosition().y,
                camera.getPosition().z, camera.getYRot(), cx, cy);
    }

    private record LegacyCanvas(GuiGraphics g) implements Canvas {

        @Override
        public void begin(boolean invert) {
            RenderSystem.enableBlend();
            if (invert) {
                RenderSystem.blendFuncSeparate(
                        GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                        GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ZERO);
            } else {
                RenderSystem.defaultBlendFunc();
            }
        }

        @Override
        public void pushArc(float centerX, float centerY, float degrees, float offsetX, float offsetY) {
            g.pose().pushPose();
            g.pose().translate(centerX, centerY, 0f);
            g.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
            g.pose().translate(offsetX, offsetY, 0f);
        }

        @Override
        public void pop() {
            g.pose().popPose();
        }

        @Override
        public void blit(int x, int y, int width, int height, int u, int v, int uWidth, int vHeight,
                         float r, float gr, float b, float a) {
            g.setColor(r, gr, b, a);
            g.blit(TEXTURE, x, y, width, height, u, v, uWidth, vHeight, Sonar.TEX_W, Sonar.TEX_H);
        }

        @Override
        public void end() {
            g.setColor(1f, 1f, 1f, 1f);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }
}

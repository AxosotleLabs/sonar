package net.axolabs.sonar.fabric;

import net.axolabs.sonar.core.Sonar;
import net.axolabs.sonar.mc.SonarClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SonarFabric implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Sonar.MOD_ID);

    @Override
    public void onInitializeClient() {
        Sonar.init(FabricLoader.getInstance().getConfigDir(), LOGGER::warn);
        HudRenderCallback.EVENT.register((g, delta) -> SonarClient.render(g));
    }
}

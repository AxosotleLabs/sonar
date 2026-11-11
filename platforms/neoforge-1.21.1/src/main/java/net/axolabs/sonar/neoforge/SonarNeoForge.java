package net.axolabs.sonar.neoforge;

import com.mojang.logging.LogUtils;
import net.axolabs.sonar.core.Sonar;
import net.axolabs.sonar.mc.SonarClient;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(value = Sonar.MOD_ID, dist = Dist.CLIENT)
public final class SonarNeoForge {

    private static final Logger LOGGER = LogUtils.getLogger();

    public SonarNeoForge(IEventBus modBus) {
        Sonar.init(FMLPaths.CONFIGDIR.get(), LOGGER::warn);
        modBus.addListener(SonarNeoForge::registerLayers);
        NeoForge.EVENT_BUS.addListener((PlaySoundSourceEvent e) -> SonarClient.onSound(e.getSound()));
        NeoForge.EVENT_BUS.addListener((PlayStreamingSourceEvent e) -> SonarClient.onSound(e.getSound()));
    }

    private static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                ResourceLocation.fromNamespaceAndPath(Sonar.MOD_ID, "sonar"),
                (g, delta) -> SonarClient.render(g));
    }
}

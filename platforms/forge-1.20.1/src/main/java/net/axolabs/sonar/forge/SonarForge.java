package net.axolabs.sonar.forge;

import com.mojang.logging.LogUtils;
import net.axolabs.sonar.core.Sonar;
import net.axolabs.sonar.mc.SonarClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

@Mod(Sonar.MOD_ID)
public final class SonarForge {

    private static final Logger LOGGER = LogUtils.getLogger();

    public SonarForge() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> SonarForge::initClient);
    }

    private static void initClient() {
        Sonar.init(FMLPaths.CONFIGDIR.get(), LOGGER::warn);
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(SonarForge::registerOverlays);
        MinecraftForge.EVENT_BUS.addListener((PlaySoundSourceEvent e) -> SonarClient.onSound(e.getSound()));
        MinecraftForge.EVENT_BUS.addListener((PlayStreamingSourceEvent e) -> SonarClient.onSound(e.getSound()));
    }

    private static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "sonar",
                (gui, g, partialTick, width, height) -> SonarClient.render(g));
    }
}

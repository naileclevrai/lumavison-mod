package fr.lumavision.artnet;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.config.ModConfig;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Starts/stops the server-side {@link ArtNetReceiver} with the server (integrated or dedicated),
 * when Art-Net is enabled in config.
 */
@Mod.EventBusSubscriber(modid = LumaVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArtNetServerEvents {

    private ArtNetServerEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        if (ModConfig.ARTNET_ENABLE.get()) {
            ArtNetReceiver.start(ModConfig.ARTNET_BIND_ADDRESS.get(), ModConfig.ARTNET_PORT.get());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ArtNetReceiver.stop();
    }
}

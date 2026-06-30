package fr.lumavision.client;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.client.ndi.NdiProvider;
import fr.lumavision.config.ModConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Applies client-side reactions to {@link ModConfig} changes (NDI discovery lifecycle).
 */
@Mod.EventBusSubscriber(modid = LumaVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LumaVisionClientConfigEvents {

    private LumaVisionClientConfigEvents() {
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ModConfig.SPEC) {
            NdiProvider.applyConfig();
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ModConfig.SPEC) {
            NdiProvider.applyConfig();
        }
    }
}

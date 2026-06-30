package fr.lumavision.client;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.client.ndi.NdiDiscoveryService;
import fr.lumavision.client.ndi.NdiRuntime;
import fr.lumavision.client.render.ScreenRenderer;
import fr.lumavision.config.ModConfig;
import fr.lumavision.registry.ModBlockEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = LumaVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LumaVisionClientMod {

    private LumaVisionClientMod() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (ModConfig.ENABLE_NDI.get() && NdiRuntime.init()) {
                NdiDiscoveryService.getInstance().start();
            }
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.LED_SCREEN.get(), ScreenRenderer::new);
    }
}

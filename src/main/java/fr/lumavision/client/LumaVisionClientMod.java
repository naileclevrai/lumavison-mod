package fr.lumavision.client;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.client.gui.ScreenConfigScreen;
import fr.lumavision.client.render.ScreenRenderer;
import fr.lumavision.client.video.catalog.ClientVideoSourceCatalog;
import fr.lumavision.registry.ModBlockEntities;
import fr.lumavision.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
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
            ClientVideoSourceCatalog.INSTANCE.start();
            MenuScreens.register(ModMenuTypes.SCREEN_CONFIG.get(), ScreenConfigScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.LED_SCREEN.get(), ScreenRenderer::new);
    }
}

package fr.lumavision;

import fr.lumavision.network.ModNetworking;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Initialisation côté client et commune.
 * <p>
 * Les renderers d'écrans LED et les textures dynamiques seront branchés ici ultérieurement.
 */
@Mod.EventBusSubscriber(modid = LumaVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LumaVisionModClient {

    private LumaVisionModClient() {
    }

    @SubscribeEvent
    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetworking::register);
    }

    @Mod.EventBusSubscriber(modid = LumaVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Client {
        private Client() {
        }

        @SubscribeEvent
        public static void onClientSetup(final FMLClientSetupEvent event) {
            LumaVisionMod.LOGGER.debug("Setup client LumaVision terminé");
        }
    }
}

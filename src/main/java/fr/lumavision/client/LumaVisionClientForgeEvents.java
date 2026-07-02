package fr.lumavision.client;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.client.relay.MediaRelayClient;
import fr.lumavision.client.relay.ScreenFrameUploader;
import fr.lumavision.client.texture.ScreenTextureManager;
import fr.lumavision.network.ScreenFrameChunkHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client lifecycle hooks: texture cleanup must run on the render thread.
 */
@Mod.EventBusSubscriber(modid = LumaVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LumaVisionClientForgeEvents {

    private LumaVisionClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            ScreenTextureManager.getInstance().tick(minecraft.level);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            scheduleTextureCleanup();
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        scheduleTextureCleanup();
    }

    private static void scheduleTextureCleanup() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            ScreenTextureManager.getInstance().clear();
            MediaRelayClient.getInstance().clear();
            ScreenFrameUploader.getInstance().clear();
            ScreenFrameChunkHandler.clearClient();
        });
    }
}

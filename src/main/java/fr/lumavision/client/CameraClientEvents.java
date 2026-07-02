package fr.lumavision.client;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.client.ndi.CameraNdiManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client lifecycle hooks driving the NDI camera senders: a per-tick reaper for cameras that stopped
 * ticking, and teardown of all senders on world unload / logout so no NDI sources leak between worlds.
 */
@Mod.EventBusSubscriber(modid = LumaVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CameraClientEvents {

    private CameraClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CameraNdiManager.getInstance().tick();
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            Minecraft.getInstance().execute(CameraNdiManager.getInstance()::shutdown);
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft.getInstance().execute(CameraNdiManager.getInstance()::shutdown);
    }
}

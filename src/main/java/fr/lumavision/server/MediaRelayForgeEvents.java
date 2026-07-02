package fr.lumavision.server;

import fr.lumavision.LumaVisionMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge hooks for the server-side media relay (NDI bridge).
 */
@Mod.EventBusSubscriber(modid = LumaVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MediaRelayForgeEvents {

    private MediaRelayForgeEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            MediaRelayManager.getInstance().onServerTick(server);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MediaRelayManager.getInstance().onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MediaRelayManager.getInstance().clear();
    }
}

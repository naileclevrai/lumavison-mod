package fr.lumavision.client;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.block.CameraBlock;
import fr.lumavision.blockentity.CameraBlockEntity;
import fr.lumavision.camera.CameraParameters;
import fr.lumavision.entity.CameraSeatEntity;
import fr.lumavision.network.CameraRigInputPacket;
import fr.lumavision.network.ModNetworking;
import fr.lumavision.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side camera-operator experience while sitting in a boom seat: the player's view becomes the
 * controlled camera's first-person view, WASD drives pan/tilt and the scroll wheel zooms, all sent to
 * the server which owns the parameters. Restores the normal view on dismount.
 */
@Mod.EventBusSubscriber(modid = LumaVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SeatOperator {

    private static float pendingScroll;
    private static Entity viewAnchor;
    private static boolean operating;

    private SeatOperator() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        CameraBlockEntity camera = player != null && mc.level != null
                && player.getVehicle() instanceof CameraSeatEntity seat
                ? findCamera(mc, seat) : null;

        if (camera == null) {
            stopOperating(mc);
            return;
        }

        BlockPos camPos = camera.getBlockPos();
        float forward = player.zza;
        float strafe = player.xxa;
        float scroll = pendingScroll;
        pendingScroll = 0.0F;
        if (forward != 0.0F || strafe != 0.0F || scroll != 0.0F) {
            ModNetworking.CHANNEL.sendToServer(new CameraRigInputPacket(camPos, forward, strafe, scroll));
        }

        // Move the player's view to the camera (first person through the lens).
        CameraParameters p = camera.parameters();
        BlockState state = camera.getBlockState();
        float baseYaw = state.hasProperty(CameraBlock.FACING) ? state.getValue(CameraBlock.FACING).toYRot() : 0.0F;
        if (viewAnchor == null) {
            viewAnchor = new CameraSeatEntity(ModEntities.CAMERA_SEAT.get(), mc.level);
        }
        viewAnchor.setPos(camPos.getX() + 0.5D, camPos.getY() + 0.5D - viewAnchor.getEyeHeight(), camPos.getZ() + 0.5D);
        viewAnchor.xo = viewAnchor.getX();
        viewAnchor.yo = viewAnchor.getY();
        viewAnchor.zo = viewAnchor.getZ();
        viewAnchor.setYRot(baseYaw + p.pan());
        viewAnchor.setXRot(p.tilt());
        viewAnchor.yRotO = viewAnchor.getYRot();
        viewAnchor.xRotO = viewAnchor.getXRot();
        if (mc.getCameraEntity() != viewAnchor) {
            mc.setCameraEntity(viewAnchor);
        }
        operating = true;
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof CameraSeatEntity) {
            pendingScroll += (float) event.getScrollDelta();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!operating) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && mc.player.getVehicle() instanceof CameraSeatEntity seat) {
            CameraBlockEntity camera = findCamera(mc, seat);
            if (camera != null) {
                event.setFOV(camera.parameters().effectiveFov());
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        operating = false;
        viewAnchor = null;
    }

    private static CameraBlockEntity findCamera(Minecraft mc, CameraSeatEntity seat) {
        BlockPos camPos = CameraSeatEntity.findControlledCamera(mc.level, seat.blockPosition());
        if (camPos != null && mc.level.getBlockEntity(camPos) instanceof CameraBlockEntity camera) {
            return camera;
        }
        return null;
    }

    private static void stopOperating(Minecraft mc) {
        if (operating) {
            operating = false;
            viewAnchor = null;
            if (mc.player != null && mc.getCameraEntity() != mc.player) {
                mc.setCameraEntity(mc.player);
            }
        }
    }
}

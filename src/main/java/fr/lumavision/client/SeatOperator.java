package fr.lumavision.client;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.block.CameraBlock;
import fr.lumavision.blockentity.CameraBlockEntity;
import fr.lumavision.camera.CameraRig;
import fr.lumavision.entity.CameraSeatEntity;
import fr.lumavision.network.CameraRigInputPacket;
import fr.lumavision.network.ModNetworking;
import fr.lumavision.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side crane operator: while sitting on a boom seat near a camera, the player's view becomes
 * the camera's shot from the end of the crane arm, WASD swings/booms the arm and the scroll wheel
 * zooms. Input is sent to the server (owner of the parameters); the view + FOV follow the camera.
 */
@Mod.EventBusSubscriber(modid = LumaVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SeatOperator {

    private static float pendingScroll;
    private static Entity viewAnchor;
    private static BlockPos controlledCamera;
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
        if (player == null || mc.level == null || !(player.getVehicle() instanceof CameraSeatEntity seat)) {
            stopOperating(mc);
            return;
        }

        if (controlledCamera == null) {
            controlledCamera = CameraSeatEntity.findControlledCamera(mc.level, seat.blockPosition());
        }
        CameraBlockEntity camera = controlledCamera != null
                && mc.level.getBlockEntity(controlledCamera) instanceof CameraBlockEntity c ? c : null;
        if (camera == null) {
            stopOperating(mc);
            return;
        }

        // Send arm/zoom input to the server. Read player.input (live keys) — zza/xxa stay 0 while riding.
        float forward = player.input.forwardImpulse;
        float strafe = player.input.leftImpulse;
        float scroll = pendingScroll;
        pendingScroll = 0.0F;
        if (forward != 0.0F || strafe != 0.0F || scroll != 0.0F) {
            ModNetworking.CHANNEL.sendToServer(new CameraRigInputPacket(controlledCamera, forward, strafe, scroll));
        }

        // Put the player's view at the arm tip, looking where the camera looks.
        float reach = CameraRig.reachFor(mc.level, controlledCamera);
        CameraRig.View view = CameraRig.compute(controlledCamera, baseYaw(camera), camera.parameters(), reach);
        if (viewAnchor == null) {
            viewAnchor = new CameraSeatEntity(ModEntities.CAMERA_SEAT.get(), mc.level);
        }
        viewAnchor.setPos(view.x(), view.y() - viewAnchor.getEyeHeight(), view.z());
        viewAnchor.xo = viewAnchor.getX();
        viewAnchor.yo = viewAnchor.getY();
        viewAnchor.zo = viewAnchor.getZ();
        viewAnchor.setYRot(view.yaw());
        viewAnchor.setXRot(view.pitch());
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
        if (!operating || controlledCamera == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(controlledCamera) instanceof CameraBlockEntity camera) {
            event.setFOV(camera.parameters().effectiveFov());
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        operating = false;
        viewAnchor = null;
        controlledCamera = null;
    }

    private static float baseYaw(CameraBlockEntity camera) {
        BlockState state = camera.getBlockState();
        return state.hasProperty(CameraBlock.FACING) ? state.getValue(CameraBlock.FACING).toYRot() : 0.0F;
    }

    private static void stopOperating(Minecraft mc) {
        if (operating || controlledCamera != null) {
            operating = false;
            controlledCamera = null;
            viewAnchor = null;
            if (mc.player != null && mc.getCameraEntity() != mc.player) {
                mc.setCameraEntity(mc.player);
            }
        }
    }
}

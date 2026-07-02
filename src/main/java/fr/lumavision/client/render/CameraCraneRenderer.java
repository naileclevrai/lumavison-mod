package fr.lumavision.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.lumavision.block.CameraBlock;
import fr.lumavision.block.CameraCraneBlock;
import fr.lumavision.blockentity.CameraBlockEntity;
import fr.lumavision.camera.CameraParameters;
import fr.lumavision.camera.CameraRig;
import fr.lumavision.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Draws the animated camera crane in-world. The turntable swings with {@code boomSwing} (composed on
 * top of the block's facing), the arm booms with {@code boomPitch}, and the camera head rides the far
 * end — matching the viewpoint {@link fr.lumavision.camera.CameraRig} shoots from. Shared with the
 * plain camera/PTZ block entities, so it only draws for the {@code camera_crane} block.
 */
@OnlyIn(Dist.CLIENT)
public final class CameraCraneRenderer implements BlockEntityRenderer<CameraBlockEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("lumavision", "textures/entity/camera_crane.png");

    private final ModelPart root;
    private final ModelPart turntable;
    private final ModelPart arm;
    private final ModelPart head;

    public CameraCraneRenderer(BlockEntityRendererProvider.Context context) {
        this.root = context.bakeLayer(CameraCraneModel.LAYER);
        this.turntable = root.getChild("turntable");
        this.arm = turntable.getChild("arm");
        this.head = arm.getChild("head");
    }

    @Override
    public void render(CameraBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!state.is(ModBlocks.CAMERA_CRANE.get())) {
            return; // plain camera / PTZ use a normal block model, not this renderer
        }

        CameraParameters p = be.parameters();
        float baseYaw = state.hasProperty(CameraBlock.FACING) ? state.getValue(CameraBlock.FACING).toYRot() : 0.0F;

        boolean mounted = state.getValue(CameraCraneBlock.MOUNTED);

        // Aim the arm where the shot actually comes from (was 180° off), boom up when pitch rises.
        turntable.yRot = (float) Math.toRadians(-(baseYaw + p.boomSwing()));
        arm.xRot = (float) Math.toRadians(p.boomPitch());
        // The generic head cube is replaced by the real camera model drawn below; keep it hidden.
        head.visible = false;

        pose.pushPose();
        pose.translate(0.5D, 0.0D, 0.5D);
        // Scale the whole rig up (in sync with CameraRig.CRANE_ARM_LENGTH); flip Y/Z into world space.
        float s = CameraRig.CRANE_SCALE;
        pose.scale(s, -s, -s);
        VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(TEXTURE));
        root.render(pose, vc, packedLight, packedOverlay);
        pose.popPose();

        // Hang the actual camera block model off the arm tip, gimbal-style, when one is mounted.
        if (mounted) {
            double[] tip = CameraRig.craneTipRelative(baseYaw, p);
            pose.pushPose();
            pose.translate(tip[0], tip[1] - CameraRig.CRANE_CAMERA_DROP, tip[2]);
            // The model's lens points north (-Z); rotating by armYaw aims it outward along the arm,
            // matching the shot direction (180 - armYaw) that CameraRig.craneView looks along.
            pose.mulPose(Axis.YP.rotationDegrees((float) tip[3]));
            float camScale = 1.1F;
            pose.scale(camScale, camScale, camScale);
            pose.translate(-0.5D, -0.5D, -0.5D); // block models render from the 0..1 corner — centre it
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    ModBlocks.CAMERA.get().defaultBlockState(), pose, buffers, packedLight, packedOverlay);
            pose.popPose();
        }
    }
}

package fr.lumavision.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.lumavision.block.CameraBlock;
import fr.lumavision.blockentity.CameraBlockEntity;
import fr.lumavision.camera.CameraParameters;
import fr.lumavision.registry.ModBlocks;
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

    public CameraCraneRenderer(BlockEntityRendererProvider.Context context) {
        this.root = context.bakeLayer(CameraCraneModel.LAYER);
        this.turntable = root.getChild("turntable");
        this.arm = turntable.getChild("arm");
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

        // Aim the whole arm where the shot points; boom it up/down. (Signs are a first pass — tune to view.)
        turntable.yRot = (float) Math.toRadians(180.0F - (baseYaw + p.boomSwing()));
        arm.xRot = (float) Math.toRadians(-p.boomPitch());

        pose.pushPose();
        pose.translate(0.5D, 0.0D, 0.5D);
        pose.scale(1.0F, -1.0F, -1.0F);
        VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(TEXTURE));
        root.render(pose, vc, packedLight, packedOverlay);
        pose.popPose();
    }
}

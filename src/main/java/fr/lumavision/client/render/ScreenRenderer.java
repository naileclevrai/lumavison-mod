package fr.lumavision.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.lumavision.block.LedScreenBlock;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.client.texture.ScreenTextureManager;
import fr.lumavision.screen.ScreenGroupMembership;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renders an LED screen surface using whatever {@link ResourceLocation}
 * the {@link ScreenTextureManager} provides — without knowing the pixel source.
 */
public final class ScreenRenderer implements BlockEntityRenderer<LedScreenBlockEntity> {

    private static final float FACE_EPSILON = 0.001F;

    public ScreenRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LedScreenBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(LedScreenBlock.FACING);
        ScreenGroupMembership group = blockEntity.getGroupMembership();
        ResourceLocation screenTexture = ScreenTextureManager.getInstance().getTexture(blockEntity);

        int light = LightTexture.FULL_BRIGHT;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(screenTexture));
        drawFacingQuad(consumer, poseStack.last(), facing,
                0.0F, 0.0F, 1.0F, 1.0F, FACE_EPSILON,
                group.uvMinU(), group.uvMinV(), group.uvMaxU(), group.uvMaxV(), light, packedOverlay);
    }

    /**
     * Draws a unit-cube-aligned quad on the given face.
     */
    private static void drawFacingQuad(VertexConsumer consumer, PoseStack.Pose pose, Direction facing,
                                       float x0, float y0, float x1, float y1, float epsilon,
                                       float u0, float v0, float u1, float v1,
                                       int light, int overlay) {
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        float nx = facing.getStepX();
        float ny = facing.getStepY();
        float nz = facing.getStepZ();

        float offset = switch (facing) {
            case DOWN -> epsilon;
            case UP -> 1.0F - epsilon;
            case NORTH -> epsilon;
            case SOUTH -> 1.0F - epsilon;
            case WEST -> epsilon;
            case EAST -> 1.0F - epsilon;
        };

        switch (facing.getAxis()) {
            case X -> {
                putVertex(consumer, matrix, normalMatrix, offset, y0, x0, u0, v1, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, offset, y0, x1, u1, v1, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, offset, y1, x1, u1, v0, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, offset, y1, x0, u0, v0, nx, ny, nz, light, overlay);
            }
            case Y -> {
                putVertex(consumer, matrix, normalMatrix, x0, offset, y0, u0, v1, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, x1, offset, y0, u1, v1, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, x1, offset, y1, u1, v0, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, x0, offset, y1, u0, v0, nx, ny, nz, light, overlay);
            }
            case Z -> {
                putVertex(consumer, matrix, normalMatrix, x0, y0, offset, u0, v1, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, x1, y0, offset, u1, v1, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, x1, y1, offset, u1, v0, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, x0, y1, offset, u0, v0, nx, ny, nz, light, overlay);
            }
        }
    }

    private static void putVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                  float x, float y, float z,
                                  float u, float v,
                                  float nx, float ny, float nz,
                                  int light, int overlay) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMatrix, nx, ny, nz)
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(LedScreenBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}

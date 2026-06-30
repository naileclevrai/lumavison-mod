package fr.lumavision.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.lumavision.block.LedScreenBlock;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.client.display.DisplayColorGrading;
import fr.lumavision.client.display.DisplayUvMapper;
import fr.lumavision.client.texture.ScreenTextureManager;
import fr.lumavision.screen.ScreenDisplaySettings;
import fr.lumavision.screen.ScreenGroupMembership;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders an LED screen surface using whatever {@link ResourceLocation}
 * the {@link ScreenTextureManager} provides — without knowing the pixel source.
 */
public final class ScreenRenderer implements BlockEntityRenderer<LedScreenBlockEntity> {

    private static final float FACE_EPSILON = 0.005F;

    public ScreenRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LedScreenBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(LedScreenBlock.FACING);
        ScreenGroupMembership group = blockEntity.getGroupMembership();
        ResourceLocation screenTexture = ScreenTextureManager.getInstance().getTexture(blockEntity);

        ScreenTextureManager.WallRenderContext context = ScreenTextureManager.getInstance()
                .getWallRenderContext(group.groupKey());
        ScreenDisplaySettings settings;
        int frameWidth;
        int frameHeight;
        int[] vertexColor;

        if (context != null) {
            settings = context.settings();
            frameWidth = context.frameWidth();
            frameHeight = context.frameHeight();
            vertexColor = context.vertexColor();
        } else {
            settings = blockEntity.getLevel() == null
                    ? ScreenDisplaySettings.DEFAULT
                    : LedScreenBlockEntity.resolveDisplaySettings(blockEntity.getLevel(), group);
            int[] frameSize = ScreenTextureManager.getInstance().getFrameSize(group.groupKey());
            frameWidth = frameSize[0];
            frameHeight = frameSize[1];
            vertexColor = DisplayColorGrading.vertexColor(settings);
        }

        DisplayUvMapper.MappedUv mapped = DisplayUvMapper.map(group, settings, frameWidth, frameHeight);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(screenTexture));
        drawFacingQuad(consumer, poseStack.last(), facing,
                mapped.quadX0(), mapped.quadY0(), mapped.quadX1(), mapped.quadY1(), FACE_EPSILON,
                mapped.u0(), mapped.v0(), mapped.u1(), mapped.v1(),
                vertexColor[0], vertexColor[1], vertexColor[2], vertexColor[3],
                packedLight, packedOverlay);
    }

    private static void drawFacingQuad(VertexConsumer consumer, PoseStack.Pose pose, Direction facing,
                                       float x0, float y0, float x1, float y1, float epsilon,
                                       float u0, float v0, float u1, float v1,
                                       int red, int green, int blue, int alpha,
                                       int light, int overlay) {
        var matrix = pose.pose();
        var normalMatrix = pose.normal();

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
                putVertex(consumer, matrix, normalMatrix, offset, y0, x0, u0, v1, red, green, blue, alpha, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, offset, y0, x1, u1, v1, red, green, blue, alpha, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, offset, y1, x1, u1, v0, red, green, blue, alpha, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, offset, y1, x0, u0, v0, red, green, blue, alpha, nx, ny, nz, light, overlay);
            }
            case Y -> {
                putVertex(consumer, matrix, normalMatrix, x0, offset, y0, u0, v1, red, green, blue, alpha, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, x1, offset, y0, u1, v1, red, green, blue, alpha, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, x1, offset, y1, u1, v0, red, green, blue, alpha, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, x0, offset, y1, u0, v0, red, green, blue, alpha, nx, ny, nz, light, overlay);
            }
            case Z -> {
                putVertex(consumer, matrix, normalMatrix, x0, y0, offset, u0, v1, red, green, blue, alpha, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, x1, y0, offset, u1, v1, red, green, blue, alpha, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, x1, y1, offset, u1, v0, red, green, blue, alpha, nx, ny, nz, light, overlay);
                putVertex(consumer, matrix, normalMatrix, x0, y1, offset, u0, v0, red, green, blue, alpha, nx, ny, nz, light, overlay);
            }
        }
    }

    private static void putVertex(VertexConsumer consumer, org.joml.Matrix4f matrix, org.joml.Matrix3f normalMatrix,
                                  float x, float y, float z,
                                  float u, float v,
                                  int red, int green, int blue, int alpha,
                                  float nx, float ny, float nz,
                                  int light, int overlay) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMatrix, nx, ny, nz)
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(LedScreenBlockEntity blockEntity) {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}

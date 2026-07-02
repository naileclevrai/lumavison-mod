package fr.lumavision.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.lumavision.entity.CameraSeatEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** The camera seat is invisible — the player just sits on the boom. Renders nothing. */
@OnlyIn(Dist.CLIENT)
public class SeatRenderer extends EntityRenderer<CameraSeatEntity> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("lumavision", "textures/block/camera_mount.png");

    public SeatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CameraSeatEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // no-op: invisible seat
    }

    @Override
    public ResourceLocation getTextureLocation(CameraSeatEntity entity) {
        return TEXTURE;
    }
}

package fr.lumavision.client.render;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Geometry for the animated camera crane: a pedestal base, a turntable that swings (yaw), and an arm
 * that booms up/down (pitch) with a camera head on the far end and a counterweight on the near end.
 * The arm reaches ~5 blocks (80px) forward, matching {@link fr.lumavision.camera.CameraRig}.
 */
@OnlyIn(Dist.CLIENT)
public final class CameraCraneModel {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("lumavision", "camera_crane"), "main");

    private CameraCraneModel() {
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -6.0F, -6.0F, 12.0F, 6.0F, 12.0F),
                PartPose.ZERO);

        PartDefinition turntable = root.addOrReplaceChild("turntable",
                CubeListBuilder.create().texOffs(0, 32).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, -6.0F, 0.0F));

        PartDefinition arm = turntable.addOrReplaceChild("arm",
                CubeListBuilder.create().texOffs(0, 64).addBox(-2.0F, -2.0F, -24.0F, 4.0F, 4.0F, 104.0F),
                PartPose.offset(0.0F, -3.0F, 0.0F));

        arm.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 90).addBox(-4.0F, -4.0F, 74.0F, 8.0F, 8.0F, 8.0F),
                PartPose.ZERO);

        arm.addOrReplaceChild("counterweight",
                CubeListBuilder.create().texOffs(0, 120).addBox(-5.0F, -5.0F, -34.0F, 10.0F, 10.0F, 10.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 256, 256);
    }
}

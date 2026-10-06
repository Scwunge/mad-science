package io.github.scwunge.madscience.client.model.entity;

import io.github.scwunge.madscience.content.entity.AbominationEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** The original AbominationMobModel: a long, flat, eight-legged body with spider leg motion. */
public class AbominationModel extends HierarchicalModel<AbominationEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart[] legs = new ModelPart[8];

    public AbominationModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        for (int i = 0; i < 8; i++) {
            legs[i] = root.getChild("leg" + (i + 1));
        }
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p = mesh.getRoot();
        p.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 17).addBox(-2F, -3F, -4F, 4, 3, 12),
                PartPose.offsetAndRotation(0F, 19F, -3F, 0.4886922F, 0F, 0F));
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 7).addBox(-3.466667F, -3F, -3F, 7, 3, 6), PartPose.offset(0F, 21F, 0F));
        p.addOrReplaceChild("rear_end", CubeListBuilder.create().texOffs(0, 17).addBox(-1.5F, -2F, -7F, 3, 2, 13),
                PartPose.offsetAndRotation(0F, 21F, 9F, -0.0872665F, 0F, 0F));
        p.addOrReplaceChild("leg8", CubeListBuilder.create().texOffs(34, 14).addBox(-1F, -1F, -1F, 11, 1, 1), PartPose.offset(4F, 20F, -1F));
        p.addOrReplaceChild("leg6", CubeListBuilder.create().texOffs(34, 10).addBox(-1F, -1F, -1F, 9, 1, 1), PartPose.offset(4F, 20F, 0F));
        p.addOrReplaceChild("leg4", CubeListBuilder.create().texOffs(34, 6).addBox(-1F, -1F, -1F, 4, 1, 1), PartPose.offset(2F, 18F, 4F));
        p.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(34, 2).addBox(-1F, -1F, -1F, 6, 1, 1), PartPose.offset(2F, 18F, 2F));
        p.addOrReplaceChild("leg7", CubeListBuilder.create().texOffs(34, 12).addBox(-10F, -1F, -1F, 11, 1, 1), PartPose.offset(-4F, 20F, -1F));
        p.addOrReplaceChild("leg5", CubeListBuilder.create().texOffs(34, 8).addBox(-8F, -1F, -1F, 9, 1, 1), PartPose.offset(-4F, 20F, 0F));
        p.addOrReplaceChild("leg3", CubeListBuilder.create().texOffs(34, 4).addBox(-3F, -1F, -1F, 4, 1, 1), PartPose.offset(-2F, 18F, 4F));
        p.addOrReplaceChild("leg1", CubeListBuilder.create().texOffs(34, 0).addBox(-5F, -1F, -1F, 6, 1, 1), PartPose.offset(-2F, 18F, 2F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(AbominationEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float quarter = Mth.PI / 4F;
        float[] baseZ = {-quarter, quarter, -quarter * 0.74F, quarter * 0.74F, -quarter * 0.74F, quarter * 0.74F, -quarter, quarter};
        float eighth = 0.3926991F;
        float[] baseY = {eighth * 2F, -eighth * 2F, eighth, -eighth, -eighth, eighth, -eighth * 2F, eighth * 2F};
        float swingY0 = -(Mth.cos(limbSwing * 0.6662F * 2F) * 0.4F) * limbSwingAmount;
        float swingY1 = -(Mth.cos(limbSwing * 0.6662F * 2F + Mth.PI) * 0.4F) * limbSwingAmount;
        float swingY2 = -(Mth.cos(limbSwing * 0.6662F * 2F + Mth.HALF_PI) * 0.4F) * limbSwingAmount;
        float swingY3 = -(Mth.cos(limbSwing * 0.6662F * 2F + Mth.PI * 3F / 2F) * 0.4F) * limbSwingAmount;
        float liftZ0 = Math.abs(Mth.sin(limbSwing * 0.6662F) * 0.4F) * limbSwingAmount;
        float liftZ1 = Math.abs(Mth.sin(limbSwing * 0.6662F + Mth.PI) * 0.4F) * limbSwingAmount;
        float liftZ2 = Math.abs(Mth.sin(limbSwing * 0.6662F + Mth.HALF_PI) * 0.4F) * limbSwingAmount;
        float liftZ3 = Math.abs(Mth.sin(limbSwing * 0.6662F + Mth.PI * 3F / 2F) * 0.4F) * limbSwingAmount;
        float[] swingY = {swingY0, -swingY0, swingY1, -swingY1, swingY2, -swingY2, swingY3, -swingY3};
        float[] liftZ = {liftZ0, -liftZ0, liftZ1, -liftZ1, liftZ2, -liftZ2, liftZ3, -liftZ3};
        for (int i = 0; i < 8; i++) {
            legs[i].xRot = 0F;
            legs[i].yRot = baseY[i] + swingY[i];
            legs[i].zRot = baseZ[i] + liftZ[i];
        }
    }
}

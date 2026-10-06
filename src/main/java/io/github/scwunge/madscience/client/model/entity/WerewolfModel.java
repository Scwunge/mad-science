package io.github.scwunge.madscience.client.model.entity;

import io.github.scwunge.madscience.content.entity.WerewolfEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** The original WerewolfMobModel: a wolf-headed biped with a tail. */
public class WerewolfModel extends HierarchicalModel<WerewolfEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart nose;
    private final ModelPart ear1;
    private final ModelPart ear2;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public WerewolfModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("wolfhead");
        this.nose = root.getChild("nose");
        this.ear1 = root.getChild("ear1");
        this.ear2 = root.getChild("ear2");
        this.rightArm = root.getChild("rightarm");
        this.leftArm = root.getChild("leftarm");
        this.rightLeg = root.getChild("rightleg");
        this.leftLeg = root.getChild("leftleg");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p = mesh.getRoot();
        p.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 2, 2, 7),
                PartPose.offsetAndRotation(-1F, 8F, 2F, -0.6320364F, 0.0371786F, 0F));
        p.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(48, 0).addBox(-2F, 0F, -5F, 3, 3, 5), PartPose.offset(0.5F, -3.7F, 0F));
        p.addOrReplaceChild("wolfhead", CubeListBuilder.create().texOffs(22, 0).addBox(-3F, -3F, -2F, 8, 8, 5), PartPose.offset(-1F, -4.7F, 0F));
        p.addOrReplaceChild("ear2", CubeListBuilder.create().texOffs(56, 8).addBox(1F, -5F, 0F, 2, 3, 2), PartPose.offset(0.9F, -4.7F, 0F));
        p.addOrReplaceChild("ear1", CubeListBuilder.create().texOffs(56, 8).addBox(-3F, -5F, 0F, 2, 3, 2), PartPose.offset(-0.9F, -4.7F, 0F));
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(24, 16).addBox(-4F, 0F, -2F, 8, 12, 4), PartPose.ZERO);
        p.addOrReplaceChild("rightarm", CubeListBuilder.create().texOffs(48, 16).addBox(-3F, -2F, -2F, 4, 12, 4), PartPose.offset(-5F, 2F, 0F));
        p.addOrReplaceChild("leftarm", CubeListBuilder.create().texOffs(48, 16).addBox(-1F, -2F, -2F, 4, 12, 4), PartPose.offset(5F, 2F, 0F));
        p.addOrReplaceChild("rightleg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4, 12, 4), PartPose.offset(-2F, 12F, 0F));
        p.addOrReplaceChild("leftleg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4, 12, 4), PartPose.offset(2F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(WerewolfEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float pitch = headPitch * Mth.DEG_TO_RAD;
        float yaw = netHeadYaw * Mth.DEG_TO_RAD;
        for (ModelPart part : new ModelPart[]{head, nose, ear1, ear2}) {
            part.xRot = pitch;
            part.yRot = yaw;
        }
        leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        rightArm.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
        leftLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        rightLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
    }
}

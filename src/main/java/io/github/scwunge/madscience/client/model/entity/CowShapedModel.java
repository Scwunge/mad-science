package io.github.scwunge.madscience.client.model.entity;

import com.google.common.collect.ImmutableList;
import io.github.scwunge.madscience.content.entity.WoolyCowEntity;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * The original's cow-shaped GMO models (Creeper Cow, Wooly Cow and its fleece): the vanilla cow body, plus the extra
 * parts the originals added (a separate udder, and the Creeper Cow's long horns). For the Wooly Cow it also does the
 * sheep's grazing head animation.
 */
public class CowShapedModel<T extends LivingEntity> extends QuadrupedModel<T> {
    private final List<ModelPart> extras = new ArrayList<>();

    public CowShapedModel(ModelPart root) {
        super(root, false, 10.0F, 4.0F, 2.0F, 2.0F, 24);
        for (String name : new String[]{"udders", "horn1", "horn2"}) {
            if (root.hasChild(name)) {
                extras.add(root.getChild(name));
            }
        }
    }

    /**
     * @param headGrow   inflation of the head (the fleece layer uses 0.1)
     * @param bodyGrow   inflation of the body (the fleece layer uses 1.75)
     * @param udders     add the original's separate udder part
     * @param horns      add the Creeper Cow's horns
     */
    public static LayerDefinition create(float headGrow, float bodyGrow, boolean udders, boolean horns) {
        MeshDefinition mesh = QuadrupedModel.createBodyMesh(12, CubeDeformation.NONE);
        PartDefinition p = mesh.getRoot();
        p.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -6.0F, 8, 8, 6, new CubeDeformation(headGrow))
                        .texOffs(22, 0).addBox("right_horn", -5.0F, -5.0F, -4.0F, 1, 3, 1)
                        .texOffs(22, 0).addBox("left_horn", 4.0F, -5.0F, -4.0F, 1, 3, 1),
                PartPose.offset(0.0F, 4.0F, -8.0F));
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(18, 4).addBox(-6.0F, -10.0F, -7.0F, 12, 18, 10, new CubeDeformation(bodyGrow))
                        .texOffs(52, 0).addBox(-2.0F, 2.0F, -8.0F, 4, 6, 1),
                PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12, 4.0F);
        p.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-4.0F, 12.0F, 7.0F));
        p.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(4.0F, 12.0F, 7.0F));
        p.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-4.0F, 12.0F, -6.0F));
        p.addOrReplaceChild("left_front_leg", leg, PartPose.offset(4.0F, 12.0F, -6.0F));
        if (udders) {
            p.addOrReplaceChild("udders", CubeListBuilder.create().texOffs(52, 0).addBox(-2F, -3F, 0F, 4, 6, 2),
                    PartPose.offsetAndRotation(0F, 14F, 6F, 1.570796F, 0F, 0F));
        }
        if (horns) {
            p.addOrReplaceChild("horn1", CubeListBuilder.create().texOffs(53, 8).addBox(-4F, -6F, -4F, 1, 4, 1), PartPose.offset(0F, 3F, -7F));
            p.addOrReplaceChild("horn2", CubeListBuilder.create().texOffs(53, 8).addBox(3F, -6F, -4F, 1, 4, 1), PartPose.offset(0F, 3F, -7F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return ImmutableList.<ModelPart>builder().addAll(super.bodyParts()).addAll(extras).build();
    }

    private float grazeAngle;

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        if (entity instanceof WoolyCowEntity cow) {
            head.y = 6.0F + cow.getHeadEatPositionScale(partialTick) * 9.0F;
            grazeAngle = cow.getHeadEatAngleScale(partialTick);
        }
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity instanceof WoolyCowEntity) {
            head.xRot = grazeAngle;
        }
    }
}

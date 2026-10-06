package io.github.scwunge.madscience.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/** Draws machine items with their block models, like the original's item renderers. */
public class MachineItemRenderer extends BlockEntityWithoutLevelRenderer {
    private record Entry(ModelLayerLocation layer, ResourceLocation texture, float scale, Predicate<String> hidden) {
    }

    private final Map<Item, Entry> entries = new HashMap<>();
    private final Map<Item, ModelPart> models = new HashMap<>();

    public MachineItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public void add(Item item, ModelLayerLocation layer, ResourceLocation texture, float scale, Predicate<String> hidden) {
        entries.put(item, new Entry(layer, texture, scale, hidden));
    }

    private static ModelPart bake(Entry entry) {
        ModelPart model = Minecraft.getInstance().getEntityModels().bakeLayer(entry.layer());
        for (Map.Entry<String, ModelPart> child : model.children.entrySet()) {
            child.getValue().visible = !entry.hidden().test(child.getKey());
        }
        return model;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        models.clear();
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Entry entry = entries.get(stack.getItem());
        if (entry == null) {
            return;
        }
        ModelPart model = models.computeIfAbsent(stack.getItem(), item -> bake(entry));
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.scale(entry.scale(), entry.scale(), entry.scale());
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.scale(-1.0F, -1.0F, 1.0F);
        model.render(pose, buffers.getBuffer(RenderType.entityCutout(entry.texture())), light, overlay);
        pose.popPose();
    }
}

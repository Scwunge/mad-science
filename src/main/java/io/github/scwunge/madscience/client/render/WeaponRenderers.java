package io.github.scwunge.madscience.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.client.model.PulseRifleCounterModel;
import io.github.scwunge.madscience.client.model.PulseRifleFlashModel;
import io.github.scwunge.madscience.client.model.PulseRifleGrenadeModel;
import io.github.scwunge.madscience.client.model.PulseRifleMagazineModel;
import io.github.scwunge.madscience.client.model.PulseRifleModel;
import io.github.scwunge.madscience.client.model.PulseRifleRoundModel;
import io.github.scwunge.madscience.client.model.RifleComponentBarrelModel;
import io.github.scwunge.madscience.client.model.RifleComponentBoltModel;
import io.github.scwunge.madscience.client.model.RifleComponentBulletCasingModel;
import io.github.scwunge.madscience.client.model.RifleComponentGrenadeCasingModel;
import io.github.scwunge.madscience.client.model.RifleComponentReceiverModel;
import io.github.scwunge.madscience.client.model.RifleComponentTriggerModel;
import io.github.scwunge.madscience.content.weapon.PulseRifleItem;
import io.github.scwunge.madscience.content.weapon.RifleState;
import io.github.scwunge.madscience.registry.ModEntities;
import io.github.scwunge.madscience.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The pulse rifle family drawn with the original Techne models: the rifle (with its two-digit ammo counter, cycling
 * bolt, muzzle flash and launcher pump), magazine, round, grenade and the rifle parts, plus the flying round and grenade.
 * In inventories the items use their original flat icons instead (see the item models).
 */
public final class WeaponRenderers {
    private static final ResourceLocation RIFLE_TEXTURE = texture("rifle");
    private static final ResourceLocation[] DIGITS = new ResourceLocation[10];
    private static final ResourceLocation FLASH_12 = texture("flash_12");
    private static final ResourceLocation FLASH_34 = texture("flash_34");

    static {
        for (int d = 0; d < 10; d++) {
            DIGITS[d] = texture("counter_" + d);
        }
    }

    /**
     * A model for an item: its layer, texture, the centre of the model (in model pixels) and how many model pixels
     * make one block, so every item fills its slot of the item space the same way.
     */
    private record Shape(ModelLayerLocation layer, Supplier<LayerDefinition> definition, ResourceLocation texture,
                         float cx, float cy, float cz, float pixelsPerBlock) {
    }

    private static final Shape RIFLE = shape("pulse_rifle", PulseRifleModel::create, RIFLE_TEXTURE, 0, 14, 12, 252);
    private static final ModelLayerLocation COUNTER = layer("pulse_rifle_counter");
    private static final ModelLayerLocation FLASH = layer("pulse_rifle_flash");
    private static final Shape MAGAZINE = shape("pulse_rifle_magazine", PulseRifleMagazineModel::create, texture("magazine"), 0, 29.5F, 27, 64);
    private static final Shape ROUND = shape("pulse_rifle_round", PulseRifleRoundModel::create, texture("round"), 0, 0, 7.5F, 24);
    private static final Shape GRENADE = shape("pulse_rifle_grenade", PulseRifleGrenadeModel::create, texture("grenade"), 0, 0, 10, 28);
    private static final Shape BARREL = shape("component_pulse_rifle_barrel", RifleComponentBarrelModel::create, texture("components"), 0, -3.5F, 2.5F, 84);
    private static final Shape BOLT = shape("component_pulse_rifle_bolt", RifleComponentBoltModel::create, texture("components"), 0, 18.5F, 3, 78);
    private static final Shape RECEIVER = shape("component_pulse_rifle_receiver", RifleComponentReceiverModel::create, texture("components"), 0, -2, -2, 56);
    private static final Shape TRIGGER = shape("component_pulse_rifle_trigger", RifleComponentTriggerModel::create, texture("components"), 0, -11, 5.5F, 74);
    private static final Shape BULLET_CASING = shape("component_pulse_rifle_bullet_casing", RifleComponentBulletCasingModel::create, texture("bullet_casing"), 0, 0, 10.5F, 24);
    private static final Shape GRENADE_CASING = shape("component_pulse_rifle_grenade_casing", RifleComponentGrenadeCasingModel::create, texture("grenade_casing"), 0, 0, 12, 28);

    private WeaponRenderers() {
    }

    private static ResourceLocation texture(String name) {
        return MadScience.id("textures/model/pulse_rifle/" + name + ".png");
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(MadScience.id(name), "main");
    }

    private static Shape shape(String name, Supplier<LayerDefinition> definition, ResourceLocation texture, float cx, float cy, float cz, float pixelsPerBlock) {
        return new Shape(layer(name), definition, texture, cx, cy, cz, pixelsPerBlock);
    }

    private static Shape[] shapes() {
        return new Shape[]{RIFLE, MAGAZINE, ROUND, GRENADE, BARREL, BOLT, RECEIVER, TRIGGER, BULLET_CASING, GRENADE_CASING};
    }

    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (Shape shape : shapes()) {
            event.registerLayerDefinition(shape.layer(), shape.definition());
        }
        event.registerLayerDefinition(COUNTER, PulseRifleCounterModel::create);
        event.registerLayerDefinition(FLASH, PulseRifleFlashModel::create);
    }

    public static void entities(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PULSE_RIFLE_ROUND.get(), ctx -> new ProjectileRenderer<>(ctx, ROUND, 0.3F));
        event.registerEntityRenderer(ModEntities.PULSE_RIFLE_GRENADE.get(), ctx -> new ProjectileRenderer<>(ctx, GRENADE, 0.4F));
    }

    /** Items drawn by {@link #itemRenderer()}. */
    public static Item[] items() {
        return new Item[]{ModItems.PULSE_RIFLE.get(), ModItems.MAGAZINE.get(), ModItems.ROUND.get(), ModItems.GRENADE.get(),
                ModItems.RIFLE_BARREL.get(), ModItems.RIFLE_BOLT.get(), ModItems.RIFLE_RECEIVER.get(), ModItems.RIFLE_TRIGGER.get(),
                ModItems.BULLET_CASING.get(), ModItems.GRENADE_CASING.get()};
    }

    private static ItemRenderer itemRenderer;

    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer == null) {
            itemRenderer = new ItemRenderer();
        }
        return itemRenderer;
    }

    /** Centres a shape on the origin, a block long, in the original's flipped Techne space. */
    private static void fit(PoseStack pose, Shape shape, float size) {
        float scale = size * 16.0F / shape.pixelsPerBlock();
        pose.scale(scale, scale, scale);
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(-shape.cx() / 16.0F, -shape.cy() / 16.0F, -shape.cz() / 16.0F);
    }

    private static void renderParts(ModelPart model, String[] names, String prefix, PoseStack pose, VertexConsumer consumer, int light, int overlay) {
        for (String name : names) {
            if (name.startsWith(prefix)) {
                model.getChild(name).render(pose, consumer, light, overlay);
            }
        }
    }

    static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        private final Map<Item, Shape> shapes = new LinkedHashMap<>();
        private final Map<ModelLayerLocation, ModelPart> models = new LinkedHashMap<>();

        ItemRenderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
            Item[] items = items();
            Shape[] all = shapes();
            for (int i = 0; i < items.length; i++) {
                shapes.put(items[i], all[i]);
            }
        }

        private ModelPart model(ModelLayerLocation layer) {
            return models.computeIfAbsent(layer, l -> Minecraft.getInstance().getEntityModels().bakeLayer(l));
        }

        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {
            models.clear();
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            Shape shape = shapes.get(stack.getItem());
            if (shape == null) {
                return;
            }
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            fit(pose, shape, 1.0F);
            if (shape == RIFLE) {
                renderRifle(stack, context, pose, buffers, light, overlay);
            } else {
                model(shape.layer()).render(pose, buffers.getBuffer(RenderType.entityCutout(shape.texture())), light, overlay);
            }
            pose.popPose();
        }

        private void renderRifle(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            RifleState state = PulseRifleItem.state(stack);
            long time = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
            boolean held = context != ItemDisplayContext.GROUND && context != ItemDisplayContext.FIXED && context != ItemDisplayContext.GUI;
            boolean firingRounds = held && state.firing() && !state.grenadeMode() && state.rounds() > 0;
            boolean pumping = held && state.firing() && state.grenadeMode() && state.grenades() > 0;
            boolean boltBack = firingRounds && time % 2 == 0;

            ModelPart rifle = model(RIFLE.layer());
            rifle.getChild("bolt").visible = !boltBack;
            rifle.getChild("boltback").visible = boltBack;
            rifle.getChild("pumpfront").visible = !pumping;
            rifle.getChild("pumpback").visible = !pumping;
            rifle.getChild("pumpfrontback").visible = pumping;
            rifle.getChild("pumpbackback").visible = pumping;
            rifle.render(pose, buffers.getBuffer(RenderType.entityCutout(RIFLE_TEXTURE)), light, overlay);

            // the counter shows rounds, or grenades in launcher mode, lit like the original (lighting off)
            int count = Mth.clamp(state.grenadeMode() ? state.grenades() : state.rounds(), 0, 99);
            ModelPart counter = model(COUNTER);
            String[] counterParts = PulseRifleCounterModel.PARTS;
            renderParts(counter, counterParts, "counter", pose, buffers.getBuffer(RenderType.entityCutout(DIGITS[count / 10])), LightTexture.FULL_BRIGHT, overlay);
            renderParts(counter, counterParts, "r_", pose, buffers.getBuffer(RenderType.entityCutout(DIGITS[count % 10])), LightTexture.FULL_BRIGHT, overlay);

            if (boltBack) {
                int flash = (int) ((time * 7 + stack.hashCode()) % 5 + 5) % 5;
                ResourceLocation texture = flash == 0 ? RIFLE_TEXTURE : flash <= 2 ? FLASH_12 : FLASH_34;
                ModelPart flashes = model(FLASH);
                VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(texture));
                for (String name : PulseRifleFlashModel.PARTS) {
                    if (name.startsWith(flash == 0 ? "flash" : "f" + flash + "_")) {
                        flashes.getChild(name).render(pose, consumer, LightTexture.FULL_BRIGHT, overlay);
                    }
                }
            }
        }
    }

    /** A flying round or grenade, nose first along its path. */
    static final class ProjectileRenderer<T extends AbstractArrow> extends EntityRenderer<T> {
        private final Shape shape;
        private final float size;
        private final ModelPart model;

        ProjectileRenderer(EntityRendererProvider.Context context, Shape shape, float size) {
            super(context);
            this.shape = shape;
            this.size = size;
            this.model = context.bakeLayer(shape.layer());
        }

        @Override
        public void render(T entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) + 180.0F));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
            fit(pose, shape, size);
            model.render(pose, buffers.getBuffer(RenderType.entityCutout(shape.texture())), light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
            super.render(entity, entityYaw, partialTick, pose, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return shape.texture();
        }
    }
}

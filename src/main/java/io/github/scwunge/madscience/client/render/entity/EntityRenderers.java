package io.github.scwunge.madscience.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.client.model.entity.AbominationModel;
import io.github.scwunge.madscience.client.model.entity.CowShapedModel;
import io.github.scwunge.madscience.client.model.entity.WerewolfModel;
import io.github.scwunge.madscience.content.entity.AbominationEntity;
import io.github.scwunge.madscience.content.entity.CreeperCowEntity;
import io.github.scwunge.madscience.content.entity.WerewolfEntity;
import io.github.scwunge.madscience.content.entity.WoolyCowEntity;
import io.github.scwunge.madscience.registry.ModEntities;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.EnderEyesLayer;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.animal.Sheep;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderers and model layers for the genetically modified creatures, using the original textures. */
@EventBusSubscriber(modid = MadScience.MODID, value = Dist.CLIENT)
public final class EntityRenderers {
    public static final ModelLayerLocation WEREWOLF = layer("werewolf");
    public static final ModelLayerLocation ABOMINATION = layer("abomination");
    public static final ModelLayerLocation CREEPER_COW = layer("creeper_cow");
    public static final ModelLayerLocation CREEPER_COW_POWER = new ModelLayerLocation(MadScience.id("creeper_cow"), "power");
    public static final ModelLayerLocation WOOLY_COW = layer("wooly_cow");
    public static final ModelLayerLocation WOOLY_COW_FUR = new ModelLayerLocation(MadScience.id("wooly_cow"), "fur");

    private EntityRenderers() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(MadScience.id(name), "main");
    }

    static ResourceLocation texture(String mob, String file) {
        return MadScience.id("textures/entity/" + mob + "/" + file + ".png");
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WEREWOLF, WerewolfModel::create);
        event.registerLayerDefinition(ABOMINATION, AbominationModel::create);
        event.registerLayerDefinition(CREEPER_COW, () -> CowShapedModel.create(0F, 0F, true, true));
        event.registerLayerDefinition(CREEPER_COW_POWER, () -> CowShapedModel.create(0.5F, 0.5F, true, true));
        event.registerLayerDefinition(WOOLY_COW, () -> CowShapedModel.create(0F, 0F, true, false));
        event.registerLayerDefinition(WOOLY_COW_FUR, () -> CowShapedModel.create(0.1F, 1.75F, false, false));
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WEREWOLF.get(), ctx -> simple(ctx, new WerewolfModel(ctx.bakeLayer(WEREWOLF)), texture("werewolf", "werewolf"), 0.5F));
        event.registerEntityRenderer(ModEntities.ABOMINATION.get(), ctx -> simple(ctx, new AbominationModel(ctx.bakeLayer(ABOMINATION)), texture("abomination", "abomination"), 0.8F));
        event.registerEntityRenderer(ModEntities.CREEPER_COW.get(), CreeperCowRenderer::new);
        event.registerEntityRenderer(ModEntities.WOOLY_COW.get(), WoolyCowRenderer::new);
        event.registerEntityRenderer(ModEntities.ENDERSLIME.get(), ctx -> new TexturedSlimeRenderer(ctx, texture("enderslime", "enderslime")));
        event.registerEntityRenderer(ModEntities.SHOGGOTH.get(), ctx -> new TexturedSlimeRenderer(ctx, texture("shoggoth", "shoggoth")));
        event.registerEntityRenderer(ModEntities.ENDER_SQUID.get(), EnderSquidRenderer::new);
    }

    private static <T extends net.minecraft.world.entity.Mob, M extends EntityModel<T>> MobRenderer<T, M> simple(
            EntityRendererProvider.Context ctx, M model, ResourceLocation texture, float shadow) {
        return new MobRenderer<>(ctx, model, shadow) {
            @Override
            public ResourceLocation getTextureLocation(T entity) {
                return texture;
            }
        };
    }

    /** Vanilla slime rendering with our texture (Enderslime, Shoggoth). */
    static class TexturedSlimeRenderer extends SlimeRenderer {
        private final ResourceLocation texture;

        TexturedSlimeRenderer(EntityRendererProvider.Context ctx, ResourceLocation texture) {
            super(ctx);
            this.texture = texture;
        }

        @Override
        public ResourceLocation getTextureLocation(Slime entity) {
            return texture;
        }
    }

    /** Enderman rendering (shaking when angry, glowing eyes) with the Ender Squid's textures. */
    static class EnderSquidRenderer extends EndermanRenderer {
        private static final ResourceLocation TEXTURE = texture("ender_squid", "ender_squid");
        private static final RenderType EYES = RenderType.eyes(texture("ender_squid", "ender_squid_eyes"));

        EnderSquidRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
            layers.removeIf(layer -> layer instanceof EnderEyesLayer);
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return EYES;
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(EnderMan entity) {
            return TEXTURE;
        }
    }

    /** Creeper-style swelling and white flash before it blows, and the charged swirl after a lightning strike. */
    static class CreeperCowRenderer extends MobRenderer<CreeperCowEntity, CowShapedModel<CreeperCowEntity>> {
        private static final ResourceLocation TEXTURE = texture("creeper_cow", "creeper_cow");
        private static final ResourceLocation POWER = texture("creeper_cow", "creeper_cow_armor");

        CreeperCowRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new CowShapedModel<>(ctx.bakeLayer(CREEPER_COW)), 0.7F);
            CowShapedModel<CreeperCowEntity> powerModel = new CowShapedModel<>(ctx.bakeLayer(CREEPER_COW_POWER));
            addLayer(new EnergySwirlLayer<>(this) {
                @Override
                protected float xOffset(float tickCount) {
                    return tickCount * 0.01F;
                }

                @Override
                protected ResourceLocation getTextureLocation() {
                    return POWER;
                }

                @Override
                protected EntityModel<CreeperCowEntity> model() {
                    return powerModel;
                }
            });
        }

        @Override
        protected void scale(CreeperCowEntity cow, PoseStack pose, float partialTick) {
            float swell = cow.getSwelling(partialTick);
            float wobble = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;
            swell = Mth.clamp(swell, 0.0F, 1.0F);
            swell *= swell;
            swell *= swell;
            float wide = (1.0F + swell * 0.4F) * wobble;
            float tall = (1.0F + swell * 0.1F) / wobble;
            pose.scale(wide, tall, wide);
        }

        @Override
        protected float getWhiteOverlayProgress(CreeperCowEntity cow, float partialTick) {
            float swell = cow.getSwelling(partialTick);
            return (int) (swell * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(swell, 0.5F, 1.0F);
        }

        @Override
        public ResourceLocation getTextureLocation(CreeperCowEntity entity) {
            return TEXTURE;
        }
    }

    /** Cow body, plus the fleece tinted by colour (the original's sheep colour table) until it is sheared. */
    static class WoolyCowRenderer extends MobRenderer<WoolyCowEntity, CowShapedModel<WoolyCowEntity>> {
        private static final ResourceLocation TEXTURE = texture("wooly_cow", "wooly_cow");
        private static final ResourceLocation FUR = texture("wooly_cow", "wooly_cow_fur");

        WoolyCowRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new CowShapedModel<>(ctx.bakeLayer(WOOLY_COW)), 0.7F);
            CowShapedModel<WoolyCowEntity> fur = new CowShapedModel<>(ctx.bakeLayer(WOOLY_COW_FUR));
            addLayer(new FurLayer(this, fur));
        }

        @Override
        public ResourceLocation getTextureLocation(WoolyCowEntity entity) {
            return TEXTURE;
        }

        private static class FurLayer extends RenderLayer<WoolyCowEntity, CowShapedModel<WoolyCowEntity>> {
            private final CowShapedModel<WoolyCowEntity> model;

            FurLayer(RenderLayerParent<WoolyCowEntity, CowShapedModel<WoolyCowEntity>> parent, CowShapedModel<WoolyCowEntity> model) {
                super(parent);
                this.model = model;
            }

            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, WoolyCowEntity cow, float limbSwing, float limbSwingAmount,
                               float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                if (cow.isSheared() || cow.isInvisible()) {
                    return;
                }
                int color = Sheep.getColor(cow.getColor());
                getParentModel().copyPropertiesTo(model);
                model.prepareMobModel(cow, limbSwing, limbSwingAmount, partialTick);
                model.setupAnim(cow, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(FUR));
                model.renderToBuffer(pose, buffer, light, LivingEntityRendererOverlay.of(cow), FastColor.ARGB32.opaque(color));
            }
        }
    }

    /** Hurt-flash overlay for layers that draw their own model. */
    static final class LivingEntityRendererOverlay {
        private LivingEntityRendererOverlay() {
        }

        static int of(net.minecraft.world.entity.LivingEntity entity) {
            return OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
        }
    }
}

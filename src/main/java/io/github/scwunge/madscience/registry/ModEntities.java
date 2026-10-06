package io.github.scwunge.madscience.registry;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.Gmo;
import io.github.scwunge.madscience.content.entity.AbominationEntity;
import io.github.scwunge.madscience.content.entity.CreeperCowEntity;
import io.github.scwunge.madscience.content.entity.EnderSquidEntity;
import io.github.scwunge.madscience.content.entity.EnderslimeEntity;
import io.github.scwunge.madscience.content.entity.ShoggothEntity;
import io.github.scwunge.madscience.content.entity.WerewolfEntity;
import io.github.scwunge.madscience.content.entity.WoolyCowEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** The genetically modified creatures and their spawn eggs (the Incubator's output). */
@EventBusSubscriber(modid = MadScience.MODID)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(Registries.ENTITY_TYPE, MadScience.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<WerewolfEntity>> WEREWOLF = register("werewolf",
            EntityType.Builder.of(WerewolfEntity::new, MobCategory.MONSTER).sized(1.5F, 2.0F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<CreeperCowEntity>> CREEPER_COW = register("creeper_cow",
            EntityType.Builder.of(CreeperCowEntity::new, MobCategory.MONSTER).sized(0.9F, 1.3F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<EnderslimeEntity>> ENDERSLIME = register("enderslime",
            EntityType.Builder.of(EnderslimeEntity::new, MobCategory.MONSTER).sized(0.52F, 0.52F).eyeHeight(0.325F).fireImmune().clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<WoolyCowEntity>> WOOLY_COW = register("wooly_cow",
            EntityType.Builder.of(WoolyCowEntity::new, MobCategory.CREATURE).sized(0.9F, 1.3F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<ShoggothEntity>> SHOGGOTH = register("shoggoth",
            EntityType.Builder.of(ShoggothEntity::new, MobCategory.MONSTER).sized(0.52F, 0.52F).eyeHeight(0.325F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<AbominationEntity>> ABOMINATION = register("abomination",
            EntityType.Builder.of(AbominationEntity::new, MobCategory.MONSTER).sized(1.4F, 0.9F).eyeHeight(0.65F).fireImmune().clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<EnderSquidEntity>> ENDER_SQUID = register("ender_squid",
            EntityType.Builder.of(EnderSquidEntity::new, MobCategory.MONSTER).sized(0.6F, 2.9F).eyeHeight(2.55F).clientTrackingRange(8));

    public static final DeferredItem<DeferredSpawnEggItem> WEREWOLF_EGG = egg("werewolf", WEREWOLF, Gmo.WEREWOLF);
    public static final DeferredItem<DeferredSpawnEggItem> CREEPER_COW_EGG = egg("creeper_cow", CREEPER_COW, Gmo.CREEPER_COW);
    public static final DeferredItem<DeferredSpawnEggItem> ENDERSLIME_EGG = egg("enderslime", ENDERSLIME, Gmo.ENDERSLIME);
    public static final DeferredItem<DeferredSpawnEggItem> WOOLY_COW_EGG = egg("wooly_cow", WOOLY_COW, Gmo.WOOLY_COW);
    public static final DeferredItem<DeferredSpawnEggItem> SHOGGOTH_EGG = egg("shoggoth", SHOGGOTH, Gmo.SHOGGOTH);
    public static final DeferredItem<DeferredSpawnEggItem> ABOMINATION_EGG = egg("abomination", ABOMINATION, Gmo.ABOMINATION);
    public static final DeferredItem<DeferredSpawnEggItem> ENDER_SQUID_EGG = egg("ender_squid", ENDER_SQUID, Gmo.ENDER_SQUID);

    private ModEntities() {
    }

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String name, EntityType.Builder<T> builder) {
        return REGISTER.register(name, () -> builder.build(MadScience.MODID + ":" + name));
    }

    private static DeferredItem<DeferredSpawnEggItem> egg(String name, Supplier<? extends EntityType<? extends Mob>> type, Gmo gmo) {
        DeferredItem<DeferredSpawnEggItem> item = ModItems.REGISTER.register(name + "_spawn_egg",
                () -> new DeferredSpawnEggItem(type, gmo.primaryColor(), gmo.secondaryColor(), new Item.Properties()));
        ModItems.TAB_ORDER.add(item);
        return item;
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(WEREWOLF.get(), WerewolfEntity.createAttributes().build());
        event.put(CREEPER_COW.get(), CreeperCowEntity.createAttributes().build());
        event.put(ENDERSLIME.get(), Monster.createMonsterAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 24.0).build());
        event.put(WOOLY_COW.get(), WoolyCowEntity.createAttributes().build());
        event.put(SHOGGOTH.get(), Monster.createMonsterAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 15.0).build());
        event.put(ABOMINATION.get(), AbominationEntity.createAttributes().build());
        event.put(ENDER_SQUID.get(), EnderSquidEntity.createAttributes().build());
    }
}

package io.github.scwunge.madscience.registry;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.fluid.MutantDnaBlock;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Liquid DNA and Liquid Mutant DNA, with the original's density 3, viscosity 4000 and light level 5. */
public final class ModFluids {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MadScience.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(net.minecraft.core.registries.Registries.FLUID, MadScience.MODID);

    public static final Kind LIQUID_DNA = new Kind("liquid_dna", LiquidBlock::new);
    public static final Kind MUTANT_DNA = new Kind("mutant_dna", MutantDnaBlock::new);

    private ModFluids() {
    }

    public static final class Kind {
        public final String name;
        public final DeferredHolder<FluidType, FluidType> type;
        public final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        public final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        public final DeferredBlock<LiquidBlock> block;
        public final DeferredItem<BucketItem> bucket;

        Kind(String name, BlockFactory blockFactory) {
            this.name = name;
            this.type = TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type." + MadScience.MODID + "." + name)
                    .density(3).viscosity(4000).lightLevel(5)
                    .canSwim(true).canDrown(true).canExtinguish(true).supportsBoating(false)));
            this.source = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties()));
            this.flowing = FLUIDS.register(name + "_flowing", () -> new BaseFlowingFluid.Flowing(properties()));
            this.block = ModBlocks.REGISTER.register(name, () -> blockFactory.create(source.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).replaceable().noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY)
                    .noLootTable().liquid().lightLevel(state -> 5)));
            this.bucket = ModItems.REGISTER.register(name + "_bucket", () -> new BucketItem(source.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
            ModItems.TAB_ORDER.add(bucket);
        }

        private BaseFlowingFluid.Properties properties() {
            return new BaseFlowingFluid.Properties(type, source, flowing).block(block).bucket(bucket)
                    .slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(20);
        }
    }

    @FunctionalInterface
    interface BlockFactory {
        LiquidBlock create(FlowingFluid fluid, BlockBehaviour.Properties properties);
    }
}

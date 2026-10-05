package io.github.scwunge.madscience.content.machine.dnaextractor;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.content.item.DecayingItem;
import io.github.scwunge.madscience.content.machine.DrainOnlyTank;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.content.recipe.ProcessingRecipe;
import io.github.scwunge.madscience.registry.ModBlockEntities;
import io.github.scwunge.madscience.registry.ModFluids;
import io.github.scwunge.madscience.registry.ModItems;
import io.github.scwunge.madscience.registry.ModMenus;
import io.github.scwunge.madscience.registry.ModRecipes;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * DNA Extractor: pulls DNA samples out of filled syringes (and DNA-rich items like feathers or gunpowder), leaving a
 * dirty syringe. Mutant DNA syringes instead fill an internal tank, emptied into buckets.
 */
public class DnaExtractorBlockEntity extends MachineBlockEntity {
    public static final int INPUT = 0, BUCKET_IN = 1, DIRTY_OUT = 2, SAMPLE_OUT = 3, BUCKET_OUT = 4;
    public static final int TANK_CAPACITY = 10_000;
    /** Base extraction time by syringe decay (0 = fresh ... 10 = expired), from the original. */
    private static final int[] DECAY_TIME = {150, 250, 375, 420, 555, 666, 720, 800, 980, 1500, 2600};
    /** Extra time for mutant DNA, from the original. */
    private static final int MUTANT_TIME = 666;

    private static final int[] TOP = {INPUT}, SIDES = {BUCKET_IN}, BOTTOM = {DIRTY_OUT, SAMPLE_OUT, BUCKET_OUT};

    private final FluidTank tank = new FluidTank(TANK_CAPACITY, stack -> stack.is(ModFluids.MUTANT_DNA.source.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler drainOnly = new DrainOnlyTank(tank);

    private int progress;
    private int maxProgress;

    public DnaExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DNA_EXTRACTOR.get(), pos, state, 5,
                MadConfig.fe(100_000), MadConfig.fe(200), 0);
    }

    public FluidTank tank() {
        return tank;
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return drainOnly;
    }

    private static boolean isMutant(ItemStack stack) {
        return stack.is(ModItems.MUTANT_SYRINGE.get());
    }

    private Optional<RecipeHolder<ProcessingRecipe>> recipe() {
        return level == null ? Optional.empty() : ModRecipes.DNA_EXTRACTING.find(level, stack(INPUT));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT -> isMutant(stack) || (level != null && ModRecipes.DNA_EXTRACTING.find(level, stack).isPresent());
            case BUCKET_IN -> stack.is(Items.BUCKET);
            default -> false;
        };
    }

    @Override
    public int[] slotsForFace(@Nullable Direction side) {
        return side == Direction.UP ? TOP : side == Direction.DOWN ? BOTTOM : SIDES;
    }

    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return slot == DIRTY_OUT || slot == SAMPLE_OUT || slot == BUCKET_OUT;
    }

    /** Whether there is something to extract and room for the results. */
    private boolean canWork(Optional<RecipeHolder<ProcessingRecipe>> recipe) {
        ItemStack input = stack(INPUT);
        if (input.isEmpty()) {
            return false;
        }
        if (recipe.isEmpty()) {
            return isMutant(input) && tank.getFluidAmount() + 1000 <= TANK_CAPACITY
                    && canOutput(DIRTY_OUT, new ItemStack(ModItems.DIRTY_SYRINGE.get()));
        }
        ProcessingRecipe r = recipe.get().value();
        return canOutput(SAMPLE_OUT, r.result()) && canOutput(DIRTY_OUT, r.remainder());
    }

    private int workTime(ItemStack input, Optional<RecipeHolder<ProcessingRecipe>> recipe) {
        int decay = input.getItem() instanceof DecayingItem ? DecayingItem.getDecay(input) : Math.min(input.getDamageValue(), 10);
        int time = DECAY_TIME[Math.max(0, Math.min(10, decay))];
        if (isMutant(input)) {
            time += MUTANT_TIME;
        }
        return time + recipe.map(r -> r.value().time()).orElse(0);
    }

    private void finish(Optional<RecipeHolder<ProcessingRecipe>> recipe) {
        if (recipe.isPresent()) {
            ProcessingRecipe r = recipe.get().value();
            output(SAMPLE_OUT, r.result().copy());
            output(DIRTY_OUT, r.remainder().copy());
        } else {
            tank.fill(new FluidStack(ModFluids.MUTANT_DNA.source.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
            output(DIRTY_OUT, new ItemStack(ModItems.DIRTY_SYRINGE.get()));
        }
        shrink(INPUT, 1);
        playSound(ModSounds.DNA_EXTRACTOR_FINISH.get(), 1.0F, 1.0F);
    }

    /** Fills an empty bucket from the mutant DNA tank. */
    private void fillBucket() {
        ItemStack filled = new ItemStack(ModFluids.MUTANT_DNA.bucket.get());
        if (stack(BUCKET_IN).is(Items.BUCKET) && tank.getFluidAmount() >= 1000 && canOutput(BUCKET_OUT, filled)) {
            tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            shrink(BUCKET_IN, 1);
            output(BUCKET_OUT, filled);
        }
    }

    @Override
    protected void tickServer() {
        Optional<RecipeHolder<ProcessingRecipe>> recipe = recipe();
        boolean working = isPowered() && canWork(recipe);
        if (working) {
            energy.consume(MadConfig.fe(1));
        }
        fillBucket();
        setActive(working);
        if (working && gameTime() % 20 == 0) {
            playSound(ModSounds.DNA_EXTRACTOR_IDLE.get(), 1.0F, 1.0F);
        }
        if (working) {
            if (progress == 0) {
                maxProgress = workTime(stack(INPUT), recipe);
            }
            progress++;
            if (progress >= maxProgress) {
                progress = 0;
                finish(recipe);
            }
        } else {
            progress = 0;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }

    @Override
    protected int guiValueCount() {
        return 6;
    }

    @Override
    protected int getGuiValue(int index) {
        return switch (index) {
            case 0 -> energyStored();
            case 1 -> energyCapacity();
            case 2 -> progress;
            case 3 -> maxProgress;
            case 4 -> tank.getFluidAmount();
            default -> TANK_CAPACITY;
        };
    }

    @Override
    public void addMenuSlots(MachineMenu menu) {
        menu.addMachineSlot(INPUT, 9, 32);
        menu.addMachineSlot(BUCKET_IN, 152, 61);
        menu.addOutputSlot(DIRTY_OUT, 72, 32);
        menu.addOutputSlot(SAMPLE_OUT, 105, 32);
        menu.addOutputSlot(BUCKET_OUT, 152, 36);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(ModMenus.DNA_EXTRACTOR.get(), containerId, inventory, this);
    }
}

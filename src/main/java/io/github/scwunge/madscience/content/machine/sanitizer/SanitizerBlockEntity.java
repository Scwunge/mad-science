package io.github.scwunge.madscience.content.machine.sanitizer;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.content.recipe.ProcessingRecipe;
import io.github.scwunge.madscience.registry.ModBlockEntities;
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
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Syringe Sanitizer: washes dirty syringes clean with water (1 mB per tick while working, 200 ticks per syringe). */
public class SanitizerBlockEntity extends MachineBlockEntity {
    public static final int WATER_IN = 0, DIRTY_IN = 1, CLEAN_OUT = 2, BUCKET_OUT = 3;
    public static final int TANK_CAPACITY = 10_000;
    private static final int[] TOP = {WATER_IN}, SIDES = {DIRTY_IN}, BOTTOM = {CLEAN_OUT, BUCKET_OUT};

    private final FluidTank tank = waterTank(TANK_CAPACITY);
    private int progress;
    private int maxProgress = 200;

    public SanitizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SANITIZER.get(), pos, state, 4, MadConfig.fe(100_000), MadConfig.fe(200), 0);
    }

    /** Pipes may fill the tank with water but not drain it, like the original. */
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return tank;
    }

    public FluidTank tank() {
        return tank;
    }

    private Optional<RecipeHolder<ProcessingRecipe>> recipe() {
        return level == null ? Optional.empty() : ModRecipes.SANITIZING.find(level, stack(DIRTY_IN));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case WATER_IN -> stack.is(Items.WATER_BUCKET);
            case DIRTY_IN -> level != null && ModRecipes.SANITIZING.find(level, stack).isPresent();
            default -> false;
        };
    }

    @Override
    public int[] slotsForFace(@Nullable Direction side) {
        return side == Direction.UP ? TOP : side == Direction.DOWN ? BOTTOM : SIDES;
    }

    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return slot == CLEAN_OUT || slot == BUCKET_OUT;
    }

    private boolean canWork(Optional<RecipeHolder<ProcessingRecipe>> recipe) {
        return recipe.isPresent() && tank.getFluidAmount() > 0 && canOutput(CLEAN_OUT, recipe.get().value().result());
    }

    @Override
    protected void tickServer() {
        Optional<RecipeHolder<ProcessingRecipe>> recipe = recipe();
        boolean working = isPowered() && canWork(recipe);
        if (working) {
            energy.consume(MadConfig.fe(1));
            tank.drain(1, IFluidHandler.FluidAction.EXECUTE);
        }
        setActive(working);
        if (working && gameTime() % 60 == 0) {
            playSound(ModSounds.SANITIZER_IDLE.get(), 1.0F, 1.0F);
        }
        pourWaterBucket(tank, WATER_IN, BUCKET_OUT);
        if (working) {
            if (progress == 0) {
                maxProgress = recipe.get().value().time();
            }
            progress++;
            if (progress >= maxProgress) {
                progress = 0;
                output(CLEAN_OUT, recipe.get().value().result().copy());
                shrink(DIRTY_IN, 1);
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
        maxProgress = Math.max(1, tag.getInt("MaxProgress"));
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
        menu.addMachineSlot(WATER_IN, 31, 34);
        menu.addMachineSlot(DIRTY_IN, 73, 34);
        menu.addOutputSlot(CLEAN_OUT, 134, 34);
        menu.addOutputSlot(BUCKET_OUT, 31, 9);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(ModMenus.SANITIZER.get(), containerId, inventory, this);
    }
}

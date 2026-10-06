package io.github.scwunge.madscience.content.machine.mainframe;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.content.recipe.MergingRecipe;
import io.github.scwunge.madscience.registry.ModBlockEntities;
import io.github.scwunge.madscience.registry.ModItems;
import io.github.scwunge.madscience.registry.ModMenus;
import io.github.scwunge.madscience.registry.ModRecipes;
import io.github.scwunge.madscience.registry.ModSounds;
import io.github.scwunge.madscience.registry.ModTags;
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

/**
 * Computer Mainframe: merges two complete genomes into a combined genome. Needs power and a redstone signal; running it
 * builds heat, which water coolant keeps in check. Above 780 heat it overheats and stops computing until it cools.
 * All heat rules and numbers are the original's.
 */
public class MainframeBlockEntity extends MachineBlockEntity {
    public static final int WATER_IN = 0, GENOME_A = 1, GENOME_B = 2, REEL_IN = 3, OUTPUT = 4, BUCKET_OUT = 5;
    public static final int TANK_CAPACITY = 10_000;
    public static final int MAX_HEAT = 1000;
    private static final int OPTIMAL_HEAT = 420;
    private static final int OVERHEAT = 780;

    /** What the screen on the model shows. */
    public enum State { OFF, POWERED, ACTIVE, OVERHEATING, NO_WATER }

    private static final int[] TOP = {WATER_IN}, SIDES = {GENOME_A, GENOME_B, REEL_IN}, BOTTOM = {OUTPUT, BUCKET_OUT};

    private final FluidTank tank = waterTank(TANK_CAPACITY);
    private int progress;
    private int maxProgress = 2600;
    private int heat;
    private State state = State.OFF;

    public MainframeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAINFRAME.get(), pos, state, 6, MadConfig.fe(100_000), MadConfig.fe(200), 0);
    }

    public FluidTank tank() {
        return tank;
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return tank;
    }

    public int heat() {
        return heat;
    }

    public State state() {
        return state;
    }

    private Optional<RecipeHolder<MergingRecipe>> recipe() {
        if (level == null || stack(GENOME_A).isEmpty() || stack(GENOME_B).isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(ModRecipes.MERGING.get(), new MergingRecipe.Input(stack(GENOME_A), stack(GENOME_B)), level);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case WATER_IN -> stack.is(Items.WATER_BUCKET);
            case GENOME_A, GENOME_B -> stack.is(ModTags.GENOMES);
            case REEL_IN -> stack.is(ModItems.EMPTY_DATA_REEL.get());
            default -> false;
        };
    }

    @Override
    public int[] slotsForFace(@Nullable Direction side) {
        return side == Direction.UP ? TOP : side == Direction.DOWN ? BOTTOM : SIDES;
    }

    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return slot == OUTPUT || slot == BUCKET_OUT;
    }

    public boolean isOverheating() {
        return heat > OVERHEAT;
    }

    public boolean isOutOfWater() {
        return tank.getFluidAmount() < 1000;
    }

    private boolean canWork(Optional<RecipeHolder<MergingRecipe>> recipe) {
        return recipe.isPresent() && !isOverheating() && stack(REEL_IN).is(ModItems.EMPTY_DATA_REEL.get())
                && tank.getFluidAmount() > 0 && canOutput(OUTPUT, recipe.get().value().result());
    }

    /** The original's heat model: warms while running, water cools it (and evaporates), cools slowly when off. */
    private void updateHeat(boolean running) {
        long time = gameTime();
        if (running) {
            energy.consume(MadConfig.fe(1));
            if (heat < MAX_HEAT && time % 5 == 0) {
                if (heat < OPTIMAL_HEAT) {
                    heat += level.random.nextInt(10);
                } else if (tank.getFluidAmount() > 1000) {
                    heat += level.random.nextInt(2);
                } else {
                    heat += level.random.nextInt(5);
                }
            }
        }
        if (heat < MAX_HEAT && tank.getFluidAmount() > 0 && time % 16 == 0 && running) {
            if (heat > 0 && tank.getFluidAmount() >= heat) {
                tank.drain(heat / 4, IFluidHandler.FluidAction.EXECUTE);
                heat--;
            }
        } else if (time % 8 == 0 && heat > 0) {
            heat--;
        }
        heat = Math.max(0, Math.min(MAX_HEAT, heat));
    }

    private void setState(State newState) {
        if (state != newState) {
            state = newState;
            syncToClient();
        }
    }

    @Override
    protected void tickServer() {
        boolean running = isPowered() && isRedstonePowered();
        updateHeat(running);
        pourWaterBucket(tank, WATER_IN, BUCKET_OUT);
        Optional<RecipeHolder<MergingRecipe>> recipe = recipe();
        boolean working = running && canWork(recipe);

        if (!running) {
            setState(State.OFF);
        } else if (isOverheating()) {
            setState(State.OVERHEATING);
        } else if (isOutOfWater()) {
            setState(State.NO_WATER);
        } else {
            setState(working ? State.ACTIVE : State.POWERED);
        }
        setActive(working);

        long time = gameTime();
        if (running && isOverheating() && time % 23 == 0) {
            playSound(ModSounds.MAINFRAME_OVERHEAT.get(), 0.42F, level.random.nextFloat() * 0.1F + 0.9F);
        }
        if (working && !isOutOfWater() && time % 24 == 0) {
            playSound(ModSounds.MAINFRAME_WORK.get(), 1.0F, 1.0F);
        }
        if (running && !isOutOfWater() && time % 172 == 0) {
            playSound(ModSounds.MAINFRAME_IDLE.get(), 0.42F, 1.0F);
        }

        if (working) {
            if (progress == 0) {
                maxProgress = recipe.get().value().time();
                playSound(ModSounds.MAINFRAME_START.get(), 1.0F, 1.0F);
            }
            progress++;
            if (progress >= maxProgress) {
                progress = 0;
                output(OUTPUT, recipe.get().value().result().copy());
                shrink(REEL_IN, 1);
                playSound(ModSounds.MAINFRAME_FINISH.get(), 1.0F, 1.0F);
            }
        } else {
            progress = 0;
        }
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putByte("State", (byte) state.ordinal());
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        state = State.values()[Math.min(State.values().length - 1, tag.getByte("State"))];
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("Heat", heat);
        tag.putByte("State", (byte) state.ordinal());
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("Progress");
        maxProgress = Math.max(1, tag.getInt("MaxProgress"));
        heat = tag.getInt("Heat");
        state = State.values()[Math.min(State.values().length - 1, tag.getByte("State"))];
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }

    @Override
    protected int guiValueCount() {
        return 8;
    }

    @Override
    protected int getGuiValue(int index) {
        return switch (index) {
            case 0 -> energyStored();
            case 1 -> energyCapacity();
            case 2 -> progress;
            case 3 -> maxProgress;
            case 4 -> tank.getFluidAmount();
            case 5 -> TANK_CAPACITY;
            case 6 -> heat;
            default -> MAX_HEAT;
        };
    }

    @Override
    public void addMenuSlots(MachineMenu menu) {
        menu.addMachineSlot(WATER_IN, 27, 27);
        menu.addMachineSlot(GENOME_A, 73, 17);
        menu.addMachineSlot(GENOME_B, 96, 17);
        menu.addMachineSlot(REEL_IN, 115, 55);
        menu.addOutputSlot(OUTPUT, 148, 38);
        menu.addOutputSlot(BUCKET_OUT, 27, 7);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(ModMenus.MAINFRAME.get(), containerId, inventory, this);
    }
}

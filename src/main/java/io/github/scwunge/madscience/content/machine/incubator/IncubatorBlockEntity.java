package io.github.scwunge.madscience.content.machine.incubator;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.content.recipe.ProcessingRecipe;
import io.github.scwunge.madscience.registry.ModBlockEntities;
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
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Genome Incubator: writes a complete genome onto a chicken egg, making a spawn egg. Needs power and a redstone signal
 * to heat up, and only works above 780 of 1000 heat. The genome is not used up.
 */
public class IncubatorBlockEntity extends MachineBlockEntity {
    public static final int EGG_IN = 0, GENOME_IN = 1, OUTPUT = 2;
    public static final int MAX_HEAT = 1000;
    private static final int WORKING_HEAT = 780;

    public enum State { IDLE, POWERED, READY, WORKING }

    private static final int[] TOP = {EGG_IN}, SIDES = {GENOME_IN}, BOTTOM = {OUTPUT};

    private int progress;
    private int maxProgress = 2600;
    private int heat;
    private State state = State.IDLE;

    public IncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INCUBATOR.get(), pos, state, 3, MadConfig.fe(100_000), MadConfig.fe(200), 0);
    }

    public State state() {
        return state;
    }

    public int heat() {
        return heat;
    }

    private Optional<RecipeHolder<ProcessingRecipe>> recipe() {
        ItemStack genome = stack(GENOME_IN);
        if (level == null || genome.isDamaged()) {
            return Optional.empty();
        }
        return ModRecipes.INCUBATING.find(level, genome);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case EGG_IN -> stack.is(Items.EGG);
            case GENOME_IN -> stack.is(ModTags.GENOMES);
            default -> false;
        };
    }

    @Override
    public int[] slotsForFace(@Nullable Direction side) {
        return side == Direction.UP ? TOP : side == Direction.DOWN ? BOTTOM : SIDES;
    }

    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return slot == OUTPUT;
    }

    private boolean canWork(Optional<RecipeHolder<ProcessingRecipe>> recipe, boolean running) {
        return running && heat > WORKING_HEAT && recipe.isPresent() && stack(EGG_IN).is(Items.EGG)
                && canOutput(OUTPUT, recipe.get().value().result());
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
        if (running) {
            energy.consume(MadConfig.fe(1));
            if (heat < MAX_HEAT) {
                heat++;
            }
        }
        if (heat > 0 && gameTime() % 5 == 0) {
            heat--;
        }

        Optional<RecipeHolder<ProcessingRecipe>> recipe = recipe();
        boolean working = canWork(recipe, running);
        setActive(working);
        if (!isRedstonePowered()) {
            setState(State.IDLE);
        } else if (working) {
            setState(State.WORKING);
        } else if (isPowered() && heat > WORKING_HEAT) {
            setState(State.READY);
        } else {
            setState(State.POWERED);
        }

        if (working && gameTime() % 20 == 0) {
            playSound(ModSounds.INCUBATOR_WORK.get(), 1.0F, 1.0F);
        }
        if (working) {
            if (progress == 0) {
                maxProgress = recipe.get().value().time();
                playSound(ModSounds.INCUBATOR_START.get(), 1.0F, 1.0F);
            }
            progress++;
            if (progress >= maxProgress) {
                progress = 0;
                output(OUTPUT, recipe.get().value().result().copy());
                shrink(EGG_IN, 1);
                playSound(ModSounds.INCUBATOR_FINISH.get(), 1.0F, 1.0F);
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("Progress");
        maxProgress = Math.max(1, tag.getInt("MaxProgress"));
        heat = tag.getInt("Heat");
        state = State.values()[Math.min(State.values().length - 1, tag.getByte("State"))];
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
            case 4 -> heat;
            default -> MAX_HEAT;
        };
    }

    @Override
    public void addMenuSlots(MachineMenu menu) {
        menu.addMachineSlot(EGG_IN, 37, 39);
        menu.addMachineSlot(GENOME_IN, 69, 39);
        menu.addOutputSlot(OUTPUT, 140, 39);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(ModMenus.INCUBATOR.get(), containerId, inventory, this);
    }
}

package io.github.scwunge.madscience.content.machine.sequencer;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.content.item.GenomeItem;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.content.recipe.ProcessingRecipe;
import io.github.scwunge.madscience.registry.ModBlockEntities;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Gene Sequencer: the first DNA sample starts a genome on an empty data reel (fully "damaged"); each further matching
 * sample put in with the unfinished genome repairs it by one point until it is complete.
 */
public class SequencerBlockEntity extends MachineBlockEntity {
    public static final int SAMPLE_IN = 0, REEL_IN = 1, OUTPUT = 2;
    private static final int[] TOP = {SAMPLE_IN}, SIDES = {REEL_IN}, BOTTOM = {OUTPUT};

    private int progress;
    private int maxProgress = 200;

    public SequencerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SEQUENCER.get(), pos, state, 3, MadConfig.fe(100_000), MadConfig.fe(200), 0);
    }

    private Optional<RecipeHolder<ProcessingRecipe>> recipe() {
        return level == null ? Optional.empty() : ModRecipes.SEQUENCING.find(level, stack(SAMPLE_IN));
    }

    private static boolean isUnfinishedGenome(ItemStack stack) {
        return stack.getItem() instanceof GenomeItem && stack.isDamaged();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SAMPLE_IN -> level != null && ModRecipes.SEQUENCING.find(level, stack).isPresent();
            case REEL_IN -> stack.is(ModItems.EMPTY_DATA_REEL.get()) || isUnfinishedGenome(stack);
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

    private boolean canWork(Optional<RecipeHolder<ProcessingRecipe>> recipe) {
        if (recipe.isEmpty() || !stack(OUTPUT).isEmpty()) {
            return false;
        }
        ItemStack reel = stack(REEL_IN);
        if (isUnfinishedGenome(reel)) {
            // the sample must match the genome being repaired
            return reel.is(recipe.get().value().result().getItem());
        }
        return reel.is(ModItems.EMPTY_DATA_REEL.get());
    }

    private void finish(ProcessingRecipe recipe) {
        ItemStack reel = stack(REEL_IN);
        if (isUnfinishedGenome(reel)) {
            ItemStack repaired = reel.copy();
            repaired.setDamageValue(repaired.getDamageValue() - 1);
            if (repaired.isDamaged()) {
                setStack(REEL_IN, repaired);
            } else {
                setStack(REEL_IN, ItemStack.EMPTY);
                output(OUTPUT, repaired);
                playSound(ModSounds.SEQUENCER_FINISH.get(), 1.0F, 1.0F);
            }
        } else {
            ItemStack genome = recipe.result().copy();
            genome.setDamageValue(genome.getMaxDamage());
            output(OUTPUT, genome);
            shrink(REEL_IN, 1);
            playSound(ModSounds.SEQUENCER_START.get(), 1.0F, 1.0F);
        }
        shrink(SAMPLE_IN, 1);
    }

    @Override
    protected void tickServer() {
        Optional<RecipeHolder<ProcessingRecipe>> recipe = recipe();
        boolean working = isPowered() && canWork(recipe);
        if (working) {
            energy.consume(MadConfig.fe(1));
        }
        setActive(working);
        if (working && gameTime() % 60 == 0) {
            playSound(ModSounds.SEQUENCER_WORK.get(), 0.42F, level.random.nextFloat() * 0.1F + 0.9F);
        }
        if (working) {
            if (progress == 0) {
                maxProgress = recipe.get().value().time();
            }
            progress++;
            if (progress >= maxProgress) {
                progress = 0;
                finish(recipe.get().value());
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("Progress");
        maxProgress = Math.max(1, tag.getInt("MaxProgress"));
    }

    @Override
    protected int guiValueCount() {
        return 4;
    }

    @Override
    protected int getGuiValue(int index) {
        return switch (index) {
            case 0 -> energyStored();
            case 1 -> energyCapacity();
            case 2 -> progress;
            default -> maxProgress;
        };
    }

    @Override
    public void addMenuSlots(MachineMenu menu) {
        menu.addMachineSlot(SAMPLE_IN, 22, 36);
        menu.addMachineSlot(REEL_IN, 54, 36);
        menu.addOutputSlot(OUTPUT, 133, 36);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(ModMenus.SEQUENCER.get(), containerId, inventory, this);
    }
}

package io.github.scwunge.madscience.content.machine.duplicator;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.registry.ModBlockEntities;
import io.github.scwunge.madscience.registry.ModItems;
import io.github.scwunge.madscience.registry.ModMenus;
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
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Data Reel Duplicator: copies a finished genome or memory reel onto an empty data reel over 2600 ticks. Needs a
 * redstone signal. The original reel is kept.
 */
public class DuplicatorBlockEntity extends MachineBlockEntity {
    public static final int SOURCE = 0, BLANK = 1, OUTPUT = 2;
    private static final int COPY_TIME = 2600;

    public enum State { OFF, IDLE, WORKING }

    private static final int[] TOP = {SOURCE}, SIDES = {BLANK}, BOTTOM = {OUTPUT};

    private int progress;
    private State state = State.OFF;

    public DuplicatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DUPLICATOR.get(), pos, state, 3, MadConfig.fe(100_000), MadConfig.fe(200), 0);
    }

    public State state() {
        return state;
    }

    public static boolean isCopyable(ItemStack stack) {
        return stack.is(ModTags.DATA_REELS) && !stack.isDamaged();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SOURCE -> isCopyable(stack);
            case BLANK -> stack.is(ModItems.EMPTY_DATA_REEL.get());
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

    private boolean canWork() {
        return isRedstonePowered() && isCopyable(stack(SOURCE)) && stack(BLANK).is(ModItems.EMPTY_DATA_REEL.get()) && stack(OUTPUT).isEmpty();
    }

    private void setState(State newState) {
        if (state != newState) {
            state = newState;
            syncToClient();
        }
    }

    @Override
    protected void tickServer() {
        boolean working = isPowered() && canWork();
        if (working) {
            energy.consume(MadConfig.fe(1));
        }
        setActive(working);
        setState(working ? State.WORKING : isPowered() ? State.IDLE : State.OFF);
        long time = gameTime();
        if (isRedstonePowered() && isPowered() && !working && time % 28 == 0) {
            playSound(ModSounds.DUPLICATOR_IDLE.get(), 1.0F, 1.0F);
        }
        if (working && time % 36 == 0) {
            playSound(ModSounds.DUPLICATOR_WORK.get(), 1.0F, 1.0F);
        }
        if (working) {
            if (progress == 0) {
                playSound(ModSounds.DUPLICATOR_START.get(), 1.0F, 1.0F);
            }
            progress++;
            if (progress >= COPY_TIME) {
                progress = 0;
                output(OUTPUT, stack(SOURCE).copyWithCount(1));
                shrink(BLANK, 1);
                playSound(ModSounds.DUPLICATOR_FINISH.get(), 1.0F, 1.0F);
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
        tag.putByte("State", (byte) state.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("Progress");
        state = State.values()[Math.min(State.values().length - 1, tag.getByte("State"))];
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
            default -> COPY_TIME;
        };
    }

    @Override
    public void addMenuSlots(MachineMenu menu) {
        menu.addMachineSlot(SOURCE, 22, 36);
        menu.addMachineSlot(BLANK, 54, 36);
        menu.addOutputSlot(OUTPUT, 133, 36);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(ModMenus.DUPLICATOR.get(), containerId, inventory, this);
    }
}

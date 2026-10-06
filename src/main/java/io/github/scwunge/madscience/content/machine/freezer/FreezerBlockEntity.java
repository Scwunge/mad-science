package io.github.scwunge.madscience.content.machine.freezer;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.content.item.DecayingItem;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.registry.ModBlockEntities;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.IntStream;

/**
 * Cryogenic Freezer: burns snow or ice to keep 21 slots of syringes and DNA samples cold. Every fuel cycle each stored
 * item loses one point of decay and one piece of fuel is used up.
 */
public class FreezerBlockEntity extends MachineBlockEntity {
    public static final int FUEL = 0, STORAGE_START = 1, STORAGE_SIZE = 21;
    /** Ticks one piece of fuel lasts, from the original. */
    private static final Map<Item, Integer> FUEL_TIME = Map.of(
            Items.SNOWBALL, 200, Items.SNOW, 400, Items.SNOW_BLOCK, 1500, Items.ICE, 2600);
    private static final int[] TOP = {FUEL};
    private static final int[] STORAGE = IntStream.range(STORAGE_START, STORAGE_START + STORAGE_SIZE).toArray();

    private int progress;
    private int maxProgress = 200;

    public FreezerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FREEZER.get(), pos, state, 1 + STORAGE_SIZE, MadConfig.fe(100_000), MadConfig.fe(200), 0);
    }

    public static int fuelTime(ItemStack stack) {
        return FUEL_TIME.getOrDefault(stack.getItem(), 0);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == FUEL ? fuelTime(stack) > 0 : stack.is(ModTags.BLOODWORK);
    }

    @Override
    public int[] slotsForFace(@Nullable Direction side) {
        return side == Direction.UP ? TOP : STORAGE;
    }

    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        // let automation pull samples back out once they are fully fresh
        return slot >= STORAGE_START && DecayingItem.getDecay(stack(slot)) == 0;
    }

    private boolean canWork() {
        return fuelTime(stack(FUEL)) > 0;
    }

    private void chill() {
        for (int slot = STORAGE_START; slot < STORAGE_START + STORAGE_SIZE; slot++) {
            ItemStack stored = stack(slot);
            if (stored.getItem() instanceof DecayingItem && DecayingItem.getDecay(stored) > 0) {
                ItemStack fresher = stored.copy();
                DecayingItem.setDecay(fresher, DecayingItem.getDecay(stored) - 1);
                setStack(slot, fresher);
            }
        }
        shrink(FUEL, 1);
    }

    @Override
    protected void tickServer() {
        boolean working = isPowered() && canWork();
        if (working && level.random.nextBoolean()) {
            // the original only drew power on about half the ticks
            energy.consume(MadConfig.fe(1));
        }
        setActive(working);
        if (working && gameTime() % 20 == 0) {
            playSound(ModSounds.FREEZER_IDLE.get(), 1.0F, 1.0F);
        }
        if (working) {
            if (progress == 0) {
                maxProgress = fuelTime(stack(FUEL));
            }
            progress++;
            if (progress >= maxProgress) {
                progress = 0;
                chill();
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
        menu.addMachineSlot(FUEL, 9, 35);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 7; col++) {
                menu.addMachineSlot(STORAGE_START + row * 7 + col, 39 + col * 18, 16 + row * 18);
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(ModMenus.FREEZER.get(), containerId, inventory, this);
    }
}

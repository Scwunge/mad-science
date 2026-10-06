package io.github.scwunge.madscience.content.machine.magloader;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.content.weapon.MagazineItem;
import io.github.scwunge.madscience.content.weapon.PulseRifleItem;
import io.github.scwunge.madscience.registry.ModBlockEntities;
import io.github.scwunge.madscience.registry.ModItems;
import io.github.scwunge.madscience.registry.ModMenus;
import io.github.scwunge.madscience.registry.ModSounds;
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
 * Magazine Loader: with power and a redstone signal, pushes 95 rounds from its bullet storage into an empty pulse rifle
 * magazine every 200 ticks (the original stopped at 95 "to prevent jamming"). Two blocks tall.
 */
public class MagazineLoaderBlockEntity extends MachineBlockEntity {
    public static final int INPUT = 0, OUTPUT = 1, STORAGE = 2, STORAGE_SIZE = 12;
    public static final int LOAD_ROUNDS = 95;
    public static final int LOAD_TIME = 200;

    private static final int[] TOP = {INPUT}, BOTTOM = {OUTPUT}, SIDES = storageSlots();

    private int progress;
    private boolean hadMagazine;
    // client view
    private boolean hasRounds;
    private boolean showMagazine;

    public MagazineLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGAZINE_LOADER.get(), pos, state, STORAGE + STORAGE_SIZE, MadConfig.fe(25_000), MadConfig.fe(200), 0);
    }

    private static int[] storageSlots() {
        int[] slots = new int[STORAGE_SIZE];
        for (int i = 0; i < STORAGE_SIZE; i++) {
            slots[i] = STORAGE + i;
        }
        return slots;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == INPUT) {
            return stack.is(ModItems.MAGAZINE.get()) && MagazineItem.rounds(stack) == 0;
        }
        return slot >= STORAGE && stack.is(ModItems.ROUND.get());
    }

    @Override
    public int[] slotsForFace(@Nullable Direction side) {
        return side == Direction.UP ? TOP : side == Direction.DOWN ? BOTTOM : SIDES;
    }

    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return slot == OUTPUT;
    }

    public int storedRounds() {
        int rounds = 0;
        for (int i = STORAGE; i < STORAGE + STORAGE_SIZE; i++) {
            rounds += stack(i).getCount();
        }
        return rounds;
    }

    /** Animation step for the pusher and the rounds in the feed: 0 idle, 1..6 as the load goes on (the original's thresholds). */
    public int pushStage() {
        if (progress <= 0) {
            return 0;
        }
        int percent = progress * 100 / LOAD_TIME;
        return percent <= 25 ? 1 : percent <= 50 ? 2 : percent <= 75 ? 3 : percent <= 85 ? 4 : percent <= 95 ? 5 : 6;
    }

    public boolean hasRounds() {
        return hasRounds;
    }

    public boolean showMagazine() {
        return showMagazine;
    }

    private boolean canLoad() {
        return isRedstonePowered() && !stack(INPUT).isEmpty() && storedRounds() >= LOAD_ROUNDS
                && canOutput(OUTPUT, PulseRifleItem.magazine(LOAD_ROUNDS));
    }

    @Override
    protected void tickServer() {
        if (isPowered() && isRedstonePowered()) {
            energy.consume(MadConfig.fe(1));
        }
        boolean magazine = !stack(INPUT).isEmpty();
        if (magazine && !hadMagazine) {
            playSound(ModSounds.MAGAZINE_LOADER_INSERT.get(), 1.0F, 1.0F);
        }
        hadMagazine = magazine;

        int stageBefore = pushStage();
        if (isPowered() && canLoad()) {
            if (progress == 0) {
                playSound(ModSounds.MAGAZINE_LOADER_PUSH_START.get(), 1.0F, 1.0F);
            } else if (gameTime() % 20 == 0) {
                playSound(ModSounds.MAGAZINE_LOADER_LOADING.get(), 1.0F, 1.0F);
            }
            progress++;
            if (progress >= LOAD_TIME) {
                progress = 0;
                load();
                playSound(ModSounds.MAGAZINE_LOADER_PUSH_STOP.get(), 1.0F, 1.0F);
            }
        } else {
            progress = 0;
        }
        setActive(progress > 0);

        boolean rounds = storedRounds() > 0;
        boolean shown = magazine || !stack(OUTPUT).isEmpty();
        if (pushStage() != stageBefore || rounds != hasRounds || shown != showMagazine) {
            hasRounds = rounds;
            showMagazine = shown;
            syncToClient();
        }
    }

    /** Takes 95 rounds from storage, fullest stacks first, and turns one empty magazine into a loaded one. */
    private void load() {
        int needed = LOAD_ROUNDS;
        while (needed > 0) {
            int fullest = -1;
            for (int i = STORAGE; i < STORAGE + STORAGE_SIZE; i++) {
                if (!stack(i).isEmpty() && (fullest < 0 || stack(i).getCount() > stack(fullest).getCount())) {
                    fullest = i;
                }
            }
            int take = Math.min(needed, stack(fullest).getCount());
            shrink(fullest, take);
            needed -= take;
        }
        shrink(INPUT, 1);
        output(OUTPUT, PulseRifleItem.magazine(LOAD_ROUNDS));
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putInt("Progress", progress);
        tag.putBoolean("HasRounds", storedRounds() > 0);
        tag.putBoolean("ShowMagazine", !stack(INPUT).isEmpty() || !stack(OUTPUT).isEmpty());
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        progress = tag.getInt("Progress");
        hasRounds = tag.getBoolean("HasRounds");
        showMagazine = tag.getBoolean("ShowMagazine");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", progress);
        tag.putBoolean("HadMagazine", hadMagazine);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("Progress");
        hadMagazine = tag.getBoolean("HadMagazine");
    }

    @Override
    protected int guiValueCount() {
        return 5;
    }

    @Override
    protected int getGuiValue(int index) {
        return switch (index) {
            case 0 -> energyStored();
            case 1 -> energyCapacity();
            case 2 -> progress;
            case 3 -> LOAD_TIME;
            default -> storedRounds();
        };
    }

    @Override
    public void addMenuSlots(MachineMenu menu) {
        menu.addMachineSlot(INPUT, 89, 34);
        menu.addOutputSlot(OUTPUT, 143, 34);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                menu.addMachineSlot(STORAGE + row * 4 + col, 8 + col * 18, 16 + row * 18);
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(ModMenus.MAGAZINE_LOADER.get(), containerId, inventory, this);
    }
}

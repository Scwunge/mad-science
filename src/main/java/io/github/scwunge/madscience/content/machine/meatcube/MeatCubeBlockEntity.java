package io.github.scwunge.madscience.content.machine.meatcube;

import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.registry.ModBlockEntities;
import io.github.scwunge.madscience.registry.ModFluids;
import io.github.scwunge.madscience.registry.ModMenus;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Disgusting Meat Cube (slime + cow, pig or chicken genomes): a living block of meat. Punch it for raw beef, pork or
 * chicken; each punch takes one of its 14 chunks of meat. Fed Liquid Mutant DNA it regrows a chunk every 10 seconds
 * (250 mB each). It twitches and its heart beats faster as it fills out.
 */
public class MeatCubeBlockEntity extends MachineBlockEntity {
    public static final int BUCKET_IN = 0, BUCKET_OUT = 1;
    public static final int MAX_MEAT = 14;
    public static final int TANK_CAPACITY = 10_000;
    private static final int REGROW_TICKS = 200;
    private static final int REGROW_COST = 250;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY, stack -> stack.is(ModFluids.MUTANT_DNA.source.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private int meat = MAX_MEAT;
    private int regrow;
    /** When the current twitch started (game time) and how many frames it lasts, for the renderer. */
    private long twitchStart = -1000;
    private int twitchFrames = 9;
    private long nextTwitch = 42;

    public MeatCubeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MEAT_CUBE.get(), pos, state, 2, 0, 0, 0);
    }

    public int meat() {
        return meat;
    }

    public FluidTank tank() {
        return tank;
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return tank;
    }

    /** Twitch animation frame (0..9) for the renderer. */
    public int twitchFrame(long time) {
        long elapsed = (time - twitchStart) / 5;
        return elapsed >= 0 && elapsed < twitchFrames ? (int) elapsed : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == BUCKET_IN && stack.is(ModFluids.MUTANT_DNA.bucket.get());
    }

    @Override
    public int[] slotsForFace(@Nullable Direction side) {
        return new int[]{BUCKET_IN, BUCKET_OUT};
    }

    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return slot == BUCKET_OUT;
    }

    /** Called when punched: tear off a chunk of meat. Returns the meat dropped, or empty if it's bare bone. */
    public ItemStack tearMeat() {
        if (meat <= 0 || level == null) {
            return ItemStack.EMPTY;
        }
        meat--;
        syncToClient();
        float type = level.random.nextFloat();
        int count = Math.max(1, level.random.nextInt(5));
        ItemStack dropped;
        if (type <= 0.2F) {
            dropped = new ItemStack(Items.BEEF, count);
        } else if (type <= 0.5F) {
            dropped = new ItemStack(Items.CHICKEN, count);
        } else if (type <= 0.8F) {
            dropped = new ItemStack(Items.PORKCHOP, count);
        } else {
            dropped = new ItemStack(Items.BEEF);
        }
        level.playSound(null, worldPosition, ModSounds.MEAT_CUBE_MEATSLAP.get(), SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
        if (level.random.nextBoolean() && level.random.nextInt(10) < 5) {
            level.playSound(null, worldPosition, ModSounds.MEAT_CUBE_MOO.get(), SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
        }
        return dropped;
    }

    private void pourMutantBucket() {
        ItemStack empty = new ItemStack(Items.BUCKET);
        if (stack(BUCKET_IN).is(ModFluids.MUTANT_DNA.bucket.get()) && canOutput(BUCKET_OUT, empty)
                && tank.getFluidAmount() + 1000 <= TANK_CAPACITY) {
            tank.fill(new FluidStack(ModFluids.MUTANT_DNA.source.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
            shrink(BUCKET_IN, 1);
            output(BUCKET_OUT, empty);
            playSound(ModSounds.MEAT_CUBE_BELLY.get(), 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
        }
    }

    @Override
    protected void tickServer() {
        long time = gameTime();
        pourMutantBucket();
        if (time >= nextTwitch && twitchFrame(time) == 0 && time - twitchStart > twitchFrames * 5L) {
            twitchStart = time;
            twitchFrames = level.random.nextInt(9);
            if (twitchFrames <= 2) {
                twitchFrames = 9;
            }
            nextTwitch = time + Math.max(level.random.nextInt(666), 20);
            playSound(ModSounds.MEAT_CUBE_IDLE.get(), 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
            syncToClient();
        }
        if (time % ((meat * 20L) + 20L) == 0) {
            playSound(ModSounds.MEAT_CUBE_HEARTBEAT.get(), 1.0F / Math.max(1, meat) + 0.42F, level.random.nextFloat() * 0.1F + 0.9F);
        }
        boolean growing = meat < MAX_MEAT && tank.getFluidAmount() > 0;
        setActive(growing);
        if (growing) {
            if (++regrow >= REGROW_TICKS) {
                regrow = 0;
                tank.drain(REGROW_COST, IFluidHandler.FluidAction.EXECUTE);
                meat++;
                playSound(ModSounds.MEAT_CUBE_BELLY.get(), 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
                syncToClient();
            }
        } else {
            regrow = 0;
        }
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putInt("Meat", meat);
        tag.putLong("TwitchStart", twitchStart);
        tag.putInt("TwitchFrames", twitchFrames);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        meat = tag.getInt("Meat");
        twitchStart = tag.getLong("TwitchStart");
        twitchFrames = tag.getInt("TwitchFrames");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Meat", meat);
        tag.putInt("Regrow", regrow);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        meat = tag.contains("Meat") ? tag.getInt("Meat") : MAX_MEAT;
        regrow = tag.getInt("Regrow");
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }

    @Override
    protected int guiValueCount() {
        return 6;
    }

    @Override
    protected int getGuiValue(int index) {
        return switch (index) {
            case 0 -> regrow;
            case 1 -> REGROW_TICKS;
            case 2 -> tank.getFluidAmount();
            case 3 -> TANK_CAPACITY;
            case 4 -> meat;
            default -> MAX_MEAT;
        };
    }

    @Override
    public void addMenuSlots(MachineMenu menu) {
        menu.addMachineSlot(BUCKET_IN, 90, 43);
        menu.addOutputSlot(BUCKET_OUT, 90, 18);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(ModMenus.MEAT_CUBE.get(), containerId, inventory, this);
    }
}

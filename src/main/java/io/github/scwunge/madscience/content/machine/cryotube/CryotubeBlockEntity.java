package io.github.scwunge.madscience.content.machine.cryotube;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.content.item.MemoryReelItem;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineMenu;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Cryogenic Tube: grows a villager from a spawn egg and harvests power from its memories. With a redstone signal it
 * hatches the egg (2600 ticks, with a 21% chance of a stillbirth), then the subject lives for 100 seconds. Every second
 * its neural activity is a random value up to the memory's level; with a nether star installed that activity is turned
 * into power. When it dies an empty data reel records its memory. Rotten flesh piles up as waste and stops the tube
 * when full. Losing the redstone signal kills the subject.
 */
public class CryotubeBlockEntity extends MachineBlockEntity {
    public static final int EGG_IN = 0, REEL_IN = 1, STAR = 2, MEMORY_OUT = 3, FLESH_OUT = 4;
    public static final int HATCH_TIME = 2600;
    public static final int MAX_HEALTH = 100;

    public enum State { OFF, ON, ALIVE, DEAD }

    private int hatchProgress;
    private int maxHatch = 200;
    private boolean alive;
    private int health;
    private int neural;
    private int neuralMax;
    private State state = State.OFF;

    public CryotubeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CRYOTUBE.get(), pos, blockState, 5, MadConfig.fe(100_000), 0, MadConfig.fe(1_000));
    }

    public State state() {
        return state;
    }

    private boolean isMemoryReel(ItemStack stack) {
        return stack.getItem() instanceof MemoryReelItem;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case EGG_IN -> stack.is(Items.VILLAGER_SPAWN_EGG);
            case REEL_IN -> stack.is(ModItems.EMPTY_DATA_REEL.get()) || isMemoryReel(stack);
            case STAR -> stack.is(Items.NETHER_STAR);
            default -> false;
        };
    }

    @Override
    public int[] slotsForFace(@Nullable Direction side) {
        return side == Direction.UP ? new int[]{EGG_IN, STAR} : side == Direction.DOWN ? new int[]{MEMORY_OUT, FLESH_OUT} : new int[]{REEL_IN};
    }

    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return slot == MEMORY_OUT || slot == FLESH_OUT;
    }

    private boolean fleshFull() {
        ItemStack flesh = stack(FLESH_OUT);
        return !flesh.isEmpty() && flesh.getCount() >= flesh.getMaxStackSize();
    }

    /** Has an egg (or a subject growing), a reel, room for the memory it will make, and isn't choked with flesh. */
    private boolean canWork() {
        ItemStack reel = stack(REEL_IN);
        if (reel.isEmpty() || fleshFull()) {
            return false;
        }
        if (reel.is(ModItems.EMPTY_DATA_REEL.get()) && !stack(MEMORY_OUT).isEmpty()) {
            return false;
        }
        return alive || hatchProgress > 0 || stack(EGG_IN).is(Items.VILLAGER_SPAWN_EGG);
    }

    private void addFlesh(int bound) {
        int count = level.random.nextInt(bound);
        if (count > 0 && !fleshFull()) {
            output(FLESH_OUT, new ItemStack(Items.ROTTEN_FLESH, Math.min(count, 64 - stack(FLESH_OUT).getCount())));
        }
    }

    private void reset() {
        alive = false;
        hatchProgress = 0;
        maxHatch = 200;
        neural = 0;
        neuralMax = 0;
        health = 0;
    }

    private void setState(State newState) {
        if (state != newState) {
            state = newState;
            syncToClient();
        }
    }

    @Override
    protected void tickServer() {
        pushEnergy();
        if (fleshFull()) {
            reset();
            setState(isRedstonePowered() ? State.DEAD : State.OFF);
            return;
        }
        boolean redstone = isRedstonePowered();
        if (!redstone) {
            if (alive || hatchProgress > 0) {
                // power cut: the subject dies
                reset();
                addFlesh(5);
                playSound(ModSounds.CRYOTUBE_OFF.get(), 1.0F, 1.0F);
            }
            setState(State.OFF);
            setActive(false);
            return;
        }
        long time = gameTime();
        boolean working = canWork();
        setActive(working);
        if (!working) {
            setState(State.ON);
            if (time % 20 == 0) {
                playSound(ModSounds.CRYOTUBE_IDLE.get(), 1.0F, 1.0F);
            }
            return;
        }
        if (!alive && hatchProgress == 0) {
            // crack an egg and start growing a subject
            playSound(ModSounds.CRYOTUBE_CRACK_EGG.get(), 1.0F, 1.0F);
            shrink(EGG_IN, 1);
            maxHatch = HATCH_TIME;
            hatchProgress = 1;
            setState(State.ON);
        } else if (!alive && hatchProgress < maxHatch) {
            if (time % 20 == 0) {
                playSound(ModSounds.CRYOTUBE_HATCHING.get(), 1.0F, 0.1F);
                playSound(ModSounds.CRYOTUBE_IDLE.get(), 1.0F, 1.0F);
            }
            if (time % 40 == 0 && level.random.nextBoolean()) {
                playSound(ModSounds.CRYOTUBE_HATCH.get(), 1.0F, 0.1F);
            }
            hatchProgress++;
        } else if (!alive) {
            if (level.random.nextBoolean() && level.random.nextInt(100) < 42) {
                // still-birth: try again with the next egg
                hatchProgress = 0;
                addFlesh(2);
                playSound(ModSounds.CRYOTUBE_CRACK_EGG.get(), 1.0F, 0.1F);
            } else {
                alive = true;
                ItemStack reel = stack(REEL_IN);
                neuralMax = reel.getItem() instanceof MemoryReelItem memory ? memory.level() : MemoryReelItem.Memory.random(level.random).level();
                neural = neuralMax;
                health = MAX_HEALTH;
                setState(State.ALIVE);
            }
        } else if (health > 0) {
            if (time % 20 == 0) {
                neural = level.random.nextInt(Math.max(1, neuralMax));
                playSound(ModSounds.CRYOTUBE_WORK.get(), 1.0F, 1.0F);
                if (stack(STAR).is(Items.NETHER_STAR)) {
                    energy.produce(neural * MadConfig.CRYOTUBE_FE_PER_NEURON.get() * 20);
                }
                health--;
                setChanged();
            }
            setState(State.ALIVE);
        } else {
            playSound(ModSounds.CRYOTUBE_STILLBIRTH.get(), 1.0F, 0.5F);
            if (stack(REEL_IN).is(ModItems.EMPTY_DATA_REEL.get())) {
                output(MEMORY_OUT, new ItemStack(MemoryReelItem.Memory.forLevel(neuralMax).item()));
                shrink(REEL_IN, 1);
            }
            reset();
            addFlesh(5);
            setState(State.ON);
        }
    }

    /** Hands generated power to any neighbour that takes FE, like the original's produce(). */
    private void pushEnergy() {
        if (energy == null || energy.getEnergyStored() <= 0 || level == null) {
            return;
        }
        for (Direction direction : Direction.values()) {
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(direction), direction.getOpposite());
            if (target != null && target.canReceive()) {
                int offered = energy.extractEnergy(energy.getMaxEnergyStored(), true);
                int taken = target.receiveEnergy(offered, false);
                energy.extractEnergy(taken, false);
            }
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
        tag.putInt("Hatch", hatchProgress);
        tag.putInt("HatchMax", maxHatch);
        tag.putBoolean("Alive", alive);
        tag.putInt("Health", health);
        tag.putInt("Neural", neural);
        tag.putInt("NeuralMax", neuralMax);
        tag.putByte("State", (byte) state.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        hatchProgress = tag.getInt("Hatch");
        maxHatch = Math.max(1, tag.getInt("HatchMax"));
        alive = tag.getBoolean("Alive");
        health = tag.getInt("Health");
        neural = tag.getInt("Neural");
        neuralMax = tag.getInt("NeuralMax");
        state = State.values()[Math.min(State.values().length - 1, tag.getByte("State"))];
    }

    public boolean isAlive() {
        return alive;
    }

    public int health() {
        return health;
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
            case 2 -> alive ? maxHatch : hatchProgress;
            case 3 -> maxHatch;
            case 4 -> health;
            case 5 -> MAX_HEALTH;
            case 6 -> neural;
            default -> Math.max(1, neuralMax);
        };
    }

    @Override
    public void addMenuSlots(MachineMenu menu) {
        menu.addMachineSlot(EGG_IN, 11, 26);
        menu.addMachineSlot(REEL_IN, 11, 47);
        menu.addMachineSlot(STAR, 113, 52);
        menu.addOutputSlot(MEMORY_OUT, 144, 22);
        menu.addOutputSlot(FLESH_OUT, 144, 56);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(ModMenus.CRYOTUBE.get(), containerId, inventory, this);
    }
}

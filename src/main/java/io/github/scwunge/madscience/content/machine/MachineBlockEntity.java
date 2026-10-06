package io.github.scwunge.madscience.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Shared base for Mad Science machines: an item inventory with per-slot rules, an optional FE buffer, server ticking,
 * redstone checks, a synced "active" flag for rendering, and GUI data. Subclasses hold their own tanks and timers.
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler items;
    @Nullable
    protected final MachineEnergy energy;
    private final Map<Direction, IItemHandler> sidedHandlers = new EnumMap<>(Direction.class);
    private final ContainerData dataAccess;

    /** Whether the machine is working; drives the animated textures and sounds on the client. */
    private boolean active;

    /** Who placed the machine; machines that change the world act as this player, so claims apply. */
    @Nullable
    private java.util.UUID owner;

    public void setOwner(@Nullable java.util.UUID owner) {
        this.owner = owner;
        setChanged();
    }

    @Nullable
    public java.util.UUID owner() {
        return owner;
    }

    /** A stand-in player for the owner, for permission checks like block-break events. */
    protected net.minecraft.world.entity.player.Player actingPlayer(net.minecraft.server.level.ServerLevel level) {
        com.mojang.authlib.GameProfile profile = owner == null
                ? new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("madscience".getBytes()), "[Mad Science]")
                : new com.mojang.authlib.GameProfile(owner, "[Mad Science]");
        return net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, profile);
    }

    /** Whether the owner may break the block at {@code pos} (claims and protection mods get their say). */
    protected boolean mayBreak(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        var event = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), actingPlayer(level));
        return !net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled();
    }

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots,
                                 int energyCapacity, int energyInput, int energyOutput) {
        super(type, pos, state);
        this.items = new ItemStackHandler(slots) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return MachineBlockEntity.this.isItemValid(slot, stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                MachineBlockEntity.this.onInventoryChanged(slot);
            }
        };
        this.energy = energyCapacity > 0 ? new MachineEnergy(energyCapacity, energyInput, energyOutput, this::setChanged) : null;
        this.dataAccess = new ContainerData() {
            // every int is sent as two shorts, since container data travels as 16-bit values
            @Override
            public int get(int index) {
                int value = getGuiValue(index / 2);
                return index % 2 == 0 ? value & 0xFFFF : value >>> 16;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return guiValueCount() * 2;
            }
        };
    }

    // ---- inventory rules ----

    public IItemHandlerModifiable items() {
        return items;
    }

    /** Whether a player or automation may put this stack in this slot. Output slots return false. */
    public abstract boolean isItemValid(int slot, ItemStack stack);

    /** Slots reachable from a side by hoppers and pipes. */
    public abstract int[] slotsForFace(@Nullable Direction side);

    public boolean canInsertFromSide(int slot, ItemStack stack, @Nullable Direction side) {
        return isItemValid(slot, stack);
    }

    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return false;
    }

    public ItemStack extractForAutomation(int slot, int amount, boolean simulate) {
        return items.extractItem(slot, amount, simulate);
    }

    public IItemHandler sidedItemHandler(@Nullable Direction side) {
        if (side == null) {
            return items;
        }
        return sidedHandlers.computeIfAbsent(side, s -> new SidedItemHandler(this, s));
    }

    protected void onInventoryChanged(int slot) {
        setChanged();
    }

    protected ItemStack stack(int slot) {
        return items.getStackInSlot(slot);
    }

    protected void setStack(int slot, ItemStack stack) {
        items.setStackInSlot(slot, stack);
    }

    /** Removes {@code count} items from a slot. */
    protected void shrink(int slot, int count) {
        ItemStack stack = items.getStackInSlot(slot).copy();
        stack.shrink(count);
        items.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
    }

    /** Whether {@code result} fits in an output slot (ignores slot rules). */
    protected boolean canOutput(int slot, ItemStack result) {
        if (result.isEmpty()) {
            return true;
        }
        ItemStack existing = items.getStackInSlot(slot);
        if (existing.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(existing, result)
                && existing.getCount() + result.getCount() <= Math.min(existing.getMaxStackSize(), items.getSlotLimit(slot));
    }

    /** Puts {@code result} into an output slot (ignores slot rules). Check {@link #canOutput} first. */
    protected void output(int slot, ItemStack result) {
        if (result.isEmpty()) {
            return;
        }
        ItemStack existing = items.getStackInSlot(slot);
        if (existing.isEmpty()) {
            items.setStackInSlot(slot, result.copy());
        } else {
            ItemStack grown = existing.copy();
            grown.grow(result.getCount());
            items.setStackInSlot(slot, grown);
        }
    }

    /**
     * Empties a water bucket from {@code inSlot} into {@code tank}, putting the empty bucket in {@code outSlot}, when the
     * whole bucket fits. Used by the water-cooled machines.
     */
    protected boolean pourWaterBucket(FluidTank tank, int inSlot, int outSlot) {
        ItemStack bucket = new ItemStack(Items.BUCKET);
        if (!stack(inSlot).is(Items.WATER_BUCKET) || !canOutput(outSlot, bucket)) {
            return false;
        }
        FluidStack water = new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME);
        if (tank.fill(water, IFluidHandler.FluidAction.SIMULATE) != FluidType.BUCKET_VOLUME) {
            return false;
        }
        tank.fill(water, IFluidHandler.FluidAction.EXECUTE);
        shrink(inSlot, 1);
        output(outSlot, bucket);
        return true;
    }

    /** A tank that only takes water and saves the machine when it changes. */
    protected FluidTank waterTank(int capacity) {
        return new FluidTank(capacity, stack -> stack.is(Fluids.WATER)) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    // ---- energy ----

    @Nullable
    public MachineEnergy energy() {
        return energy;
    }

    public boolean isPowered() {
        return energy != null && energy.hasEnergy();
    }

    public int energyStored() {
        return energy == null ? 0 : energy.getEnergyStored();
    }

    public int energyCapacity() {
        return energy == null ? 0 : energy.getMaxEnergyStored();
    }

    // ---- ticking ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        machine.tickServer();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        machine.tickClient();
    }

    protected abstract void tickServer();

    protected void tickClient() {
    }

    public boolean isRedstonePowered() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    protected void playSound(SoundEvent sound, float volume, float pitch) {
        if (level != null) {
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
        }
    }

    protected long gameTime() {
        return level == null ? 0 : level.getGameTime();
    }

    // ---- client sync ----

    public boolean isActive() {
        return active;
    }

    protected void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            syncToClient();
        }
    }

    /** Sends the client-visible state (see {@link #writeClientData}) to players tracking this block. */
    protected void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Extra state the renderer needs. Keep it small: it is sent whenever {@link #syncToClient} is called. */
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Active", active);
    }

    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        active = tag.getBoolean("Active");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeClientData(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readClientData(tag, registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Update packets carry {@link #writeClientData} only; read them the same way (the default goes through the save loader). */
    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        readClientData(packet.getTag(), registries);
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        if (energy != null) {
            tag.put("Energy", energy.serializeNBT(registries));
        }
        tag.putBoolean("Active", active);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            // load into a scratch handler so a save from a version with a different slot count can't resize ours
            ItemStackHandler loaded = new ItemStackHandler();
            loaded.deserializeNBT(registries, tag.getCompound("Items"));
            for (int i = 0; i < items.getSlots(); i++) {
                items.setStackInSlot(i, i < loaded.getSlots() ? loaded.getStackInSlot(i) : ItemStack.EMPTY);
            }
        }
        if (energy != null && tag.contains("Energy")) {
            energy.deserializeNBT(registries, tag.get("Energy"));
        }
        active = tag.getBoolean("Active");
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(i));
        }
    }

    // ---- GUI ----

    /** Number of int values shown in the GUI. */
    protected abstract int guiValueCount();

    /** GUI value {@code index}: energy, progress, tank levels, ... as each machine defines. */
    protected abstract int getGuiValue(int index);

    public ContainerData dataAccess() {
        return dataAccess;
    }

    /** Adds this machine's slots to its menu at the original GUI positions. */
    public abstract void addMenuSlots(MachineMenu menu);

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public abstract AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player);
}

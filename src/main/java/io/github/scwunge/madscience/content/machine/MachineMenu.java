package io.github.scwunge.madscience.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * One menu class for every machine: the block entity adds its own slots at the original positions, then the player
 * inventory goes in the usual place. GUI numbers come from the block entity's {@link ContainerData}.
 */
public class MachineMenu extends AbstractContainerMenu {
    private final MachineBlockEntity machine;
    private final ContainerData data;
    private int machineSlots;

    /**
     * Client side. The block entity normally exists on the client too; if it hasn't arrived yet (the menu can open in
     * the same tick the block was placed) a stand-in is made from the block state. Slots and gauges are synced by the
     * menu either way.
     */
    public MachineMenu(MenuType<?> type, int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(type, containerId, inventory, clientMachine(inventory.player.level(), buf.readBlockPos(), Block.stateById(buf.readVarInt())), true);
    }

    private static MachineBlockEntity clientMachine(Level level, BlockPos pos, BlockState state) {
        // the client's copy can be stale (the block was just replaced), so it must match the server's block
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine && machine.getBlockState().is(state.getBlock())) {
            return machine;
        }
        if (state.getBlock() instanceof EntityBlock block && block.newBlockEntity(pos, state) instanceof MachineBlockEntity machine) {
            machine.setLevel(level);
            return machine;
        }
        throw new IllegalStateException("no Mad Science machine at " + pos);
    }

    /** Opens a machine's menu for a player, sending what the client needs to build it. */
    public static void open(ServerPlayer player, MachineBlockEntity machine) {
        BlockPos pos = machine.getBlockPos();
        BlockState state = machine.getBlockState();
        player.openMenu(machine, buf -> {
            buf.writeBlockPos(pos);
            buf.writeVarInt(Block.getId(state));
        });
    }

    public MachineMenu(MenuType<?> type, int containerId, Inventory inventory, MachineBlockEntity machine) {
        this(type, containerId, inventory, machine, false);
    }

    private MachineMenu(MenuType<?> type, int containerId, Inventory inventory, MachineBlockEntity machine, boolean client) {
        super(type, containerId);
        this.machine = machine;
        this.data = client ? new SimpleContainerData(machine.dataAccess().getCount()) : machine.dataAccess();
        machine.addMenuSlots(this);
        machineSlots = slots.size();
        addPlayerInventory(inventory, 8, 84);
        addDataSlots(data);
    }

    public MachineBlockEntity machine() {
        return machine;
    }

    /** Adds a machine slot. Call from {@link MachineBlockEntity#addMenuSlots}. */
    public void addMachineSlot(int index, int x, int y) {
        addSlot(new SlotItemHandler(machine.items(), index, x, y));
    }

    /** Adds a take-only slot (outputs). */
    public void addOutputSlot(int index, int x, int y) {
        addSlot(new SlotItemHandler(machine.items(), index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    protected void addPlayerInventory(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, x + col * 18, y + 58));
        }
    }

    /** GUI value {@code index} as defined by the machine (reassembled from the two synced shorts). */
    public int value(int index) {
        return (data.get(index * 2) & 0xFFFF) | (data.get(index * 2 + 1) << 16);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = machineSlots;
        int hotbarStart = playerStart + 27;
        int end = playerStart + 36;
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, playerStart, end, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else {
            boolean moved = false;
            for (int i = 0; i < machineSlots && !stack.isEmpty(); i++) {
                if (slots.get(i).mayPlace(stack) && moveItemStackTo(stack, i, i + 1, false)) {
                    moved = true;
                }
            }
            if (!moved) {
                if (index < hotbarStart) {
                    if (!moveItemStackTo(stack, hotbarStart, end, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(stack, playerStart, hotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }
}

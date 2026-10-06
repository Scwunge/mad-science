package io.github.scwunge.madscience.content.machine.cnc;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.content.recipe.CncRecipe;
import io.github.scwunge.madscience.registry.ModBlockEntities;
import io.github.scwunge.madscience.registry.ModMenus;
import io.github.scwunge.madscience.registry.ModRecipes;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * CnC Machine: with power, water and a redstone signal, presses a block of iron and water-jet cuts it into the part named
 * on the first page of the written book in it (see {@link CncRecipe}). Takes 1000 ticks; the book is kept. Two blocks tall.
 */
public class CncMachineBlockEntity extends MachineBlockEntity {
    public static final int WATER_IN = 0, IRON = 1, BOOK = 2, OUTPUT = 3, BUCKET_OUT = 4;
    public static final int CUT_TIME = 1000;
    public static final int TANK_CAPACITY = 10_000;
    /** The animation (and the original's sounds) run in 17 steps: pressing up to 6, water cutting from 7. */
    public static final int STAGES = 17;

    /** What the screen and model show. */
    public enum Status { OFF, OFFLINE, NOT_READY, NEED_WATER, INVALID_BOOK, WORKING }

    private static final int[] TOP = {BOOK}, SIDES = {WATER_IN, IRON}, BOTTOM = {OUTPUT, BUCKET_OUT};

    private final FluidTank tank = waterTank(TANK_CAPACITY);
    private int progress;
    private Status status = Status.OFF;
    private boolean hadIron;
    private boolean hadRedstone;
    private boolean warnedInvalid;
    // client view
    private boolean hasIron;

    public CncMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CNC_MACHINE.get(), pos, state, 5, MadConfig.fe(25_000), MadConfig.fe(200), 0);
    }

    public FluidTank tank() {
        return tank;
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return tank;
    }

    public Status status() {
        return status;
    }

    public int stage() {
        return progress * STAGES / CUT_TIME;
    }

    public int progress() {
        return progress;
    }

    public boolean hasIron() {
        return hasIron;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case WATER_IN -> stack.is(Items.WATER_BUCKET);
            case IRON -> stack.is(Items.IRON_BLOCK);
            case BOOK -> stack.is(Items.WRITTEN_BOOK);
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

    /** The first page of a written book, as plain text. */
    public static String firstPage(ItemStack book) {
        WrittenBookContent content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null || content.pages().isEmpty()) {
            return "";
        }
        return content.pages().getFirst().raw().getString();
    }

    private Optional<RecipeHolder<CncRecipe>> recipe() {
        if (level == null || stack(BOOK).isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(ModRecipes.CNC.get(), new CncRecipe.Input(firstPage(stack(BOOK))), level);
    }

    /** Everything but the book's contents is in place (the original's canSmelt). */
    private boolean ready() {
        return stack(IRON).is(Items.IRON_BLOCK) && stack(OUTPUT).isEmpty() && isRedstonePowered() && !stack(BOOK).isEmpty()
                && tank.getFluidAmount() > 0;
    }

    @Override
    protected void tickServer() {
        pourWaterBucket(tank, WATER_IN, BUCKET_OUT);
        boolean ready = ready();
        boolean powered = isPowered();
        if (powered && ready) {
            energy.consume(MadConfig.fe(1));
        }
        int stageBefore = stage();
        Optional<RecipeHolder<CncRecipe>> recipe = ready && powered ? recipe() : Optional.empty();
        boolean working = ready && powered && recipe.isPresent();
        if (working) {
            warnedInvalid = false;
            progress++;
            if (stage() >= 7) {
                tank.drain(1, IFluidHandler.FluidAction.EXECUTE);
            }
            if (progress >= CUT_TIME) {
                progress = 0;
                output(OUTPUT, recipe.get().value().assemble(new CncRecipe.Input(firstPage(stack(BOOK))), level.registryAccess()));
                shrink(IRON, 1);
                playSound(ModSounds.CNC_FINISHED.get(), 1.0F, 1.0F);
            }
        } else {
            progress = 0;
        }
        setActive(working);
        sounds(ready && powered, recipe.isPresent());

        Status newStatus;
        if (!powered) {
            newStatus = Status.OFF;
        } else if (!isRedstonePowered()) {
            newStatus = Status.OFFLINE;
        } else if (tank.getFluidAmount() <= 0) {
            newStatus = Status.NEED_WATER;
        } else if (!ready) {
            newStatus = Status.NOT_READY;
        } else if (recipe.isEmpty()) {
            newStatus = Status.INVALID_BOOK;
        } else {
            newStatus = Status.WORKING;
        }
        boolean iron = stack(IRON).is(Items.IRON_BLOCK);
        if (newStatus != status || stage() != stageBefore || iron != hasIron) {
            status = newStatus;
            hasIron = iron;
            syncToClient();
        }
    }

    /** The original's sound cues, keyed to the 17 animation steps. */
    private void sounds(boolean ready, boolean validBook) {
        boolean iron = stack(IRON).is(Items.IRON_BLOCK);
        if (iron && !hadIron) {
            playSound(ModSounds.CNC_INSERT_IRON_BLOCK.get(), 0.5F, 1.0F);
        }
        hadIron = iron;
        if (isRedstonePowered() && !hadRedstone) {
            playSound(ModSounds.CNC_POWER_ON.get(), 0.5F, 1.0F);
        }
        hadRedstone = isRedstonePowered();
        if (!ready) {
            return;
        }
        if (!validBook) {
            if (!warnedInvalid) {
                warnedInvalid = true;
                playSound(ModSounds.CNC_INVALID_BOOK.get(), 0.5F, 1.0F);
            }
            return;
        }
        long time = gameTime();
        int stage = stage();
        if (stage <= 6 && progress > 0) {
            if (time % 60 == 0) {
                playSound(ModSounds.CNC_PRESSING_WORK.get(), 0.5F, 1.0F);
            }
            if (stage < 4 && time % 40 == 0) {
                playSound(ModSounds.CNC_PRESS.get(), 0.5F, 1.0F);
            }
            if (stage == 4 && progress == CUT_TIME * 4 / STAGES + 1) {
                playSound(ModSounds.CNC_PRESS_STOP.get(), 1.0F, 1.0F);
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.666, worldPosition.getZ() + 0.5,
                            8, 0.3, 0.2, 0.3, 0.02);
                }
            }
        }
        if (stage >= 7) {
            if (progress == (CUT_TIME * 7 + STAGES - 1) / STAGES) {
                playSound(ModSounds.CNC_FINISH_CRUSHING.get(), 1.0F, 1.0F);
            }
            if (stage <= 15 && time % 80 == 0) {
                playSound(ModSounds.CNC_WATER_WORK.get(), 0.5F, 1.0F);
            }
            if (stage > 7 && stage <= 15 && time % 72 == 0) {
                playSound(ModSounds.CNC_WATER_FLOW.get(), 0.5F, 1.0F);
            }
            if (level instanceof ServerLevel server && time % 4 == 0) {
                server.sendParticles(ParticleTypes.SPLASH, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        3, 0.3, 0.1, 0.3, 0.1);
            }
        }
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putInt("Progress", progress);
        tag.putByte("Status", (byte) status.ordinal());
        tag.putBoolean("HasIron", stack(IRON).is(Items.IRON_BLOCK));
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        progress = tag.getInt("Progress");
        status = Status.values()[Math.min(Status.values().length - 1, tag.getByte("Status"))];
        hasIron = tag.getBoolean("HasIron");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", progress);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("Progress");
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }

    @Override
    protected int guiValueCount() {
        return 7;
    }

    @Override
    protected int getGuiValue(int index) {
        return switch (index) {
            case 0 -> energyStored();
            case 1 -> energyCapacity();
            case 2 -> progress;
            case 3 -> CUT_TIME;
            case 4 -> tank.getFluidAmount();
            case 5 -> TANK_CAPACITY;
            default -> status.ordinal();
        };
    }

    @Override
    public void addMenuSlots(MachineMenu menu) {
        menu.addMachineSlot(WATER_IN, 31, 34);
        menu.addMachineSlot(IRON, 67, 44);
        menu.addMachineSlot(BOOK, 67, 17);
        menu.addOutputSlot(OUTPUT, 131, 44);
        menu.addOutputSlot(BUCKET_OUT, 31, 9);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(ModMenus.CNC_MACHINE.get(), containerId, inventory, this);
    }
}

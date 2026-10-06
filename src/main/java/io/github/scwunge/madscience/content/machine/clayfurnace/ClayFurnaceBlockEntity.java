package io.github.scwunge.madscience.content.machine.clayfurnace;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.content.recipe.ProcessingRecipe;
import io.github.scwunge.madscience.registry.ModBlockEntities;
import io.github.scwunge.madscience.registry.ModMenus;
import io.github.scwunge.madscience.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Clay Furnace: an unpowered, one-shot ore cooker. Load a block of coal and an ore block, light it with flint and steel,
 * wait out the burn (7 minutes by default), then hit the smouldering furnace to break the clay shell. The red-hot
 * block inside cools over about 25 seconds (hit it before then and it turns to lava); once cooled, hit it again to
 * collect the metal block.
 */
public class ClayFurnaceBlockEntity extends MachineBlockEntity {
    public static final int COAL = 0, ORE = 1;
    /** Ticks per red-hot cooling frame, and how many frames, from the original. */
    public static final int COOL_FRAME_TICKS = 100, COOL_FRAMES = 5;

    public enum Phase { IDLE, BURNING, SMOULDERING, RED_HOT, COOLED }

    private Phase phase = Phase.IDLE;
    private int progress;
    private int maxProgress = 1;
    private int coolTicks;

    public ClayFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CLAY_FURNACE.get(), pos, state, 2, 0, 0, 0);
    }

    public Phase phase() {
        return phase;
    }

    /** Red-hot frame 0..4 while cooling. */
    public int coolFrame() {
        return Math.min(COOL_FRAMES - 1, coolTicks / COOL_FRAME_TICKS);
    }

    private Optional<RecipeHolder<ProcessingRecipe>> recipe() {
        return level == null ? Optional.empty() : ModRecipes.CLAY_SMELTING.find(level, stack(ORE));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (phase != Phase.IDLE) {
            return false;
        }
        return switch (slot) {
            case COAL -> stack.is(Items.COAL_BLOCK);
            case ORE -> level != null && ModRecipes.CLAY_SMELTING.find(level, stack).isPresent();
            default -> false;
        };
    }

    @Override
    public int[] slotsForFace(@Nullable Direction side) {
        return side == Direction.UP ? new int[]{COAL} : new int[]{ORE};
    }

    public boolean canLight() {
        return phase == Phase.IDLE && stack(COAL).is(Items.COAL_BLOCK) && recipe().isPresent();
    }

    /** Called when lit with flint and steel. */
    public boolean light() {
        if (!canLight()) {
            return false;
        }
        progress = 0;
        maxProgress = MadConfig.CLAY_FURNACE_SECONDS.get() * 20;
        setPhase(Phase.BURNING);
        return true;
    }

    /** Hitting the smouldering furnace breaks the clay shell and exposes the red-hot block. */
    public void breakShell() {
        if (phase == Phase.SMOULDERING) {
            playSound(SoundEvents.SAND_BREAK, 1.0F, 1.0F);
            playSound(SoundEvents.FIRE_EXTINGUISH, 1.0F, 1.0F);
            coolTicks = 0;
            setPhase(Phase.RED_HOT);
        }
    }

    /** The finished block, consuming the fuel and ore, or empty if the recipe went away. */
    public ItemStack takeResult() {
        Optional<RecipeHolder<ProcessingRecipe>> recipe = recipe();
        if (recipe.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = recipe.get().value().result().copy();
        shrink(COAL, 1);
        shrink(ORE, 1);
        return result;
    }

    /** The block the cooled furnace turns into, if the result is a block. */
    @Nullable
    public BlockState resultBlock() {
        Optional<RecipeHolder<ProcessingRecipe>> recipe = recipe();
        if (recipe.isPresent() && recipe.get().value().result().getItem() instanceof BlockItem blockItem) {
            return blockItem.getBlock().defaultBlockState();
        }
        return null;
    }

    private void setPhase(Phase newPhase) {
        if (phase != newPhase) {
            phase = newPhase;
            setActive(newPhase == Phase.BURNING);
            syncToClient();
        }
    }

    @Override
    protected void tickServer() {
        ServerLevel server = (ServerLevel) level;
        long time = gameTime();
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY(), z = worldPosition.getZ() + 0.5;
        switch (phase) {
            case BURNING -> {
                if (time % 20 == 0) {
                    server.sendParticles(ParticleTypes.FLAME, x, y + 0.65, z, 1, 0.1, 0.0, 0.1, 0.01);
                }
                if (time % 25 == 0) {
                    server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.5, z, 2, 0.2, 0.3, 0.2, 0.02);
                }
                if (time % 100 == 0 && level.random.nextBoolean()) {
                    playSound(SoundEvents.FIRE_AMBIENT, 1.0F, 1.0F);
                }
                if (!stack(COAL).is(Items.COAL_BLOCK)) {
                    // the fuel was taken out: the fire goes out
                    setPhase(Phase.IDLE);
                    progress = 0;
                    break;
                }
                if (++progress >= maxProgress) {
                    playSound(SoundEvents.FIRE_EXTINGUISH, 1.0F, 1.0F);
                    setPhase(Phase.SMOULDERING);
                }
            }
            case SMOULDERING -> {
                if (time % (20L * (1 + level.random.nextInt(4))) == 0) {
                    server.sendParticles(ParticleTypes.SMOKE, x, y + 0.65, z, 1, 0.1, 0.0, 0.1, 0.01);
                }
            }
            case RED_HOT -> {
                int frame = coolFrame();
                coolTicks++;
                if (coolFrame() != frame) {
                    playSound(SoundEvents.FIRE_EXTINGUISH, 1.0F, 1.0F);
                    server.sendParticles(ParticleTypes.SMOKE, x, y + 0.65, z, 3, 0.2, 0.1, 0.2, 0.01);
                    syncToClient();
                }
                if (coolTicks >= COOL_FRAME_TICKS * COOL_FRAMES) {
                    setPhase(Phase.COOLED);
                }
            }
            default -> {
            }
        }
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putByte("Phase", (byte) phase.ordinal());
        tag.putInt("CoolTicks", coolTicks);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        phase = Phase.values()[Math.min(Phase.values().length - 1, tag.getByte("Phase"))];
        coolTicks = tag.getInt("CoolTicks");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("Phase", (byte) phase.ordinal());
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("CoolTicks", coolTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        phase = Phase.values()[Math.min(Phase.values().length - 1, tag.getByte("Phase"))];
        progress = tag.getInt("Progress");
        maxProgress = Math.max(1, tag.getInt("MaxProgress"));
        coolTicks = tag.getInt("CoolTicks");
    }

    @Override
    protected int guiValueCount() {
        return 2;
    }

    @Override
    protected int getGuiValue(int index) {
        return index == 0 ? progress : maxProgress;
    }

    @Override
    public void addMenuSlots(MachineMenu menu) {
        menu.addMachineSlot(COAL, 62, 50);
        menu.addMachineSlot(ORE, 98, 50);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(ModMenus.CLAY_FURNACE.get(), containerId, inventory, this);
    }

    /** Drops what's inside unless the ore has already been turned into the result. */
    @Override
    public void dropContents(net.minecraft.world.level.Level level, BlockPos pos) {
        if (phase == Phase.IDLE || phase == Phase.BURNING) {
            super.dropContents(level, pos);
        }
    }
}

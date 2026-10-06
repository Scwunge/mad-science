package io.github.scwunge.madscience.content.machine.soniclocator;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.registry.ModBlockEntities;
import io.github.scwunge.madscience.registry.ModMenus;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Soniclocator: give it gravel and a sample of the block you want, plus power and a redstone signal. It charges for
 * 400 ticks, then thumps: one matching block somewhere in its chunk (searched up to y 127) is swapped for a piece of
 * gravel and lands in its output. Each thump halves its stored energy, withers and blinds everything within 16 blocks,
 * and needs five seconds to cool down. Two Soniclocators thumping too close together blow each other up.
 */
public class SoniclocatorBlockEntity extends MachineBlockEntity {
    public static final int GRAVEL_IN = 0, TARGET = 1, OUTPUT = 2;
    public static final int MAX_CHARGE = 400;
    private static final int COOLDOWN_FRAMES = 5;

    /** Loaded Soniclocators, for the "too close together" rule. */
    private static final Set<GlobalPos> LOADED = ConcurrentHashMap.newKeySet();

    public enum State { OFF, UNDERVOLT, IDLE, CHARGING, EMPTY, COOLDOWN }

    private int charge;
    private int cooldownFrame = -1;
    private long thumps;
    private long lastTargets;
    private State state = State.OFF;

    public SoniclocatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SONICLOCATOR.get(), pos, blockState, 3, MadConfig.fe(100_000), MadConfig.fe(200), 0);
    }

    public State state() {
        return state;
    }

    public int charge() {
        return charge;
    }

    public int cooldownFrame() {
        return Math.max(0, cooldownFrame);
    }

    /** Called when a player places it: the original greeted you with a start-up sound. */
    @Override
    public void setOwner(@Nullable java.util.UUID owner) {
        super.setOwner(owner);
        playSound(ModSounds.SONICLOCATOR_PLACE.get(), 1.0F, 1.0F);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            LOADED.add(GlobalPos.of(level.dimension(), worldPosition));
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) {
            LOADED.remove(GlobalPos.of(level.dimension(), worldPosition));
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null) {
            LOADED.remove(GlobalPos.of(level.dimension(), worldPosition));
        }
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case GRAVEL_IN -> stack.is(Items.GRAVEL);
            case TARGET -> stack.getItem() instanceof BlockItem;
            default -> false;
        };
    }

    @Override
    protected void onInventoryChanged(int slot) {
        super.onInventoryChanged(slot);
        // a new target means a fresh search
        if (slot == TARGET) {
            thumps = 0;
            lastTargets = 0;
        }
    }

    @Override
    public int[] slotsForFace(@Nullable Direction side) {
        return side == Direction.UP ? new int[]{GRAVEL_IN} : side == Direction.DOWN ? new int[]{OUTPUT} : new int[]{TARGET};
    }

    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return slot == OUTPUT;
    }

    private boolean canWork() {
        ItemStack target = stack(TARGET);
        if (!stack(GRAVEL_IN).is(Items.GRAVEL) || !(target.getItem() instanceof BlockItem)) {
            return false;
        }
        ItemStack out = stack(OUTPUT);
        return out.isEmpty() || (ItemStack.isSameItem(out, target) && out.getCount() < out.getMaxStackSize());
    }

    private boolean outOfTargets() {
        return thumps > 0 && lastTargets <= 0;
    }

    private void setState(State newState) {
        if (state != newState) {
            state = newState;
            syncToClient();
        }
    }

    @Override
    protected void tickServer() {
        boolean redstone = isRedstonePowered();
        boolean powered = isPowered();
        if (powered && redstone) {
            energy.consume(MadConfig.fe(1));
        }
        long time = gameTime();
        boolean ready = powered && redstone && canWork();

        if (cooldownFrame >= 0) {
            if (!(powered && redstone)) {
                cooldownFrame = -1;
            } else {
                ((ServerLevel) level).sendParticles(ParticleTypes.EXPLOSION, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                        1, 0.3, 0.5, 0.3, 0.0);
                if (time % 20 == 0) {
                    playSound(ModSounds.SONICLOCATOR_COOLDOWN_BEEP.get(), 1.0F, 1.0F);
                    cooldownFrame++;
                    syncToClient();
                    if (cooldownFrame >= COOLDOWN_FRAMES) {
                        cooldownFrame = -1;
                        playSound(ModSounds.SONICLOCATOR_COOLDOWN.get(), 1.0F, 1.0F);
                    }
                }
                setState(State.COOLDOWN);
                charge = 0;
                return;
            }
        }

        if (!redstone) {
            charge = 0;
            setState(State.OFF);
        } else if (!powered) {
            charge = 0;
            setState(State.UNDERVOLT);
            if (time % 20 == 0) {
                playSound(ModSounds.SONICLOCATOR_COOLDOWN_BEEP.get(), 1.0F, 1.0F);
            }
        } else if (!ready) {
            charge = 0;
            setState(State.IDLE);
        } else if (outOfTargets()) {
            charge = 0;
            setState(State.EMPTY);
            if (time % 20 == 0) {
                playSound(ModSounds.SONICLOCATOR_EMPTY.get(), 1.0F, 1.0F);
            }
        } else {
            setState(State.CHARGING);
            if (charge == 0) {
                playSound(ModSounds.SONICLOCATOR_THUMP_START.get(), 0.42F, 1.0F);
            }
            charge++;
            if (time % 20 == 0) {
                playSound(ModSounds.SONICLOCATOR_THUMP_CHARGE.get(), 0.42F, 1.0F);
            }
            if (time % 34 == 0) {
                playSound(ModSounds.SONICLOCATOR_IDLE_CHARGED.get(), 0.42F, Math.min(2.0F, charge * 0.1F));
            }
            if (time % 10 == 0) {
                syncToClient(); // thumpers rise with the charge
            }
            if (charge >= MAX_CHARGE) {
                charge = 0;
                thump();
                cooldownFrame = 0;
                syncToClient();
            }
        }
        if (ready && time % 46 == 0) {
            playSound(ModSounds.SONICLOCATOR_IDLE.get(), 1.0F, 1.0F);
        }
    }

    private void thump() {
        ServerLevel server = (ServerLevel) level;
        if (blowUpIfCrowded(server)) {
            return;
        }
        ItemStack target = stack(TARGET);
        Block targetBlock = ((BlockItem) target.getItem()).getBlock();
        List<BlockPos> found = findTargets(server, targetBlock);
        thumps++;
        lastTargets = found.size();
        if (found.isEmpty()) {
            playSound(ModSounds.SONICLOCATOR_EMPTY.get(), 1.0F, 1.0F);
            return;
        }
        BlockPos pick = pickAllowed(server, found);
        if (pick == null) {
            // everything it found is protected
            playSound(ModSounds.SONICLOCATOR_EMPTY.get(), 1.0F, 1.0F);
            return;
        }
        // the original popped a tiny explosion at both ends; these are cosmetic and break nothing
        server.explode(null, pick.getX(), pick.getY(), pick.getZ(), 0.42F, Level.ExplosionInteraction.NONE);
        server.explode(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 0.42F, Level.ExplosionInteraction.NONE);
        server.setBlock(pick, Blocks.GRAVEL.defaultBlockState(), 3);

        playSound(ModSounds.SONICLOCATOR_THUMP.get(), 10.0F, 1.0F);
        AABB area = new AABB(worldPosition).inflate(16);
        for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, area)) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 200));
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60));
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200));
        }
        if (energy.getEnergyStored() > 0) {
            energy.consume(energy.getEnergyStored() / 2);
        }
        output(OUTPUT, new ItemStack(target.getItem()));
        shrink(GRAVEL_IN, 1);
        playSound(ModSounds.SONICLOCATOR_FINISH.get(), 10.0F, 1.0F);
    }

    /** A random match the owner may dig up (claims and protection mods are asked), or null. */
    @Nullable
    private BlockPos pickAllowed(ServerLevel server, List<BlockPos> found) {
        java.util.Collections.shuffle(found, new java.util.Random(server.random.nextLong()));
        for (int i = 0; i < Math.min(found.size(), 32); i++) {
            if (mayBreak(server, found.get(i))) {
                return found.get(i);
            }
        }
        return null;
    }

    /** Matching blocks in this machine's chunk. */
    private List<BlockPos> findTargets(ServerLevel server, Block targetBlock) {
        LevelChunk chunk = server.getChunkAt(worldPosition);
        int top = Math.min(MadConfig.SONICLOCATOR_SCAN_TOP.get(), server.getMaxBuildHeight() - 1);
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        List<BlockPos> found = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = server.getMinBuildHeight(); y <= top; y++) {
                    cursor.set(minX + x, y, minZ + z);
                    if (chunk.getBlockState(cursor).is(targetBlock)) {
                        found.add(cursor.immutable());
                    }
                }
            }
        }
        return found;
    }

    /** The original's rule: two Soniclocators in range of each other both explode when one thumps. */
    private boolean blowUpIfCrowded(ServerLevel server) {
        int range = MadConfig.SONICLOCATOR_CONFLICT_RANGE.get();
        if (range <= 0) {
            return false;
        }
        for (GlobalPos other : LOADED) {
            if (!other.dimension().equals(server.dimension()) || other.pos().equals(worldPosition)
                    || other.pos().distSqr(worldPosition) >= (double) range * range) {
                continue;
            }
            if (!server.isLoaded(other.pos()) || !(server.getBlockEntity(other.pos()) instanceof SoniclocatorBlockEntity live) || live.isRemoved()) {
                LOADED.remove(other); // stale entry
                continue;
            }
            for (BlockPos pos : new BlockPos[]{other.pos(), worldPosition}) {
                server.playSound(null, pos, ModSounds.SONICLOCATOR_EXPLODE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                server.destroyBlock(pos, false);
                server.explode(null, pos.getX(), pos.getY(), pos.getZ(), 6.0F, true, Level.ExplosionInteraction.TNT);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putByte("State", (byte) state.ordinal());
        tag.putInt("Charge", charge);
        tag.putInt("Cooldown", cooldownFrame);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        state = State.values()[Math.min(State.values().length - 1, tag.getByte("State"))];
        charge = tag.getInt("Charge");
        cooldownFrame = tag.getInt("Cooldown");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Charge", charge);
        tag.putInt("Cooldown", cooldownFrame);
        tag.putLong("Thumps", thumps);
        tag.putLong("LastTargets", lastTargets);
        tag.putByte("State", (byte) state.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        charge = tag.getInt("Charge");
        cooldownFrame = tag.contains("Cooldown") ? tag.getInt("Cooldown") : -1;
        thumps = tag.getLong("Thumps");
        lastTargets = tag.getLong("LastTargets");
        state = State.values()[Math.min(State.values().length - 1, tag.getByte("State"))];
    }

    @Override
    protected int guiValueCount() {
        return 6;
    }

    @Override
    protected int getGuiValue(int index) {
        return switch (index) {
            case 0 -> energyStored();
            case 1 -> energyCapacity();
            case 2 -> charge;
            case 3 -> MAX_CHARGE;
            case 4 -> (int) lastTargets;
            default -> (int) thumps;
        };
    }

    @Override
    public void addMenuSlots(MachineMenu menu) {
        menu.addMachineSlot(GRAVEL_IN, 27, 35);
        menu.addMachineSlot(TARGET, 58, 35);
        menu.addOutputSlot(OUTPUT, 127, 35);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(ModMenus.SONICLOCATOR.get(), containerId, inventory, this);
    }
}

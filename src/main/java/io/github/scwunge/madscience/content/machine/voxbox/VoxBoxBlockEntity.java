package io.github.scwunge.madscience.content.machine.voxbox;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.registry.ModBlockEntities;
import io.github.scwunge.madscience.registry.ModMenus;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Announcement System (the VoxBox): put a written book in, give it power and a redstone pulse, and it reads the book
 * out to everyone nearby. The original spoke the words with Half-Life's announcer clips, which aren't the Mad Science
 * authors' to give away, so this port shows the announcement in chat with a chime instead.
 */
public class VoxBoxBlockEntity extends MachineBlockEntity {
    public static final int BOOK = 0;
    /** Energy per word, from the original (VOXBOX_CONSUME = 128). */
    private static final int ENERGY_PER_WORD = 128;
    /** The original's average clip length per word, in ticks. */
    private static final int TICKS_PER_WORD = 9;

    private boolean wasPowered;
    private int talkTicks;

    public VoxBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VOX_BOX.get(), pos, state, 1, MadConfig.fe(25_000), MadConfig.fe(200), 0);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(Items.WRITTEN_BOOK);
    }

    @Override
    public int[] slotsForFace(@Nullable Direction side) {
        return new int[]{BOOK};
    }

    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return true;
    }

    /** The book's text as one line, or null if there is no readable book. */
    @Nullable
    public String announcement() {
        WrittenBookContent content = stack(BOOK).get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (Component page : content.getPages(false)) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(page.getString());
        }
        String result = text.toString().replaceAll("\\s+", " ").trim();
        return result.isEmpty() ? null : result;
    }

    /** Reads the book out to players within range. Returns whether it spoke. */
    public boolean announce() {
        String text = announcement();
        if (text == null || !(level instanceof ServerLevel server)) {
            return false;
        }
        int words = text.split(" ").length;
        int cost = MadConfig.fe(ENERGY_PER_WORD) * words;
        if (energy.getEnergyStored() <= 0) {
            return false;
        }
        energy.consume(cost);
        Component message = Component.translatable("chat.madscience.vox_box", Component.literal(text).withStyle(ChatFormatting.YELLOW))
                .withStyle(ChatFormatting.GOLD);
        double range = MadConfig.VOX_BOX_RANGE.get();
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(worldPosition.getCenter()) <= range * range) {
                player.sendSystemMessage(message);
            }
        }
        server.playSound(null, worldPosition, ModSounds.VOX_BOX_CHIME.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        talkTicks = Math.max(20, words * TICKS_PER_WORD);
        setActive(true);
        return true;
    }

    @Override
    protected void tickServer() {
        boolean powered = isRedstonePowered();
        if (powered && !wasPowered && isPowered()) {
            announce();
        }
        wasPowered = powered;
        if (talkTicks > 0 && --talkTicks == 0) {
            setActive(false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("WasPowered", wasPowered);
        tag.putInt("TalkTicks", talkTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        wasPowered = tag.getBoolean("WasPowered");
        talkTicks = tag.getInt("TalkTicks");
    }

    @Override
    protected int guiValueCount() {
        return 2;
    }

    @Override
    protected int getGuiValue(int index) {
        return index == 0 ? energyStored() : energyCapacity();
    }

    @Override
    public void addMenuSlots(MachineMenu menu) {
        menu.addMachineSlot(BOOK, 79, 25);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(ModMenus.VOX_BOX.get(), containerId, inventory, this);
    }
}

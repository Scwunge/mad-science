package io.github.scwunge.madscience.content.weapon;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.content.item.TooltipItem;
import io.github.scwunge.madscience.registry.ModDataComponents;
import io.github.scwunge.madscience.registry.ModItems;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Pulse rifle. Hold attack to fire: in rifle mode a round leaves every 12 ticks while rounds last; in grenade mode each
 * press launches one grenade. Use to switch modes; sneak-use to reload (from the fullest magazine, or up to four
 * grenades) or, if loaded, to unload. Ammo lives on the item, so it's all server-side.
 */
public class PulseRifleItem extends TooltipItem {
    public PulseRifleItem(Properties properties) {
        super(properties.stacksTo(1).component(ModDataComponents.RIFLE.get(), RifleState.EMPTY));
    }

    public static RifleState state(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.RIFLE.get(), RifleState.EMPTY);
    }

    private static void playAt(Player player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack rifle = player.getItemInHand(hand);
        if (!level.isClientSide) {
            RifleState state = state(rifle);
            if (player.isShiftKeyDown()) {
                rifle.set(ModDataComponents.RIFLE.get(), state.grenadeMode() ? cycleGrenades(player, state) : cycleMagazine(player, state));
            } else {
                rifle.set(ModDataComponents.RIFLE.get(), state.withGrenadeMode(!state.grenadeMode()));
                playAt(player, ModSounds.PULSE_RIFLE_EMPTY.get(), 1.0F, 0.42F);
            }
            showAmmo(player, state(rifle));
        }
        return InteractionResultHolder.sidedSuccess(rifle, level.isClientSide);
    }

    /** Unloads the magazine (with its rounds), or loads the fullest one in the inventory. */
    private static RifleState cycleMagazine(Player player, RifleState state) {
        if (state.rounds() > 0) {
            give(player, magazine(state.rounds()));
            playAt(player, ModSounds.PULSE_RIFLE_UNLOAD.get(), 1.0F, 1.0F);
            return state.withMagazine(false, 0);
        }
        if (state.magazineInserted()) {
            // the spent magazine comes back out first
            give(player, magazine(0));
            state = state.withMagazine(false, 0);
        }
        int best = -1, bestRounds = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            int rounds = stack.is(ModItems.MAGAZINE.get()) ? stack.getOrDefault(ModDataComponents.ROUNDS.get(), 0) : 0;
            if (rounds > bestRounds) {
                best = i;
                bestRounds = rounds;
            }
        }
        if (best < 0) {
            playAt(player, ModSounds.PULSE_RIFLE_EMPTY.get(), 0.5F, 1.0F);
            return state;
        }
        player.getInventory().getItem(best).shrink(1);
        playAt(player, ModSounds.PULSE_RIFLE_RELOAD.get(), 1.0F, 1.0F);
        return state.withMagazine(true, bestRounds);
    }

    /** Unloads all grenades, or loads up to four from the inventory. */
    private static RifleState cycleGrenades(Player player, RifleState state) {
        if (state.grenades() > 0) {
            give(player, new ItemStack(ModItems.GRENADE.get(), state.grenades()));
            playAt(player, ModSounds.PULSE_RIFLE_UNLOAD.get(), 1.0F, 1.0F);
            return state.withGrenades(0);
        }
        int loaded = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && loaded < RifleState.MAX_GRENADES; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.GRENADE.get())) {
                int take = Math.min(stack.getCount(), RifleState.MAX_GRENADES - loaded);
                stack.shrink(take);
                loaded += take;
            }
        }
        if (loaded > 0) {
            playAt(player, ModSounds.PULSE_RIFLE_RELOAD_GRENADE.get(), 1.0F, 1.0F);
        } else {
            playAt(player, ModSounds.PULSE_RIFLE_EMPTY.get(), 0.5F, 1.0F);
        }
        return state.withGrenades(loaded);
    }

    public static ItemStack magazine(int rounds) {
        ItemStack magazine = new ItemStack(ModItems.MAGAZINE.get());
        magazine.set(ModDataComponents.ROUNDS.get(), rounds);
        return magazine;
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /**
     * One tick of the trigger being held. {@code firstTick} is true on the tick it was pressed. Returns whether
     * anything left the barrel.
     */
    public static boolean triggerTick(ServerPlayer player, ItemStack rifle, boolean firstTick) {
        if (!MadConfig.PULSE_RIFLE_ENABLED.get()) {
            if (firstTick) {
                player.displayClientMessage(Component.translatable("message.madscience.pulse_rifle_disabled"), true);
            }
            return false;
        }
        RifleState state = state(rifle);
        Level level = player.level();
        if (state.grenadeMode()) {
            if (!firstTick) {
                return false;
            }
            if (state.grenades() <= 0) {
                playAt(player, ModSounds.PULSE_RIFLE_EMPTY.get(), 0.5F, 1.0F);
                return false;
            }
            PulseRifleGrenade grenade = new PulseRifleGrenade(level, player, rifle);
            grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(grenade);
            playAt(player, ModSounds.PULSE_RIFLE_FIRE_GRENADE.get(), 1.0F, 1.0F);
            state = state.withGrenades(state.grenades() - 1);
        } else {
            if (state.rounds() <= 0) {
                if (firstTick) {
                    playAt(player, ModSounds.PULSE_RIFLE_EMPTY.get(), 0.5F, 1.0F);
                }
                return false;
            }
            if (!firstTick && level.getGameTime() % 12 != 0) {
                return false;
            }
            PulseRifleRound round = new PulseRifleRound(level, player, rifle);
            round.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 4.2F * 1.5F, 1.0F);
            level.addFreshEntity(round);
            playAt(player, ModSounds.PULSE_RIFLE_FIRE.get(), 0.75F, 1.0F);
            state = state.withRounds(state.rounds() - 1);
        }
        rifle.set(ModDataComponents.RIFLE.get(), state);
        showAmmo(player, state);
        return true;
    }

    /** After a grenade launch, letting go of the trigger racks the next one in. */
    public static void chamberGrenade(ServerPlayer player) {
        playAt(player, ModSounds.PULSE_RIFLE_CHAMBER_GRENADE.get(), 1.0F, 1.0F);
    }

    private static void showAmmo(Player player, RifleState state) {
        player.displayClientMessage(Component.translatable("message.madscience.pulse_rifle_ammo", state.rounds(), RifleState.MAX_ROUNDS,
                state.grenades(), RifleState.MAX_GRENADES, Component.translatable(state.grenadeMode()
                        ? "message.madscience.pulse_rifle_mode_grenade" : "message.madscience.pulse_rifle_mode_rifle")), true);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // ammo counts change with every shot; only a real swap should bob the rifle
        return slotChanged || !newStack.is(oldStack.getItem());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        // a rifle put away mid-burst, or left firing by a logout, stops showing as firing
        if (!level.isClientSide && state(stack).firing() && !(selected && RifleTrigger.isHeld(entity))) {
            stack.set(ModDataComponents.RIFLE.get(), state(stack).withFiring(false));
        }
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        RifleState state = state(stack);
        tooltip.add(Component.translatable("tooltip.madscience.pulse_rifle_ammo", state.rounds(), state.grenades()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(state.grenadeMode() ? "message.madscience.pulse_rifle_mode_grenade" : "message.madscience.pulse_rifle_mode_rifle")
                .withStyle(state.grenadeMode() ? ChatFormatting.AQUA : ChatFormatting.RED));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

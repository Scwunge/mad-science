package io.github.scwunge.madscience.content.sign;

import io.github.scwunge.madscience.registry.ModEntities;
import io.github.scwunge.madscience.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A one-block warning sign hung on a wall. Its owner changes the symbol by sneaking and hitting it, and takes it down
 * with a normal hit; anyone else who hits it just reads what it warns about (server operators can still take it down).
 */
public class WarningSignEntity extends HangingEntity {
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(WarningSignEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID owner;
    private String ownerName = "";

    public WarningSignEntity(EntityType<? extends WarningSignEntity> type, Level level) {
        super(type, level);
    }

    public WarningSignEntity(Level level, BlockPos pos, Direction direction, @Nullable Player owner) {
        super(ModEntities.WARNING_SIGN.get(), level, pos);
        setDirection(direction);
        if (owner != null) {
            this.owner = owner.getUUID();
            this.ownerName = owner.getGameProfile().getName();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, WarningSignType.GENERIC_WARNING.ordinal());
    }

    public WarningSignType signType() {
        return WarningSignType.byId(entityData.get(TYPE));
    }

    public void setSignType(WarningSignType type) {
        entityData.set(TYPE, type.ordinal());
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public boolean mayEdit(Player player) {
        return owner == null || owner.equals(player.getUUID()) || player.hasPermissions(2);
    }

    @Override
    public boolean skipAttackInteraction(Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (level().isClientSide) {
            return true;
        }
        if (player.isShiftKeyDown()) {
            if (!mayEdit(player)) {
                player.displayClientMessage(Component.translatable("message.madscience.warning_sign_owned", ownerName), false);
                return true;
            }
            WarningSignType next = signType().next();
            setSignType(next);
            player.displayClientMessage(Component.translatable("message.madscience.warning_sign_type", next.ordinal() + 1,
                    WarningSignType.values().length, next.description()), true);
            return true;
        }
        if (!mayEdit(player)) {
            player.displayClientMessage(signType().description(), true);
            return true;
        }
        return super.skipAttackInteraction(entity);
    }

    @Override
    protected AABB calculateBoundingBox(BlockPos pos, Direction direction) {
        Vec3 centre = Vec3.atCenterOf(pos).relative(direction, -0.46875);
        Direction.Axis axis = direction.getAxis();
        return AABB.ofSize(centre, axis == Direction.Axis.X ? 0.0625 : 1, 1, axis == Direction.Axis.Z ? 0.0625 : 1);
    }

    @Override
    public void dropItem(@Nullable Entity brokenBy) {
        if (!level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            return;
        }
        playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
        if (brokenBy instanceof Player player && player.hasInfiniteMaterials()) {
            return;
        }
        spawnAtLocation(new ItemStack(ModItems.WARNING_SIGN.get()));
    }

    @Override
    public void playPlacementSound() {
        playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("Sign", signType().getSerializedName());
        tag.putByte("facing", (byte) direction.get2DDataValue());
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        setSignType(WarningSignType.byName(tag.getString("Sign")));
        direction = Direction.from2DDataValue(tag.getByte("facing"));
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        super.readAdditionalSaveData(tag);
        setDirection(direction);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        return new ClientboundAddEntityPacket(this, direction.get3DDataValue(), getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        setDirection(Direction.from3DDataValue(packet.getData()));
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.WARNING_SIGN.get());
    }
}

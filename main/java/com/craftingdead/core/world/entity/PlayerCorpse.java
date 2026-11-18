package com.craftingdead.core.world.entity;

import com.craftingdead.core.ServerConfig;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.craftingdead.core.world.item.equipment.Equipment;
import java.util.Collection;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PlayerCorpse extends Entity implements MenuProvider {

  public static final int CONTAINER_SIZE = 54;
  private static final int LIFETIME_TICKS = 20 * 60 * 5;
  private static final EntityDataAccessor<Integer> PARTS = SynchedEntityData.defineId(
      PlayerCorpse.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(
      PlayerCorpse.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<String> OWNER_UUID = SynchedEntityData.defineId(
      PlayerCorpse.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<Boolean> FULL_SIZE = SynchedEntityData.defineId(
      PlayerCorpse.class, EntityDataSerializers.BOOLEAN);

  private SimpleContainer container = new SimpleContainer(CONTAINER_SIZE);
  private int lifetime;

  public PlayerCorpse(EntityType<?> type, Level level) {
    super(type, level);
    this.noPhysics = false;
  }

  public static void create(ServerPlayer player, Collection<ItemEntity> drops,
      PlayerExtension<?> extension) {
    PlayerCorpse corpse = new PlayerCorpse(ModEntityTypes.PLAYER_CORPSE.get(), player.level());
    corpse.setPos(player.getX(), player.getY() + 0.05D, player.getZ());
    corpse.setYRot(player.getYRot());
    corpse.setOwner(player.getUUID(), player.getGameProfile().getName());
    corpse.setFullSize(ServerConfig.instance.corpseSizeMode.get()
        == ServerConfig.CorpseSizeMode.FULL);

    for (ItemEntity drop : drops) {
      corpse.addItem(drop.getItem());
    }
    drops.clear();

    if (extension != null) {
      for (Equipment.Slot slot : Equipment.Slot.values()) {
        ItemStack stack = extension.getItemInSlot(slot);
        if (!stack.isEmpty()) {
          corpse.addItem(stack.copy());
          extension.setItemInSlot(slot, ItemStack.EMPTY);
        }
      }
      ItemStack handcuffs = extension.getHandcuffs();
      if (!handcuffs.isEmpty()) {
        corpse.addItem(handcuffs.copy());
        extension.setHandcuffs(ItemStack.EMPTY);
      }
    }
    player.level().addFreshEntity(corpse);
  }

  private void addItem(ItemStack stack) {
    ItemStack remaining = this.container.addItem(stack.copy());
    if (!remaining.isEmpty() && !this.level().isClientSide()) {
      this.spawnAtLocation(remaining);
    }
  }

  private void setOwner(UUID uuid, String name) {
    this.entityData.set(OWNER_UUID, uuid.toString());
    this.entityData.set(OWNER_NAME, name);
  }

  public UUID getOwnerUuid() {
    try {
      return UUID.fromString(this.entityData.get(OWNER_UUID));
    } catch (IllegalArgumentException ignored) {
      return new UUID(0L, 0L);
    }
  }

  public int getRemainingParts() {
    return this.entityData.get(PARTS);
  }

  public boolean isFullSize() {
    return this.entityData.get(FULL_SIZE);
  }

  private void setFullSize(boolean fullSize) {
    this.entityData.set(FULL_SIZE, fullSize);
    this.refreshDimensions();
  }

  @Override
  public EntityDimensions getDimensions(Pose pose) {
    return this.isFullSize()
        ? EntityDimensions.fixed(1.8F, 0.6F)
        : EntityDimensions.fixed(0.8F, 0.35F);
  }

  @Override
  public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
    super.onSyncedDataUpdated(key);
    if (FULL_SIZE.equals(key)) {
      this.refreshDimensions();
    }
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    builder.define(PARTS, 5);
    builder.define(OWNER_NAME, "");
    builder.define(OWNER_UUID, "");
    builder.define(FULL_SIZE, false);
  }

  @Override
  public void tick() {
    super.tick();
    if (!this.onGround()) {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.08D, 0.0D));
      this.move(MoverType.SELF, this.getDeltaMovement());
    } else {
      this.setDeltaMovement(Vec3.ZERO);
    }
    if (!this.level().isClientSide() && ++this.lifetime >= LIFETIME_TICKS) {
      this.discard();
    }
  }

  @Override
  public boolean isPickable() {
    return true;
  }

  @Override
  public InteractionResult interactAt(Player player, Vec3 location, InteractionHand hand) {
    if (!this.level().isClientSide()) {
      player.openMenu(this);
    }
    return InteractionResult.sidedSuccess(this.level().isClientSide());
  }

  @Override
  public boolean hurt(DamageSource source, float amount) {
    if (this.level().isClientSide() || this.isRemoved()) {
      return true;
    }
    if (source.getEntity() instanceof Player player
        && player.getMainHandItem().isEmpty()) {
      int remaining = this.getRemainingParts() - 1;
      this.entityData.set(PARTS, remaining);
      if (remaining <= 0) {
        this.discard();
      }
      return true;
    }
    return false;
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("container.craftingdead.player_corpse",
        this.entityData.get(OWNER_NAME));
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    return ChestMenu.sixRows(id, inventory, this.container);
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag tag) {
    NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    ContainerHelper.loadAllItems(tag, items, this.registryAccess());
    this.container = new SimpleContainer(items.toArray(ItemStack[]::new));
    this.lifetime = tag.getInt("Lifetime");
    this.entityData.set(PARTS, tag.getInt("Parts"));
    this.entityData.set(OWNER_NAME, tag.getString("OwnerName"));
    this.entityData.set(OWNER_UUID, tag.getString("OwnerUuid"));
    this.setFullSize(tag.getBoolean("FullSize"));
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag tag) {
    NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    for (int i = 0; i < CONTAINER_SIZE; i++) {
      items.set(i, this.container.getItem(i));
    }
    ContainerHelper.saveAllItems(tag, items, this.registryAccess());
    tag.putInt("Lifetime", this.lifetime);
    tag.putInt("Parts", this.getRemainingParts());
    tag.putString("OwnerName", this.entityData.get(OWNER_NAME));
    tag.putString("OwnerUuid", this.entityData.get(OWNER_UUID));
    tag.putBoolean("FullSize", this.isFullSize());
  }
}

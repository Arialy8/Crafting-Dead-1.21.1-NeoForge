package com.craftingdead.survival.world.entity;

import java.util.Comparator;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import com.craftingdead.core.world.damagesource.ModDamageSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import org.jetbrains.annotations.Nullable;

public class SupplyDrop extends Entity implements MenuProvider {

  private static final TicketType<ChunkPos> TICKET = TicketType.create("supply_drop",
      Comparator.comparingLong(ChunkPos::toLong));
  private static final int CONTAINER_SIZE = 54;
  private static final int LIFETIME_TICKS = 20 * 60 * 5;
  private static final float MAX_DURABILITY = 100.0F;
  private static final EntityDataAccessor<Boolean> LANDED = SynchedEntityData.defineId(
      SupplyDrop.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Float> DURABILITY = SynchedEntityData.defineId(
      SupplyDrop.class, EntityDataSerializers.FLOAT);

  private SimpleContainer container = new SimpleContainer(CONTAINER_SIZE);
  @Nullable private ResourceKey<LootTable> lootTable;
  private long lootTableSeed;
  private int groundedTicks;
  private boolean chunkLoaded;

  public SupplyDrop(EntityType<?> type, Level level) {
    super(type, level);
  }

  public SupplyDrop(EntityType<?> type, Level level, ResourceKey<LootTable> lootTable,
      long lootTableSeed, double x, double y, double z) {
    this(type, level);
    this.lootTable = lootTable;
    this.lootTableSeed = lootTableSeed;
    this.setPos(x, y, z);
    this.setDeltaMovement(Vec3.ZERO);
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    builder.define(LANDED, false);
    builder.define(DURABILITY, MAX_DURABILITY);
  }

  public boolean hasLanded() {
    return this.entityData.get(LANDED);
  }

  @Override
  public void tick() {
    super.tick();
    if (!this.level().isClientSide() && !this.chunkLoaded
        && this.level() instanceof ServerLevel serverLevel) {
      ChunkPos chunkPos = new ChunkPos(this.blockPosition());
      serverLevel.getChunkSource().addRegionTicket(TICKET, chunkPos, 1, chunkPos);
      this.chunkLoaded = true;
    }
    if (!this.onGround()) {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.0025D, 0.0D));
      if (this.getDeltaMovement().y < -0.12D) {
        this.setDeltaMovement(this.getDeltaMovement().x, -0.12D, this.getDeltaMovement().z);
      }
      this.groundedTicks = 0;
    } else {
      this.setDeltaMovement(Vec3.ZERO);
      if (!this.hasLanded()) {
        this.entityData.set(LANDED, true);
      }
      if (++this.groundedTicks >= LIFETIME_TICKS) {
        this.discard();
        return;
      }
    }
    this.move(MoverType.SELF, this.getDeltaMovement());
  }

  @Override
  public boolean isPickable() {
    return true;
  }

  @Override
  public InteractionResult interactAt(Player player, Vec3 location, InteractionHand hand) {
    if (!this.level().isClientSide() && this.onGround()) {
      player.openMenu(this);
    }
    return InteractionResult.sidedSuccess(this.level().isClientSide());
  }

  @Override
  public boolean hurt(DamageSource source, float amount) {
    if (this.level().isClientSide() || this.isRemoved()) {
      return true;
    }
    if (this.isInvulnerableTo(source)) {
      return false;
    }
    if (source.is(ModDamageSource.GRENADE_TYPE)) {
      this.level().playSound(null, this.blockPosition(),
          net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
          net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
      if (this.level() instanceof ServerLevel serverLevel) {
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
            this.getX(), this.getY() + 0.5D, this.getZ(),
            1, 0.0D, 0.0D, 0.0D, 0.0D);
      }
      this.kill();
      return true;
    }
    return false;
  }

  @Override
  public void remove(RemovalReason reason) {
    if (!this.level().isClientSide() && reason.shouldDestroy()) {
      Containers.dropContents(this.level(), this, this.container);
    }
    if (!this.level().isClientSide() && this.chunkLoaded
        && this.level() instanceof ServerLevel serverLevel) {
      ChunkPos chunkPos = new ChunkPos(this.blockPosition());
      serverLevel.getChunkSource().removeRegionTicket(TICKET, chunkPos, 1, chunkPos);
      this.chunkLoaded = false;
    }
    super.remove(reason);
  }

  private void unpackLoot(ServerPlayer player) {
    if (this.lootTable == null || !(this.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    ResourceKey<LootTable> key = this.lootTable;
    LootTable table = serverLevel.getServer().reloadableRegistries().getLootTable(key);
    CriteriaTriggers.GENERATE_LOOT.trigger(player, key);
    this.lootTable = null;
    String path = key.location().getPath();
    if (key.location().getNamespace().equals("craftingdeadimmerse")
        && path.matches("rank_drops/rank([1-9]|1[0-9])")) {
      int rank = Integer.parseInt(path.substring("rank_drops/rank".length()));
      if (com.craftingdead.immerse.world.airdrop.RankRewardData.get(serverLevel.getServer())
          .fillCustom(rank, this.container, net.minecraft.util.RandomSource.create(this.lootTableSeed)))
        return;
    }
    LootParams params = new LootParams.Builder(serverLevel)
        .withParameter(LootContextParams.ORIGIN, this.position())
        .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
        .withLuck(player.getLuck())
        .create(LootContextParamSets.CHEST);
    table.fill(this.container, params, this.lootTableSeed);
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    if (this.lootTable != null && player.isSpectator()) {
      return null;
    }
    if (player instanceof ServerPlayer serverPlayer) {
      this.unpackLoot(serverPlayer);
    }
    return ChestMenu.sixRows(id, inventory, this.container);
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag tag) {
    if (tag.contains("LootTable", Tag.TAG_STRING)) {
      ResourceLocation id = ResourceLocation.tryParse(tag.getString("LootTable"));
      if (id != null) {
        this.lootTable = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, id);
      }
      this.lootTableSeed = tag.getLong("LootTableSeed");
    } else {
      NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
      ContainerHelper.loadAllItems(tag, items, this.registryAccess());
      this.container = new SimpleContainer(items.toArray(ItemStack[]::new));
    }
    this.groundedTicks = tag.getInt("GroundedTicks");
    this.entityData.set(DURABILITY,
        tag.contains("Durability", Tag.TAG_FLOAT) ? tag.getFloat("Durability") : MAX_DURABILITY);
    this.entityData.set(LANDED, tag.getBoolean("Landed"));
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag tag) {
    if (this.lootTable != null) {
      tag.putString("LootTable", this.lootTable.location().toString());
      tag.putLong("LootTableSeed", this.lootTableSeed);
    } else {
      NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
      for (int i = 0; i < CONTAINER_SIZE; i++) {
        items.set(i, this.container.getItem(i));
      }
      ContainerHelper.saveAllItems(tag, items, this.registryAccess());
    }
    tag.putInt("GroundedTicks", this.groundedTicks);
    tag.putFloat("Durability", this.entityData.get(DURABILITY));
    tag.putBoolean("Landed", this.hasLanded());
  }
}

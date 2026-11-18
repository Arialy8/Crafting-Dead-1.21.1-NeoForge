/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 *
 * This Non-Commercial Software License Agreement (the "Agreement") is made between
 * you (the "Licensee") and NEXUSNODE (BRAD HUNTER). (the "Licensor").
 * By installing or otherwise using Crafting Dead (the "Software"), you agree to be
 * bound by the terms and conditions of this Agreement as may be revised from time
 * to time at Licensor's sole discretion.
 *
 * If you do not agree to the terms and conditions of this Agreement do not download,
 * copy, reproduce or otherwise use any of the source code available online at any time.
 *
 * https://github.com/nexusnode/crafting-dead/blob/1.18.x/LICENSE.txt
 *
 * https://craftingdead.net/terms.php
 */

package com.craftingdead.core.world.entity.extension;

import com.craftingdead.core.world.item.ClothingItem;
import com.craftingdead.core.world.item.equipment.Equipment.Slot;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.ServerConfig;
import com.craftingdead.core.event.LivingExtensionEvent;
import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.network.NetworkChannel;
import com.craftingdead.core.network.message.play.CancelActionMessage;
import com.craftingdead.core.network.message.play.CrouchMessage;
import com.craftingdead.core.network.message.play.PerformActionMessage;
import com.craftingdead.core.sounds.ModSoundEvents;
import com.craftingdead.core.world.action.Action;
import com.craftingdead.core.world.action.ActionObserver;
import com.craftingdead.core.world.action.ActionType;
import com.craftingdead.core.world.entity.EntityUtil;
import com.craftingdead.core.world.item.MeleeWeaponItem;
import com.craftingdead.core.world.item.StorageItem;
import com.craftingdead.core.world.item.StorageStackDataComponentUtil;
import com.craftingdead.core.world.item.equipment.Clothing;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.craftingdead.core.world.item.equipment.Hat;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.AbstractGun;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import com.craftingdead.core.util.ItemHandlerNbtUtil;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

public class BaseLivingExtension<E extends LivingEntity, H extends LivingHandler>
    implements LivingExtension<E, H> {

  /**
   * The vanilla entity.
   */
  private final E entity;

  protected final Map<LivingHandlerType<? extends H>, H> handlers = new Object2ObjectArrayMap<>();

  protected final Map<LivingHandlerType<? extends H>, H> dirtyHandlers =
      new Object2ObjectArrayMap<>();

  private final IntSet dirtySlots = new IntOpenHashSet();

  /**
   * Keeps an incomplete or stale server jar from breaking the complete living-entity tick loop.
   * A clean Crafting Dead jar always contains Clothing; this guard only handles deployment errors.
   */
  private static volatile boolean clothingTypeAvailable = true;

  private final EntitySnapshot[] snapshots = new EntitySnapshot[20];

  /** Last stack whose attribute modifiers were applied for each custom equipment slot. */
  private final ItemStack[] appliedEquipmentStacks = new ItemStack[Equipment.Slot.values().length];

  protected final ItemStackHandler itemHandler =
      new ItemStackHandler(Equipment.Slot.values().length) {
        @Override
        protected void onLoad() {
          if (this.getSlots() != Equipment.Slot.values().length) {
            this.setSize(Equipment.Slot.values().length);
          }
          Arrays.stream(Equipment.Slot.values())
              .mapToInt(Equipment.Slot::getIndex)
              .forEach(BaseLivingExtension.this::syncEquipmentModifiers);
        }

        @Override
        public void onContentsChanged(int slot) {
          BaseLivingExtension.this.syncEquipmentModifiers(slot);
          if (!BaseLivingExtension.this.level().isClientSide()) {
            BaseLivingExtension.this.dirtySlots.add(slot);
          }
        }
      };

  /**
   * The last held {@link ItemStack} - used to check if the entity has switched item.
   */
  protected ItemStack lastHeldStack = null;

  protected float[] equipmentDropChances = new float[Equipment.Slot.values().length];

  @Nullable
  private Action action;

  @Nullable
  private ActionObserver actionObserver;

  private boolean movementBlocked;

  private boolean crouching;

  private Vec3 lastPos;

  private boolean moving;

  private Visibility cachedVisibility = Visibility.VISIBLE;

  BaseLivingExtension(E entity) {
    this.entity = entity;
    Arrays.fill(this.equipmentDropChances, 2.0F);
  }

  @Override
  public void load() {
    NeoForge.EVENT_BUS.post(new LivingExtensionEvent.Load(this));
  }

  @Override
  public <T extends H> void registerHandler(LivingHandlerType<T> type, T handler) {
    if (this.handlers.put(type, handler) != null) {
      throw new IllegalArgumentException("Duplicate handler: " + type);
    }
  }

  @Override
  public <T extends H> void removeHandler(LivingHandlerType<T> type) {
    this.handlers.remove(type);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends LivingHandler> Optional<T> getHandler(LivingHandlerType<T> type) {
    return Optional.ofNullable((T) this.handlers.get(type));
  }

  @Override
  public <T extends LivingHandler> T getHandlerOrThrow(LivingHandlerType<T> type) {
    @SuppressWarnings("unchecked")
    T handler = (T) this.handlers.get(type);
    if (handler == null) {
      throw new IllegalStateException("Missing handler: " + type);
    }
    return handler;
  }

  @Override
  public Optional<Action> getAction() {
    return Optional.ofNullable(this.action);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Action> boolean performAction(T action, boolean force, boolean sendUpdate) {
    if (action == null || action.type() == null) {
      return false;
    }

    try {
      var performEvent = new LivingExtensionEvent.PerformAction<>(this, action);
      NeoForge.EVENT_BUS.post(performEvent);
      if (performEvent.isCanceled()) {
        return false;
      }

      if (this.isObservingAction()
          || action.target().map(LivingExtension::isObservingAction).orElse(false)) {
        return false;
      }

      if (!action.start(false)) {
        return false;
      }

      this.cancelAction(true);
      this.action = action;
      this.setActionObserver(action.createPerformerObserver());

      action.target().ifPresent(target -> target.setActionObserver(action.createTargetObserver()));

      if (sendUpdate) {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        ((ActionType<T>) action.type()).encode(action, buf);
        var payload = new PerformActionMessage(action.type(), this.entity().getId(), buf);
        if (this.level().isClientSide()) {
          PacketDistributor.sendToServer(payload);
        } else {
          PacketDistributor.sendToPlayersTrackingEntityAndSelf(this.entity(), payload);
        }
      }
      return true;
    } catch (RuntimeException ex) {
      return false;
    }
  }

  @Override
  public void cancelAction(boolean sendUpdate) {
    if (this.action == null) {
      return;
    }
    this.stopAction(Action.StopReason.CANCELLED);
    if (sendUpdate) {
      var payload = new CancelActionMessage(this.entity().getId());
      if (this.level().isClientSide()) {
        PacketDistributor.sendToServer(payload);
      } else {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(this.entity(), payload);
      }
    }
  }

  @Override
  public void setActionObserver(ActionObserver actionObserver) {
    this.actionObserver = actionObserver;
  }

  @Override
  public Optional<ActionObserver> getActionObserver() {
    return Optional.ofNullable(this.actionObserver);
  }

  private void stopAction(Action.StopReason reason) {
    if (this.action != null) {
      this.action.stop(reason);
      this.action.target().ifPresent(target -> target.setActionObserver(null));
      this.setActionObserver(null);
      this.action = null;
    }
  }

  @Override
  public void setMovementBlocked(boolean movementBlocked) {
    this.movementBlocked = movementBlocked;
  }

  @Override
  public boolean isMovementBlocked() {
    return this.movementBlocked;
  }

  @Override
  public boolean isMoving() {
    return this.moving;
  }

  @Override
  public void tick() {
    var heldStack = this.mainHandItem();
    if (heldStack != this.lastHeldStack) {
      if (this.lastHeldStack != null) {
        Gun.get(this.lastHeldStack)
            .ifPresent(gun -> gun.reset(this));
      }
      if ((this.lastHeldStack == null || !heldStack.is(this.lastHeldStack.getItem()))
          && Gun.get(heldStack).isPresent()) {
        this.entity.playSound(ModSoundEvents.GUN_EQUIP.get(), 0.25F, 1.0F);
      }
      this.lastHeldStack = heldStack;
    }

    // Reset this every tick
    this.movementBlocked = false;

    if (this.action != null && this.action.tick()) {
      // Action may have cancelled itself
      if (this.action != null) {
        this.stopAction(Action.StopReason.COMPLETED);
      }
    }

    Gun.get(heldStack).ifPresent(gun -> gun.tick(this));

    if (clothingTypeAvailable) {
      try {
        this.getEquipmentInSlot(Equipment.Slot.CLOTHING, Clothing.class)
            .ifPresent(this::tickClothing);
      } catch (NoClassDefFoundError error) {
        // Continue ticking guns, actions, hats and snapshots instead of failing every entity tick.
        clothingTypeAvailable = false;
      }
    }
    this.getEquipmentInSlot(Equipment.Slot.HAT, Hat.class)
        .ifPresent(this::tickHat);

    if (!this.level().isClientSide()) {
      // This is called at the start of the entity tick so it's equivalent of last tick's position.
      this.snapshots[this.entity.getServer().getTickCount() % 20] = this.makeSnapshot(1.0F);
    }

    this.moving = !this.entity.position().equals(this.lastPos);
    this.lastPos = this.entity.position();

    this.handlers.forEach(this::tickHandler);
  }

  protected void tickHandler(LivingHandlerType<? extends H> type, H handler) {
    handler.tick();

    // Precedence = (1) INVISIBLE (2) PARTIALLY_VISIBLE (3) VISIBLE
    this.cachedVisibility = Visibility.VISIBLE;
    switch (handler.getVisibility()) {
      case INVISIBLE:
        this.cachedVisibility = Visibility.INVISIBLE;
      case PARTIALLY_VISIBLE:
        if (this.cachedVisibility == Visibility.VISIBLE) {
          this.cachedVisibility = Visibility.PARTIALLY_VISIBLE;
        }
        break;
      default:
        break;
    }

    if (handler.isMovementBlocked()) {
      this.movementBlocked = true;
    }

    if (handler.requiresSync()) {
      this.dirtyHandlers.put(type, handler);
    }
  }

  private void tickHat(Hat hat) {
    if (hat.waterBreathing() && this.entity.isEyeInFluid(FluidTags.WATER)) {
      this.entity.addEffect(
          new MobEffectInstance(MobEffects.WATER_BREATHING, 2, 0, false, false, false));
    }

    if (hat.nightVision()) {
      this.entity.addEffect(
          new MobEffectInstance(MobEffects.NIGHT_VISION, 2, 0, false, false, false));
    }
  }

  private void tickClothing(Clothing clothing) {
    if (clothing.fireImmunity()) {
      if (this.entity.getRemainingFireTicks() > 0) {
        this.entity.clearFire();
      }

      this.entity.addEffect(
          new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2, 0, false, false, false));
    }

    if (clothing.enhancesSwimming() && this.entity.isEyeInFluid(FluidTags.WATER)) {
      this.entity.addEffect(
          new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 2, 0, false, false, false));
    }
  }

  @Override
  public float handleDamaged(DamageSource source, float amount) {
    var damage = this.handlers.values().stream().reduce(amount,
        (result, extension) -> extension.handleDamaged(source, result), (u, t) -> t);
    // Handle scenarios where the player is the source of the damage (attacking another entity)
    if (source.getEntity() instanceof Player player) {
      if (ServerConfig.instance.backstabEnabled.get()) {
        var usedMeleeWeapon = player.getItemInHand(player.getUsedItemHand())
            .getItem() instanceof MeleeWeaponItem;
        if (usedMeleeWeapon && !EntityUtil.canSee(this.entity(), player, 90F)) {
          damage *= ServerConfig.instance.backstabBonusDamage.get().floatValue();
        }
      }

      if (ServerConfig.instance.criticalHitEnable.get()) {
        if (ServerConfig.instance.criticalHitChance.get() > player.getRandom().nextFloat()) {
          damage *= ServerConfig.instance.criticalHitBonusDamage.get().floatValue();
        }
      }
    }

    // Handle scenarios where a player is receiving damage
    if (this.entity() instanceof Player player) {
      var playerExtension = PlayerExtension.get(player);
      if (playerExtension != null && !playerExtension.getItemInSlot(Slot.CLOTHING).isEmpty()) {
        // Makes sure that the damage income is from an entity
        if (source.getEntity() != null) {
          var clothingItem = ClothingItem.getClothingItem(player);
          if (clothingItem != null) {
            damage = clothingItem.calculateDamage(damage);
          }
        }
      }
    }

    return damage;
  }

  @Override
  public boolean handleHurt(DamageSource source, float amount) {
    return this.handlers.values().stream().anyMatch(e -> e.handleHurt(source, amount));
  }

  @Override
  public boolean handleKill(Entity target) {
    return this.handlers.values().stream().anyMatch(e -> e.handleKill(target));
  }

  @Override
  public boolean handleDeath(DamageSource cause) {
    if (this.handlers.values().stream().anyMatch(e -> e.handleDeath(cause))) {
      return true;
    }
    this.cancelAction(true);
    return false;
  }

  @Override
  public boolean handleDeathLoot(DamageSource cause, Collection<ItemEntity> drops,
      int lootingLevel) {
    if (this.handlers.values().stream()
        .anyMatch(e -> e.handleDeathLoot(cause, drops, lootingLevel))) {
      return true;
    }

    if (!this.keepInventory()) {
      for (int i = 0; i < this.itemHandler.getSlots(); i++) {
        var itemStack = this.itemHandler.extractItem(i, Integer.MAX_VALUE, false);
        var dropChance = this.entity() instanceof com.craftingdead.survival.world.entity.SurvivalZombie zombie
            ? com.craftingdead.survival.world.entity.ZombieLoadouts.dropChance(zombie, itemStack,
                Equipment.Slot.values()[i], this.equipmentDropChances[i])
            : this.equipmentDropChances[i];
        if (!itemStack.isEmpty()
            && Math.max(this.random().nextFloat() - (lootingLevel * 0.01F), 0.0F) < dropChance) {
          Gun.get(itemStack).ifPresent(gun -> {
            if (gun instanceof AbstractGun abstractGun) {
              abstractGun.serializeNBT();
            }
          });
          if (itemStack.getItem() instanceof StorageItem) {
            var storageState = StorageStackDataComponentUtil.readStorageState(itemStack);
            StorageStackDataComponentUtil.writeStorageState(itemStack, storageState);
          }
          var itemEntity = new ItemEntity(this.level(), this.entity().getX(),
              this.entity().getY(), this.entity().getZ(), itemStack);
          itemEntity.setDefaultPickUpDelay();
          drops.add(itemEntity);
        }
      }
    }
    return false;
  }

  @Override
  public boolean handleBlockPlace(BlockSnapshot replacedBlock, BlockState placedBlock,
      BlockState placedAgainst) {
    return this.handlers.values().stream()
        .anyMatch(e -> e.handleBlockPlace(replacedBlock, placedBlock, placedAgainst));
  }

  @Override
  public boolean handleMultiBlockPlace(List<BlockSnapshot> replacedBlocks, BlockState placedBlock,
      BlockState placedAgainst) {
    return this.handlers.values().stream()
        .anyMatch(e -> e.handleMultiBlockPlace(replacedBlocks, placedBlock, placedAgainst));
  }

  protected boolean keepInventory() {
    return false;
  }

  @Override
  public Visibility getVisibility() {
    return this.cachedVisibility;
  }

  @Override
  public EntitySnapshot getSnapshot(int tick) {
    final int currentTick = this.entity.getServer().getTickCount();
    if (tick == currentTick) {
      return this.makeSnapshot(1.0F);
    } else if (tick < currentTick - 20) {
      return this.snapshots[0];
    } else if (tick > currentTick) {
      throw new IllegalStateException("Tick bigger than current tick");
    }

    final int snapshotIndex = tick % 20;
    var snapshot = this.snapshots[snapshotIndex];
    if (snapshot == null) {
      throw new IndexOutOfBoundsException();
    }
    return snapshot;
  }

  @Override
  public boolean isCrouching() {
    return this.crouching;
  }

  @Override
  public void setCrouching(boolean crouching, boolean sendUpdate) {
    if (!this.entity.onGround()) {
      return;
    }
    this.crouching = crouching;
    if (sendUpdate) {
      var payload = new CrouchMessage(this.entity().getId(), crouching);
      if (this.level().isClientSide()) {
        PacketDistributor.sendToServer(payload);
      } else {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(this.entity(), payload);
      }
    }
  }

  @Override
  public E entity() {
    return this.entity;
  }

  @Override
  public float getEquipmentDropChance(Equipment.Slot slot) {
    return this.equipmentDropChances[slot.getIndex()];
  }

  @Override
  public void setEquipmentDropChance(Equipment.Slot slot, float chance) {
    this.equipmentDropChances[slot.getIndex()] = chance;
  }

  @Override
  public ItemStack getItemInSlot(Equipment.Slot slot) {
    return this.itemHandler.getStackInSlot(slot.getIndex());
  }

  @Override
  public ItemStack setItemInSlot(Equipment.Slot slot, ItemStack itemStack) {
    var oldStack = this.getItemInSlot(slot);
    this.itemHandler.setStackInSlot(slot.getIndex(), itemStack);
    return oldStack;
  }

  private void syncEquipmentModifiers(int slot) {
    if (slot < 0 || slot >= this.appliedEquipmentStacks.length) {
      return;
    }

    var attributes = this.entity.getAttributes();
    float healthBeforeChange = this.entity.getHealth();
    ItemStack oldStack = this.appliedEquipmentStacks[slot];
    if (oldStack != null && !oldStack.isEmpty()) {
      Equipment.get(oldStack)
          .map(Equipment::attributeModifiers)
          .ifPresent(attributes::removeAttributeModifiers);
    }

    ItemStack newStack = this.itemHandler.getStackInSlot(slot);
    Equipment.get(newStack)
        .map(Equipment::attributeModifiers)
        .ifPresent(attributes::addTransientAttributeModifiers);
    this.appliedEquipmentStacks[slot] = newStack.isEmpty() ? ItemStack.EMPTY : newStack.copy();

    // Clothing changes only the maximum. Vanilla regeneration remains responsible for restoring
    // health, while removing clothing clamps excess health to the resulting cap without touching
    // max-health modifiers supplied by effects or other systems.
    if (this.entity.isAlive() && this.entity.getHealth() > this.entity.getMaxHealth()) {
      this.entity.setHealth(Math.min(healthBeforeChange, this.entity.getMaxHealth()));
    }
  }

  @Override
  public CompoundTag serializeNBT() {
    var tag = new CompoundTag();
    tag.put("inventory", ItemHandlerNbtUtil.serialize(this.itemHandler));
    for (var entry : this.handlers.entrySet()) {
      var extensionTag = entry.getValue().serializeNBT();
      if (!extensionTag.isEmpty()) {
        tag.put(entry.getKey().toString(), extensionTag);
      }
    }

    var dropChances = new ListTag();
    for (float dropChance : this.equipmentDropChances) {
      dropChances.add(FloatTag.valueOf(dropChance));
    }
    tag.put("dropChances", dropChances);
    return tag;
  }

  @Override
  public void deserializeNBT(CompoundTag tag) {
    ItemHandlerNbtUtil.deserialize(this.itemHandler, tag.getCompound("inventory"));
    for (var entry : this.handlers.entrySet()) {
      var extensionTag = tag.getCompound(entry.getKey().toString());
      if (!extensionTag.isEmpty()) {
        entry.getValue().deserializeNBT(extensionTag);
      }
    }

    if (tag.contains("dropChances", Tag.TAG_LIST)) {
      var dropChances = tag.getList("dropChances", Tag.TAG_FLOAT);

      for (int i = 0; i < dropChances.size(); i++) {
        this.equipmentDropChances[i] = dropChances.getFloat(i);
      }
    }
  }

  @Override
  public int hashCode() {
    return this.entity.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj)
        || (obj instanceof LivingExtension<?, ?> extension
            && extension.entity().equals(this.entity));
  }

  @Override
  public void encode(FriendlyByteBuf out, boolean writeAll) {
    // Item Handler
    if (writeAll) {
      for (int i = 0; i < this.itemHandler.getSlots(); i++) {
        NetworkBufferUtil.writeShort(out, i, 0, 254);
        NetworkBufferUtil.writeItem(out, this.itemHandler.getStackInSlot(i));
      }
    } else {
      this.dirtySlots.forEach(slot -> {
        NetworkBufferUtil.writeShort(out, slot, 0, 254);
        NetworkBufferUtil.writeItem(out, this.itemHandler.getStackInSlot(slot));
      });
      this.dirtySlots.clear();
    }
    NetworkBufferUtil.writeShort(out, 255, 0, 255);

    // Handlers
    var handlersToSend = writeAll ? this.handlers.entrySet() : this.dirtyHandlers.entrySet();
    int handlerLimit = Math.max(this.handlers.size() + 16, 64);
    int handlerCount = Math.min(handlersToSend.size(), handlerLimit);
    NetworkBufferUtil.writeVarInt(out, handlerCount, 0, handlerLimit);
    int encodedHandlers = 0;
    for (var entry : handlersToSend) {
      if (encodedHandlers++ >= handlerCount) {
        break;
      }
      NetworkBufferUtil.writeResourceLocation(out,
          entry == null || entry.getKey() == null ? null : entry.getKey().id(), null);
      var handlerData = new FriendlyByteBuf(Unpooled.buffer());
      try {
        if (entry != null && entry.getValue() != null) {
          entry.getValue().encode(handlerData, writeAll);
        }
        NetworkBufferUtil.writePayload(out, handlerData);
      } catch (RuntimeException ex) {
        NetworkBufferUtil.writePayload(out, null);
      } finally {
        handlerData.release();
      }
    }
    this.dirtyHandlers.clear();
  }

  @Override
  public void decode(FriendlyByteBuf in) {
    if (in == null) {
      return;
    }

    // Item Handler
    int decodedSlots = 0;
    int maxSlotReads = Math.max(this.itemHandler.getSlots() + 16, 32);
    while (in.readableBytes() > 0 && decodedSlots++ < maxSlotReads) {
      int slot = NetworkBufferUtil.readShort(in, 255) & 0xFFFF;
      if (slot == 255) {
        break;
      }
      var stack = NetworkBufferUtil.readItem(in);
      if (slot >= 0 && slot < this.itemHandler.getSlots()) {
        this.itemHandler.setStackInSlot(slot, stack);
      }
    }

    // Handlers
    int handlersSize = NetworkBufferUtil.readCappedVarInt(in, 0,
        Math.max(this.handlers.size() + 16, 64), 0);
    for (int x = 0; x < handlersSize && in.readableBytes() > 0; x++) {
      var id = NetworkBufferUtil.readResourceLocation(in, null);
      var handlerData = NetworkBufferUtil.readPayload(in);
      try {
        var handler = id == null ? null : this.handlers.get(new LivingHandlerType<>(id));
        if (handler != null) {
          handler.decode(handlerData);
        }
      } finally {
        handlerData.release();
      }
    }
  }

  @Override
  public boolean requiresSync() {
    return !this.dirtySlots.isEmpty() || !this.dirtyHandlers.isEmpty();
  }
}

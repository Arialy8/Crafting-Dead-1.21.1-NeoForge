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

package com.craftingdead.core.world.action.reload;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Optional;
import com.craftingdead.core.ServerConfig;
import com.craftingdead.core.network.message.play.SyncGunEquipmentSlotMessage;
import com.craftingdead.core.network.NetworkChannel;
import com.craftingdead.core.event.CollectMagazineItemHandlers;
import com.craftingdead.core.world.action.ActionType;
import com.craftingdead.core.world.action.ActionTypes;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.craftingdead.core.world.item.MagazineItem;
import com.craftingdead.core.world.item.gun.ammoprovider.MagazineAmmoProvider;
import com.craftingdead.core.world.item.gun.magazine.Magazine;
import com.craftingdead.core.world.item.gun.magazine.MagazineStackDataComponentUtil;
import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import com.craftingdead.core.world.item.gun.GunAmmoStateUtil;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public class MagazineReloadAction extends AbstractReloadAction {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Nullable
  private final MagazineAmmoProvider ammoProvider;

  private ItemStack newMagazineStack;

  @Nullable
  private MagazineLocation magazineLocation;

  public MagazineReloadAction(LivingExtension<?, ?> performer) {
    super(performer);
    var ammoProvider = this.gun == null ? null : this.gun.getAmmoProvider();
    this.ammoProvider = ammoProvider instanceof MagazineAmmoProvider magazineAmmoProvider
        ? magazineAmmoProvider
        : null;
  }

  @Override
  public ActionType<?> type() {
    return ActionTypes.MAGAZINE_RELOAD.get();
  }

  @Override
  public boolean start(boolean simulate) {
    if (this.performer() == null || this.performer().entity() == null
        || this.gun == null || this.ammoProvider == null) {
      
      return false;
    }

    

    var result = this.findMagazine(this.performer());
    if (this.performer().entity().isSprinting() || result.isEmpty()) {
      
      return false;
    }

    

    if (!simulate) {
      this.magazineLocation = result.get();
      this.newMagazineStack = this.prepareMagazineStack(
          this.magazineLocation.itemHandler.extractItem(
              this.magazineLocation.slot, 1, this.performer().level().isClientSide()));
      if (this.performer().level().isClientSide() && !this.newMagazineStack.isEmpty()) {
        // Display the incoming magazine during the insertion animation only.
        // The server remains authoritative for the actual loaded ammunition.
        this.ammoProvider.setMagazineStack(this.newMagazineStack.copy());
      }
    }

    return super.start(simulate);
  }

  @Override
  protected void loadNewMagazineStack(boolean displayOnly) {
    if (this.ammoProvider == null || this.performer() == null || this.performer().entity() == null
        || this.gun == null) {
      return;
    }

    boolean clientSide = displayOnly || this.performer().level().isClientSide();

    if ((this.newMagazineStack == null || this.newMagazineStack.isEmpty())
        && this.magazineLocation != null) {
      this.newMagazineStack = this.prepareMagazineStack(
          this.magazineLocation.itemHandler.extractItem(
              this.magazineLocation.slot, 1, clientSide));
    }

    if (this.newMagazineStack == null || this.newMagazineStack.isEmpty()) {
      return;
    }

    if (clientSide) {
      // Client-side reload completion is animation/display only. Do not write the magazine into
      // the local Gun capability or ItemStack until the server accepts the reload and returns a
      // SyncGunSlot packet. Cache a short HUD-only preview so the ammo overlay does not flicker to
      // "empty magazine" for one frame between animation completion and the authoritative sync.
      // The preview is intentionally not used by shooting logic.
      GunAmmoStateUtil.cacheClientReloadPreview(this.performer().entity().getId(),
          EquipmentSlot.MAINHAND, this.performer().mainHandItem(), this.newMagazineStack);
      
      return;
    }

    this.ammoProvider.setMagazineStack(this.newMagazineStack.copy());
    this.gun.setAmmoProvider(this.ammoProvider);
    this.persistLoadedMagazineToGunStack(this.newMagazineStack);

    


    if (this.performer().entity() instanceof Player player) {
      player.getInventory().setChanged();
    }

    this.syncGunToClient();

    if (!this.oldMagazineStack.isEmpty()
        && this.oldMagazineStack.getItem() instanceof MagazineItem
        && this.performer().entity() instanceof Player player
        && !(Magazine.get(this.oldMagazineStack).map(Magazine::isEmpty)
            .orElse(true)
            && ServerConfig.instance.reloadDestroyMagWhenEmpty.get())) {
      player.addItem(this.oldMagazineStack);
    } else if (!this.oldMagazineStack.isEmpty()) {
      
    }
  }

  @Override
  protected void revert() {
    if (this.ammoProvider == null || this.performer() == null || this.performer().entity() == null) {
      return;
    }
    this.ammoProvider.setMagazineStack(this.oldMagazineStack == null ? ItemStack.EMPTY : this.oldMagazineStack);
    this.persistAndSyncGun();
    if (this.magazineLocation == null || this.newMagazineStack == null || this.newMagazineStack.isEmpty()) {
      return;
    }
    var remainingStack = this.magazineLocation.itemHandler().insertItem(
        this.magazineLocation.slot(), this.newMagazineStack, false);
    if (!remainingStack.isEmpty()) {
      this.performer().entity().spawnAtLocation(remainingStack);
    }
  }

  private void persistAndSyncGun() {
    if (this.gun == null) {
      return;
    }

    if (this.ammoProvider != null) {
      this.gun.setAmmoProvider(this.ammoProvider);
      this.persistLoadedMagazineToGunStack(this.ammoProvider.getMagazineStack());
    }

    if (this.performer() == null || this.performer().entity() == null
        || this.performer().level().isClientSide()) {
      return;
    }

    if (this.performer().entity() instanceof Player player) {
      player.getInventory().setChanged();
    }

    this.syncGunToClient();
  }


  private void persistLoadedMagazineToGunStack(ItemStack magazineStack) {
    if (this.gun == null || magazineStack == null) {
      return;
    }

    ItemStack mainHandStack = this.performer().mainHandItem();
    if (mainHandStack != null && !mainHandStack.isEmpty()
        && mainHandStack.is(this.gun.getItemStack().getItem())) {
      // Reload actions can retain a Gun runtime created from an older stack snapshot. Mirror the
      // current weapon enchantments into that runtime before it is encoded in the reload sync.
      // The equipped stack remains authoritative and is never replaced here.
      this.gun.getItemStack().set(DataComponents.ENCHANTMENTS,
          mainHandStack.getOrDefault(DataComponents.ENCHANTMENTS,
              net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY));
      GunAmmoStateUtil.writeLoadedMagazine(mainHandStack, magazineStack);
      if (this.performer() != null && this.performer().entity() != null
          && !this.performer().level().isClientSide()) {
        GunAmmoStateUtil.cacheAuthoritativeLoadedMagazine(this.performer().entity().getId(),
            EquipmentSlot.MAINHAND, mainHandStack, magazineStack);
      }
    }

    GunAmmoStateUtil.writeLoadedMagazine(this.gun.getItemStack(), magazineStack);
  }

  private void syncGunToClient() {
    if (this.gun == null || this.performer() == null || this.performer().entity() == null
        || this.performer().level().isClientSide()) {
      return;
    }

    if (this.performer().entity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
      NetworkChannel.PLAY.sendToPlayer(serverPlayer,
          new SyncGunEquipmentSlotMessage(this.performer().entity().getId(),
              EquipmentSlot.MAINHAND, this.gun, true));
    }

    PacketDistributor.sendToPlayersTrackingEntity(this.performer().entity(),
        new SyncGunEquipmentSlotMessage(this.performer().entity().getId(),
            EquipmentSlot.MAINHAND, this.gun, true));
  }

  private List<IItemHandler> collectItemHandlers(LivingExtension<?, ?> living) {
    var builder = ImmutableList.<IItemHandler>builder();
    if (living == null || living.entity() == null) {
      return builder.build();
    }

    var event = new CollectMagazineItemHandlers(living);
    NeoForge.EVENT_BUS.post(event);
    event.getItemHandlers().stream()
        .filter(java.util.Objects::nonNull)
        .forEach(builder::add);

    // Vest - first
    var vestStack = living.getItemInSlot(Equipment.Slot.VEST);
    if (!vestStack.isEmpty()) {
      var handler = vestStack.getCapability(Capabilities.ItemHandler.ITEM);
      if (handler != null) {
        builder.add(handler);
      }
    }

    // Backpack - second
    var backpackStack = living.getItemInSlot(Equipment.Slot.BACKPACK);
    if (!backpackStack.isEmpty()) {
      var handler = backpackStack.getCapability(Capabilities.ItemHandler.ITEM);
      if (handler != null) {
        builder.add(handler);
      }
    }

    // Inventory - third. Player ITEM_HANDLER capability is not always exposed in 1.21.1,
    // so wrap the vanilla inventory directly when possible.
    if (living.entity() instanceof Player player) {
      builder.add(new InvWrapper(player.getInventory()));
    } else if (living.entity() != null) {
      var handler = Capabilities.ItemHandler.ENTITY.getCapability(living.entity(), null);
      if (handler != null) {
        builder.add(handler);
      }
    }

    // Creative Inventory if the player is in creative mode - fourth
    if (this.gun != null && living.entity() instanceof Player player && player.getAbilities().instabuild) {
      var defaultMagazine = this.gun.getDefaultMagazineStack();
      builder.add(new InvWrapper(new SimpleContainer(defaultMagazine)));
    }

    return builder.build();
  }

  private Optional<MagazineLocation> findMagazine(LivingExtension<?, ?> living) {
    if (this.gun == null) {
      return Optional.empty();
    }
    for (var itemHandler : this.collectItemHandlers(living)) {
      if (itemHandler == null) {
        continue;
      }
      for (int i = 0; i < Math.max(0, itemHandler.getSlots()); ++i) {
        var itemStack = itemHandler.getStackInSlot(i);
        if (itemStack != null && !itemStack.isEmpty()
            && this.gun.getAcceptedMagazines().contains(itemStack.getItem())
            && this.hasAmmo(itemStack)) {
          return Optional.of(new MagazineLocation(itemHandler, i));
        }
      }
    }
    return Optional.empty();
  }


  private boolean hasAmmo(ItemStack itemStack) {
    if (!(itemStack.getItem() instanceof MagazineItem magazineItem)) {
      return false;
    }
    return Magazine.get(itemStack)
        .map(magazine -> magazine.getSize() > 0)
        .orElseGet(() -> MagazineStackDataComponentUtil.getSize(itemStack, magazineItem.getSize()) > 0);
  }

  private ItemStack prepareMagazineStack(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return ItemStack.EMPTY;
    }

    stack = stack.copy();
    stack.setCount(1);

    if (stack.getItem() instanceof MagazineItem magazineItem) {
      var magazine = Magazine.get(stack).orElse(null);
      if (magazine != null) {
        MagazineStackDataComponentUtil.writeMagazineState(stack, magazine.serializeNBT());
      } else {
        MagazineStackDataComponentUtil.setSize(stack,
            MagazineStackDataComponentUtil.getSize(stack, magazineItem.getSize()));
      }
    }

    return stack;
  }


  private record MagazineLocation(IItemHandler itemHandler, int slot) {}
}

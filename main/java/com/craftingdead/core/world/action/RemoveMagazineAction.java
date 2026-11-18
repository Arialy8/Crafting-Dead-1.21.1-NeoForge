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

package com.craftingdead.core.world.action;

import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.ServerConfig;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.ammoprovider.AmmoProvider;
import com.craftingdead.core.world.item.gun.ammoprovider.MagazineAmmoProvider;
import com.craftingdead.core.world.item.gun.magazine.Magazine;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class RemoveMagazineAction extends TimedAction {

  private final LivingExtension<?, ?> performer;

  @Nullable
  private final Gun gun;

  protected final int slot;

  private final ItemStack oldMagazineStack;

  @Nullable
  private final MagazineAmmoProvider ammoProvider;

  public RemoveMagazineAction(LivingExtension<?, ?> performer) {
    this.performer = performer;
    if (this.performer != null && this.performer.entity() instanceof Player player) {
      this.slot = player.getInventory().selected;
    } else {
      this.slot = -1;
    }
    this.gun = performer == null ? null : performer.mainHandGun().orElse(null);
    AmmoProvider ammoProvider = this.gun == null ? null : this.gun.getAmmoProvider();
    this.ammoProvider = ammoProvider instanceof MagazineAmmoProvider magazineAmmoProvider
        ? magazineAmmoProvider
        : null;
    this.oldMagazineStack = this.ammoProvider == null ? ItemStack.EMPTY
        : this.ammoProvider.getMagazineStack();
  }

  @Override
  public ActionType<?> type() {
    return ActionTypes.REMOVE_MAGAZINE.get();
  }

  @Override
  protected int getTotalDurationTicks() {
    return this.gun == null ? 1 : Math.max(1, this.gun.getReloadDurationTicks());
  }

  @Override
  public boolean start(boolean simulate) {
    if (this.performer() == null || this.performer().entity() == null
        || this.gun == null || this.ammoProvider == null
        || this.performer().entity().isSprinting() || this.oldMagazineStack.isEmpty()) {
      return false;
    }

    if (simulate) {
      return true;
    }

    if (this.gun.isPerformingSecondaryAction()) {
      this.gun.setPerformingSecondaryAction(this.performer(), false, false);
    }

    if (this.performer().level().isClientSide()) {
      this.gun.startClientReload(true);
    }

    return true;
  }

  @Override
  public boolean tick() {
    if (this.performer() == null || this.performer().entity() == null
        || this.gun == null || this.ammoProvider == null) {
      return false;
    }
    if (!this.performer().level().isClientSide()
        && (this.performer.mainHandItem().isEmpty()
        || !this.performer.mainHandItem().is(this.gun.getItemStack().getItem())
        || (this.slot != -1
        && this.performer().entity() instanceof Player player
        && player.getInventory().selected != this.slot)
        || this.performer().entity().isSprinting())) {
      this.performer().cancelAction(true);
      return false;
    }
    return super.tick();
  }

  @Override
  public void stop(StopReason reason) {
    super.stop(reason);

    if (this.performer() == null || this.performer().entity() == null
        || this.gun == null || this.ammoProvider == null || reason == null) {
      return;
    }

    if (reason.isCompleted()) {
      if (!this.performer().level().isClientSide()) {
        // This will be synced to the client by the gun.
        this.ammoProvider.setMagazineStack(ItemStack.EMPTY);
        if (!this.oldMagazineStack.isEmpty()
            && this.performer().entity() instanceof Player
            && !(Magazine.get(this.oldMagazineStack).map(Magazine::isEmpty)
            .orElse(true)
            && ServerConfig.instance.reloadDestroyMagWhenEmpty.get())) {
          ((Player) this.performer().entity()).addItem(this.oldMagazineStack);
        }
      }
      return;
    }

    if (this.performer().level().isClientSide()) {
      this.gun.cancelClientReload();
    }
    // Call this on both sides as we change the client side stack for the remove animation.
    this.ammoProvider.setMagazineStack(this.oldMagazineStack);
  }

  @Override
  public LivingExtension<?, ?> performer() {
    return this.performer;
  }
}

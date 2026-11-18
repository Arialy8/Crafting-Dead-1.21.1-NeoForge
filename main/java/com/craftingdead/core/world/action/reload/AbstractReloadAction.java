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

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.ServerConfig;
import com.craftingdead.core.world.action.TimedAction;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.item.gun.Gun;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractReloadAction extends TimedAction {

  protected final LivingExtension<?, ?> performer;

  @Nullable
  protected final Gun gun;

  protected final int slot;

  protected final ItemStack oldMagazineStack;

  public AbstractReloadAction(LivingExtension<?, ?> performer) {
    this.performer = performer;
    if (this.performer != null && this.performer.entity() instanceof Player player) {
      this.slot = player.getInventory().selected;
    } else {
      this.slot = -1;
    }
    this.gun = performer == null ? null
        : Gun.get(performer.mainHandItem()).orElse(null);
    this.oldMagazineStack = this.gun == null || this.gun.getAmmoProvider() == null
        ? ItemStack.EMPTY
        : this.gun.getAmmoProvider().getMagazineStack();
  }

  @Override
  public LivingExtension<?, ?> performer() {
    return this.performer;
  }

  @Override
  protected int getTotalDurationTicks() {
    return this.gun == null ? 1
        : Math.max(1, this.gun.getReloadDurationTicks() + ServerConfig.instance.reloadDuration.get());
  }

  @Override
  public boolean start(boolean simulate) {
    if (this.performer() == null || this.performer().entity() == null || this.gun == null) {
      return false;
    }
    if (this.performer().entity().isSprinting() /*|| this.performer().isCrouching()*/) {
      return false;
    }

    if (simulate) {
      return true;
    }

    if (this.gun.isPerformingSecondaryAction()) {
      this.gun.setPerformingSecondaryAction(this.performer(), false, false);
    }

    this.gun.getReloadSound()
        .ifPresent(sound -> this.performer().level().playSound(null,
            this.performer().entity(), sound, SoundSource.PLAYERS, 1.0F, 1.0F));

    if (this.performer().level().isClientSide()) {
      this.gun.startClientReload(false);
    }

    return true;
  }

  protected abstract void loadNewMagazineStack(boolean displayOnly);

  @Override
  public boolean tick() {
    if (this.performer() == null || this.performer().entity() == null || this.gun == null) {
      return false;
    }
    if (!this.performer().level().isClientSide()
        && (this.performer().mainHandItem().isEmpty()
        || !this.performer().mainHandItem().is(this.gun.getItemStack().getItem())
        || (this.slot != -1
        && this.performer().entity() instanceof Player player
        && player.getInventory().selected != this.slot)
        || this.performer().entity().isSprinting())) {
      this.performer.cancelAction(true);
      return false;
    }
    return super.tick();
  }

  @Override
  public void stop(StopReason reason) {
    super.stop(reason);
    if (this.performer() == null || this.performer().entity() == null || this.gun == null) {
      return;
    }

    if (reason != null && reason.isCompleted()) {
      // Server performs the real inventory mutation. The client also applies a
      // display-only magazine snapshot so the gun can shoot immediately after
      // the reload animation completes, instead of dry-firing until a later
      // inventory sync arrives.
      this.loadNewMagazineStack(this.performer().level().isClientSide());
      return;
    }

    if (this.performer().level().isClientSide()) {
      this.gun.cancelClientReload();
    }

    // Revert all changes as we've been cancelled
    this.revert();
  }

  protected abstract void revert();
}

/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */

package com.craftingdead.core.world.action;

import com.craftingdead.core.world.entity.extension.LivingExtension;

/**
 * Safe fallback used when stale/invalid client action packets reference an action that cannot be
 * constructed on the current entity state. It never starts and immediately finishes if ticked.
 */
public final class NoopAction implements Action {

  private final LivingExtension<?, ?> performer;
  private final ActionType<?> type;

  public NoopAction(LivingExtension<?, ?> performer, ActionType<?> type) {
    this.performer = performer;
    this.type = type;
  }

  @Override
  public boolean start(boolean simulate) {
    return false;
  }

  @Override
  public boolean tick() {
    return true;
  }

  @Override
  public void stop(StopReason reason) {}

  @Override
  public LivingExtension<?, ?> performer() {
    return this.performer;
  }

  @Override
  public ActionType<?> type() {
    return this.type;
  }
}

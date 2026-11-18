/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */
package com.craftingdead.core.client.renderer.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Forge 1.21.x item client extensions for Crafting Dead items.
 *
 * <p>Dynamic item rendering is now routed through {@link IClientItemExtensions} and a
 * singleton {@link BlockEntityWithoutLevelRenderer}. The old renderer-dispatch mixin remains
 * as a compatibility hook while the model JSON and item classes are migrated incrementally.</p>
 */
public final class CraftingDeadItemExtensions implements IClientItemExtensions {

  public static final CraftingDeadItemExtensions INSTANCE = new CraftingDeadItemExtensions();

  private CraftingDeadItemExtensions() {}

  @Override
  public BlockEntityWithoutLevelRenderer getCustomRenderer() {
    return CraftingDeadItemStackRenderer.getInstance();
  }
}

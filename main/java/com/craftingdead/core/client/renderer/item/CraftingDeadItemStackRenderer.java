/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */
package com.craftingdead.core.client.renderer.item;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Shared Forge 1.21.x BEWLR for Crafting Dead custom item renderers.
 *
 * <p>Forge recommends a single BEWLR instance per mod. This class delegates to the existing
 * data-driven {@link ItemRenderDispatcher} so the old renderer JSON files under
 * {@code assets/craftingdead/renderers/item} can continue to drive gun rendering.</p>
 */
public final class CraftingDeadItemStackRenderer extends BlockEntityWithoutLevelRenderer {

  private static CraftingDeadItemStackRenderer instance;

  public static CraftingDeadItemStackRenderer getInstance() {
    if (instance == null) {
      var minecraft = Minecraft.getInstance();
      instance = new CraftingDeadItemStackRenderer(minecraft);
    }
    return instance;
  }

  private CraftingDeadItemStackRenderer(Minecraft minecraft) {
    super(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
  }

  @Override
  public void renderByItem(ItemStack itemStack, ItemDisplayContext displayContext,
      PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
    var minecraft = Minecraft.getInstance();
    var living = displayContext.firstPerson() && minecraft.player != null
        ? LivingExtension.get(minecraft.player)
        : null;
    CraftingDead.getInstance().getClientDist().getItemRendererManager().renderItem(itemStack,
        displayContext, living, poseStack, bufferSource, packedLight, packedOverlay);
  }
}

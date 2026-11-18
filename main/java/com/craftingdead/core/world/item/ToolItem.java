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

package com.craftingdead.core.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ToolItem extends Item {

  public ToolItem(Properties properties) {
    super(properties);
  }

    public boolean canAttackBlock(BlockState blockState, Level world,
      BlockPos blockPos, Player playerEntity) {
    return !playerEntity.isCreative();
  }

    public float getDestroySpeed(ItemStack itemStack, BlockState blockState) {
    if (blockState.is(Blocks.COBWEB)) {
      return 15.0F;
    } else {
      return isFastPlantLikeBlock(blockState) ? 1.5F : 1.0F;
    }
  }

  private static boolean isFastPlantLikeBlock(BlockState blockState) {
    // 1.20+ removed the old Material enum. Keep the original intent by checking
    // common plant/soft block tags and known vanilla plant blocks instead.
    return blockState.is(BlockTags.LEAVES)
        || blockState.is(BlockTags.FLOWERS)
        || blockState.is(BlockTags.SAPLINGS)
        || blockState.is(BlockTags.CROPS)
        || blockState.is(Blocks.SHORT_GRASS)
        || blockState.is(Blocks.TALL_GRASS)
        || blockState.is(Blocks.FERN)
        || blockState.is(Blocks.LARGE_FERN)
        || blockState.is(Blocks.VINE)
        || blockState.is(Blocks.SEAGRASS)
        || blockState.is(Blocks.TALL_SEAGRASS)
        || blockState.is(Blocks.KELP)
        || blockState.is(Blocks.KELP_PLANT);
  }

    public boolean hurtEnemy(ItemStack itemStack, LivingEntity targetEntity,
      LivingEntity attackerEntity) {
    itemStack.hurtAndBreak(1, attackerEntity, EquipmentSlot.MAINHAND);
    return true;
  }

    public boolean mineBlock(ItemStack stack, Level worldIn, BlockState state, BlockPos pos,
      LivingEntity entityLiving) {
    if (state.getDestroySpeed(worldIn, pos) != 0.0F) {
      stack.hurtAndBreak(2, entityLiving, EquipmentSlot.MAINHAND);
    }
    return true;
  }

  public boolean isCorrectToolForDrops(BlockState blockIn) {
    return blockIn.is(Blocks.COBWEB);
  }
}

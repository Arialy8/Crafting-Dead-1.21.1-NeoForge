package com.craftingdead.immerse.world.level.block;

import com.craftingdead.immerse.world.inventory.BaseCenterMenu;
import com.craftingdead.immerse.world.level.block.entity.BaseCenterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class BaseCenterBlock extends Block implements EntityBlock {

  public BaseCenterBlock(Properties properties) {
    super(properties);
  }

  @Override
  public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new BaseCenterBlockEntity(pos, state);
  }

  @Override
  public void setPlacedBy(Level level, BlockPos pos, BlockState state,
      @Nullable LivingEntity placer, ItemStack stack) {
    super.setPlacedBy(level, pos, state, placer, stack);
    if (!level.isClientSide()
        && placer instanceof Player player
        && level.getBlockEntity(pos) instanceof BaseCenterBlockEntity baseCenter) {
      baseCenter.setOwner(player);
    }
  }

  @Override
  protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
      Player player, InteractionHand hand, BlockHitResult hitResult) {
    if (!(level.getBlockEntity(pos) instanceof BaseCenterBlockEntity baseCenter)) {
      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    if (!baseCenter.canUse(player) && !baseCenter.isBreached()) {
      if (!level.isClientSide()) {
        player.displayClientMessage(Component.translatable(
            "message.craftingdeadimmerse.base_no_use_permission"),
            true);
      }
      return ItemInteractionResult.SUCCESS;
    }
    if (!level.isClientSide() && hand == InteractionHand.MAIN_HAND) {
      player.openMenu(new SimpleMenuProvider(
          (windowId, inventory, menuPlayer) -> new BaseCenterMenu(windowId, inventory, pos),
          Component.translatable("container.craftingdeadimmerse.base_center")));
    }
    return ItemInteractionResult.SUCCESS;
  }

  @Override
  protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
      boolean movedByPiston) {
    if (!state.is(newState.getBlock())
        && level.getBlockEntity(pos) instanceof BaseCenterBlockEntity baseCenter) {
      baseCenter.unregister();
    }
    super.onRemove(state, level, pos, newState, movedByPiston);
  }
}

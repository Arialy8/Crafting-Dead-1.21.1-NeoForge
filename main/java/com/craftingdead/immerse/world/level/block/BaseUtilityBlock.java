package com.craftingdead.immerse.world.level.block;
import com.craftingdead.immerse.world.base.BaseSecurity;
import com.craftingdead.immerse.world.level.block.entity.BaseUtilityBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public final class BaseUtilityBlock extends Block implements EntityBlock {
  public BaseUtilityBlock(Properties properties) { super(properties); }
  @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BaseUtilityBlockEntity(pos, state); }
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
    return level.isClientSide() ? null : (world, pos, current, entity) -> {
      if (entity instanceof BaseUtilityBlockEntity machine) machine.tick();
    };
  }
  @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (!BaseSecurity.canOpenContainer(level, pos, player)) return InteractionResult.FAIL;
    if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BaseUtilityBlockEntity machine) {
      player.openMenu(machine);
      player.displayClientMessage(net.minecraft.network.chat.Component.literal(machine.instructions()), false);
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
  }
  @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
    if (state.getBlock() != next.getBlock() && level.getBlockEntity(pos) instanceof BaseUtilityBlockEntity machine)
      Containers.dropContents(level, pos, machine);
    super.onRemove(state, level, pos, next, moved);
  }
}

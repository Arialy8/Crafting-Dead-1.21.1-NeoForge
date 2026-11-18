package com.craftingdead.core.world.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Places the T-72B3 without delegating placement to another vehicle mod. */
public final class T72B3Item extends Item {

  public T72B3Item(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    Level level = context.getLevel();
    if (level.isClientSide()) {
      return InteractionResult.SUCCESS;
    }

    BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
    T72B3Entity tank = VehicleContent.T72B3.get().create(level);
    if (tank == null) {
      return InteractionResult.FAIL;
    }
    tank.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
        context.getPlayer() == null ? 0.0F : context.getPlayer().getYRot(), 0.0F);
    level.addFreshEntity(tank);
    if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
      context.getItemInHand().shrink(1);
    }
    return InteractionResult.CONSUME;
  }
}

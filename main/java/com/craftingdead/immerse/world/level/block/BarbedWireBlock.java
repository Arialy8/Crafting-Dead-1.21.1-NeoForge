package com.craftingdead.immerse.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BarbedWireBlock extends Block {

  public static final MapCodec<BarbedWireBlock> CODEC = simpleCodec(BarbedWireBlock::new);

  public BarbedWireBlock(Properties properties) {
    super(properties);
  }

  @Override
  protected MapCodec<? extends Block> codec() {
    return CODEC;
  }

  @Override
  protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
    if (!level.isClientSide() && entity.tickCount % 10 == 0) {
      entity.hurt(level.damageSources().cactus(), 2.0F);
    }
  }
}

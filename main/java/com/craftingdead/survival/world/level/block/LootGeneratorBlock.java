package com.craftingdead.survival.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LootGeneratorBlock extends Block {

  public static final MapCodec<LootGeneratorBlock> CODEC = simpleCodec(
      properties -> new LootGeneratorBlock(properties, () -> null));

  private static final int DEFAULT_REFRESH_DELAY_TICKS = 20 * 60 * 20;
  private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D);

  private final Supplier<Block> lootBlock;
  private final Supplier<? extends ParticleOptions> particleOptions;
  private final int refreshDelayTicks;

  public LootGeneratorBlock(Properties properties, Supplier<Block> lootBlock) {
    this(properties, lootBlock, () -> null, DEFAULT_REFRESH_DELAY_TICKS);
  }

  public LootGeneratorBlock(Properties properties, Supplier<Block> lootBlock,
      Supplier<? extends ParticleOptions> particleOptions) {
    this(properties, lootBlock, particleOptions, DEFAULT_REFRESH_DELAY_TICKS);
  }

  public LootGeneratorBlock(Properties properties, Supplier<Block> lootBlock,
      Supplier<? extends ParticleOptions> particleOptions, int refreshDelayTicks) {
    super(properties);
    this.lootBlock = lootBlock;
    this.particleOptions = particleOptions;
    this.refreshDelayTicks = refreshDelayTicks;
  }

  @Override
  protected MapCodec<? extends Block> codec() {
    return CODEC;
  }

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.INVISIBLE;
  }

  @Override
  protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
      CollisionContext context) {
    return SHAPE;
  }

  @Override
  protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
      boolean movedByPiston) {
    super.onPlace(state, level, pos, oldState, movedByPiston);
    if (!level.isClientSide()) {
      level.scheduleTick(pos, this, 1);
    }
  }

  @Override
  protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    super.tick(state, level, pos, random);
    this.tryGenerateLoot(level, pos);
    level.scheduleTick(pos, this, this.refreshDelayTicks);
  }

  @Override
  public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    super.animateTick(state, level, pos, random);
    var particle = this.particleOptions.get();
    if (particle == null || !this.hasCreativePlayerNearby(level, pos)) {
      return;
    }
    level.addParticle(particle,
        pos.getX() + 0.5D,
        pos.getY() - 0.45D,
        pos.getZ() + 0.5D,
        0.0D, 0.0D, 0.0D);
  }

  private void tryGenerateLoot(ServerLevel level, BlockPos pos) {
    var loot = this.lootBlock.get();
    if (loot == null) {
      return;
    }

    var lootState = loot.defaultBlockState();
    boolean lootExists = level.getBlockStates(new AABB(
        pos.getX() - 2.0D, pos.getY(), pos.getZ() - 2.0D,
        pos.getX() + 3.0D, pos.getY() + 1.0D, pos.getZ() + 3.0D))
        .anyMatch(lootState::equals);
    if (lootExists) {
      return;
    }

    BlockPos preciseTarget = pos.below();
    if (level.isEmptyBlock(preciseTarget)
        && Block.canSupportCenter(level, preciseTarget.below(), Direction.UP)) {
      level.setBlockAndUpdate(preciseTarget, lootState);
      return;
    }

    for (int radius = 1; radius <= 2; radius++) {
      for (int x = -radius; x <= radius; x++) {
        for (int z = -radius; z <= radius; z++) {
          if (Math.max(Math.abs(x), Math.abs(z)) != radius) {
            continue;
          }
          BlockPos targetPos = pos.offset(x, 0, z);
          if (level.isEmptyBlock(targetPos)
              && Block.canSupportCenter(level, targetPos.below(), Direction.UP)) {
            level.setBlockAndUpdate(targetPos, lootState);
            return;
          }
        }
      }
    }
  }

  private boolean hasCreativePlayerNearby(Level level, BlockPos pos) {
    return !level.getEntitiesOfClass(Player.class,
        new AABB(pos.getX() - 8.0D, pos.getY() - 8.0D, pos.getZ() - 8.0D,
            pos.getX() + 9.0D, pos.getY() + 9.0D, pos.getZ() + 9.0D),
        Player::isCreative).isEmpty();
  }
}

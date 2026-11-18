package com.craftingdead.survival.world.item;

import com.craftingdead.survival.world.entity.SupplyDrop;
import com.craftingdead.survival.world.entity.SurvivalEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.block.state.BlockState;

public class SupplyDropRadioItem extends Item {

  private static final int CALL_DURATION_TICKS = 20 * 3;
  private static final int COOLDOWN_TICKS = 20 * 60;

  private final ResourceKey<LootTable> lootTable;

  public SupplyDropRadioItem(Properties properties, ResourceKey<LootTable> lootTable) {
    super(properties);
    this.lootTable = lootTable;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    var itemStack = player.getItemInHand(hand);
    player.startUsingItem(hand);
    return InteractionResultHolder.consume(itemStack);
  }

  @Override
  public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity livingEntity) {
    if (!(livingEntity instanceof Player player) || !(level instanceof ServerLevel serverLevel)) {
      return itemStack;
    }
    var targetPos = this.findDropPosition(serverLevel, player);
    if (targetPos == null) {
      player.displayClientMessage(
          Component.translatable("message.craftingdeadsurvival.supply_drop.no_space"), true);
      return itemStack;
    }

    BlockPos spawnPos = targetPos.above(25);
    SupplyDrop supplyDrop = new SupplyDrop(SurvivalEntityTypes.SUPPLY_DROP.get(), level,
        this.lootTable, level.getRandom().nextLong(), targetPos.getX() + 0.5D,
        spawnPos.getY(), targetPos.getZ() + 0.5D);
    level.addFreshEntity(supplyDrop);
    level.playSound(null, targetPos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.25F);
    serverLevel.sendParticles(ParticleTypes.CLOUD,
        spawnPos.getX() + 0.5D, spawnPos.getY() + 0.5D, spawnPos.getZ() + 0.5D,
        40, 0.7D, 0.4D, 0.7D, 0.03D);

    player.awardStat(Stats.ITEM_USED.get(this));
    player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
    itemStack.shrink(1);
    player.displayClientMessage(
        Component.translatable("message.craftingdeadsurvival.supply_drop.called"), true);

    return itemStack;
  }

  @Override
  public int getUseDuration(ItemStack stack, LivingEntity entity) {
    return CALL_DURATION_TICKS;
  }

  @Override
  public UseAnim getUseAnimation(ItemStack stack) {
    return UseAnim.BOW;
  }

  private BlockPos findDropPosition(ServerLevel level, Player player) {
    BlockPos origin = player.blockPosition();
    Direction facing = Direction.fromYRot(player.getYRot());
    var preferred = origin.relative(facing, 3);
    for (int yOffset = 0; yOffset <= 2; yOffset++) {
      var pos = preferred.above(yOffset);
      if (this.canPlaceDrop(level, pos)) {
        return pos;
      }
    }

    for (int radius = 1; radius <= 8; radius++) {
      for (int xOffset = -radius; xOffset <= radius; xOffset++) {
        for (int zOffset = -radius; zOffset <= radius; zOffset++) {
          for (int yOffset = -1; yOffset <= 2; yOffset++) {
            var pos = origin.offset(xOffset, yOffset, zOffset);
            if (this.canPlaceDrop(level, pos)) {
              return pos;
            }
          }
        }
      }
    }
    return null;
  }

  private boolean canPlaceDrop(ServerLevel level, BlockPos pos) {
    if (!level.isInWorldBounds(pos)) {
      return false;
    }
    BlockState state = level.getBlockState(pos);
    return (state.isAir() || state.canBeReplaced())
        && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
  }

  public static boolean spawnSupplyDrop(ServerLevel level, BlockPos targetPos,
      ResourceKey<LootTable> lootTable) {
    if (level == null || targetPos == null || lootTable == null) {
      return false;
    }
    BlockPos ground = level.getHeightmapPos(
        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetPos);
    BlockPos spawnPos = ground.above(25);
    SupplyDrop supplyDrop = new SupplyDrop(SurvivalEntityTypes.SUPPLY_DROP.get(), level,
        lootTable, level.getRandom().nextLong(), ground.getX() + 0.5D,
        spawnPos.getY(), ground.getZ() + 0.5D);
    boolean added = level.addFreshEntity(supplyDrop);
    if (added) {
      level.playSound(null, ground, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.25F);
      level.sendParticles(ParticleTypes.CLOUD, spawnPos.getX() + 0.5D,
          spawnPos.getY() + 0.5D, spawnPos.getZ() + 0.5D,
          40, 0.7D, 0.4D, 0.7D, 0.03D);
    }
    return added;
  }
}

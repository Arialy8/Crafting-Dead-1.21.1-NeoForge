package com.craftingdead.immerse.world.base;

import com.craftingdead.immerse.world.level.block.entity.BaseCenterBlockEntity;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class BaseSecurity {

  public static boolean canBuildBlueprint(Level level, BlockPos pos, Player player) {
    if (player != null && player.getAbilities().instabuild) {
      return true;
    }
    return findBase(level, pos).map(base -> base.hasPermission(player, BasePermission.BUILD))
        .orElse(false);
  }

  public static boolean canBuild(Level level, BlockPos pos, Player player) {
    return canUse(level, pos, player, BasePermission.BUILD, true);
  }

  public static boolean canBreak(Level level, BlockPos pos, Player player) {
    return canUse(level, pos, player, BasePermission.BREAK, true);
  }

  public static boolean canOpenContainer(Level level, BlockPos pos, Player player) {
    return canUse(level, pos, player, BasePermission.CONTAINER, true);
  }

  public static boolean canCraft(Level level, BlockPos pos, Player player) {
    return canUse(level, pos, player, BasePermission.CRAFT, false);
  }

  public static boolean canUseProtectedPos(Level level, BlockPos pos, Player player) {
    if (player != null && player.getAbilities().instabuild) {
      return true;
    }
    return findBase(level, pos).map(base -> base.canUse(player) || base.isBreached()).orElse(true);
  }

  private static boolean canUse(Level level, BlockPos pos, Player player,
      BasePermission permission, boolean outsideAllowed) {
    if (player != null && player.getAbilities().instabuild) {
      return true;
    }
    return findBase(level, pos).map(base -> base.hasPermission(player, permission)
            || (base.isBreached()
                && (permission == BasePermission.BREAK
                    || permission == BasePermission.CONTAINER)))
        .orElse(outsideAllowed);
  }

  public static boolean canPlaceBaseCenter(Level level, BlockPos pos, Player player) {
    if (player == null || level == null || pos == null) {
      return false;
    }
    if (isBlueprintPlacementRestricted(level, pos, player)) {
      return false;
    }
    if (player.getAbilities().instabuild) {
      return true;
    }
    if (BaseRegistry.getBasePos(level, player.getUUID()).isPresent()) {
      return false;
    }
    return findOverlappingBase(level, pos).isEmpty();
  }

  public static boolean isBlueprintPlacementRestricted(Level level, BlockPos pos, Player player) {
    if (!(level instanceof ServerLevel serverLevel) || pos == null) {
      return false;
    }
    if (player instanceof ServerPlayer serverPlayer
        && serverPlayer.isCreative() && serverPlayer.hasPermissions(2)) {
      return false;
    }
    return BlueprintRestrictionData.isRestricted(serverLevel, pos);
  }

  public static boolean hasRegisteredBase(Level level, Player player) {
    return level != null && player != null
        && BaseRegistry.getBasePos(level, player.getUUID()).isPresent();
  }

  public static Optional<BaseCenterBlockEntity> findOverlappingBase(Level level, BlockPos pos) {
    if (level == null || pos == null) {
      return Optional.empty();
    }
    int radius = BaseCenterBlockEntity.PROTECTION_RADIUS * 2;
    if (!level.isClientSide()) {
      return BaseRegistry.findBasePos(level, pos, radius)
          .map(level::getBlockEntity)
          .filter(BaseCenterBlockEntity.class::isInstance)
          .map(BaseCenterBlockEntity.class::cast);
    }
    BlockPos min = pos.offset(-radius, -radius, -radius);
    BlockPos max = pos.offset(radius, radius, radius);
    for (BlockPos candidatePos : BlockPos.betweenClosed(min, max)) {
      if (level.getBlockEntity(candidatePos) instanceof BaseCenterBlockEntity baseCenter) {
        int dx = Math.abs(candidatePos.getX() - pos.getX());
        int dy = Math.abs(candidatePos.getY() - pos.getY());
        int dz = Math.abs(candidatePos.getZ() - pos.getZ());
        if (dx <= radius && dy <= radius && dz <= radius) {
          return Optional.of(baseCenter);
        }
      }
    }
    return Optional.empty();
  }

  public static Optional<BaseCenterBlockEntity> findBase(Level level, BlockPos pos) {
    if (level == null || pos == null) {
      return Optional.empty();
    }
    int radius = BaseCenterBlockEntity.PROTECTION_RADIUS;
    if (!level.isClientSide()) {
      return BaseRegistry.findBasePos(level, pos, radius)
          .map(level::getBlockEntity)
          .filter(BaseCenterBlockEntity.class::isInstance)
          .map(BaseCenterBlockEntity.class::cast);
    }
    BlockPos min = pos.offset(-radius, -radius, -radius);
    BlockPos max = pos.offset(radius, radius, radius);
    BaseCenterBlockEntity closest = null;
    double closestDistance = Double.MAX_VALUE;
    for (BlockPos candidatePos : BlockPos.betweenClosed(min, max)) {
      if (level.getBlockEntity(candidatePos) instanceof BaseCenterBlockEntity baseCenter) {
        int dx = Math.abs(candidatePos.getX() - pos.getX());
        int dy = Math.abs(candidatePos.getY() - pos.getY());
        int dz = Math.abs(candidatePos.getZ() - pos.getZ());
        double distance = candidatePos.distSqr(pos);
        if (dx <= radius && dy <= radius && dz <= radius && distance < closestDistance) {
          closest = baseCenter;
          closestDistance = distance;
        }
      }
    }
    return Optional.ofNullable(closest);
  }

  private BaseSecurity() {}
}

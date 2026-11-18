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

package com.craftingdead.core.util;

import java.util.Optional;
import com.craftingdead.core.tags.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RayTraceUtil {

  private static final double MAX_GUN_TRACE_DISTANCE = 512.0D;
  private static final double LOADED_CHUNK_PROBE_STEP = 4.0D;
  private static final int MAX_PASSTHROUGH_STEPS = 64;

  public static Optional<EntityHitResult> rayTraceEntities(Entity fromEntity) {
    var reachDistanceAttribute = fromEntity instanceof Player player
        ? player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE)
        : null;
    var distance = reachDistanceAttribute == null ? 4.0D : reachDistanceAttribute.getValue();
    var startPos = fromEntity.getEyePosition(1.0F);
    var look = fromEntity.getViewVector(1.0F);
    var scaledLook = look.scale(distance);
    var endPos = startPos.add(scaledLook);
    return Optional.ofNullable(getEntityHitResult(fromEntity, startPos, endPos, scaledLook,
        EntitySelector.ENTITY_STILL_ALIVE.and(EntitySelector.NO_CREATIVE_OR_SPECTATOR),
        distance * distance));
  }

  /**
   * Ray traces living entities for item actions.
   *
   * <p>The vanilla/old rayTraceEntities path excludes creative players via
   * {@code EntitySelector.NO_CREATIVE_OR_SPECTATOR}. That breaks multiplayer medicine and
   * handcuffs tests when the target player is in creative mode. Medical items and handcuffs
   * should be able to target non-spectator players regardless of game mode, so this helper keeps
   * creative players targetable while still ignoring the performer and spectators.</p>
   */
  public static Optional<EntityHitResult> rayTraceLivingEntities(Entity fromEntity) {
    var reachDistanceAttribute = fromEntity instanceof Player player
        ? player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE)
        : null;
    var distance = reachDistanceAttribute == null ? 4.0D : reachDistanceAttribute.getValue();
    var startPos = fromEntity.getEyePosition(1.0F);
    var look = fromEntity.getViewVector(1.0F);
    var scaledLook = look.scale(distance);
    var endPos = startPos.add(scaledLook);
    return Optional.ofNullable(getEntityHitResult(fromEntity, startPos, endPos, scaledLook,
        entity -> entity != fromEntity
            && !entity.isSpectator()
            && entity instanceof LivingEntity livingEntity
            && livingEntity.isAlive(),
        distance * distance));
  }

  /**
   * Ray traces blocks and entities.
   *
   * @param fromEntity - the entity performing the ray trace
   * @param distance - maximum distance
   * @param partialTick - partialTick for frame interpolation
   * @return an optional hit result
   */
  public static Optional<? extends HitResult> rayTrace(Entity fromEntity,
      double distance, float partialTick, float pitchOffset, float yawOffset) {
    var startPos = fromEntity.getEyePosition(partialTick);

    var look = calculateViewVector(fromEntity.getViewXRot(partialTick) + pitchOffset,
        fromEntity.getViewYRot(partialTick) + yawOffset);
    distance = getSafeTraceDistance(startPos, look, distance, fromEntity.level());
    if (distance <= 0.0D) {
      return Optional.empty();
    }

    var scaledLook = look.scale(distance);
    var endPos = startPos.add(scaledLook);

    var blockHitResult = rayTraceBlocks(startPos, distance, look, fromEntity.level(), fromEntity);

    var sqrDistance = blockHitResult.isPresent()
        ? blockHitResult.get().getLocation().distanceToSqr(startPos)
        : distance * distance;

    var entityHitResult = getEntityHitResult(fromEntity, startPos, endPos, scaledLook,
        LIVING_ENTITY_STILL_ALIVE.and(EntitySelector.NO_CREATIVE_OR_SPECTATOR),
        sqrDistance);

    return entityHitResult == null ? blockHitResult : Optional.of(entityHitResult);
  }

  /**
   * 1.21.1 compatibility wrapper for entity ray traces.
   *
   * <p>The old Crafting Dead code called ProjectileUtil directly from many gun paths. Keeping
   * the call centralized here makes it easier to switch to the Level-first ProjectileUtil
   * overload if the target Forge/NeoForge mapping requires it later.
   */
  public static EntityHitResult getEntityHitResult(Entity fromEntity, Vec3 startPos, Vec3 endPos,
      Vec3 scaledLook, Predicate<Entity> filter, double sqrDistance) {
    return ProjectileUtil.getEntityHitResult(fromEntity, startPos, endPos,
        fromEntity.getBoundingBox().expandTowards(scaledLook).inflate(1.0D, 1.0D, 1.0D),
        filter, sqrDistance);
  }

  private static final Predicate<Entity> LIVING_ENTITY_STILL_ALIVE =
      entity -> entity instanceof LivingEntity livingEntity && livingEntity.isAlive();

  /**
   * Ray traces blocks, ignoring fluids.
   * 
   * @param startPos- start position
   * @param distance- maximum distance
   * @param look- look/rotation vector
   * @param level- the level
   * @return an optional hit result
   */
  public static Optional<BlockHitResult> rayTraceBlocks(Vec3 startPos, double distance,
      Vec3 look, Level level) {
    return rayTraceBlocks(startPos, distance, look,
        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, level, null);
  }

  public static Optional<BlockHitResult> rayTraceBlocks(Vec3 startPos, double distance,
      Vec3 look, Level level, Entity contextEntity) {
    return rayTraceBlocks(startPos, distance, look,
        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, level, contextEntity);
  }

  /**
   * Ray traces blocks.
   * 
   * @param startPos - start position
   * @param distance - maximum distance
   * @param look - look/rotation vector
   * @param blockMode - block clipping mode
   * @param fluidMode - fluid clipping mode
   * @param level - the level
   * @return an optional hit result
   */
  public static Optional<BlockHitResult> rayTraceBlocks(Vec3 startPos, double distance,
      Vec3 look, ClipContext.Block blockMode, ClipContext.Fluid fluidMode, Level level) {
    return rayTraceBlocks(startPos, distance, look, blockMode, fluidMode, level, null);
  }

  public static Optional<BlockHitResult> rayTraceBlocks(Vec3 startPos, double distance,
      Vec3 look, ClipContext.Block blockMode, ClipContext.Fluid fluidMode, Level level,
      Entity contextEntity) {
    if (contextEntity == null) {
      // Minecraft 1.21.1 ClipContext no longer accepts a null entity; using null crashes in
      // EntityCollisionContext. If an older call path cannot provide an entity, skip block
      // clipping rather than killing the server shot task.
      return Optional.empty();
    }

    distance = getSafeTraceDistance(startPos, look, distance, level);
    if (distance <= 0.0D) {
      return Optional.empty();
    }

    var currentPos = startPos;
    final var endPos = startPos.add(look.scale(distance));

    BlockHitResult hitResult = null;
    BlockPos lastBlockPos = null;
    int passThroughSteps = 0;
    while (passThroughSteps++ < MAX_PASSTHROUGH_STEPS) {
      if (currentPos.distanceTo(startPos) >= distance) {
        break;
      }

      var context = new ClipContext(currentPos, endPos, blockMode, fluidMode, contextEntity);
      hitResult = level.clip(context);

      if (hitResult != null) {
        // Not sure about this one, but I have a concern about inaccuracy of Double which could lead
        // to an endless loop
        var blockPos = hitResult.getBlockPos();
        if (lastBlockPos != null && lastBlockPos.equals(blockPos)) {
          break;
        }
        lastBlockPos = blockPos;

        var blockState = level.getBlockState(blockPos);
        if (blockState.canOcclude() && !blockState.is(ModBlockTags.BULLETS_PASS_THROUGH)) {
          break;
        }

        var hitPos = hitResult.getLocation();
        var shape = context.getBlockShape(blockState, level, blockPos);
        if (!shape.isEmpty()) {
          var bounds = shape.bounds();
          var xDist = look.x() < 0.0D
              ? hitPos.x() - bounds.minX - blockPos.getX()
              : blockPos.getX() - hitPos.x() + bounds.maxX;
          var yDist = look.y() < 0.0D
              ? hitPos.y() - bounds.minY - blockPos.getY()
              : blockPos.getY() - hitPos.y() + bounds.maxY;
          var zDist = look.z() < 0.0D
              ? hitPos.z() - bounds.minZ - blockPos.getZ()
              : blockPos.getZ() - hitPos.z() + bounds.maxZ;
          var xRayDist =
              Math.abs(look.x()) != 0.0D ? xDist / Math.abs(look.x()) : Double.MAX_VALUE;
          var yRayDist =
              Math.abs(look.y()) != 0.0D ? yDist / Math.abs(look.y()) : Double.MAX_VALUE;
          var zRayDist =
              Math.abs(look.z()) != 0.0D ? zDist / Math.abs(look.z()) : Double.MAX_VALUE;

          var rayDist = Math.min(xRayDist, Math.min(zRayDist, yRayDist));
          currentPos = hitPos.add(look.scale(rayDist));
        }
      }
    }

    return Optional.ofNullable(hitResult);
  }

  private static double getSafeTraceDistance(Vec3 startPos, Vec3 look, double requestedDistance,
      Level level) {
    if (!Double.isFinite(requestedDistance) || requestedDistance <= 0.0D
        || !Double.isFinite(startPos.x) || !Double.isFinite(startPos.y)
        || !Double.isFinite(startPos.z) || !Double.isFinite(look.x)
        || !Double.isFinite(look.y) || !Double.isFinite(look.z)
        || look.lengthSqr() < 1.0E-8D) {
      return 0.0D;
    }

    double distance = Math.min(requestedDistance, MAX_GUN_TRACE_DISTANCE);
    if (!(level instanceof ServerLevel)) {
      return distance;
    }

    double lastLoadedDistance = 0.0D;
    for (double probe = 0.0D; probe <= distance; probe += LOADED_CHUNK_PROBE_STEP) {
      BlockPos probePos = BlockPos.containing(startPos.add(look.scale(probe)));
      if (!level.hasChunkAt(probePos)) {
        break;
      }
      lastLoadedDistance = probe;
    }

    if (lastLoadedDistance < distance) {
      return Math.max(0.0D, lastLoadedDistance);
    }
    return distance;
  }

  public static Vec3 calculateViewVector(float pitch, float yaw) {
    float pitchRad = pitch * ((float) Math.PI / 180F);
    float yawRad = -yaw * ((float) Math.PI / 180F);
    float yawCos = Mth.cos(yawRad);
    float yawSin = Mth.sin(yawRad);
    float pitchCos = Mth.cos(pitchRad);
    float pitchSin = Mth.sin(pitchRad);
    return new Vec3((yawSin * pitchCos), (-pitchSin), (yawCos * pitchCos));
  }
}

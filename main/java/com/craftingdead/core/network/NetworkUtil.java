/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */

package com.craftingdead.core.network;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class NetworkUtil {

  public static Entity getEntityOrSender(IPayloadContext context, int entityId) {
    return getEntityOrSender(context, entityId, Entity.class);
  }

  public static <T extends Entity> T getEntityOrSender(IPayloadContext context, int entityId,
      Class<T> clazz) {
    return findEntityOrSender(context, entityId, clazz)
        .orElseThrow(() -> new IllegalStateException(
            String.format("Entity/sender with ID %s of type %s is absent", entityId,
                clazz == null ? "unknown" : clazz.getName())));
  }

  public static Optional<Entity> findEntityOrSender(IPayloadContext context, int entityId) {
    return findEntityOrSender(context, entityId, Entity.class).map(entity -> entity);
  }

  public static <T extends Entity> Optional<T> findEntityOrSender(IPayloadContext context,
      int entityId, Class<T> clazz) {
    if (context == null || clazz == null) {
      return Optional.empty();
    }
    if (context.flow() == PacketFlow.SERVERBOUND) {
      return Optional.ofNullable(context.player()).filter(clazz::isInstance).map(clazz::cast);
    }
    return findEntity(context, entityId, clazz);
  }

  public static boolean isServerReception(IPayloadContext context) {
    return context != null && context.flow() == PacketFlow.SERVERBOUND;
  }

  public static boolean isClientReception(IPayloadContext context) {
    return context != null && context.flow() == PacketFlow.CLIENTBOUND;
  }

  public static Optional<Entity> findEntity(IPayloadContext context, int entityId) {
    return findEntity(context, entityId, Entity.class).map(entity -> entity);
  }

  public static <T extends Entity> Optional<T> findEntity(IPayloadContext context, int entityId,
      Class<T> clazz) {
    if (context == null || clazz == null) {
      return Optional.empty();
    }
    if (context.flow() == PacketFlow.SERVERBOUND) {
      var sender = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
      if (sender != null && sender.level() != null) {
        return Optional.ofNullable(sender.level().getEntity(entityId))
            .filter(clazz::isInstance)
            .map(clazz::cast);
      }
      return Optional.empty();
    }
    var level = context.player() == null ? null : context.player().level();
    if (level != null) {
      return Optional.ofNullable(level.getEntity(entityId))
          .filter(clazz::isInstance)
          .map(clazz::cast);
    }
    return Optional.empty();
  }

  public static Entity getEntity(IPayloadContext context, int entityId) {
    return getEntity(context, entityId, Entity.class);
  }

  public static <T extends Entity> T getEntity(IPayloadContext context, int entityId,
      Class<T> clazz) {
    return findEntity(context, entityId, clazz)
        .orElseThrow(() -> new IllegalStateException(
            String.format("Entity with ID %s of type %s is absent", entityId,
                clazz == null ? "unknown" : clazz.getName())));
  }
}

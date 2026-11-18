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

package com.craftingdead.core.network.message.play;

import java.util.Collection;
import java.util.Map;
import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.world.entity.extension.EntitySnapshot;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.craftingdead.core.world.item.gun.PendingHit;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public record ValidatePendingHitMessage(Map<Integer, Collection<PendingHit>> hits) implements CustomPacketPayload {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final int MAX_ENTITY_HIT_BUCKETS = 256;
  private static final int MAX_HITS_PER_ENTITY = 128;

  public static final CustomPacketPayload.Type<ValidatePendingHitMessage> TYPE = NetworkPayloadIds.VALIDATE_PENDING_HIT;

  public static final StreamCodec<FriendlyByteBuf, ValidatePendingHitMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(ValidatePendingHitMessage::encode, ValidatePendingHitMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {
    var safeHits = this.hits == null ? Map.<Integer, Collection<PendingHit>>of() : this.hits;
    var buckets = new Int2ObjectLinkedOpenHashMap<Collection<PendingHit>>();
    for (var hit : safeHits.entrySet()) {
      if (buckets.size() >= MAX_ENTITY_HIT_BUCKETS || hit.getValue() == null) {
        break;
      }
      var values = new ObjectArrayList<PendingHit>();
      for (var value : hit.getValue()) {
        if (values.size() >= MAX_HITS_PER_ENTITY) {
          break;
        }
        if (value != null && value.playerSnapshot() != null && value.hitSnapshot() != null) {
          values.add(value);
        }
      }
      if (!values.isEmpty()) {
        buckets.put(hit.getKey(), values);
      }
    }

    NetworkBufferUtil.writeVarInt(out, buckets.size(), 0, MAX_ENTITY_HIT_BUCKETS);
    for (var hit : buckets.entrySet()) {
      NetworkBufferUtil.writeVarInt(out, hit.getKey(), -1, Integer.MAX_VALUE);
      NetworkBufferUtil.writeVarInt(out, hit.getValue().size(), 0, MAX_HITS_PER_ENTITY);
      for (var value : hit.getValue()) {
        NetworkBufferUtil.writeByte(out, value.tickOffset(), Byte.MIN_VALUE, Byte.MAX_VALUE);
        value.playerSnapshot().encode(out);
        value.hitSnapshot().encode(out);
        NetworkBufferUtil.writeVarLong(out, value.randomSeed(), Long.MIN_VALUE, Long.MAX_VALUE);
        NetworkBufferUtil.writeVarInt(out, value.shotCount(), 0, Integer.MAX_VALUE);
      }
    }
  }

  public static ValidatePendingHitMessage decode(FriendlyByteBuf in) {
    final int hitsSize = NetworkBufferUtil.readCappedVarInt(in, MAX_ENTITY_HIT_BUCKETS);
    var hits = new Int2ObjectLinkedOpenHashMap<Collection<PendingHit>>();
    for (int i = 0; i < hitsSize; i++) {
      int key = NetworkBufferUtil.readVarInt(in, -1);
      int valueSize = NetworkBufferUtil.readCappedVarInt(in, MAX_HITS_PER_ENTITY);
      var value = new ObjectArrayList<PendingHit>();
      for (int j = 0; j < valueSize; j++) {
        value.add(new PendingHit(NetworkBufferUtil.readByte(in, 0),
            EntitySnapshot.decode(in), EntitySnapshot.decode(in),
            NetworkBufferUtil.readVarLong(in, 0L),
            Math.max(0, NetworkBufferUtil.readVarInt(in, 0))));
      }
      if (key >= 0) {
        hits.put(key, value);
      }
    }
    return new ValidatePendingHitMessage(hits);
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      var sender = context.player();
      if (sender == null) {
        return;
      }
      @SuppressWarnings("unchecked")
      var player = (PlayerExtension<ServerPlayer>) (PlayerExtension<?>) PlayerExtension.get(sender);
      if (player == null || this.hits == null) {
        
        return;
      }
      int hitCount = this.hits.values().stream()
          .filter(java.util.Objects::nonNull)
          .mapToInt(Collection::size)
          .sum();
      
      var gunOptional = player.mainHandGun();
      if (!gunOptional.isPresent()) {
        
        return;
      }
      gunOptional.ifPresent(gun -> {
        for (var hit : this.hits.entrySet()) {
          if (hit.getValue() == null) {
            continue;
          }
          var entity = player.level().getEntity(hit.getKey());
          if (entity == null) {
            continue;
          }
          if (!(entity instanceof LivingEntity livingEntity)) {
            continue;
          }
          var hitLiving = LivingExtension.get(livingEntity);
          if (hitLiving == null) {
            continue;
          }
          for (var value : hit.getValue()) {
            if (value != null) {
              gun.validatePendingHit(player, hitLiving, value);
            }
          }
        }
      });
    });
    return true;
  }
}

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

import com.craftingdead.core.network.NetworkUtil;
import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.world.item.gun.Gun;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncGunContainerSlotMessage(int entityId, int slot, FriendlyByteBuf data) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<SyncGunContainerSlotMessage> TYPE = NetworkPayloadIds.SYNC_GUN_CONTAINER_SLOT;

  public static final StreamCodec<FriendlyByteBuf, SyncGunContainerSlotMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(SyncGunContainerSlotMessage::encode, SyncGunContainerSlotMessage::decode);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }


  public SyncGunContainerSlotMessage(int entityId, int slot, Gun gun, boolean writeAll) {
    this(entityId, slot, new FriendlyByteBuf(Unpooled.buffer()));
    if (gun != null) {
      gun.encode(this.data, writeAll);
    }
  }

  public void encode(FriendlyByteBuf out) {
    NetworkBufferUtil.writeVarInt(out, this.entityId, -1, Integer.MAX_VALUE);
    NetworkBufferUtil.writeShort(out, this.slot, 0, Short.MAX_VALUE);
    NetworkBufferUtil.writePayload(out, this.data);
  }

  public static SyncGunContainerSlotMessage decode(FriendlyByteBuf in) {
    int entityId = NetworkBufferUtil.readVarInt(in, -1);
    int slot = NetworkBufferUtil.readShort(in, -1);
    return new SyncGunContainerSlotMessage(entityId, slot, NetworkBufferUtil.readPayload(in));
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> NetworkUtil.findEntity(context, this.entityId, Player.class)
        .filter(player -> this.data != null && this.slot >= 0)
        .map(this::getSyncedSlotStack)
        .filter(stack -> !stack.isEmpty())
        .flatMap(itemStack -> Gun.get(itemStack))
        .ifPresent(gun -> {
          try {
            this.data.readerIndex(0);
          } catch (RuntimeException ignored) {
          }
          gun.decode(this.data);
        }));
    return true;
  }

  private ItemStack getSyncedSlotStack(Player player) {
    // SyncGunContainerSlotMessage is produced from the server-side menu that changed.
    // In the equipment screen that is player.containerMenu, not player.inventoryMenu.
    // The old 1.21.1 port decoded into inventoryMenu only, so the gun-crafting result
    // slot never received the decoded attachments/skin on the client.
    if (player.containerMenu != null && this.slot < player.containerMenu.slots.size()) {
      return player.containerMenu.getSlot(this.slot).getItem();
    }
    if (player.inventoryMenu != null && this.slot < player.inventoryMenu.slots.size()) {
      return player.inventoryMenu.getSlot(this.slot).getItem();
    }
    return ItemStack.EMPTY;
  }
}

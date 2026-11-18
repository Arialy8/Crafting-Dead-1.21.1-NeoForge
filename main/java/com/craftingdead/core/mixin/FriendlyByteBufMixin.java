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

package com.craftingdead.core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.world.item.gun.Gun;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

// 1.21.x: hook FriendlyByteBuf#writeItem/readItem to keep legacy Gun capability sync alive.
@Mixin(FriendlyByteBuf.class)
public class FriendlyByteBufMixin {

  private void writeItem(ItemStack itemStack,
      CallbackInfoReturnable<FriendlyByteBuf> callbackInfo) {
    FriendlyByteBuf packetBuffer = (FriendlyByteBuf) (Object) this;
    Gun gun = itemStack == null || itemStack.isEmpty()
        ? null
        : Gun.get(itemStack).orElse(null);

    if (gun == null) {
      NetworkBufferUtil.writeVarInt(packetBuffer, 0, 0, 0);
    } else {
      FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
      try {
        gun.encode(data, true);
        NetworkBufferUtil.writePayload(packetBuffer, data);
      } catch (RuntimeException ex) {
        NetworkBufferUtil.writePayload(packetBuffer, null);
      } finally {
        data.release();
      }
    }
  }

  private void readItem(CallbackInfoReturnable<ItemStack> callbackInfo) {
    FriendlyByteBuf packetBuffer = (FriendlyByteBuf) (Object) this;
    ItemStack itemStack = callbackInfo.getReturnValue();
    FriendlyByteBuf data = NetworkBufferUtil.readPayload(packetBuffer);
    try {
      if (data.readableBytes() > 0 && itemStack != null && !itemStack.isEmpty()) {
        Gun gun = Gun.get(itemStack).orElse(null);
        if (gun != null) {
          gun.decode(data);
        }
      }
    } finally {
      data.release();
    }
  }
}

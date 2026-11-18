/*
 * Crafting Dead 1.21.1 port helper.
 */
package com.craftingdead.core.network.payload;

import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Compatibility adapter for the 1.21.x CustomPacketPayload migration.
 *
 * <p>Most legacy messages already expose {@code encode(FriendlyByteBuf)} / {@code decode(FriendlyByteBuf)}
 * methods. This helper wraps those methods as a vanilla {@link StreamCodec}, which is the format needed by
 * the new payload registrar. Keeping the adapter here avoids rewriting every message body at once.</p>
 */
public final class LegacyPayloadCodec {

  @FunctionalInterface
  public interface Encoder<T extends CustomPacketPayload> {
    void encode(T payload, FriendlyByteBuf buffer);
  }

  private LegacyPayloadCodec() {}

  public static <T extends CustomPacketPayload> StreamCodec<FriendlyByteBuf, T> codec(
      Encoder<T> encoder,
      Function<FriendlyByteBuf, T> decoder) {
    return new StreamCodec<>() {
      @Override
      public void encode(FriendlyByteBuf buffer, T value) {
        encoder.encode(value, buffer);
      }

      @Override
      public T decode(FriendlyByteBuf buffer) {
        return decoder.apply(buffer);
      }
    };
  }
}

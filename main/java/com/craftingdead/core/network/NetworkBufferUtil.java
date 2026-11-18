/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */

package com.craftingdead.core.network;

import com.craftingdead.core.util.ItemStackNbtUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;

public final class NetworkBufferUtil {

  private static final int MAX_SYNC_PAYLOAD_BYTES = 1 << 20;
  private static final int MAX_UTF_BYTES = 32_767;
  private static final ResourceLocation FALLBACK_ID =
      ResourceLocation.fromNamespaceAndPath("craftingdead", "missing");

  private NetworkBufferUtil() {}

  public static FriendlyByteBuf readPayload(FriendlyByteBuf in) {
    if (in == null || in.readableBytes() <= 0) {
      return new FriendlyByteBuf(Unpooled.EMPTY_BUFFER);
    }
    int requestedLength = readCappedVarInt(in, 0, MAX_SYNC_PAYLOAD_BYTES, 0);
    int length = Math.max(0,
        Math.min(requestedLength, Math.min(in.readableBytes(), MAX_SYNC_PAYLOAD_BYTES)));
    byte[] data = new byte[length];
    if (length > 0) {
      in.readBytes(data);
    }
    if (requestedLength > length) {
      skipBytes(in, requestedLength - length);
    }
    return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
  }

  public static void writePayload(FriendlyByteBuf out, FriendlyByteBuf data) {
    if (out == null) {
      return;
    }
    if (data == null) {
      writeVarInt(out, 0, 0, MAX_SYNC_PAYLOAD_BYTES);
      return;
    }
    int readableBytes = Math.max(0, Math.min(data.readableBytes(), MAX_SYNC_PAYLOAD_BYTES));
    writeVarInt(out, readableBytes, 0, MAX_SYNC_PAYLOAD_BYTES);
    if (readableBytes > 0) {
      try {
        out.writeBytes(data, data.readerIndex(), readableBytes);
      } catch (RuntimeException ignored) {
        // Ignore write failures from closed/released buffers during defensive sync paths.
      }
    }
  }

  public static int readCappedVarInt(FriendlyByteBuf in, int maxValue) {
    return readCappedVarInt(in, 0, maxValue, 0);
  }

  public static int readCappedVarInt(FriendlyByteBuf in, int minValue, int maxValue,
      int fallback) {
    int low = Math.min(minValue, maxValue);
    int high = Math.max(minValue, maxValue);
    int value = readVarInt(in, fallback);
    return Math.max(low, Math.min(value, high));
  }

  public static int readVarInt(FriendlyByteBuf in, int fallback) {
    try {
      return in == null || in.readableBytes() <= 0 ? fallback : in.readVarInt();
    } catch (RuntimeException ex) {
      return fallback;
    }
  }

  public static long readVarLong(FriendlyByteBuf in, long fallback) {
    try {
      return in == null || in.readableBytes() <= 0 ? fallback : in.readVarLong();
    } catch (RuntimeException ex) {
      return fallback;
    }
  }

  public static int readInt(FriendlyByteBuf in, int fallback) {
    try {
      return in == null || in.readableBytes() < Integer.BYTES ? fallback : in.readInt();
    } catch (RuntimeException ex) {
      return fallback;
    }
  }

  public static short readShort(FriendlyByteBuf in, int fallback) {
    try {
      return (short) (in == null || in.readableBytes() < Short.BYTES ? fallback : in.readShort());
    } catch (RuntimeException ex) {
      return (short) fallback;
    }
  }

  public static byte readByte(FriendlyByteBuf in, int fallback) {
    try {
      return (byte) (in == null || in.readableBytes() < 1 ? fallback : in.readByte());
    } catch (RuntimeException ex) {
      return (byte) fallback;
    }
  }

  public static boolean readBoolean(FriendlyByteBuf in, boolean fallback) {
    try {
      return in == null || in.readableBytes() < 1 ? fallback : in.readBoolean();
    } catch (RuntimeException ex) {
      return fallback;
    }
  }

  public static float readFloat(FriendlyByteBuf in, float fallback) {
    try {
      float value = in == null || in.readableBytes() < Float.BYTES ? fallback : in.readFloat();
      return Float.isFinite(value) ? value : fallback;
    } catch (RuntimeException ex) {
      return fallback;
    }
  }

  public static double readDouble(FriendlyByteBuf in, double fallback) {
    try {
      double value = in == null || in.readableBytes() < Double.BYTES ? fallback : in.readDouble();
      return Double.isFinite(value) ? value : fallback;
    } catch (RuntimeException ex) {
      return fallback;
    }
  }

  public static String readUtf(FriendlyByteBuf in, int maxLength, String fallback) {
    try {
      int safeMaxLength = Math.max(0, Math.min(maxLength, MAX_UTF_BYTES));
      return in == null || in.readableBytes() <= 0 ? fallback : in.readUtf(safeMaxLength);
    } catch (RuntimeException ex) {
      return fallback;
    }
  }

  public static BlockPos readBlockPos(FriendlyByteBuf in, BlockPos fallback) {
    try {
      return in == null || in.readableBytes() < Long.BYTES ? fallback : in.readBlockPos();
    } catch (RuntimeException ex) {
      return fallback;
    }
  }

  public static ResourceLocation readResourceLocation(FriendlyByteBuf in,
      ResourceLocation fallback) {
    try {
      var id = in == null || in.readableBytes() <= 0 ? null : in.readResourceLocation();
      return id == null ? fallback : id;
    } catch (RuntimeException ex) {
      return fallback;
    }
  }

  public static BlockHitResult readBlockHitResult(FriendlyByteBuf in, BlockHitResult fallback) {
    try {
      return in == null || in.readableBytes() <= 0 ? fallback : in.readBlockHitResult();
    } catch (RuntimeException ex) {
      return fallback;
    }
  }

  public static BlockHitResult fallbackBlockHitResult() {
    return new BlockHitResult(Vec3.ZERO, Direction.DOWN, BlockPos.ZERO, false);
  }

  public static <E extends Enum<E>> E readEnum(FriendlyByteBuf in, Class<E> enumClass,
      E fallback) {
    try {
      E value = in == null || enumClass == null || in.readableBytes() <= 0
          ? null
          : in.readEnum(enumClass);
      return value == null ? fallback : value;
    } catch (RuntimeException ex) {
      return fallback;
    }
  }



  public static void writeVarInt(FriendlyByteBuf out, int value, int minValue, int maxValue) {
    if (out == null) {
      return;
    }
    int low = Math.min(minValue, maxValue);
    int high = Math.max(minValue, maxValue);
    try {
      out.writeVarInt(Math.max(low, Math.min(value, high)));
    } catch (RuntimeException ignored) {
      // Ignore write failures from closed/released buffers during defensive sync paths.
    }
  }



  public static void writeVarLong(FriendlyByteBuf out, long value, long minValue, long maxValue) {
    if (out == null) {
      return;
    }
    long low = Math.min(minValue, maxValue);
    long high = Math.max(minValue, maxValue);
    try {
      out.writeVarLong(Math.max(low, Math.min(value, high)));
    } catch (RuntimeException ignored) {
      // Ignore write failures from closed/released buffers during defensive sync paths.
    }
  }

  public static void writeShort(FriendlyByteBuf out, int value, int minValue, int maxValue) {
    if (out == null) {
      return;
    }
    int low = Math.min(minValue, maxValue);
    int high = Math.max(minValue, maxValue);
    try {
      out.writeShort(Math.max(low, Math.min(value, high)));
    } catch (RuntimeException ignored) {
      // Ignore write failures from closed/released buffers during defensive sync paths.
    }
  }

  public static void writeByte(FriendlyByteBuf out, int value, int minValue, int maxValue) {
    if (out == null) {
      return;
    }
    int low = Math.min(minValue, maxValue);
    int high = Math.max(minValue, maxValue);
    try {
      out.writeByte(Math.max(low, Math.min(value, high)));
    } catch (RuntimeException ignored) {
      // Ignore write failures from closed/released buffers during defensive sync paths.
    }
  }

  public static void writeInt(FriendlyByteBuf out, int value, int minValue, int maxValue) {
    if (out == null) {
      return;
    }
    int low = Math.min(minValue, maxValue);
    int high = Math.max(minValue, maxValue);
    try {
      out.writeInt(Math.max(low, Math.min(value, high)));
    } catch (RuntimeException ignored) {
      // Ignore write failures from closed/released buffers during defensive sync paths.
    }
  }

  public static void writeFloat(FriendlyByteBuf out, float value, float minValue, float maxValue) {
    if (out == null) {
      return;
    }
    float safeValue = Float.isFinite(value) ? value : minValue;
    float low = Math.min(minValue, maxValue);
    float high = Math.max(minValue, maxValue);
    try {
      out.writeFloat(Math.max(low, Math.min(safeValue, high)));
    } catch (RuntimeException ignored) {
      // Ignore write failures from closed/released buffers during defensive sync paths.
    }
  }

  public static void writeDouble(FriendlyByteBuf out, double value, double fallback) {
    if (out == null) {
      return;
    }
    try {
      out.writeDouble(Double.isFinite(value) ? value : fallback);
    } catch (RuntimeException ignored) {
      // Ignore write failures from closed/released buffers during defensive sync paths.
    }
  }

  public static void writeBlockPos(FriendlyByteBuf out, BlockPos pos) {
    if (out == null) {
      return;
    }
    try {
      out.writeBlockPos(pos == null ? BlockPos.ZERO : pos);
    } catch (RuntimeException ignored) {
      // Ignore write failures from closed/released buffers during defensive sync paths.
    }
  }

  public static void writeBlockHitResult(FriendlyByteBuf out, BlockHitResult hitResult) {
    if (out == null) {
      return;
    }
    try {
      out.writeBlockHitResult(hitResult == null ? fallbackBlockHitResult() : hitResult);
    } catch (RuntimeException ignored) {
      // Ignore write failures from closed/released buffers during defensive sync paths.
    }
  }

  public static void writeBoolean(FriendlyByteBuf out, boolean value) {
    if (out == null) {
      return;
    }
    try {
      out.writeBoolean(value);
    } catch (RuntimeException ignored) {
      // Ignore write failures from closed/released buffers during defensive sync paths.
    }
  }

  public static void writeEnum(FriendlyByteBuf out, Enum<?> value, Enum<?> fallback) {
    if (out == null) {
      return;
    }
    Enum<?> safeValue = value == null ? fallback : value;
    if (safeValue == null) {
      return;
    }
    try {
      out.writeEnum(safeValue);
    } catch (RuntimeException ignored) {
      // Ignore write failures from closed/released buffers during defensive sync paths.
    }
  }

  public static void writeResourceLocation(FriendlyByteBuf out, ResourceLocation id,
      ResourceLocation fallback) {
    if (out == null) {
      return;
    }
    ResourceLocation safeFallback = fallback == null ? FALLBACK_ID : fallback;
    try {
      out.writeResourceLocation(id == null ? safeFallback : id);
    } catch (RuntimeException ignored) {
      if (safeFallback != id) {
        try {
          out.writeResourceLocation(safeFallback);
        } catch (RuntimeException ignoredAgain) {
          // Ignore write failures from closed/released buffers during defensive sync paths.
        }
      }
    }
  }

  public static void writeUtf(FriendlyByteBuf out, String value, int maxLength) {
    if (out == null) {
      return;
    }
    try {
      int safeMaxLength = Math.max(0, Math.min(maxLength, MAX_UTF_BYTES));
      String safeValue = value == null ? "" : value;
      if (safeValue.length() > safeMaxLength) {
        safeValue = safeValue.substring(0, safeMaxLength);
      }
      out.writeUtf(safeValue, safeMaxLength);
    } catch (RuntimeException ignored) {
      // Ignore write failures from closed/released buffers during defensive sync paths.
    }
  }

  public static ItemStack readItem(FriendlyByteBuf in) {
    try {
      if (in == null || in.readableBytes() <= 0) {
        return ItemStack.EMPTY;
      }
      if (in instanceof RegistryFriendlyByteBuf registryBuf) {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode(registryBuf);
      }

      // SyncGun* messages store their nested payload in a plain FriendlyByteBuf.
      // The optional item stream codec requires RegistryFriendlyByteBuf and would
      // otherwise decode as EMPTY, clearing loaded magazines on the client.
      CompoundTag tag = in.readNbt();
      return ItemStackNbtUtil.parseOptional(tag);
    } catch (RuntimeException ex) {
      skipRemaining(in);
      return ItemStack.EMPTY;
    }
  }

  public static void writeItem(FriendlyByteBuf out, ItemStack stack) {
    if (out == null) {
      return;
    }
    try {
      if (out instanceof RegistryFriendlyByteBuf registryBuf) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(registryBuf, stack == null ? ItemStack.EMPTY : stack);
      } else {
        out.writeNbt(ItemStackNbtUtil.save(stack == null ? ItemStack.EMPTY : stack));
      }
    } catch (RuntimeException ex) {
      try {
        if (out instanceof RegistryFriendlyByteBuf registryBuf) {
          ItemStack.OPTIONAL_STREAM_CODEC.encode(registryBuf, ItemStack.EMPTY);
        } else {
          out.writeNbt(new CompoundTag());
        }
      } catch (RuntimeException ignored) {
        // Ignore write failures from closed/released buffers during defensive sync paths.
      }
    }
  }

  public static void skipRemaining(FriendlyByteBuf in) {
    if (in == null) {
      return;
    }
    skipBytes(in, in.readableBytes());
  }

  public static void skipBytes(FriendlyByteBuf in, int requestedBytes) {
    if (in == null || requestedBytes <= 0) {
      return;
    }
    try {
      in.skipBytes(Math.min(in.readableBytes(), requestedBytes));
    } catch (RuntimeException ignored) {
      in.readerIndex(in.writerIndex());
    }
  }
}

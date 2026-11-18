package com.craftingdead.core.network.message.play;

import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.core.world.vehicle.T72B3Entity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Carries the controlling passenger's movement input to the authoritative tank entity. */
public record TankInputMessage(float throttle, float turn, float lookYaw, float lookPitch,
    int actions)
    implements CustomPacketPayload {

  public static final Type<TankInputMessage> TYPE = NetworkPayloadIds.TANK_INPUT;
  public static final StreamCodec<FriendlyByteBuf, TankInputMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(TankInputMessage::encode, TankInputMessage::decode);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {
    NetworkBufferUtil.writeFloat(out, this.throttle, -1.0F, 1.0F);
    NetworkBufferUtil.writeFloat(out, this.turn, -1.0F, 1.0F);
    NetworkBufferUtil.writeFloat(out, Mth.wrapDegrees(this.lookYaw), -180.0F, 180.0F);
    NetworkBufferUtil.writeFloat(out, Mth.clamp(this.lookPitch, -90.0F, 90.0F), -90.0F, 90.0F);
    out.writeByte(this.actions & 0xFF);
  }

  public static TankInputMessage decode(FriendlyByteBuf in) {
    return new TankInputMessage(
        Mth.clamp(NetworkBufferUtil.readFloat(in, 0.0F), -1.0F, 1.0F),
        Mth.clamp(NetworkBufferUtil.readFloat(in, 0.0F), -1.0F, 1.0F),
        Mth.wrapDegrees(NetworkBufferUtil.readFloat(in, 0.0F)),
        Mth.clamp(NetworkBufferUtil.readFloat(in, 0.0F), -90.0F, 90.0F),
        in.readUnsignedByte());
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      var player = context.player();
      if (player != null && player.getVehicle() instanceof T72B3Entity tank) {
        tank.acceptPassengerInput(player, this.throttle, this.turn, this.lookYaw,
            this.lookPitch, this.actions);
      }
    });
    return true;
  }
}

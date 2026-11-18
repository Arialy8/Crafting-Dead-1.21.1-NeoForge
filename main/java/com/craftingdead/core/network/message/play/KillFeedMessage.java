package com.craftingdead.core.network.message.play;

import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.immerse.client.ImmerseSurvivalHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record KillFeedMessage(String killer, String target, ResourceLocation weapon)
    implements CustomPacketPayload {

  public static final Type<KillFeedMessage> TYPE = NetworkPayloadIds.KILL_FEED;
  public static final StreamCodec<FriendlyByteBuf, KillFeedMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(KillFeedMessage::encode, KillFeedMessage::decode);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  private void encode(FriendlyByteBuf out) {
    out.writeUtf(this.killer, 128);
    out.writeUtf(this.target, 128);
    out.writeResourceLocation(this.weapon);
  }

  private static KillFeedMessage decode(FriendlyByteBuf in) {
    return new KillFeedMessage(in.readUtf(128), in.readUtf(128), in.readResourceLocation());
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> ImmerseSurvivalHud.addKill(
        this.killer, this.target, this.weapon));
    return true;
  }
}

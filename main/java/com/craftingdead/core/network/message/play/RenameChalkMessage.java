package com.craftingdead.core.network.message.play;
import com.craftingdead.core.world.item.ChalkItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record RenameChalkMessage(InteractionHand hand, String name) implements CustomPacketPayload {
  public static final Type<RenameChalkMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("craftingdead", "rename_chalk"));
  public static final StreamCodec<FriendlyByteBuf, RenameChalkMessage> CODEC = StreamCodec.of(
      (buf, msg) -> { buf.writeEnum(msg.hand); buf.writeUtf(msg.name, 64); },
      buf -> new RenameChalkMessage(buf.readEnum(InteractionHand.class), buf.readUtf(64)));
  @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
  public void handle(IPayloadContext context) {
    context.enqueueWork(() -> {
      var player = context.player();
      var stack = player.getItemInHand(hand);
      String clean = name.strip();
      if (!(stack.getItem() instanceof ChalkItem) || clean.isEmpty() || clean.length() > 64
          || clean.codePoints().anyMatch(c -> Character.isISOControl(c) || c == 167)) return;
      stack.set(DataComponents.CUSTOM_NAME, Component.literal(clean).withStyle(style -> style.withItalic(false)));
      player.containerMenu.broadcastChanges();
    });
  }
}

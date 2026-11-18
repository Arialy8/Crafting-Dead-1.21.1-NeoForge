/*
 * Crafting Dead 1.21.1 port helper.
 */
package com.craftingdead.core.network.payload;

import com.craftingdead.core.network.message.play.BlockDestroyActionMessage;
import com.craftingdead.core.network.message.play.BlockDestroyParticleMessage;
import com.craftingdead.core.network.message.play.C2SFriendActionMessage;
import com.craftingdead.core.network.message.play.C2SFriendRequestActionMessage;
import com.craftingdead.core.network.message.play.C2SFriendTeamActionMessage;
import com.craftingdead.core.network.message.play.C2SOpenFriendMenuMessage;
import com.craftingdead.core.network.message.play.CancelActionMessage;
import com.craftingdead.core.network.message.play.CrouchMessage;
import com.craftingdead.core.network.message.play.DamageHandcuffsMessage;
import com.craftingdead.core.network.message.play.EnableCombatModeMessage;
import com.craftingdead.core.network.message.play.HitMessage;
import com.craftingdead.core.network.message.play.KillFeedMessage;
import com.craftingdead.core.network.message.play.NPCTriggerPressedMessage;
import com.craftingdead.core.network.message.play.OpenCraftingMenuMessage;
import com.craftingdead.core.network.message.play.OpenEquipmentMenuMessage;
import com.craftingdead.core.network.message.play.OpenStorageMessage;
import com.craftingdead.core.network.message.play.ParachuteSyncMessage;
import com.craftingdead.core.network.message.play.PerformActionMessage;
import com.craftingdead.core.network.message.play.RightClickStateMessage;
import com.craftingdead.core.network.message.play.SecondaryActionMessage;
import com.craftingdead.core.network.message.play.SetFireModeMessage;
import com.craftingdead.core.network.message.play.SyncGunContainerSlotMessage;
import com.craftingdead.core.network.message.play.SyncGunEquipmentSlotMessage;
import com.craftingdead.core.network.message.play.SyncHydrationMessage;
import com.craftingdead.core.network.message.play.SyncSurvivalStatsMessage;
import com.craftingdead.core.network.message.play.SyncLivingMessage;
import com.craftingdead.core.network.message.play.SyncProtectionConfigMessage;
import com.craftingdead.core.network.message.play.S2CSyncFriendMenuMessage;
import com.craftingdead.core.network.message.play.S2CSyncFriendMessage;
import com.craftingdead.core.network.message.play.SyncBloodMoonMessage;
import com.craftingdead.core.network.message.play.TraumaPacket;
import com.craftingdead.core.network.message.play.TriggerPressedMessage;
import com.craftingdead.core.network.message.play.TankInputMessage;
import com.craftingdead.core.network.message.play.ValidatePendingHitMessage;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Central registry table for the 1.21.x payload migration.
 *
 * <p>The current Forge branch still keeps {@code SimpleChannel} as a compatibility bridge, but all
 * messages now expose {@code TYPE} and {@code STREAM_CODEC}. This table keeps the direction and codec
 * metadata in one place so the final payload registrar can be added without hunting through the gun,
 * HUD, entity and capability code.</p>
 */
public final class NetworkPayloadRegistrations {

  public enum Direction {
    PLAY_TO_CLIENT,
    PLAY_TO_SERVER,
    BIDIRECTIONAL
  }

  public record PayloadRegistration<T extends CustomPacketPayload>(
      int legacyDiscriminator,
      Class<T> messageClass,
      CustomPacketPayload.Type<T> type,
      StreamCodec<FriendlyByteBuf, T> streamCodec,
      Direction direction) {}

  public static final List<PayloadRegistration<?>> PLAY = List.of(
      client(0x00, SyncLivingMessage.class, SyncLivingMessage.TYPE, SyncLivingMessage.STREAM_CODEC),
      server(0x01, OpenEquipmentMenuMessage.class, OpenEquipmentMenuMessage.TYPE, OpenEquipmentMenuMessage.STREAM_CODEC),
      both(0x02, SecondaryActionMessage.class, SecondaryActionMessage.TYPE, SecondaryActionMessage.STREAM_CODEC),
      both(0x03, SetFireModeMessage.class, SetFireModeMessage.TYPE, SetFireModeMessage.STREAM_CODEC),
      both(0x04, TriggerPressedMessage.class, TriggerPressedMessage.TYPE, TriggerPressedMessage.STREAM_CODEC),
      both(0x05, NPCTriggerPressedMessage.class, NPCTriggerPressedMessage.TYPE, NPCTriggerPressedMessage.STREAM_CODEC),
      client(0x06, SyncGunContainerSlotMessage.class, SyncGunContainerSlotMessage.TYPE, SyncGunContainerSlotMessage.STREAM_CODEC),
      server(0x07, OpenStorageMessage.class, OpenStorageMessage.TYPE, OpenStorageMessage.STREAM_CODEC),
      both(0x08, PerformActionMessage.class, PerformActionMessage.TYPE, PerformActionMessage.STREAM_CODEC),
      both(0x09, CancelActionMessage.class, CancelActionMessage.TYPE, CancelActionMessage.STREAM_CODEC),
      server(0x0A, ValidatePendingHitMessage.class, ValidatePendingHitMessage.TYPE, ValidatePendingHitMessage.STREAM_CODEC),
      both(0x0B, CrouchMessage.class, CrouchMessage.TYPE, CrouchMessage.STREAM_CODEC),
      client(0x0C, HitMessage.class, HitMessage.TYPE, HitMessage.STREAM_CODEC),
      client(0x0D, SyncGunEquipmentSlotMessage.class, SyncGunEquipmentSlotMessage.TYPE, SyncGunEquipmentSlotMessage.STREAM_CODEC),
      server(0x0E, EnableCombatModeMessage.class, EnableCombatModeMessage.TYPE, EnableCombatModeMessage.STREAM_CODEC),
      server(0x0F, DamageHandcuffsMessage.class, DamageHandcuffsMessage.TYPE, DamageHandcuffsMessage.STREAM_CODEC),
      client(0x11, ParachuteSyncMessage.class, ParachuteSyncMessage.TYPE, ParachuteSyncMessage.STREAM_CODEC),
      server(0x12, OpenCraftingMenuMessage.class, OpenCraftingMenuMessage.TYPE, OpenCraftingMenuMessage.STREAM_CODEC),
      client(0x13, TraumaPacket.class, TraumaPacket.TYPE, TraumaPacket.STREAM_CODEC),
      client(0x14, SyncProtectionConfigMessage.class, SyncProtectionConfigMessage.TYPE, SyncProtectionConfigMessage.STREAM_CODEC),
      server(0x15, RightClickStateMessage.class, RightClickStateMessage.TYPE, RightClickStateMessage.STREAM_CODEC),
      client(0x16, BlockDestroyParticleMessage.class, BlockDestroyParticleMessage.TYPE, BlockDestroyParticleMessage.STREAM_CODEC),
      server(0x17, BlockDestroyActionMessage.class, BlockDestroyActionMessage.TYPE, BlockDestroyActionMessage.STREAM_CODEC),
      client(0x18, SyncHydrationMessage.class, SyncHydrationMessage.TYPE, SyncHydrationMessage.STREAM_CODEC),
      client(0x19, KillFeedMessage.class, KillFeedMessage.TYPE, KillFeedMessage.STREAM_CODEC),
      client(0x1A, SyncSurvivalStatsMessage.class, SyncSurvivalStatsMessage.TYPE,
          SyncSurvivalStatsMessage.STREAM_CODEC),
      server(0x1B, C2SOpenFriendMenuMessage.class, C2SOpenFriendMenuMessage.TYPE,
          C2SOpenFriendMenuMessage.STREAM_CODEC),
      server(0x1C, C2SFriendActionMessage.class, C2SFriendActionMessage.TYPE,
          C2SFriendActionMessage.STREAM_CODEC),
      server(0x1D, C2SFriendRequestActionMessage.class, C2SFriendRequestActionMessage.TYPE,
          C2SFriendRequestActionMessage.STREAM_CODEC),
      client(0x1E, S2CSyncFriendMenuMessage.class, S2CSyncFriendMenuMessage.TYPE,
          S2CSyncFriendMenuMessage.STREAM_CODEC),
      client(0x1F, S2CSyncFriendMessage.class, S2CSyncFriendMessage.TYPE,
          S2CSyncFriendMessage.STREAM_CODEC),
      server(0x20, C2SFriendTeamActionMessage.class, C2SFriendTeamActionMessage.TYPE,
          C2SFriendTeamActionMessage.STREAM_CODEC),
      client(0x21, SyncBloodMoonMessage.class, SyncBloodMoonMessage.TYPE,
          SyncBloodMoonMessage.STREAM_CODEC),
      server(0x22, TankInputMessage.class, TankInputMessage.TYPE,
          TankInputMessage.STREAM_CODEC));


  public static Optional<PayloadRegistration<?>> findByMessageClass(Class<?> messageClass) {
    return PLAY.stream().filter(registration -> registration.messageClass() == messageClass).findFirst();
  }

  public static Optional<PayloadRegistration<?>> findByPayloadType(CustomPacketPayload.Type<?> type) {
    return PLAY.stream().filter(registration -> registration.type().equals(type)).findFirst();
  }

  public static boolean isKnownPayload(CustomPacketPayload payload) {
    return findByPayloadType(payload.type()).isPresent();
  }

  /**
   * Lightweight startup validation for the staged 1.21.x network migration.
   *
   * <p>This catches duplicate vanilla payload ids or duplicate legacy discriminators before a
   * client/server connection attempts to decode a packet. It intentionally does not replace the
   * legacy SimpleChannel bridge yet; it only ensures the bridge and payload table stay in sync.</p>
   */
  public static void validatePlayTable() {
    Set<Integer> legacyIds = new HashSet<>();
    Set<Object> payloadIds = new HashSet<>();
    for (PayloadRegistration<?> registration : PLAY) {
      if (!legacyIds.add(registration.legacyDiscriminator())) {
        throw new IllegalStateException("Duplicate Crafting Dead legacy network discriminator: 0x"
            + Integer.toHexString(registration.legacyDiscriminator()));
      }
      if (!payloadIds.add(registration.type().id())) {
        throw new IllegalStateException("Duplicate Crafting Dead payload id: " + registration.type().id());
      }
    }
  }

  public static String describe(CustomPacketPayload payload) {
    return findByPayloadType(payload.type())
        .map(registration -> registration.messageClass().getSimpleName() + "[" + registration.direction()
            + ", legacy=0x" + Integer.toHexString(registration.legacyDiscriminator()) + "]")
        .orElse(payload.type().id().toString());
  }

  private NetworkPayloadRegistrations() {}

  private static <T extends CustomPacketPayload> PayloadRegistration<T> client(int legacyDiscriminator,
      Class<T> messageClass, CustomPacketPayload.Type<T> type,
      StreamCodec<FriendlyByteBuf, T> streamCodec) {
    return new PayloadRegistration<>(legacyDiscriminator, messageClass, type, streamCodec,
        Direction.PLAY_TO_CLIENT);
  }

  private static <T extends CustomPacketPayload> PayloadRegistration<T> server(int legacyDiscriminator,
      Class<T> messageClass, CustomPacketPayload.Type<T> type,
      StreamCodec<FriendlyByteBuf, T> streamCodec) {
    return new PayloadRegistration<>(legacyDiscriminator, messageClass, type, streamCodec,
        Direction.PLAY_TO_SERVER);
  }

  private static <T extends CustomPacketPayload> PayloadRegistration<T> both(int legacyDiscriminator,
      Class<T> messageClass, CustomPacketPayload.Type<T> type,
      StreamCodec<FriendlyByteBuf, T> streamCodec) {
    return new PayloadRegistration<>(legacyDiscriminator, messageClass, type, streamCodec,
        Direction.BIDIRECTIONAL);
  }
}

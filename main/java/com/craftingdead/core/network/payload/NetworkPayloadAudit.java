/*
 * Crafting Dead 1.21.1 port helper.
 */
package com.craftingdead.core.network.payload;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Small validation facade used while the network layer is moving from Forge SimpleChannel to
 * vanilla/Forge CustomPacketPayload registration.
 */
public final class NetworkPayloadAudit {

  private static boolean validated;

  private NetworkPayloadAudit() {}

  public static void validateOnce() {
    if (!validated) {
      NetworkPayloadRegistrations.validatePlayTable();
      validated = true;
    }
  }

  public static void validateOutgoing(Object message) {
    if (message instanceof CustomPacketPayload payload
        && !NetworkPayloadRegistrations.isKnownPayload(payload)) {
      throw new IllegalArgumentException("Unregistered Crafting Dead payload: " + payload.type().id());
    }
  }
}

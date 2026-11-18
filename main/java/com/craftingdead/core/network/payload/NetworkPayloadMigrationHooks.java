/*
 * Crafting Dead 1.21.1 port helper.
 */
package com.craftingdead.core.network.payload;

/**
 * Notes and single entry point for the final 1.21.x payload registration pass.
 *
 * <p>Forge 1.21.x payload registration is no longer the same as the old SimpleChannel
 * discriminator table. The migration has already prepared every play message with a
 * {@code CustomPacketPayload.Type} and {@code STREAM_CODEC}. Once the project is compiled against
 * the exact Forge 52.x networking classes, this class should be wired to the mod event bus and the
 * legacy {@code NetworkChannel#registerMessages} table can be removed.</p>
 *
 * <p>This class intentionally avoids importing the final Forge payload event while the source is
 * still being statically migrated without an IDE/compiler. That keeps this step from locking the
 * code to a wrong minor Forge API name.</p>
 */
public final class NetworkPayloadMigrationHooks {

  private NetworkPayloadMigrationHooks() {}

  public static int preparedPlayPayloadCount() {
    return NetworkPayloadRegistrations.PLAY.size();
  }
}

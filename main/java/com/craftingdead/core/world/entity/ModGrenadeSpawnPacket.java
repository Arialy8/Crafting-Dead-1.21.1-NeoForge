/*
 * Crafting Dead 1.21.1 migration helper.
 */
package com.craftingdead.core.world.entity;

import com.craftingdead.core.world.entity.grenade.Grenade;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;

/**
 * Centralizes custom entity spawn packet creation for Forge/Minecraft 1.21.x.
 *
 * <p>Crafting Dead 1.18.x used {@code NetworkHooks.getEntitySpawningPacket(this)} plus
 * {@code IEntityAdditionalSpawnData}. Minecraft 1.21.x changed the vanilla spawn packet signature to
 * {@code Entity#getAddEntityPacket(ServerEntity)} and {@link ClientboundAddEntityPacket} constructors now
 * receive the {@link ServerEntity} tracker entry. Keeping the creation here makes it easier to add a
 * dedicated custom-payload sync for grenade-only spawn data later if needed.
 */
public final class ModGrenadeSpawnPacket {

  private ModGrenadeSpawnPacket() {}

  public static Packet<ClientGamePacketListener> create(Grenade grenade, ServerEntity serverEntity) {
    return new ClientboundAddEntityPacket(grenade, serverEntity);
  }
}

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

package com.craftingdead.core.world.entity.extension;

import com.craftingdead.core.network.NetworkBufferUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public record EntitySnapshot(Vec3 position, AABB boundingBox, Vec2 rotation,
    Vec3 velocity, boolean crouching, float eyeHeight, boolean complete) {

  public EntitySnapshot(Vec3 position, AABB boundingBox, Vec2 rotation, Vec3 velocity,
      boolean crouching) {
    this(position, boundingBox, rotation, velocity, crouching, -1, false);
  }

  public EntitySnapshot combineUntrustedSnapshot(EntitySnapshot snapshot) {
    if (!this.complete) {
      throw new UnsupportedOperationException("Snapshot not complete");
    }

    var position = snapshot.position;
    if (this.position.distanceTo(snapshot.position) > 1.0D) {
      position = this.position;
    }

    var boundingBox = snapshot.boundingBox;
    if (Math.abs(this.boundingBox.getSize()
        - snapshot.boundingBox.getSize()) > 1.0E-10D) {
      boundingBox = this.boundingBox;
    }

    var rotation = snapshot.rotation;
    if (Mth.degreesDifferenceAbs(this.rotation.x, snapshot.rotation.x) > 10.0D ||
        Mth.degreesDifferenceAbs(this.rotation.y, snapshot.rotation.y) > 10.0D) {
      rotation = this.rotation;
    }

    var deltaMovement = snapshot.velocity;
    if (this.velocity.distanceTo(snapshot.velocity) > 0.1D) {
      deltaMovement = this.velocity;
    }

    return new EntitySnapshot(position, boundingBox, rotation, deltaMovement, snapshot.crouching,
        this.eyeHeight, true);
  }

  public void encode(FriendlyByteBuf out) {
    var safePosition = this.position == null ? Vec3.ZERO : this.position;
    var safeBox = this.boundingBox == null
        ? new AABB(safePosition.x - 0.3D, safePosition.y, safePosition.z - 0.3D,
            safePosition.x + 0.3D, safePosition.y + 1.8D, safePosition.z + 0.3D)
        : this.boundingBox;
    var safeRotation = this.rotation == null ? Vec2.ZERO : this.rotation;
    var safeVelocity = this.velocity == null ? Vec3.ZERO : this.velocity;
    NetworkBufferUtil.writeDouble(out, safePosition.x(), 0.0D);
    NetworkBufferUtil.writeDouble(out, safePosition.y(), 0.0D);
    NetworkBufferUtil.writeDouble(out, safePosition.z(), 0.0D);
    NetworkBufferUtil.writeDouble(out, safeBox.minX, safePosition.x - 0.3D);
    NetworkBufferUtil.writeDouble(out, safeBox.minY, safePosition.y);
    NetworkBufferUtil.writeDouble(out, safeBox.minZ, safePosition.z - 0.3D);
    NetworkBufferUtil.writeDouble(out, safeBox.maxX, safePosition.x + 0.3D);
    NetworkBufferUtil.writeDouble(out, safeBox.maxY, safePosition.y + 1.8D);
    NetworkBufferUtil.writeDouble(out, safeBox.maxZ, safePosition.z + 0.3D);
    NetworkBufferUtil.writeFloat(out, safeRotation.x, -360.0F, 360.0F);
    NetworkBufferUtil.writeFloat(out, safeRotation.y, -360.0F, 360.0F);
    NetworkBufferUtil.writeDouble(out, safeVelocity.x, 0.0D);
    NetworkBufferUtil.writeDouble(out, safeVelocity.y, 0.0D);
    NetworkBufferUtil.writeDouble(out, safeVelocity.z, 0.0D);
    NetworkBufferUtil.writeBoolean(out, this.crouching);
  }

  public static EntitySnapshot decode(FriendlyByteBuf in) {
    var position = new Vec3(NetworkBufferUtil.readDouble(in, 0.0D),
        NetworkBufferUtil.readDouble(in, 0.0D),
        NetworkBufferUtil.readDouble(in, 0.0D));
    var minX = NetworkBufferUtil.readDouble(in, position.x - 0.3D);
    var minY = NetworkBufferUtil.readDouble(in, position.y);
    var minZ = NetworkBufferUtil.readDouble(in, position.z - 0.3D);
    var maxX = NetworkBufferUtil.readDouble(in, position.x + 0.3D);
    var maxY = NetworkBufferUtil.readDouble(in, position.y + 1.8D);
    var maxZ = NetworkBufferUtil.readDouble(in, position.z + 0.3D);
    if (maxX < minX) {
      var tmp = maxX;
      maxX = minX;
      minX = tmp;
    }
    if (maxY < minY) {
      var tmp = maxY;
      maxY = minY;
      minY = tmp;
    }
    if (maxZ < minZ) {
      var tmp = maxZ;
      maxZ = minZ;
      minZ = tmp;
    }
    var rotation = new Vec2(NetworkBufferUtil.readFloat(in, 0.0F),
        NetworkBufferUtil.readFloat(in, 0.0F));
    var velocity = new Vec3(NetworkBufferUtil.readDouble(in, 0.0D),
        NetworkBufferUtil.readDouble(in, 0.0D),
        NetworkBufferUtil.readDouble(in, 0.0D));
    return new EntitySnapshot(position, new AABB(minX, minY, minZ, maxX, maxY, maxZ),
        rotation, velocity, NetworkBufferUtil.readBoolean(in, false));
  }
}

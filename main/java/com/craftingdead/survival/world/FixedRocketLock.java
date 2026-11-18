package com.craftingdead.survival.world;

import net.minecraft.world.phys.Vec3;

/** Immutable snapshot of the aim point; never reads the target again after warning. */
public final class FixedRocketLock {
  private Vec3 point;
  private long fireAt;
  public void arm(Vec3 targetPosition, long tick) {
    if (point != null) return;
    point = targetPosition;
    fireAt = tick + 60;
  }
  public Vec3 point() { return point; }
  public boolean ready(long tick) { return point != null && tick >= fireAt; }
  public void clear() { point = null; }
}

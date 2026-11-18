package com.craftingdead.core.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes vanilla's collision-aware camera spacing to the tank camera handler. */
@Mixin(Camera.class)
public interface CameraInvoker {

  @Invoker("setPosition")
  void craftingdead$setPosition(Vec3 position);
}

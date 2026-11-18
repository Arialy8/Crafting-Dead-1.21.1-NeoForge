package com.craftingdead.core.mixin;

import com.craftingdead.core.world.vehicle.T72B3Entity;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the tank camera authoritative after shoulder-camera mods adjust vanilla setup. */
@Mixin(value = Camera.class, priority = 500)
public abstract class TankCameraMixin {

  @Shadow
  protected abstract void setPosition(Vec3 position);

  @Shadow
  protected abstract void setRotation(float yaw, float pitch);

  @Inject(method = "setup", at = @At("RETURN"))
  private void craftingdead$applyTankCamera(BlockGetter level, Entity cameraEntity,
      boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo callbackInfo) {
    if (!detached || !(cameraEntity.getVehicle() instanceof T72B3Entity tank)
        || tank.getControllingPassenger() != cameraEntity) {
      return;
    }

    float yaw = cameraEntity.getViewYRot(partialTick);
    float pitch = Math.max(16.0F, cameraEntity.getViewXRot(partialTick));
    if (thirdPersonReverse) {
      yaw = Mth.wrapDegrees(yaw + 180.0F);
      pitch = -pitch;
    }
    Vec3 view = Vec3.directionFromRotation(pitch, yaw);
    // Anchor to the same partial-tick transform used by the entity renderer. Using position()
    // sampled only the 20 Hz simulation state and made an otherwise smooth tank camera judder.
    Vec3 anchor = tank.getPosition(partialTick).add(0.0D, 2.35D, 0.0D);
    this.setRotation(yaw, pitch);
    this.setPosition(anchor.subtract(view.scale(19.0D)));
  }
}

package com.craftingdead.core.mixin;

import com.craftingdead.core.client.ClientDist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

  @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
  private static void suppressLocalPlayerHurtTint(LivingEntity entity,
      float whiteOverlayProgress, CallbackInfoReturnable<Integer> callbackInfo) {
    if (!ClientDist.isDamageFlashEnabled()
        && entity == Minecraft.getInstance().player) {
      callbackInfo.setReturnValue(OverlayTexture.pack(
          OverlayTexture.u(whiteOverlayProgress), OverlayTexture.v(false)));
    }
  }
}

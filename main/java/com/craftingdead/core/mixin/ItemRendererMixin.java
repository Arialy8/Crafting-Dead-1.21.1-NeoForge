package com.craftingdead.core.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import com.craftingdead.core.world.item.GunItem;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Forces gun items to use the custom BEWLR renderer for in-game display
 * contexts (first/third person, item frames, etc.) while keeping the
 * flat {@code item/generated} icon in GUI and NONE contexts.
 *
 * <p>Works by: (1) capturing the render context at method entry via
 * {@link ModifyVariable} (type-based matching, immune to signature changes),
 * then (2) redirecting the {@link BakedModel#isCustomRenderer()} call to
 * return {@code true} for gun items in non-GUI contexts.</p>
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

  @Unique
  private static final ThreadLocal<ItemDisplayContext> CURRENT_CONTEXT =
      ThreadLocal.withInitial(() -> null);

  @Unique
  private static final ThreadLocal<Integer> RECURSION_DEPTH =
      ThreadLocal.withInitial(() -> 0);

  @Unique
  private static boolean craftingdead$shouldRenderGunModel(ItemDisplayContext context) {
    return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
        || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
        || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
        || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
  }

  /**
   * Capture render context at method entry.
   */
  @Inject(
      method = "render",
      at = @At("HEAD"),
      cancellable = true)
  private void craftingdead$captureContext(ItemStack stack, ItemDisplayContext context,
      boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
      int packedOverlay, BakedModel model, CallbackInfo ci) {
    if (stack.getItem() instanceof GunItem
        && craftingdead$shouldRenderGunModel(context)) {
      IClientItemExtensions.of(stack).getCustomRenderer().renderByItem(stack, context, poseStack,
          bufferSource, packedLight, packedOverlay);
      ci.cancel();
      return;
    }

    int depth = RECURSION_DEPTH.get();
    if (depth == 0) {
      CURRENT_CONTEXT.set(
          stack.getItem() instanceof GunItem ? context : null);
    }
    RECURSION_DEPTH.set(depth + 1);
  }

  /**
   * Force custom renderer for gun items in non-GUI contexts by redirecting
   * the {@code isCustomRenderer()} call within {@code render()}.
   *
   * <p>The redirect only applies to calls within {@code render()}, so
   * calling {@code model.isCustomRenderer()} from this handler calls the
   * original method directly (no recursion).</p>
   */
  @Redirect(
      method = "render",
      at = @At(value = "INVOKE",
          target = "Lnet/minecraft/client/resources/model/BakedModel;isCustomRenderer()Z"))
  private boolean craftingdead$redirectCustomRenderer(BakedModel model) {
    ItemDisplayContext ctx = CURRENT_CONTEXT.get();
    if (ctx != null
        && craftingdead$shouldRenderGunModel(ctx)) {
      return true;
    }
    return model.isCustomRenderer();
  }

  /**
   * Clear render context after method returns.
   * Minimal handler params — matches any {@code render()} signature.
   */
  @Inject(
      method = "render",
      at = @At("RETURN"))
  private void craftingdead$clearContext(ItemStack stack, ItemDisplayContext context,
      boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
      int packedOverlay, BakedModel model, CallbackInfo ci) {
    int depth = RECURSION_DEPTH.get() - 1;
    RECURSION_DEPTH.set(depth);
    if (depth <= 0) {
      CURRENT_CONTEXT.set(null);
    }
  }
}

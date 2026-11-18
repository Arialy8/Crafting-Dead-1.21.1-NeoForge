package com.craftingdead.core.client.renderer.entity;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.client.util.RenderUtil;
import com.craftingdead.core.world.entity.projectile.RpgRocket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Renders the loaded RPG rocket model in flight, aligned to its velocity. */
public class RpgRocketRenderer extends EntityRenderer<RpgRocket> {

  private static final ResourceLocation MODEL =
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "magazine/rocket");

  private final ItemRenderer itemRenderer;

  public RpgRocketRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.itemRenderer = context.getItemRenderer();
    this.shadowRadius = 0.18F;
  }

  @Override
  public void render(RpgRocket rocket, float entityYaw, float partialTick,
      PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
    var movement = rocket.getDeltaMovement();
    if (movement.lengthSqr() < 1.0E-7D) {
      super.render(rocket, entityYaw, partialTick, poseStack, bufferSource, packedLight);
      return;
    }

    var horizontalSpeed = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
    var yaw = (float) (Mth.atan2(movement.x, movement.z) * Mth.RAD_TO_DEG);
    var pitch = (float) (Mth.atan2(movement.y, horizontalSpeed) * Mth.RAD_TO_DEG);

    poseStack.pushPose();
    // The model's nose points toward -Z, so rotate that axis onto the velocity vector.
    poseStack.mulPose(Axis.YP.rotationDegrees(yaw + 180.0F));
    poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    // Centre the existing loaded-rocket geometry around the projectile origin.
    poseStack.translate(-0.5D, -0.625D, 0.565D);

    var stack = rocket.getItem();
    var model = this.itemRenderer.getItemModelShaper().getModelManager()
        .getModel(new ModelResourceLocation(MODEL, "standalone"));
    var renderType = ItemBlockRenderTypes.getRenderType(stack, true);
    var vertexConsumer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, true, false);
    RenderUtil.renderModelLists(model, stack, poseStack, vertexConsumer,
        packedLight, OverlayTexture.NO_OVERLAY, 1.0F);
    poseStack.popPose();

    super.render(rocket, entityYaw, partialTick, poseStack, bufferSource, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(RpgRocket rocket) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}

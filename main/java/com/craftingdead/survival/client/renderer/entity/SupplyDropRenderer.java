package com.craftingdead.survival.client.renderer.entity;

import com.craftingdead.core.client.model.geom.ModModelLayers;
import com.craftingdead.survival.CraftingDeadSurvival;
import com.craftingdead.survival.client.model.SupplyDropModel;
import com.craftingdead.survival.world.entity.SupplyDrop;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class SupplyDropRenderer extends EntityRenderer<SupplyDrop> {

  private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      CraftingDeadSurvival.ID, "textures/entity/supply_drop.png");
  private final SupplyDropModel model;

  public SupplyDropRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.model = new SupplyDropModel(context.bakeLayer(ModModelLayers.SUPPLY_DROP));
    this.shadowRadius = 1.0F;
  }

  @Override
  public void render(SupplyDrop entity, float yaw, float partialTick, PoseStack poseStack,
      MultiBufferSource buffers, int packedLight) {
    poseStack.pushPose();
    poseStack.translate(0.0D, 1.51D, 0.0D);
    poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
    this.model.setParachuteVisible(!entity.hasLanded());
    var consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
    this.model.root().render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
    poseStack.popPose();
    super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(SupplyDrop entity) {
    return TEXTURE;
  }
}

package com.craftingdead.immerse.client.renderer;

import com.craftingdead.immerse.world.level.block.entity.BaseCenterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

/** Displays the owning player's head on top of the base center. */
public class BaseCenterRenderer implements BlockEntityRenderer<BaseCenterBlockEntity> {

  private final SkullModelBase headModel;

  public BaseCenterRenderer(BlockEntityRendererProvider.Context context) {
    this.headModel = new SkullModel(context.bakeLayer(ModelLayers.PLAYER_HEAD));
  }

  @Override
  public void render(BaseCenterBlockEntity baseCenter, float partialTick, PoseStack poseStack,
      MultiBufferSource buffers, int packedLight, int packedOverlay) {
    if (baseCenter.getOwner() == null) {
      return;
    }
    Minecraft minecraft = Minecraft.getInstance();
    PlayerInfo ownerInfo = minecraft.getConnection() == null ? null
        : minecraft.getConnection().getPlayerInfo(baseCenter.getOwner());
    ResourceLocation skin = ownerInfo == null
        ? (baseCenter.getOwnerProfile() == null
            ? DefaultPlayerSkin.get(baseCenter.getOwner()).texture()
            : minecraft.getSkinManager().getInsecureSkin(baseCenter.getOwnerProfile().gameProfile()).texture())
        : ownerInfo.getSkin().texture();
    RenderType renderType = RenderType.entityTranslucent(skin);
    poseStack.pushPose();
    // SkullBlockRenderer centers its model inside the scaled local block space.
    poseStack.translate(0.25D, 1.01D, 0.25D);
    poseStack.scale(0.50F, 0.50F, 0.50F);
    SkullBlockRenderer.renderSkull(null, 180.0F, 0.0F, poseStack, buffers, packedLight,
        this.headModel, renderType);
    poseStack.popPose();
  }
}

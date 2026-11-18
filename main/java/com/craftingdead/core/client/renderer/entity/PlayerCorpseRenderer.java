package com.craftingdead.core.client.renderer.entity;

import com.craftingdead.core.world.entity.PlayerCorpse;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class PlayerCorpseRenderer extends EntityRenderer<PlayerCorpse> {

  private static final ResourceLocation DEFAULT_TEXTURE =
      ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

  private final PlayerModel<LivingEntity> wideModel;
  private final PlayerModel<LivingEntity> slimModel;
  private final Map<UUID, ResourceLocation> cachedSkins = new HashMap<>();
  private final Map<UUID, Boolean> cachedSlim = new HashMap<>();

  public PlayerCorpseRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.wideModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
    this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    this.shadowRadius = 0.7F;
  }

  @Override
  public void render(PlayerCorpse corpse, float yaw, float partialTick, PoseStack poseStack,
      MultiBufferSource buffers, int packedLight) {
    UUID owner = corpse.getOwnerUuid();
    this.updateSkin(owner);
    PlayerModel<LivingEntity> model = this.cachedSlim.getOrDefault(owner, false)
        ? this.slimModel : this.wideModel;
    this.configureParts(model, corpse.getRemainingParts());

    poseStack.pushPose();
    poseStack.translate(0.0D, 0.12D, 0.0D);
    float scale = corpse.isFullSize() ? 1.50F : 1.12F;
    poseStack.scale(scale, scale, scale);
    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - corpse.getYRot()));
    poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
    poseStack.translate(0.0D, -1.5D, 0.0D);
    ResourceLocation texture = this.cachedSkins.getOrDefault(owner, DEFAULT_TEXTURE);
    model.renderToBuffer(poseStack,
        buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),
        packedLight, OverlayTexture.NO_OVERLAY);
    poseStack.popPose();
    super.render(corpse, yaw, partialTick, poseStack, buffers, packedLight);
  }

  private void updateSkin(UUID owner) {
    if (this.cachedSkins.containsKey(owner)) {
      return;
    }
    var connection = Minecraft.getInstance().getConnection();
    PlayerInfo info = connection == null ? null : connection.getPlayerInfo(owner);
    if (info != null) {
      this.cachedSkins.put(owner, info.getSkin().texture());
      this.cachedSlim.put(owner, "slim".equals(info.getSkin().model().id()));
    }
  }

  private void configureParts(PlayerModel<LivingEntity> model, int parts) {
    model.setAllVisible(true);
    model.leftLeg.visible = parts >= 5;
    model.leftPants.visible = parts >= 5;
    model.rightLeg.visible = parts >= 4;
    model.rightPants.visible = parts >= 4;
    model.leftArm.visible = parts >= 3;
    model.leftSleeve.visible = parts >= 3;
    model.rightArm.visible = parts >= 2;
    model.rightSleeve.visible = parts >= 2;
    model.body.visible = parts >= 2;
    model.jacket.visible = parts >= 2;
    model.head.visible = parts >= 1;
    model.hat.visible = parts >= 1;
  }

  @Override
  public ResourceLocation getTextureLocation(PlayerCorpse corpse) {
    return this.cachedSkins.getOrDefault(corpse.getOwnerUuid(), DEFAULT_TEXTURE);
  }
}

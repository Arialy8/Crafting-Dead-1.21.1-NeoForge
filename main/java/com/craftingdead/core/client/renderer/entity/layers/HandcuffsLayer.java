package com.craftingdead.core.client.renderer.entity.layers;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.client.model.geom.ModModelLayers;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

public class HandcuffsLayer<T extends Player, M extends EntityModel<T> & ArmedModel>
    extends RenderLayer<T, M> {

  private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      CraftingDead.ID, "textures/entity/handcuffs.png");

  private final ModelPart left;
  private final ModelPart right;
  private final ModelPart middle;

  public HandcuffsLayer(RenderLayerParent<T, M> parent, EntityModelSet entityModels) {
    super(parent);
    ModelPart root = entityModels.bakeLayer(ModModelLayers.HANDCUFFS);
    this.left = root.getChild("left");
    this.right = root.getChild("right");
    this.middle = root.getChild("middle");
  }

  @Override
  public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
      T entity, float limbSwing, float limbSwingAmount, float partialTick,
      float ageTicks, float headYaw, float headPitch) {
    PlayerExtension<?> player = PlayerExtension.get(entity);
    if (player == null || !player.isHandcuffed()) {
      return;
    }

    var consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
    poseStack.pushPose();
    poseStack.scale(1.4F, 1.4F, 1.4F);

    poseStack.pushPose();
    this.getParentModel().translateToHand(HumanoidArm.RIGHT, poseStack);
    poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
    poseStack.translate(-0.32D, -0.06D, -0.4D);
    this.right.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
    poseStack.popPose();

    poseStack.pushPose();
    this.getParentModel().translateToHand(HumanoidArm.LEFT, poseStack);
    poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
    poseStack.translate(0.07D, -0.06D, -0.4D);
    this.left.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
    poseStack.popPose();

    poseStack.pushPose();
    poseStack.translate(-0.22D, 0.34D, 0.14D);
    poseStack.scale(1.3F, 1.0F, 1.0F);
    this.middle.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
    poseStack.popPose();
    poseStack.popPose();
  }

  public static LayerDefinition createHandcuffsBodyLayer() {
    MeshDefinition mesh = new MeshDefinition();
    var root = mesh.getRoot();
    root.addOrReplaceChild("middle",
        CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, 4, 1, 1),
        PartPose.offset(0, 0.5F, 0));
    root.addOrReplaceChild("left",
        CubeListBuilder.create().texOffs(0, 0)
            .addBox(-1, -24, 0, 1, 2, 1)
            .addBox(-3, -22, 0, 2, 1, 1)
            .addBox(-3, -25, 0, 2, 1, 1)
            .addBox(-4, -24, 0, 1, 2, 1),
        PartPose.offset(0, 24, 0));
    root.addOrReplaceChild("right",
        CubeListBuilder.create().texOffs(0, 0)
            .addBox(5, -22, 0, 2, 1, 1)
            .addBox(7, -24, 0, 1, 2, 1)
            .addBox(5, -25, 0, 2, 1, 1)
            .addBox(4, -24, 0, 1, 2, 1),
        PartPose.offset(0, 24, 0));
    return LayerDefinition.create(mesh, 64, 32);
  }
}

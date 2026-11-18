/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 *
 * This Non-Commercial Software License Agreement (the "Agreement") is made between
 * you (the "Licensee") and NEXUSNODE (BRAD HUNTER). (the "Licensor").
 * By installing or otherwise using Crafting Dead (the "Software"), you agree to be
 * bound by the terms and conditions of this Agreement as may be revised from time
 * to time at Licensor's sole discretion.
 *
 * If you do not agree to the terms and conditions of this Agreement do not download,
 * copy, reproduce or otherwise use any of the source code available online at any time.
 *
 * https://github.com/nexusnode/crafting-dead/blob/1.18.x/LICENSE.txt
 *
 * https://craftingdead.net/terms.php
 */

package com.craftingdead.core.client.renderer.entity.layers;

import java.util.Objects;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.client.util.RenderUtil;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.item.ModItems;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;

/**
 * Layer that renders {@link IEquipableModel}s attached to a player's body.
 */
public class EquipmentLayer<T extends LivingEntity, M extends EntityModel<T> & HeadedModel>
    extends RenderLayer<T, M> {

  private final Equipment.Slot slot;

  /**
   * Item display context used for rendering the equipment model.
   * 1.21.x applies different baked transforms than 1.18.x, so body/head equipment
   * uses a dedicated raw-model context plus player-model attachment transforms.
   */
  private final ItemDisplayContext displayContext;

  /**
   * Whether this model should be rotated when the player is crouching.
   */
  private final boolean useCrouchOrientation;

  private final Attachment attachment;

  /**
   * Optional arbitrary transformation right before rendering the {@link ItemStack}.
   */
  @Nullable
  private final Consumer<PoseStack> transformation;

  private EquipmentLayer(Builder<T, M> builder) {
    super(builder.renderer);
    this.slot = builder.slot;
    this.displayContext = builder.displayContext;
    this.useCrouchOrientation = builder.useCrouchOrientation;
    this.attachment = builder.attachment;
    this.transformation = builder.tranformation;
  }

  @Override
  public void render(PoseStack poseStack, MultiBufferSource bufferSource,
      int packedLight, T livingEntity, float limbSwing, float limbSwingAmount,
      float partialTicks, float ageTicks, float headYaw, float headPitch) {
    if (poseStack == null || bufferSource == null || livingEntity == null || this.slot == null) {
      return;
    }
    final var minecraft = Minecraft.getInstance();
    if (minecraft == null || minecraft.getItemRenderer() == null) {
      return;
    }

    final var invisible = livingEntity.isInvisible();
    final var partiallyVisible =
        livingEntity.isInvisible() && !livingEntity.isInvisibleTo(minecraft.player);
    if (!partiallyVisible && invisible) {
      return;
    }

    final var itemRenderer = minecraft.getItemRenderer();
    var living = LivingExtension.get(livingEntity);
    if (living != null) {
      final var itemStack = living.getItemInSlot(this.slot);
      if (itemStack == null || itemStack.isEmpty()) {
        return;
      }
      if (this.slot == Equipment.Slot.GUN && itemStack.is(ModItems.RPG.get())) {
        return;
      }

      var bakedModel = itemRenderer.getModel(itemStack, livingEntity.level(), livingEntity, 0);
      if (bakedModel == null) {
        return;
      }

      poseStack.pushPose();
      try {
        // Applies crouching rotation is needed
        if (this.useCrouchOrientation && livingEntity.isCrouching()) {
          RenderUtil.applyPlayerCrouchRotation(poseStack);
        }

        this.attachment.apply(this.getParentModel(), livingEntity, itemStack, poseStack);

        // Applies the arbitrary transformation if needed
        if (this.transformation != null) {
          this.transformation.accept(poseStack);
        }

        if (this.attachment == Attachment.HEAD || this.attachment == Attachment.BODY) {
          if (!WornObjRenderer.render(itemStack, this.attachment, poseStack,
              bufferSource, packedLight)) {
            this.renderLegacyEquipmentModel(itemRenderer, itemStack, bakedModel, poseStack,
                bufferSource, packedLight);
          }
        } else if (this.slot == Equipment.Slot.GUN) {
          if (!this.renderBackGunModel(itemStack, living, poseStack, bufferSource, packedLight)) {
            itemRenderer.render(itemStack, this.displayContext, false,
                poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, bakedModel);
          }
        } else {
          itemRenderer.render(itemStack, this.displayContext, false,
              poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, bakedModel);
        }
      } catch (RuntimeException ignored) {
        // Skip invalid equipment models during resource reload/render races.
      } finally {
        poseStack.popPose();
      }
    }
  }

  /**
   * Renders the selected OBJ model without ItemRenderer's vanilla -0.5 item-space translation.
   * Legacy worn models already define their own origin and are positioned by the player bone.
   */
  private void renderLegacyEquipmentModel(net.minecraft.client.renderer.entity.ItemRenderer itemRenderer,
      ItemStack itemStack, BakedModel bakedModel, PoseStack poseStack,
      MultiBufferSource bufferSource, int packedLight) {
    BakedModel resolvedModel = ClientHooks.handleCameraTransforms(
        poseStack, bakedModel, this.displayContext, false);
    for (BakedModel pass : resolvedModel.getRenderPasses(itemStack, true)) {
      for (var renderType : pass.getRenderTypes(itemStack, true)) {
        VertexConsumer consumer = net.minecraft.client.renderer.entity.ItemRenderer
            .getFoilBufferDirect(bufferSource, renderType, true, itemStack.hasFoil());
        itemRenderer.renderModelLists(pass, itemStack, packedLight, OverlayTexture.NO_OVERLAY,
            poseStack, consumer);
      }
    }
  }

  /** Uses the gun renderer's real model, magazine and attachments for the back slot. */
  private boolean renderBackGunModel(ItemStack itemStack, LivingExtension<?, ?> living,
      PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
    return CraftingDead.getInstance().getClientDist().getItemRendererManager().renderItem(
        itemStack, ItemDisplayContext.HEAD, living, poseStack, bufferSource,
        packedLight, OverlayTexture.NO_OVERLAY);
  }

  public enum Attachment {
    NONE(ItemDisplayContext.HEAD) {
      @Override
      <T extends LivingEntity, M extends EntityModel<T> & HeadedModel> void apply(
          M parentModel, T livingEntity, ItemStack itemStack, PoseStack poseStack) {}
    },
    HEAD(ItemDisplayContext.NONE) {
      @Override
      <T extends LivingEntity, M extends EntityModel<T> & HeadedModel> void apply(
          M parentModel, T livingEntity, ItemStack itemStack, PoseStack poseStack) {
        if (parentModel == null || parentModel.getHead() == null) {
          return;
        }

        if (livingEntity.isBaby()) {
          poseStack.translate(0.0D, 0.03125D, 0.0D);
          poseStack.scale(0.7F, 0.7F, 0.7F);
          poseStack.translate(0.0D, 1.0D, 0.0D);
        }

        parentModel.getHead().translateAndRotate(poseStack);
      }
    },
    BODY(ItemDisplayContext.NONE) {
      @Override
      <T extends LivingEntity, M extends EntityModel<T> & HeadedModel> void apply(
          M parentModel, T livingEntity, ItemStack itemStack, PoseStack poseStack) {
        if (parentModel instanceof HumanoidModel<?> model) {
          model.body.translateAndRotate(poseStack);
        }
      }
    };

    private final ItemDisplayContext displayContext;

    Attachment(ItemDisplayContext displayContext) {
      this.displayContext = displayContext;
    }

    abstract <T extends LivingEntity, M extends EntityModel<T> & HeadedModel> void apply(
        M parentModel, T livingEntity, ItemStack itemStack, PoseStack poseStack);
  }

  private record HatRenderProfile(
      float headOriginY,
      float translationX, float translationY, float translationZ,
      float rotationX, float rotationY, float rotationZ,
      float scaleX, float scaleY, float scaleZ) {

    private static final HatRenderProfile STANDARD_HELMET =
        new HatRenderProfile(1.30F,
            0.0F, -0.08F, 0.0F,
            0.0F, 0.0F, 0.0F,
            1.0F, 1.0F, 1.0F);
    private static final HatRenderProfile HARD_HAT =
        new HatRenderProfile(1.29F,
            0.03F, -0.08F, 0.0F,
            0.0F, 0.0F, 0.0F,
            0.86F, 0.86F, 0.70F);
    private static final HatRenderProfile BULKY_HELMET =
        new HatRenderProfile(1.02F,
            0.0F, -0.27F, -0.02F,
            0.0F, 0.0F, 0.0F,
            0.60F, 0.60F, 0.58F);
    private static final HatRenderProfile MILITARY_HELMET =
        new HatRenderProfile(0.95F,
            0.26F, -0.02F, 0.26F,
            0.0F, 0.0F, 0.0F,
            0.43F, 0.43F, 0.43F);
    private static final HatRenderProfile MASK =
        new HatRenderProfile(1.30F,
            0.0F, -0.02F, -0.32F,
            0.0F, 0.0F, 0.0F,
            0.92F, 0.92F, 0.92F);
    private static final HatRenderProfile GAS_MASK =
        new HatRenderProfile(1.28F,
            0.17F, -0.02F, -0.30F,
            0.0F, 0.0F, 0.0F,
            0.78F, 0.78F, 0.78F);
    private static final HatRenderProfile NIGHT_VISION =
        new HatRenderProfile(1.30F,
            0.0F, -0.21F, -0.21F,
            0.0F, 0.0F, 0.0F,
            0.95F, 0.95F, 0.90F);
    private static final HatRenderProfile LARGE_HAT =
        new HatRenderProfile(1.30F,
            0.02F, -0.06F, 0.0F,
            0.0F, 0.0F, 0.0F,
            0.82F, 0.82F, 0.82F);
    private static final HatRenderProfile EARS =
        new HatRenderProfile(1.42F,
            0.0F, -0.06F, -0.02F,
            0.0F, 0.0F, 0.0F,
            0.92F, 0.86F, 0.92F);
    private static final HatRenderProfile KNIGHT_HELMET =
        new HatRenderProfile(1.30F,
            0.0F, -0.06F, 0.0F,
            0.0F, 180.0F, 0.0F,
            0.82F, 0.82F, 0.82F);
    private static final HatRenderProfile SCUBA_MASK =
        new HatRenderProfile(1.26F,
            0.12F, -0.02F, -0.30F,
            0.0F, 90.0F, 0.0F,
            0.78F, 0.78F, 0.78F);

    static HatRenderProfile forStack(ItemStack itemStack) {
      ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
      if (itemId == null || !CraftingDead.ID.equals(itemId.getNamespace())) {
        return STANDARD_HELMET;
      }

      return switch (itemId.getPath()) {
        case "bunny_hat" -> EARS;
        case "nv_goggles_hat" -> NIGHT_VISION;
        case "scuba_mask" -> SCUBA_MASK;
        case "gas_mask" -> GAS_MASK;
        case "knight_hat" -> KNIGHT_HELMET;
        case "blue_hard_hat", "green_hard_hat", "orange_hard_hat", "yellow_hard_hat" -> HARD_HAT;
        case "cow_mask", "creeper_mask", "payday2_mask", "payday_mask", "pumpkin_mask",
            "sheep_mask", "zombie_mask", "cookie_mask", "deadpool_mask", "doctor_mask",
            "hacker_mask", "ninja_hat", "ski_mask" -> MASK;
        case "chief_fireman_hat", "fireman_hat", "santa_hat", "top_hat", "trapper_hat",
            "ushanka_hat", "beanie_hat", "radar_cap" -> LARGE_HAT;
        case "army_helmet", "camo_helmet", "combat_bdu_helmet", "ghillie_hat",
            "green_army_helmet", "grey_army_helmet", "hazmat_hat", "juggernaut_helmet",
            "military_hazmat_hat", "pilot_helmet", "spetsnaz_helmet",
            "winter_military_helmet" -> MILITARY_HELMET;
        case "black_ballistic_hat", "green_ballistic_helmet" -> BULKY_HELMET;
        default -> STANDARD_HELMET;
      };
    }

    static String name(HatRenderProfile profile) {
      if (profile == HARD_HAT) return "HARD_HAT";
      if (profile == BULKY_HELMET) return "BULKY_HELMET";
      if (profile == MILITARY_HELMET) return "MILITARY_HELMET";
      if (profile == MASK) return "MASK";
      if (profile == GAS_MASK) return "GAS_MASK";
      if (profile == NIGHT_VISION) return "NIGHT_VISION";
      if (profile == LARGE_HAT) return "LARGE_HAT";
      if (profile == EARS) return "EARS";
      if (profile == KNIGHT_HELMET) return "KNIGHT_HELMET";
      if (profile == SCUBA_MASK) return "SCUBA_MASK";
      return "STANDARD_HELMET";
    }

    void apply(PoseStack poseStack) {
      poseStack.translate(this.translationX, this.translationY, this.translationZ);
      if (this.rotationZ != 0.0F) {
        poseStack.mulPose(Axis.ZP.rotationDegrees(this.rotationZ));
      }
      if (this.rotationY != 0.0F) {
        poseStack.mulPose(Axis.YP.rotationDegrees(this.rotationY));
      }
      if (this.rotationX != 0.0F) {
        poseStack.mulPose(Axis.XP.rotationDegrees(this.rotationX));
      }
      poseStack.scale(this.scaleX, this.scaleY, this.scaleZ);

      poseStack.translate(0.0F, this.headOriginY, 0.0F);
      poseStack.scale(1.0F, -1.0F, -1.0F);
    }
  }

  public static <T extends LivingEntity, M extends EntityModel<T> & HeadedModel> Builder<T, M> builder(
      LivingEntityRenderer<T, M> renderer) {
    return new Builder<>(renderer);
  }

  public static class Builder<T extends LivingEntity, M extends EntityModel<T> & HeadedModel> {

    private final LivingEntityRenderer<T, M> renderer;
    private Equipment.Slot slot;
    private ItemDisplayContext displayContext = ItemDisplayContext.HEAD;
    @Nullable
    private Consumer<PoseStack> tranformation;
    private boolean useCrouchOrientation;
    private Attachment attachment = Attachment.NONE;

    private Builder(LivingEntityRenderer<T, M> renderer) {
      this.renderer = renderer;
    }

    public Builder<T, M> slot(Equipment.Slot slot) {
      this.slot = slot;
      return this;
    }

    public Builder<T, M> displayContext(ItemDisplayContext displayContext) {
      this.displayContext = Objects.requireNonNull(displayContext, "displayContext");
      return this;
    }

    public Builder<T, M> attachment(Attachment attachment) {
      this.attachment = Objects.requireNonNull(attachment, "attachment");
      this.displayContext = attachment.displayContext;
      return this;
    }

    public Builder<T, M> transformation(Consumer<PoseStack> transformation) {
      this.tranformation = transformation;
      return this;
    }

    public Builder<T, M> useCrouchOrientation(boolean useCrouchOrientation) {
      this.useCrouchOrientation = useCrouchOrientation;
      return this;
    }

    public EquipmentLayer<T, M> build() {
      Objects.requireNonNull(this.renderer, "The renderer must not be null");
      Objects.requireNonNull(this.slot, "The slot must not be null");
      return new EquipmentLayer<>(this);
    }
  }
}

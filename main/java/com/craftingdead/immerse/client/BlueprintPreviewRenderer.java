package com.craftingdead.immerse.client;

import com.craftingdead.immerse.world.base.BaseSecurity;
import com.craftingdead.immerse.world.item.BuildBlueprintItem;
import com.craftingdead.immerse.world.level.block.entity.BaseCenterBlockEntity;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public class BlueprintPreviewRenderer {

  private final Minecraft minecraft = Minecraft.getInstance();

  @SubscribeEvent
  public void renderPreview(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
        || this.minecraft.player == null
        || this.minecraft.level == null
        || this.minecraft.hitResult == null
        || this.minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
      return;
    }

    InteractionHand hand = this.findBlueprintHand();
    if (hand == null) {
      return;
    }

    ItemStack stack = this.minecraft.player.getItemInHand(hand);
    if (!(stack.getItem() instanceof BuildBlueprintItem blueprintItem)) {
      return;
    }

    var hitResult = (BlockHitResult) this.minecraft.hitResult;
    var context = new UseOnContext(this.minecraft.player, hand, hitResult);
    var placements = BuildBlueprintItem.getPlacements(blueprintItem.getBlueprintName(), context);
    if (placements.isEmpty()) {
      return;
    }

    var cameraPos = event.getCamera().getPosition();
    var poseStack = event.getPoseStack();
    VertexConsumer lines = this.minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());

    poseStack.pushPose();
    this.renderBaseRange(lines, poseStack, cameraPos.x, cameraPos.y, cameraPos.z);
    for (var placement : placements) {
      AABB box = new AABB(placement.pos()).move(-cameraPos.x, -cameraPos.y, -cameraPos.z)
          .inflate(0.002D);
      if (placement.replaceable()) {
        LevelRenderer.renderLineBox(poseStack, lines, box, 0.1F, 0.9F, 0.25F, 0.85F);
      } else {
        LevelRenderer.renderLineBox(poseStack, lines, box, 1.0F, 0.15F, 0.1F, 0.9F);
      }
    }
    poseStack.popPose();
    this.minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
  }

  private void renderBaseRange(VertexConsumer lines, com.mojang.blaze3d.vertex.PoseStack poseStack,
      double cameraX, double cameraY, double cameraZ) {
    BaseSecurity.findBase(this.minecraft.level, this.minecraft.player.blockPosition())
        .ifPresent(baseCenter -> {
          BlockPos center = baseCenter.getBlockPos();
          int radius = BaseCenterBlockEntity.PROTECTION_RADIUS;
          AABB range = new AABB(
              center.getX() - radius, center.getY() - radius, center.getZ() - radius,
              center.getX() + radius + 1, center.getY() + radius + 1, center.getZ() + radius + 1)
              .move(-cameraX, -cameraY, -cameraZ)
              .inflate(0.005D);
          LevelRenderer.renderLineBox(poseStack, lines, range, 0.2F, 0.55F, 1.0F, 0.7F);
        });
  }

  private InteractionHand findBlueprintHand() {
    if (this.minecraft.player.getMainHandItem().getItem() instanceof BuildBlueprintItem) {
      return InteractionHand.MAIN_HAND;
    }
    if (this.minecraft.player.getOffhandItem().getItem() instanceof BuildBlueprintItem) {
      return InteractionHand.OFF_HAND;
    }
    return null;
  }
}

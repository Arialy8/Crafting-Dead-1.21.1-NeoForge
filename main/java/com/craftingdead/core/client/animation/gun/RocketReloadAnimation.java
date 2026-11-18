/*
 * Crafting Dead
 * Copyright (C) 2022 NexusNode LTD
 */

package com.craftingdead.core.client.animation.gun;

import com.craftingdead.core.client.animation.TimedAnimation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Vector3f;

public class RocketReloadAnimation extends TimedAnimation {

  public RocketReloadAnimation(int lifetimeTicks) {
    super(lifetimeTicks);
  }

  @Override
  public void apply(float partialTick, PoseStack poseStack) {
    float progress = progress(partialTick);
    float lowered = envelope(progress);
    poseStack.translate(-0.035D * lowered, 0.08D * lowered, 0.08D * lowered);
    poseStack.mulPose(Axis.XP.rotationDegrees(-7.0F * lowered));
    poseStack.mulPose(Axis.YP.rotationDegrees(-9.0F * lowered));
    poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F * lowered));
  }

  @Override
  public void applyArm(InteractionHand hand, HumanoidArm arm, float partialTick,
      PoseStack poseStack) {
    if (hand == InteractionHand.MAIN_HAND) {
      return;
    }
    float progress = progress(partialTick);
    float reach = segment(progress, 0.12F, 0.38F);
    float insert = segment(progress, 0.48F, 0.78F);
    float release = segment(progress, 0.78F, 1.0F);
    float hold = Mth.clamp(reach - release, 0.0F, 1.0F);
    float side = arm == HumanoidArm.RIGHT ? -1.0F : 1.0F;
    poseStack.translate(side * 0.045D * hold,
        (0.11D - 0.05D * insert) * hold,
        (-0.12D + 0.18D * insert) * hold);
    poseStack.mulPose(Axis.XP.rotationDegrees(-14.0F * hold));
    poseStack.mulPose(Axis.YP.rotationDegrees(side * 12.0F * hold));
  }

  @Override
  public void applyMagazine(float partialTick, PoseStack poseStack) {
    float progress = progress(partialTick);
    float approach = segment(progress, 0.10F, 0.48F);
    float insert = segment(progress, 0.48F, 0.80F);
    float distance = (1.0F - approach) * 0.55F + (1.0F - insert) * 0.25F;
    poseStack.translate(0.10D * (1.0F - insert), 0.04D, distance);
    poseStack.mulPose(Axis.YP.rotationDegrees(7.0F * (1.0F - insert)));
  }

  @Override
  public void applyCamera(float partialTick, Vector3f rotations) {}

  private float progress(float partialTick) {
    return Mth.clamp(this.lerpProgress(partialTick), 0.0F, 1.0F);
  }

  private static float envelope(float progress) {
    if (progress < 0.22F) {
      return smooth(progress / 0.22F);
    }
    if (progress < 0.82F) {
      return 1.0F;
    }
    return 1.0F - smooth((progress - 0.82F) / 0.18F);
  }

  private static float segment(float progress, float start, float end) {
    return smooth((progress - start) / (end - start));
  }

  private static float smooth(float value) {
    float clamped = Mth.clamp(value, 0.0F, 1.0F);
    return clamped * clamped * (3.0F - 2.0F * clamped);
  }
}

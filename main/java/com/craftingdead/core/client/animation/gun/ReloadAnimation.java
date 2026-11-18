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

package com.craftingdead.core.client.animation.gun;

import com.craftingdead.core.client.animation.TimedAnimation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import com.mojang.math.Axis;

public class ReloadAnimation extends TimedAnimation {

  private boolean removingMagazine;

  public ReloadAnimation(int lifetimeTicks) {
    super(lifetimeTicks);
  }

  public void setRemovingMagazine(boolean removingMagazine) {
    this.removingMagazine = removingMagazine;
  }

  @Override
  public void apply(float partialTicks, PoseStack poseStack) {
    this.applyTranslations(partialTicks, poseStack);
  }

  @Override
  public void applyArm(InteractionHand hand, HumanoidArm arm, float partialTicks, PoseStack poseStack) {
    float progress = Mth.clamp(this.lerpProgress(partialTicks), 0.0F, 1.0F);
    if (hand == InteractionHand.MAIN_HAND) {
      float lowered = reloadEnvelope(progress);
      poseStack.translate(0.008D * lowered, 0.018D * lowered, 0.012D * lowered);
      poseStack.mulPose(Axis.XP.rotationDegrees(-2.0F * lowered));
      poseStack.mulPose(Axis.ZP.rotationDegrees(-2.5F * lowered));
      return;
    }

    float side = arm == HumanoidArm.RIGHT ? -1.0F : 1.0F;
    float approach;
    float travel;
    float release;
    if (this.removingMagazine) {
      approach = segment(progress, 0.05F, 0.25F);
      travel = segment(progress, 0.30F, 0.72F);
      release = segment(progress, 0.78F, 1.0F);
    } else {
      approach = segment(progress, 0.04F, 0.20F);
      travel = 1.0F - segment(progress, 0.26F, 0.72F);
      release = segment(progress, 0.76F, 1.0F);
    }
    float grip = Mth.clamp(approach - release, 0.0F, 1.0F);
    float carried = grip * travel;

    // The support hand starts below the frame and follows the magazine into the well.
    poseStack.translate(side * (0.050D * grip + 0.080D * carried),
        -0.060D * grip - 0.420D * carried,
        0.025D * grip + 0.060D * carried);
    poseStack.mulPose(Axis.XP.rotationDegrees(-9.0F * grip - 5.0F * carried));
    poseStack.mulPose(Axis.YP.rotationDegrees(side * (8.0F * grip + 4.0F * carried)));
    poseStack.mulPose(Axis.ZP.rotationDegrees(side * (-7.0F * grip - 3.0F * carried)));
  }

  @Override
  public void applyMagazine(float partialTicks, PoseStack poseStack) {
    float progress = Mth.clamp(this.lerpProgress(partialTicks), 0.0F, 1.0F);
    float displaced;
    if (this.removingMagazine) {
      displaced = segment(progress, 0.28F, 0.76F);
    } else {
      displaced = 1.0F - segment(progress, 0.14F, 0.74F);
    }

    // The old magazine drops down; the replacement follows the exact reverse path upward.
    float lateral = segment(displaced, 0.35F, 1.0F);
    poseStack.translate(0.100D * lateral, -0.620D * displaced, 0.070D * lateral);
    poseStack.mulPose(Axis.XP.rotationDegrees(6.0F * lateral));
    poseStack.mulPose(Axis.ZP.rotationDegrees(-4.0F * lateral));
  }

  private void applyTranslations(float partialTicks, PoseStack poseStack) {
    float progress = Mth.clamp(this.lerpProgress(partialTicks), 0.0F, 1.0F);
    float lowered = reloadEnvelope(progress);

    // Keep the weapon anchored to the shoulder and expose the magazine well at the edge.
    poseStack.translate(0.025D * lowered, 0.045D * lowered, 0.025D * lowered);
    poseStack.mulPose(Axis.XP.rotationDegrees(-4.0F * lowered));
    poseStack.mulPose(Axis.YP.rotationDegrees(3.0F * lowered));
    poseStack.mulPose(Axis.ZP.rotationDegrees(-7.0F * lowered));
  }

  @Override
  public void applyCamera(float partialTicks, Vector3f rotations) {
    // Reloading must not shake or steer the player's camera.
  }

  private static float smoothStep(float value) {
    float clamped = Mth.clamp(value, 0.0F, 1.0F);
    return clamped * clamped * (3.0F - 2.0F * clamped);
  }

  private static float segment(float progress, float start, float end) {
    return smoothStep((progress - start) / (end - start));
  }

  private static float reloadEnvelope(float progress) {
    if (progress < 0.25F) {
      return smoothStep(progress / 0.25F);
    }
    if (progress < 0.68F) {
      return 1.0F;
    }
    return 1.0F - smoothStep((progress - 0.68F) / 0.32F);
  }
}

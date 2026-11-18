package com.craftingdead.survival.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

public final class SupplyDropModel {

  private final ModelPart root;
  private final ModelPart parachute;

  public SupplyDropModel(ModelPart root) {
    this.root = root;
    this.parachute = root.getChild("parachute");
  }

  public static LayerDefinition createBodyLayer() {
    var mesh = new MeshDefinition();
    var root = mesh.getRoot();
    root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0)
        .addBox(0F, 0.01F, 0F, 32, 4, 32, true), PartPose.offset(-16F, 20F, -16F));
    root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 38)
        .addBox(0F, 0F, 0F, 30, 18, 15, true), PartPose.offset(-15F, 2F, 0F));
    root.addOrReplaceChild("side", CubeListBuilder.create().texOffs(0, 73)
        .addBox(0F, 0F, 0F, 14, 14, 14, true), PartPose.offset(0F, 6F, -15F));
    root.addOrReplaceChild("latch_left", CubeListBuilder.create().texOffs(0, 105)
        .addBox(0F, 0F, 0F, 11, 6, 6, true), PartPose.offset(-13F, 14F, -15F));
    root.addOrReplaceChild("latch_right", CubeListBuilder.create().texOffs(0, 119)
        .addBox(0F, 0F, 0F, 11, 6, 6, true), PartPose.offset(-13F, 14F, -7F));
    root.addOrReplaceChild("parachute", CubeListBuilder.create().texOffs(0, 133)
        .addBox(0F, 0F, 0F, 40, 30, 40, true), PartPose.offset(-20F, -50F, -20F));
    return LayerDefinition.create(mesh, 256, 256);
  }

  public void setParachuteVisible(boolean visible) {
    this.parachute.visible = visible;
  }

  public ModelPart root() {
    return this.root;
  }
}

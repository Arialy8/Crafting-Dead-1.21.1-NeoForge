package com.craftingdead.survival.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

public final class PipeBombModel {

  private PipeBombModel() {}

  public static LayerDefinition createBodyLayer() {
    var mesh = new MeshDefinition();
    var anchor = mesh.getRoot().addOrReplaceChild("anchor", CubeListBuilder.create(),
        PartPose.offset(0.0F, 4.0F, 0.0F));
    var body = anchor.addOrReplaceChild("body", CubeListBuilder.create()
        .texOffs(8, 4).addBox(-2.25F, -6.0F, 0.0F, 4.0F, 3.0F, 0.0F)
        .texOffs(12, 0).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 1.0F, 3.0F)
        .texOffs(0, 4).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 5.0F, 2.0F)
        .texOffs(0, 0).addBox(-1.5F, 3.0F, -1.5F, 3.0F, 1.0F, 3.0F)
        .texOffs(0, 11).addBox(-1.5F, -1.5F, -0.25F, 1.0F, 2.0F, 1.0F)
        .texOffs(12, 11).addBox(-0.25F, -0.75F, -1.25F, 1.0F, 1.0F, 1.0F),
        PartPose.ZERO);
    body.addOrReplaceChild("light", CubeListBuilder.create().texOffs(8, 11)
        .addBox(-1.9023F, -2.1563F, -0.4735F, 1.0F, 1.0F, 1.0F)
        .texOffs(4, 11).addBox(-1.5023F, -1.1563F, -0.4735F, 1.0F, 2.0F, 1.0F),
        PartPose.offsetAndRotation(0.0104F, 0.4063F, -0.0271F, 0.0F, -0.7854F, 0.0F));
    return LayerDefinition.create(mesh, 32, 32);
  }
}

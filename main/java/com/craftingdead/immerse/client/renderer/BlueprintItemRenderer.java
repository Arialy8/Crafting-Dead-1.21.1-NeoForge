package com.craftingdead.immerse.client.renderer;

import com.craftingdead.immerse.world.item.BuildBlueprintItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Show the blueprint's actual target, including special-rendered items such as chests. */
public final class BlueprintItemRenderer extends BlockEntityWithoutLevelRenderer {
  private static BlueprintItemRenderer instance;
  private BlueprintItemRenderer(Minecraft minecraft) {
    super(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
  }
  public static BlueprintItemRenderer getInstance() {
    if (instance == null) instance = new BlueprintItemRenderer(Minecraft.getInstance());
    return instance;
  }
  @Override public void renderByItem(ItemStack stack, ItemDisplayContext context,
      PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
    if (!(stack.getItem() instanceof BuildBlueprintItem blueprint)) return;
    var target = BuildBlueprintItem.getTargetBlock(blueprint.getBlueprintName());
    if (target == null) return;
    Minecraft minecraft = Minecraft.getInstance();
    pose.pushPose();
    pose.translate(0.5, 0.5, 0.5);
    minecraft.getItemRenderer().renderStatic(new ItemStack(
        com.craftingdead.immerse.world.item.ImmerseItems.ITEMS.get("blank_blueprint").get()),
        context, light, overlay, pose, buffers, minecraft.level, 0);
    pose.translate(0.16, 0.13, 0.20);
    pose.scale(0.55F, 0.55F, 0.55F);
    minecraft.getItemRenderer().renderStatic(new ItemStack(target), context,
        light, overlay, pose, buffers, minecraft.level, 0);
    pose.popPose();
  }
}

package com.craftingdead.core.world.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Salvaged vehicle component that must be restored at a crafting table before installation. */
public final class DamagedVehiclePartItem extends Item {

  private final String repairedPartKey;

  public DamagedVehiclePartItem(Properties properties, String repairedPartKey) {
    super(properties);
    this.repairedPartKey = repairedPartKey;
  }

  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
      TooltipFlag tooltipFlag) {
    lines.add(Component.translatable("tooltip.craftingdead.vehicle_part.damaged")
        .withStyle(ChatFormatting.RED));
    lines.add(Component.translatable("tooltip.craftingdead.vehicle_part.repair",
        Component.translatable(this.repairedPartKey)).withStyle(ChatFormatting.GRAY));
  }
}

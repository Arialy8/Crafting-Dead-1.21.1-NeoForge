package com.craftingdead.core.world.item;
import com.craftingdead.core.CraftingDead;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
public final class ChalkItem extends Item {
  public ChalkItem(Properties properties) { super(properties.stacksTo(1)); }
  @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    if (level.isClientSide()) CraftingDead.getInstance().getClientDist().openChalkScreen(hand);
    return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
  }
}

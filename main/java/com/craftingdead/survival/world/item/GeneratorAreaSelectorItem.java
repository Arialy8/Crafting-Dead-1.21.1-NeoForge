package com.craftingdead.survival.world.item;

import com.craftingdead.core.util.ItemStackDataUtil;
import com.craftingdead.survival.world.inventory.GeneratorSelectorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GeneratorAreaSelectorItem extends Item {

  public static final int TYPE_COUNT = 5;
  private static final String COUNT = "GeneratorCount";
  private static final String WEIGHT_PREFIX = "GeneratorWeight";

  public GeneratorAreaSelectorItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (hand != InteractionHand.MAIN_HAND) {
      return InteractionResultHolder.pass(stack);
    }
    if (!level.isClientSide()) {
      player.openMenu(new SimpleMenuProvider(
          (id, inventory, menuPlayer) -> new GeneratorSelectorMenu(id, inventory),
          Component.translatable("container.craftingdeadsurvival.generator_selector")));
    }
    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
  }

  public static int getCount(ItemStack stack) {
    var tag = ItemStackDataUtil.getCustomDataCopy(stack);
    return tag.contains(COUNT) ? Math.max(1, Math.min(500, tag.getInt(COUNT))) : 25;
  }

  public static int getWeight(ItemStack stack, int index) {
    var tag = ItemStackDataUtil.getCustomDataCopy(stack);
    String key = WEIGHT_PREFIX + index;
    return tag.contains(key) ? Math.max(0, Math.min(100, tag.getInt(key))) : 20;
  }

  public static void setCount(ItemStack stack, int count) {
    ItemStackDataUtil.updateCustomData(stack,
        tag -> tag.putInt(COUNT, Math.max(1, Math.min(500, count))));
  }

  public static boolean setWeight(ItemStack stack, int index, int value) {
    if (index < 0 || index >= TYPE_COUNT || value < 0 || value > 100) {
      return false;
    }
    int total = value;
    for (int i = 0; i < TYPE_COUNT; i++) {
      if (i != index) {
        total += getWeight(stack, i);
      }
    }
    if (total > 100) {
      return false;
    }
    ItemStackDataUtil.updateCustomData(stack, tag -> tag.putInt(WEIGHT_PREFIX + index, value));
    return true;
  }

  public static void adjustWeight(ItemStack stack, int index, int delta) {
    if (index < 0 || index >= TYPE_COUNT || delta == 0) {
      return;
    }
    int other = (index + 1) % TYPE_COUNT;
    int selected = getWeight(stack, index);
    int donor = getWeight(stack, other);
    int applied = delta > 0 ? Math.min(delta, donor) : -Math.min(-delta, selected);
    ItemStackDataUtil.updateCustomData(stack, tag -> {
      tag.putInt(WEIGHT_PREFIX + index, selected + applied);
      tag.putInt(WEIGHT_PREFIX + other, donor - applied);
    });
  }
}

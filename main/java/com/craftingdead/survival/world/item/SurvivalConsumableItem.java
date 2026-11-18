package com.craftingdead.survival.world.item;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.ModAttachmentTypes;
import com.craftingdead.survival.world.hydration.HydrationData;
import java.util.function.Supplier;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class SurvivalConsumableItem extends Item {

  private final @Nullable FoodProperties foodProperties;
  private final boolean drink;
  private final int water;
  private final @Nullable Supplier<Item> remainder;

  public SurvivalConsumableItem(Properties properties, @Nullable FoodProperties foodProperties,
      boolean drink, int water, @Nullable Supplier<Item> remainder) {
    super(properties);
    this.foodProperties = foodProperties;
    this.drink = drink;
    this.water = Math.max(0, water);
    this.remainder = remainder;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    var itemStack = player.getItemInHand(hand);
    boolean canEat = this.foodProperties != null && player.canEat(false);
    boolean canHydrate = this.water > 0 && this.canHydrate(player);
    if (this.drink && this.water > 0 && !canHydrate) {
      return InteractionResultHolder.fail(itemStack);
    }
    if (!this.drink && (this.foodProperties != null || this.water > 0) && !canEat && !canHydrate) {
      return InteractionResultHolder.fail(itemStack);
    }
    player.startUsingItem(hand);
    return InteractionResultHolder.consume(itemStack);
  }

  @Override
  public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity livingEntity) {
    if (livingEntity instanceof ServerPlayer serverPlayer) {
      CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, itemStack);
    }

    if (livingEntity instanceof Player player) {
      player.awardStat(Stats.ITEM_USED.get(this));
      if (this.foodProperties != null) {
        player.getFoodData().eat(this.foodProperties);
      }
      if (this.water > 0 && player instanceof ServerPlayer serverPlayer) {
        CraftingDead.restoreHydration(serverPlayer, this.water);
      }
      if (!player.getAbilities().instabuild) {
        itemStack.shrink(1);
        this.giveRemainder(player);
      }
    }

    return itemStack;
  }

  @Override
  public UseAnim getUseAnimation(ItemStack itemStack) {
    return this.drink ? UseAnim.DRINK : UseAnim.EAT;
  }

  @Override
  public int getUseDuration(ItemStack itemStack, LivingEntity livingEntity) {
    return 32;
  }

  private void giveRemainder(Player player) {
    if (this.remainder == null) {
      return;
    }
    var remainderStack = new ItemStack(this.remainder.get());
    if (!player.getInventory().add(remainderStack)) {
      player.drop(remainderStack, false);
    }
  }

  private boolean canHydrate(Player player) {
    return player.getData(ModAttachmentTypes.getHydration()).getWater() < HydrationData.MAX_WATER;
  }
}

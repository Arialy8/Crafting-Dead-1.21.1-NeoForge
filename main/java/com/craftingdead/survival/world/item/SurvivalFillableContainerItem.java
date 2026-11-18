package com.craftingdead.survival.world.item;

import java.util.function.Supplier;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class SurvivalFillableContainerItem extends Item {

  private final Supplier<Item> filledItem;

  public SurvivalFillableContainerItem(Properties properties, Supplier<Item> filledItem) {
    super(properties);
    this.filledItem = filledItem;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    var itemStack = player.getItemInHand(hand);
    var hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
    if (hitResult.getType() != HitResult.Type.BLOCK
        || !(hitResult instanceof BlockHitResult blockHitResult)
        || !level.getFluidState(blockHitResult.getBlockPos()).is(FluidTags.WATER)
        || !level.getFluidState(blockHitResult.getBlockPos()).isSource()) {
      return InteractionResultHolder.pass(itemStack);
    }

    if (level.isClientSide()) {
      return InteractionResultHolder.sidedSuccess(itemStack, true);
    }

    if (player instanceof ServerPlayer serverPlayer) {
      CriteriaTriggers.FILLED_BUCKET.trigger(serverPlayer, itemStack);
    }
    player.awardStat(Stats.ITEM_USED.get(this));
    level.playSound(null, player.getX(), player.getY(), player.getZ(),
        SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);

    ItemStack result = ItemUtils.createFilledResult(itemStack, player,
        new ItemStack(this.filledItem.get()));
    return InteractionResultHolder.sidedSuccess(result, false);
  }
}

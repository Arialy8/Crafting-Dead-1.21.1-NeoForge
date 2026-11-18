package com.craftingdead.core.world.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Continuous fuel-fed weapon inspired by the 1.6.4 flamethrower. No terrain ignition. */
public final class FlamethrowerItem extends Item {
  public FlamethrowerItem(Properties properties) { super(properties.stacksTo(1)); }
  private static ItemStack fuel(Player player) {
    for (var stack : player.getInventory().items)
      if (stack.is(ModItems.FLAMETHROWER_FUEL.get()) && stack.getDamageValue() < stack.getMaxDamage()) return stack;
    return ItemStack.EMPTY;
  }
  @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    if (!player.getAbilities().instabuild && fuel(player).isEmpty()) {
      if (!level.isClientSide()) player.displayClientMessage(net.minecraft.network.chat.Component.literal("缺少喷火枪燃料罐"), true);
      return InteractionResultHolder.fail(player.getItemInHand(hand));
    }
    player.startUsingItem(hand);
    return InteractionResultHolder.consume(player.getItemInHand(hand));
  }
  @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }
  @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }
  @Override public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remaining) {
    if (!(world instanceof ServerLevel level) || !(user instanceof Player player) || remaining % 4 != 0) return;
    ItemStack fuel = fuel(player);
    if (!player.getAbilities().instabuild) {
      if (fuel.isEmpty()) { player.stopUsingItem(); return; }
      fuel.setDamageValue(fuel.getDamageValue() + 1);
      if (fuel.getDamageValue() >= fuel.getMaxDamage()) fuel.shrink(1);
    }
    Vec3 start = player.getEyePosition(), direction = player.getLookAngle();
    Vec3 end = level.clip(new ClipContext(start, start.add(direction.scale(10)),
        ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player)).getLocation();
    double length = start.distanceTo(end);
    for (double distance = 0.7; distance < length; distance += 0.45) {
      Vec3 point = start.add(direction.scale(distance));
      level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 3, .08, .08, .08, .015);
    }
    if (remaining % 20 == 0)
      level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, .6F, .65F);
    for (var target : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(.4),
        entity -> entity != player && entity.isAlive() && !entity.isSpectator())) {
      if (target instanceof Player other && !player.canHarmPlayer(other)) continue;
      var hit = target.getBoundingBox().inflate(.2).clip(start, end);
      if (hit.isEmpty()) continue;
      var obstruction = level.clip(new ClipContext(start, hit.get(), ClipContext.Block.COLLIDER,
          ClipContext.Fluid.ANY, player));
      if (obstruction.getType() != HitResult.Type.MISS) continue;
      if (target.hurt(player.damageSources().playerAttack(player), 2F)) target.igniteForSeconds(4);
    }
  }
}

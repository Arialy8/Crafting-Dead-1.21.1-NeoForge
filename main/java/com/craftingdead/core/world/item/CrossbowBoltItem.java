package com.craftingdead.core.world.item;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
public final class CrossbowBoltItem extends ArrowItem {
  public CrossbowBoltItem(Properties properties) { super(properties); }
  @Override public AbstractArrow createArrow(Level level, ItemStack ammunition, LivingEntity shooter, ItemStack weapon) {
    var arrow = super.createArrow(level, ammunition, shooter, weapon);
    arrow.setBaseDamage(6.0);
    return arrow;
  }
}

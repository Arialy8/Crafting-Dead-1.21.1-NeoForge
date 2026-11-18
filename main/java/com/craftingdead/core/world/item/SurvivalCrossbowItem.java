package com.craftingdead.core.world.item;
import java.util.function.Predicate;
import net.minecraft.world.item.*;
/** Uses vanilla charged-projectile synchronization and reload rules with dedicated bolts. */
public final class SurvivalCrossbowItem extends CrossbowItem {
  public SurvivalCrossbowItem(Properties properties) { super(properties.durability(465)); }
  @Override public Predicate<ItemStack> getAllSupportedProjectiles() {
    return stack -> stack.is(ModItems.CROSSBOW_BOLT.get());
  }
  @Override public Predicate<ItemStack> getSupportedHeldProjectiles() { return getAllSupportedProjectiles(); }
}

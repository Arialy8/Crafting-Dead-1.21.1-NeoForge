package com.craftingdead.immerse.world.base;
import com.craftingdead.core.world.item.gun.Gun;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
/** Yields and durations transcribed from the supplied 1.18 TCD.EU grinder table. */
public final class LegacyGrinderRecipes {
  private static final Map<String, int[]> RECIPES = Map.ofEntries(
      Map.entry("craftingdead:m1911", new int[]{2, 40}),
      Map.entry("craftingdead:g18", new int[]{2, 40}),
      Map.entry("craftingdead:m9", new int[]{2, 40}),
      Map.entry("craftingdead:desert_eagle", new int[]{2, 40}),
      Map.entry("craftingdead:p250", new int[]{2, 40}),
      Map.entry("craftingdead:magnum", new int[]{2, 40}),
      Map.entry("craftingdead:fn57", new int[]{2, 40}),
      Map.entry("craftingdead:taser", new int[]{2, 40}),
      Map.entry("craftingdead:mac10", new int[]{4, 80}),
      Map.entry("craftingdead:p90", new int[]{4, 80}),
      Map.entry("craftingdead:vector", new int[]{4, 80}),
      Map.entry("craftingdead:mp5a5", new int[]{4, 80}),
      Map.entry("craftingdead:m4a1", new int[]{8, 160}),
      Map.entry("craftingdead:scarl", new int[]{8, 160}),
      Map.entry("craftingdead:ak47", new int[]{8, 160}),
      Map.entry("craftingdead:fnfal", new int[]{8, 160}),
      Map.entry("craftingdead:acr", new int[]{8, 160}),
      Map.entry("craftingdead:hk417", new int[]{8, 160}),
      Map.entry("craftingdead:mpt55", new int[]{8, 160}),
      Map.entry("craftingdead:g36c", new int[]{8, 160}),
      Map.entry("craftingdead:m1garand", new int[]{8, 160}),
      Map.entry("craftingdead:sporter22", new int[]{8, 160}),
      Map.entry("craftingdead:trench_gun", new int[]{8, 160}),
      Map.entry("craftingdead:mossberg", new int[]{8, 160}),
      Map.entry("craftingdead:m107", new int[]{12, 240}),
      Map.entry("craftingdead:as50", new int[]{12, 240}),
      Map.entry("craftingdead:awp", new int[]{12, 240}),
      Map.entry("craftingdead:dmr", new int[]{12, 240}),
      Map.entry("craftingdead:m240b", new int[]{15, 300}),
      Map.entry("craftingdead:rpk", new int[]{15, 300}),
      Map.entry("craftingdead:mk48mod", new int[]{15, 300}),
      Map.entry("craftingdead:minigun", new int[]{20, 400}));
  public static int[] find(ItemStack stack) {
    if (stack.isEmpty()) return null;
    var gun = Gun.get(stack);
    if (gun.isEmpty() || !gun.get().getAttachments().isEmpty()
        || !gun.get().getAmmoProvider().getMagazineStack().isEmpty()) return null;
    return RECIPES.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
  }
  private LegacyGrinderRecipes() {}
}

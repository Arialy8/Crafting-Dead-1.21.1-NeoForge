package com.craftingdead.immerse.world.inventory;

import com.craftingdead.immerse.CraftingDeadImmerse;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ImmerseMenuTypes {

  public static final DeferredRegister<MenuType<?>> deferredRegister =
      DeferredRegister.create(Registries.MENU, CraftingDeadImmerse.ID);

  public static final Supplier<MenuType<BaseCenterMenu>> BASE_CENTER =
      deferredRegister.register("base_center",
          () -> new MenuType<>(BaseCenterMenu::new, FeatureFlags.DEFAULT_FLAGS));

  private ImmerseMenuTypes() {}
  public static final Supplier<MenuType<RankRewardMenu>> RANK_REWARDS =
      deferredRegister.register("rank_rewards",
          () -> new MenuType<>(RankRewardMenu::new, FeatureFlags.DEFAULT_FLAGS));
}

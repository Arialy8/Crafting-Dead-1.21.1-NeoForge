package com.craftingdead.survival.world.inventory;

import com.craftingdead.survival.CraftingDeadSurvival;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SurvivalMenuTypes {
  public static final DeferredRegister<MenuType<?>> REGISTER =
      DeferredRegister.create(Registries.MENU, CraftingDeadSurvival.ID);
  public static final Supplier<MenuType<GeneratorSelectorMenu>> GENERATOR_SELECTOR =
      REGISTER.register("generator_selector",
          () -> new MenuType<>(GeneratorSelectorMenu::new, FeatureFlags.DEFAULT_FLAGS));
  private SurvivalMenuTypes() {}
}

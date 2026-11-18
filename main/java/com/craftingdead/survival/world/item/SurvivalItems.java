package com.craftingdead.survival.world.item;

import com.craftingdead.core.world.action.ActionTypes;
import com.craftingdead.core.world.item.ActionItem;
import com.craftingdead.core.world.item.GrenadeItem;
import com.craftingdead.survival.world.entity.PipeBomb;
import com.craftingdead.survival.CraftingDeadSurvival;
import com.craftingdead.survival.world.entity.SurvivalEntityTypes;
import com.craftingdead.survival.world.level.block.SurvivalBlocks;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.Comparator;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SurvivalItems {

  public static final DeferredRegister<Item> deferredRegister =
      DeferredRegister.create(Registries.ITEM, CraftingDeadSurvival.ID);

  public static final DeferredRegister<CreativeModeTab> tabDeferredRegister =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CraftingDeadSurvival.ID);

  private static final List<String> ITEM_NAMES = List.of(
      "advanced_zombie_spawn_egg",
      "thrower_zombie_spawn_egg",
      "rocket_zombie_spawn_egg",
      "alfa_zombie_spawn_egg",
      "blueberry",
      "bounty_hunter_zombie_spawn_egg",
      "can_opener",
      "candy_bar",
      "canned_beans",
      "canned_corned_beef",
      "canned_custard",
      "canned_dog_food",
      "canned_pasta",
      "canned_peaches",
      "canned_pickles",
      "canned_sweetcorn",
      "canned_tomato_soup",
      "canned_tuna",
      "cereal",
      "cheesy_chips",
      "chips",
      "cola",
      "cure_syringe",
      "desert_raider_zombie_spawn_egg",
      "doctor_zombie_spawn_egg",
      "emerald_cereal",
      "empty_cola",
      "empty_flask",
      "empty_iced_tea",
      "empty_iron_brew",
      "empty_juice_pouch",
      "empty_lemon_soda",
      "empty_milk_carton",
      "empty_orange_soda",
      "empty_pepe_soda",
      "empty_sprite",
      "empty_water_bottle",
      "empty_water_canteen",
      "empty_zombie_energy",
      "fast_zombie_spawn_egg",
      "firefighter_zombie_spawn_egg",
      "flake_cereal",
      "flask",
      "giant_zombie_spawn_egg",
      "hazmat_zombie_spawn_egg",
      "iced_tea",
      "iron_brew",
      "juggernaut_zombie_spawn_egg",
      "juice_pouch",
      "lemon_soda",
      "medical_drop_radio",
      "military_drop_radio",
      "generator_area_selector",
      "milk_carton",
      "miner_zombie_spawn_egg",
      "morphine_syringe",
      "mre",
      "multi_tool",
      "ninja_zombie_spawn_egg",
      "noodles",
      "nutty_cereal",
      "open_canned_beans",
      "open_canned_corned_beef",
      "open_canned_custard",
      "open_canned_dog_food",
      "open_canned_pasta",
      "open_canned_peaches",
      "open_canned_pickles",
      "open_canned_sweetcorn",
      "open_canned_tomato_soup",
      "open_canned_tuna",
      "orange",
      "orange_soda",
      "pear",
      "pepe_soda",
      "pilot_zombie_spawn_egg",
      "pipe_bomb",
      "police_zombie_spawn_egg",
      "popcorn",
      "power_bar",
      "ranch_chips",
      "raspberry",
      "rbi_syringe",
      "rice_bag",
      "rope",
      "rotten_apple",
      "rotten_blueberry",
      "rotten_melon_slice",
      "rotten_orange",
      "rotten_pear",
      "rotten_raspberry",
      "salted_chips",
      "scout_zombie_spawn_egg",
      "screwdriver",
      "sniper_zombie_spawn_egg",
      "soldier_zombie_spawn_egg",
      "splint",
      "sprite",
      "swat_zombie_spawn_egg",
      "tank_zombie_spawn_egg",
      "water_bottle",
      "water_canteen",
      "weak_zombie_spawn_egg",
      "zombie_energy");

  public static final Map<String, Supplier<Item>> ITEMS = new LinkedHashMap<>();
  private static final Set<String> CORE_MEDICAL_ITEMS = Set.of(
      "splint", "morphine_syringe", "cure_syringe", "rbi_syringe");

  static {
    for (var entry : SurvivalBlocks.BLOCKS.entrySet()) {
      ITEMS.put(entry.getKey(), deferredRegister.register(entry.getKey(),
          () -> new BlockItem(entry.getValue().get(), new Item.Properties())));
    }
    for (String name : ITEM_NAMES) {
      ITEMS.put(name, deferredRegister.register(name, () -> createItem(name)));
    }
  }

  public static final Supplier<CreativeModeTab> TAB = tabDeferredRegister.register("main",
      () -> CreativeModeTab.builder()
          .title(Component.translatable("itemGroup.craftingdeadsurvival"))
          .icon(SurvivalItems::createTabIcon)
          .displayItems((parameters, output) -> ITEMS.entrySet().stream()
              .filter(entry -> !CORE_MEDICAL_ITEMS.contains(entry.getKey()))
              .sorted(Comparator.comparingInt(entry -> tabCategory(entry.getKey())))
              .forEach(entry -> output.accept(entry.getValue().get())))
          .build());

  private static ItemStack createTabIcon() {
    return new ItemStack(ITEMS.getOrDefault("military_drop_radio",
        ITEMS.values().iterator().next()).get());
  }

  private static int tabCategory(String name) {
    if (SurvivalBlocks.BLOCKS.containsKey(name)) {
      return 0;
    }
    if (name.equals("can_opener") || name.equals("screwdriver") || name.equals("multi_tool")
        || name.equals("rope") || name.equals("pipe_bomb")) {
      return 1;
    }
    if (name.endsWith("_drop_radio") || name.equals("generator_area_selector")) {
      return 2;
    }
    if (name.startsWith("empty_") || name.equals("water_canteen")
        || name.equals("water_bottle") || name.equals("flask")) {
      return 3;
    }
    if (name.contains("soda") || name.equals("cola") || name.equals("iced_tea")
        || name.equals("iron_brew") || name.equals("juice_pouch")
        || name.equals("milk_carton") || name.equals("pepe_soda")
        || name.equals("sprite") || name.equals("zombie_energy")) {
      return 4;
    }
    if (name.endsWith("_spawn_egg")) {
      return 6;
    }
    return 5;
  }

  private static Item createItem(String name) {
    if (name.equals("generator_area_selector")) {
      return new GeneratorAreaSelectorItem(new Item.Properties().stacksTo(1));
    }
    if (name.equals("pipe_bomb")) {
      return new GrenadeItem((GrenadeItem.Properties) new GrenadeItem.Properties()
          .setGrenadeEntitySupplier((thrower, level) -> thrower == null
              ? new PipeBomb(level) : new PipeBomb(thrower, level))
          .stacksTo(3));
    }
    var spawnEgg = spawnEggItem(name);
    if (spawnEgg != null) {
      return spawnEgg;
    }
    var medicalItem = medicalItem(name);
    if (medicalItem != null) {
      return medicalItem;
    }
    var supplyDropRadio = supplyDropRadio(name);
    if (supplyDropRadio != null) {
      return supplyDropRadio;
    }
    var toolItem = toolItem(name);
    if (toolItem != null) {
      return toolItem;
    }
    var filledItemName = filledItemName(name);
    if (filledItemName != null) {
      return new SurvivalFillableContainerItem(new Item.Properties().stacksTo(3),
          () -> ITEMS.get(filledItemName).get());
    }
    var foodProperties = foodProperties(name);
    var properties = new Item.Properties().stacksTo(3);
    if (foodProperties != null) {
      properties.food(foodProperties);
    }
    var emptyItemName = emptyItemName(name);
    int water = waterValue(name);
    if (foodProperties != null || emptyItemName != null || water > 0) {
      return new SurvivalConsumableItem(properties, foodProperties, emptyItemName != null, water,
          emptyItemName == null ? null : () -> ITEMS.get(emptyItemName).get());
    }
    return new Item(properties);
  }

  private static Item spawnEggItem(String name) {
    if (!name.endsWith("_spawn_egg")) {
      return null;
    }
    var entityName = name.substring(0, name.length() - "_spawn_egg".length());
    var entityType = SurvivalEntityTypes.ZOMBIES.get(entityName);
    if (entityType == null) {
      return null;
    }
    return new SpawnEggItem(entityType.get(), 0x3F5E34, 0x7FA15A, new Item.Properties());
  }

  private static Item supplyDropRadio(String name) {
    return switch (name) {
      case "medical_drop_radio" -> new SupplyDropRadioItem(new Item.Properties().stacksTo(1),
          net.minecraft.resources.ResourceKey.create(
              net.minecraft.core.registries.Registries.LOOT_TABLE,
              net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                  CraftingDeadSurvival.ID, "supply_drops/medical")));
      case "military_drop_radio" -> new SupplyDropRadioItem(new Item.Properties().stacksTo(1),
          net.minecraft.resources.ResourceKey.create(
              net.minecraft.core.registries.Registries.LOOT_TABLE,
              net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                  CraftingDeadSurvival.ID, "supply_drops/military")));
      default -> null;
    };
  }

  private static Item toolItem(String name) {
    return switch (name) {
      case "can_opener" -> new SurvivalToolItem(new Item.Properties().durability(8));
      case "screwdriver" -> new SurvivalToolItem(new Item.Properties().durability(12));
      case "multi_tool" -> new SurvivalToolItem(new Item.Properties().durability(80));
      default -> null;
    };
  }

  private static FoodProperties foodProperties(String name) {
    return switch (name) {
      case "power_bar" -> food(4, 0.3F);
      case "candy_bar" -> food(6, 0.3F);
      case "cereal", "nutty_cereal", "emerald_cereal", "flake_cereal" -> food(10, 0.6F);
      case "open_canned_sweetcorn", "open_canned_tuna", "open_canned_pasta" -> food(6, 0.6F);
      case "open_canned_beans", "open_canned_corned_beef", "rice_bag" -> food(8, 0.6F);
      case "open_canned_custard", "open_canned_pickles", "open_canned_tomato_soup", "blueberry",
          "pear" -> food(4, 0.6F);
      case "open_canned_dog_food" -> food(2, 0.6F);
      case "mre" -> food(16, 0.6F);
      case "orange" -> food(5, 0.6F);
      case "rotten_orange", "rotten_melon_slice", "chips", "ranch_chips", "cheesy_chips",
          "salted_chips", "popcorn", "raspberry" -> food(3, 0.6F);
      case "rotten_pear", "rotten_apple", "rotten_blueberry", "rotten_raspberry" -> food(1, 0.6F);
      case "noodles" -> food(6, 0.6F);
      default -> null;
    };
  }

  private static FoodProperties food(int nutrition, float saturation) {
    return new FoodProperties.Builder()
        .nutrition(nutrition)
        .saturationModifier(saturation)
        .build();
  }

  private static int waterValue(String name) {
    return switch (name) {
      case "water_bottle" -> 8;
      case "water_canteen" -> 9;
      case "flask" -> 7;
      case "milk_carton", "sprite", "cola" -> 6;
      case "iced_tea", "iron_brew", "juice_pouch", "lemon_soda", "orange_soda", "pepe_soda",
          "zombie_energy" -> 4;
      case "mre" -> 7;
      case "open_canned_custard" -> 3;
      case "open_canned_tomato_soup" -> 2;
      case "open_canned_peaches", "blueberry", "orange", "pear", "raspberry" -> 1;
      default -> 0;
    };
  }

  private static String emptyItemName(String name) {
    return switch (name) {
      case "water_bottle" -> "empty_water_bottle";
      case "water_canteen" -> "empty_water_canteen";
      case "flask" -> "empty_flask";
      case "iced_tea" -> "empty_iced_tea";
      case "iron_brew" -> "empty_iron_brew";
      case "juice_pouch" -> "empty_juice_pouch";
      case "lemon_soda" -> "empty_lemon_soda";
      case "milk_carton" -> "empty_milk_carton";
      case "orange_soda" -> "empty_orange_soda";
      case "pepe_soda" -> "empty_pepe_soda";
      case "sprite" -> "empty_sprite";
      case "cola" -> "empty_cola";
      case "zombie_energy" -> "empty_zombie_energy";
      default -> null;
    };
  }

  private static String filledItemName(String name) {
    return switch (name) {
      case "empty_water_bottle" -> "water_bottle";
      case "empty_water_canteen" -> "water_canteen";
      case "empty_flask" -> "flask";
      default -> null;
    };
  }

  private static Item medicalItem(String name) {
    return switch (name) {
      case "splint" -> new ActionItem(ActionTypes.USE_SPLINT, new Item.Properties().stacksTo(3));
      case "morphine_syringe" -> new ActionItem(ActionTypes.USE_MORPHINE_SYRINGE,
          new Item.Properties().stacksTo(3));
      case "cure_syringe" -> new ActionItem(ActionTypes.USE_CURE_SYRINGE,
          new Item.Properties().stacksTo(3));
      case "rbi_syringe" -> new ActionItem(ActionTypes.USE_RBI_SYRINGE,
          new Item.Properties().stacksTo(3));
      default -> null;
    };
  }

  private SurvivalItems() {}
}

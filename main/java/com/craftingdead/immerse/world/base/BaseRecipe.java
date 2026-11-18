package com.craftingdead.immerse.world.base;

import com.craftingdead.core.world.item.ModItems;
import com.craftingdead.immerse.world.item.ImmerseItems;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.craftingdead.immerse.world.level.block.entity.BaseCenterBlockEntity;

/** TECU-style base recipes which consume resources across the whole player inventory. */
public record BaseRecipe(String id, String output, List<Cost> costs, Domain domain) {

  public enum Domain { PLAYER, BASE }

  public record Cost(Item item, int count) {}

  public static final String CORE = "base_center";
  private static final Map<String, BaseRecipe> RECIPES = new LinkedHashMap<>();

  static {
    registerPlayer(CORE, "base_center_blueprint",
        cost(ImmerseItems.BLANK_BLUEPRINT, 1),
        cost("stone_pebble", 200),
        cost("split_log", 90),
        new Cost(ModItems.SCRAP_METAL.get(), 45));
    registerPlayer("barbed_wire", "barbed_wire_wall_blueprint",
        cost(ImmerseItems.BLANK_BLUEPRINT, 1), new Cost(ModItems.SCRAP_METAL.get(), 15));
    register("wood_block", "wood_block_blueprint", cost("split_log", 5));
    register("wood_wall", "wood_wall_blueprint", cost("split_log", 45));
    register("wood_door_wall", "wood_door_wall_blueprint", cost("split_log", 50));
    register("wood_platform", "wood_platform_blueprint", cost("split_log", 45));
    register("wood_slab", "wood_slab_blueprint", cost("split_log", 3));
    register("wood_stairs", "wood_stairs_blueprint", cost("split_log", 5));
    register("stone_block", "stone_block_blueprint", cost("stone_pebble", 10));
    register("stone_wall", "stone_wall_blueprint", cost("stone_pebble", 90));
    register("stone_door_wall", "stone_door_wall_blueprint",
        cost("stone_pebble", 70), cost("split_log", 15));
    register("stone_platform", "stone_platform_blueprint", cost("stone_pebble", 90));
    register("stone_slab", "stone_slab_blueprint", cost("stone_pebble", 6));
    register("stone_stairs", "stone_stairs_blueprint", cost("stone_pebble", 10));
    register("chest", "chest_blueprint", cost("split_log", 70));
    register("campfire", "campfire_blueprint", cost("split_log", 40), cost("stone_pebble", 15));
    register("wired_fence", "wired_fence_blueprint",
        cost("split_log", 30), new Cost(ModItems.SCRAP_METAL.get(), 15));
    for (String material : List.of("metal", "concrete")) {
      for (String shape : List.of("block", "wall", "door_wall", "platform", "slab", "stairs")) {
        int units = shape.equals("wall") || shape.equals("platform") ? 9 : shape.equals("door_wall") ? 7 : 1;
        register(material + "_" + shape, material + "_" + shape + "_blueprint",
            new Cost(ModItems.SCRAP_METAL.get(), units * (material.equals("metal") ? 10 : 5)),
            cost("stone_pebble", material.equals("concrete") ? units * 20 : 0));
      }
    }
    for (String name : List.of("wood_door", "wood_fence", "wood_fence_gate", "wood_sign",
        "siege_ladder", "sleeping_bag")) register(name, name + "_blueprint", cost("split_log", 30));
    register("metal_door", "metal_door_blueprint", new Cost(ModItems.SCRAP_METAL.get(), 40));
    register("steel_door", "steel_door_blueprint", new Cost(ModItems.SCRAP_METAL.get(), 80));
    register("furnace", "furnace_blueprint", cost("stone_pebble", 80));
    register("sandbag_slab", "sandbag_slab_blueprint", cost("stone_pebble", 15));
    register("reinforced_barbed_wire", "reinforced_barbed_wire_blueprint", new Cost(ModItems.SCRAP_METAL.get(), 35));
    register("grinder", "grinder_blueprint", new Cost(ModItems.SCRAP_METAL.get(), 100), cost("stone_pebble", 50));
    register("water_pump", "water_pump_blueprint", new Cost(ModItems.SCRAP_METAL.get(), 60), cost("stone_pebble", 30));
    register("cfm_stripped_oak_mail_box", "cfm_stripped_oak_mail_box_blueprint", cost("split_log", 35));
    register("cfm_stripped_spruce_chair", "cfm_stripped_spruce_chair_blueprint", cost("split_log", 20));
    for (String blueprint : ImmerseItems.BLUEPRINT_NAMES) {
      if (RECIPES.values().stream().noneMatch(recipe -> recipe.output().equals(blueprint))) {
        String material = blueprint.contains("stone") || blueprint.contains("glass") ? "stone_pebble" : "split_log";
        register(blueprint.replace("_blueprint", ""), blueprint, cost(material, 45));
      }
    }
  }

  private static Cost cost(String item, int count) {
    return new Cost(ImmerseItems.ITEMS.get(item).get(), count);
  }

  private static void register(String id, String output, Cost... costs) {
    RECIPES.put(id, new BaseRecipe(id, output, java.util.Arrays.stream(costs)
        .filter(cost -> cost.count() > 0).toList(), Domain.BASE));
  }

  private static void registerPlayer(String id, String output, Cost... costs) {
    RECIPES.put(id, new BaseRecipe(id, output, List.of(costs), Domain.PLAYER));
  }

  public static Map<String, BaseRecipe> recipes() {
    return Map.copyOf(RECIPES);
  }

  public static BaseRecipe get(String id) {
    return RECIPES.get(id);
  }

  public static List<BaseRecipe> playerRecipes() {
    return RECIPES.values().stream().filter(recipe -> recipe.domain == Domain.PLAYER).toList();
  }

  public static List<BaseRecipe> baseRecipes() {
    return RECIPES.values().stream().filter(recipe -> recipe.domain == Domain.BASE).toList();
  }

  public int count(Player player, Item item) {
    int total = 0;
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (stack.is(item)) {
        total += stack.getCount();
      }
    }
    return total;
  }

  public int count(Player player, Item item, BaseCenterBlockEntity baseCenter) {
    return this.count(player, item) + (baseCenter == null ? 0 : baseCenter.getStoredCount(item));
  }

  public boolean canCraft(Player player) {
    return player.getAbilities().instabuild
        || this.costs.stream().allMatch(cost -> count(player, cost.item()) >= cost.count());
  }

  public boolean canCraft(Player player, BaseCenterBlockEntity baseCenter) {
    return player.getAbilities().instabuild || this.costs.stream()
        .allMatch(cost -> count(player, cost.item(), baseCenter) >= cost.count());
  }

  public boolean craft(Player player) {
    if (player == null || !canCraft(player)) {
      return false;
    }
    if (!player.getAbilities().instabuild) {
      for (Cost cost : this.costs) {
        remove(player, cost.item(), cost.count());
      }
    }
    ItemStack outputStack = new ItemStack(ImmerseItems.ITEMS.get(this.output).get());
    if (!player.addItem(outputStack)) {
      player.drop(outputStack, false);
    }
    player.getInventory().setChanged();
    return true;
  }

  public boolean craft(Player player, BaseCenterBlockEntity baseCenter) {
    if (player == null || !this.canCraft(player, baseCenter)) {
      return false;
    }
    if (!player.getAbilities().instabuild) {
      for (Cost cost : this.costs) {
        int fromStorage = baseCenter == null ? 0
            : baseCenter.consumeStored(cost.item(), cost.count());
        remove(player, cost.item(), cost.count() - fromStorage);
      }
    }
    ItemStack outputStack = new ItemStack(ImmerseItems.ITEMS.get(this.output).get());
    if (!player.addItem(outputStack)) {
      player.drop(outputStack, false);
    }
    player.getInventory().setChanged();
    return true;
  }

  private static void remove(Player player, Item item, int amount) {
    int remaining = amount;
    for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (!stack.is(item)) {
        continue;
      }
      int removed = Math.min(stack.getCount(), remaining);
      stack.shrink(removed);
      remaining -= removed;
    }
  }
}

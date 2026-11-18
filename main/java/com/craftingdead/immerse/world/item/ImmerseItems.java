package com.craftingdead.immerse.world.item;

import com.craftingdead.immerse.CraftingDeadImmerse;
import com.craftingdead.immerse.world.action.ImmerseActionTypes;
import com.craftingdead.immerse.world.level.block.ImmerseBlocks;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ImmerseItems {

  public static final DeferredRegister<Item> deferredRegister =
      DeferredRegister.create(Registries.ITEM, CraftingDeadImmerse.ID);

  public static final DeferredRegister<CreativeModeTab> tabDeferredRegister =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CraftingDeadImmerse.ID);

  public static final String BLANK_BLUEPRINT = "blank_blueprint";
  public static final String SPLIT_LOG = "split_log";
  public static final String STONE_PEBBLE = "stone_pebble";

  public static final List<String> BLUEPRINT_NAMES = List.of(
      "acacia_door_blueprint",
      "acacia_plank_platform_blueprint",
      "acacia_plank_wall_blueprint",
      "base_center_blueprint",
      "birch_door_blueprint",
      "birch_plank_platform_blueprint",
      "birch_plank_wall_blueprint",
      "barbed_wire_wall_blueprint",
      "campfire_blueprint",
      "chest_blueprint",
      "cobblestone_wall_blueprint",
      "dark_oak_door_blueprint",
      "dark_oak_plank_platform_blueprint",
      "dark_oak_plank_wall_blueprint",
      "jungle_door_blueprint",
      "jungle_plank_platform_blueprint",
      "jungle_plank_wall_blueprint",
      "glass_wall_blueprint",
      "oak_door_blueprint",
      "oak_plank_platform_blueprint",
      "oak_plank_wall_blueprint",
      "spruce_door_blueprint",
      "spruce_plank_platform_blueprint",
      "spruce_plank_wall_blueprint",
      "stone_brick_platform_blueprint",
      "stone_brick_wall_blueprint",
      "wood_block_blueprint",
      "wood_wall_blueprint",
      "wood_door_wall_blueprint",
      "wood_platform_blueprint",
      "wood_slab_blueprint",
      "wood_stairs_blueprint",
      "stone_block_blueprint",
      "stone_wall_blueprint",
      "stone_door_wall_blueprint",
      "stone_platform_blueprint",
      "stone_slab_blueprint",
      "stone_stairs_blueprint",
      "metal_block_blueprint",
      "metal_wall_blueprint",
      "metal_door_wall_blueprint",
      "metal_platform_blueprint",
      "metal_slab_blueprint",
      "metal_stairs_blueprint",
      "concrete_block_blueprint",
      "concrete_wall_blueprint",
      "concrete_door_wall_blueprint",
      "concrete_platform_blueprint",
      "concrete_slab_blueprint",
      "concrete_stairs_blueprint",
      "wood_door_blueprint",
      "metal_door_blueprint",
      "steel_door_blueprint",
      "furnace_blueprint",
      "wood_fence_blueprint",
      "wood_fence_gate_blueprint",
      "wood_sign_blueprint",
      "wired_fence_blueprint",
      "reinforced_barbed_wire_blueprint",
      "sandbag_slab_blueprint",
      "siege_ladder_blueprint",
      "sleeping_bag_blueprint",
      "grinder_blueprint",
      "water_pump_blueprint",
      "cfm_stripped_oak_mail_box_blueprint",
      "cfm_stripped_spruce_chair_blueprint");

  public static final Map<String, Supplier<Item>> ITEMS = new LinkedHashMap<>();

  static {
    for (var entry : ImmerseBlocks.BLOCKS.entrySet()) {
      ITEMS.put(entry.getKey(), deferredRegister.register(entry.getKey(),
          () -> new BlockItem(entry.getValue().get(), new Item.Properties())));
    }
    ITEMS.put(BLANK_BLUEPRINT, deferredRegister.register(BLANK_BLUEPRINT,
        () -> new Item(new Item.Properties().stacksTo(16))));
    ITEMS.put(SPLIT_LOG, deferredRegister.register(SPLIT_LOG,
        () -> new Item(new Item.Properties())));
    ITEMS.put(STONE_PEBBLE, deferredRegister.register(STONE_PEBBLE,
        () -> new Item(new Item.Properties())));
    for (String name : BLUEPRINT_NAMES) {
      ITEMS.put(name, deferredRegister.register(name,
          () -> new BuildBlueprintItem(name, ImmerseActionTypes.getBuildAction(name),
              new Item.Properties().stacksTo(16))));
    }
  }

  public static final Supplier<CreativeModeTab> TAB = tabDeferredRegister.register("main",
      () -> CreativeModeTab.builder()
          .title(Component.translatable("itemGroup.craftingdeadimmerse"))
          .icon(ImmerseItems::createTabIcon)
          .displayItems((parameters, output) -> ITEMS.values().forEach(item -> output.accept(item.get())))
          .build());

  private static ItemStack createTabIcon() {
    return new ItemStack(ITEMS.getOrDefault("base_center", ITEMS.values().iterator().next()).get());
  }

  public static ItemInteractionResult craftBlueprintAtBase(Level level, Player player,
      ItemStack blankBlueprint) {
    String blueprintName = getBlueprintForIngredient(player.getOffhandItem());
    if (blueprintName == null) {
      if (!level.isClientSide()) {
        player.displayClientMessage(Component.translatable(
            "message.craftingdeadimmerse.base_blueprint_missing_material"), true);
      }
      return ItemInteractionResult.SUCCESS;
    }

    if (!level.isClientSide()) {
      ItemStack ingredient = player.getOffhandItem();
      ItemStack result = new ItemStack(ITEMS.get(blueprintName).get());
      if (!player.getAbilities().instabuild) {
        blankBlueprint.shrink(1);
        ingredient.shrink(1);
      }
      if (!player.addItem(result)) {
        player.drop(result, false);
      }
      player.displayClientMessage(Component.translatable(
          "message.craftingdeadimmerse.base_blueprint_crafted", result.getHoverName()), true);
    }
    return ItemInteractionResult.SUCCESS;
  }

  public static String getBlueprintForIngredient(ItemStack ingredient) {
    if (ingredient.isEmpty()) {
      return null;
    }
    Item item = ingredient.getItem();
    if (item == Items.CAMPFIRE) {
      return "campfire_blueprint";
    }
    if (item == Items.CHEST) {
      return "chest_blueprint";
    }
    if (item == ImmerseBlocks.BLOCKS.get("barbed_wire").get().asItem()
        || item == Items.IRON_BARS) {
      return "barbed_wire_wall_blueprint";
    }
    if (item == Items.STONE_BRICKS) {
      return "stone_brick_wall_blueprint";
    }
    if (item == Items.STONE_BRICK_SLAB) {
      return "stone_brick_platform_blueprint";
    }
    if (item == Items.GLASS) {
      return "glass_wall_blueprint";
    }
    if (item == Items.COBBLESTONE) {
      return "cobblestone_wall_blueprint";
    }
    if (item == Items.OAK_PLANKS) {
      return "oak_plank_wall_blueprint";
    }
    if (item == Items.SPRUCE_PLANKS) {
      return "spruce_plank_wall_blueprint";
    }
    if (item == Items.BIRCH_PLANKS) {
      return "birch_plank_wall_blueprint";
    }
    if (item == Items.JUNGLE_PLANKS) {
      return "jungle_plank_wall_blueprint";
    }
    if (item == Items.ACACIA_PLANKS) {
      return "acacia_plank_wall_blueprint";
    }
    if (item == Items.DARK_OAK_PLANKS) {
      return "dark_oak_plank_wall_blueprint";
    }
    if (item == Items.OAK_SLAB) {
      return "oak_plank_platform_blueprint";
    }
    if (item == Items.SPRUCE_SLAB) {
      return "spruce_plank_platform_blueprint";
    }
    if (item == Items.BIRCH_SLAB) {
      return "birch_plank_platform_blueprint";
    }
    if (item == Items.JUNGLE_SLAB) {
      return "jungle_plank_platform_blueprint";
    }
    if (item == Items.ACACIA_SLAB) {
      return "acacia_plank_platform_blueprint";
    }
    if (item == Items.DARK_OAK_SLAB) {
      return "dark_oak_plank_platform_blueprint";
    }
    if (item == Items.OAK_DOOR) {
      return "oak_door_blueprint";
    }
    if (item == Items.SPRUCE_DOOR) {
      return "spruce_door_blueprint";
    }
    if (item == Items.BIRCH_DOOR) {
      return "birch_door_blueprint";
    }
    if (item == Items.JUNGLE_DOOR) {
      return "jungle_door_blueprint";
    }
    if (item == Items.ACACIA_DOOR) {
      return "acacia_door_blueprint";
    }
    if (item == Items.DARK_OAK_DOOR) {
      return "dark_oak_door_blueprint";
    }
    return null;
  }

  private ImmerseItems() {}
}

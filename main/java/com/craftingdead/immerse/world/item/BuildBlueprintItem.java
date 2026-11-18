package com.craftingdead.immerse.world.item;

import com.craftingdead.core.world.item.ActionItem;
import com.craftingdead.core.world.action.item.BlockItemActionType;
import com.craftingdead.immerse.world.base.BaseSecurity;
import com.craftingdead.immerse.world.level.block.ImmerseBlocks;
import com.craftingdead.immerse.world.level.block.entity.BaseCenterBlockEntity;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

public class BuildBlueprintItem extends ActionItem {

  private static final Map<String, Block> WOOD_BLOCKS = Map.of(
      "oak", Blocks.OAK_PLANKS,
      "spruce", Blocks.SPRUCE_PLANKS,
      "birch", Blocks.BIRCH_PLANKS,
      "jungle", Blocks.JUNGLE_PLANKS,
      "acacia", Blocks.ACACIA_PLANKS,
      "dark_oak", Blocks.DARK_OAK_PLANKS);

  private static final Map<String, Block> SLAB_BLOCKS = Map.of(
      "oak", Blocks.OAK_SLAB,
      "spruce", Blocks.SPRUCE_SLAB,
      "birch", Blocks.BIRCH_SLAB,
      "jungle", Blocks.JUNGLE_SLAB,
      "acacia", Blocks.ACACIA_SLAB,
      "dark_oak", Blocks.DARK_OAK_SLAB);

  private static final Map<String, Block> DOOR_BLOCKS = Map.of(
      "oak", Blocks.OAK_DOOR,
      "spruce", Blocks.SPRUCE_DOOR,
      "birch", Blocks.BIRCH_DOOR,
      "jungle", Blocks.JUNGLE_DOOR,
      "acacia", Blocks.ACACIA_DOOR,
      "dark_oak", Blocks.DARK_OAK_DOOR);

  private final String blueprintName;

  public record Placement(BlockPos pos, BlockState state, boolean replaceable) {}

  public BuildBlueprintItem(String blueprintName,
      Supplier<? extends BlockItemActionType> itemActionType, Properties properties) {
    super(itemActionType, properties);
    this.blueprintName = blueprintName;
  }

  public String getBlueprintName() {
    return this.blueprintName;
  }

  @Override
  public void initializeClient(java.util.function.Consumer<
      net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
    consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
      @Override
      public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return com.craftingdead.immerse.client.renderer.BlueprintItemRenderer.getInstance();
      }
    });
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    if (!canBuild(this.blueprintName, context)) {
      if (!context.getLevel().isClientSide() && context.getPlayer() != null) {
        context.getPlayer().displayClientMessage(getCannotBuildMessage(this.blueprintName, context),
            true);
      }
      return InteractionResult.PASS;
    }
    return super.useOn(context);
  }

  public static boolean build(String blueprintName, UseOnContext context) {
    if (!canBuild(blueprintName, context)) {
      return false;
    }

    Level level = context.getLevel();
    List<Placement> placements = getPlacements(blueprintName, context);
    Map<BlockPos, BlockState> previousStates = new LinkedHashMap<>();
    for (Placement placement : placements) {
      previousStates.put(placement.pos(), level.getBlockState(placement.pos()));
      if (!level.setBlock(placement.pos(), placement.state(), Block.UPDATE_ALL)) {
        previousStates.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_ALL));
        return false;
      }
      if ("base_center_blueprint".equals(blueprintName)
          && context.getPlayer() != null
          && level.getBlockEntity(placement.pos()) instanceof BaseCenterBlockEntity baseCenter) {
        baseCenter.setOwner(context.getPlayer());
      }
    }
    return true;
  }

  public static boolean canBuild(String blueprintName, UseOnContext context) {
    if (context == null || context.getLevel() == null || getTargetBlock(blueprintName) == null) {
      return false;
    }
    if ("base_center_blueprint".equals(blueprintName)
        && !BaseSecurity.canPlaceBaseCenter(context.getLevel(), getBuildPos(context),
            context.getPlayer())) {
      return false;
    }
    for (Placement placement : getPlacements(blueprintName, context)) {
      if (!"base_center_blueprint".equals(blueprintName)
          && !BaseSecurity.canBuildBlueprint(context.getLevel(), placement.pos(),
              context.getPlayer())) {
        return false;
      }
      if (!placement.replaceable()) {
        return false;
      }
    }
    return true;
  }

  private static Component getCannotBuildMessage(String blueprintName, UseOnContext context) {
    if ("base_center_blueprint".equals(blueprintName)
        && BaseSecurity.isBlueprintPlacementRestricted(context.getLevel(),
            getBuildPos(context), context.getPlayer())) {
      return Component.translatable("message.craftingdeadimmerse.blueprint_region_forbidden");
    }
    if ("base_center_blueprint".equals(blueprintName)
        && BaseSecurity.hasRegisteredBase(context.getLevel(), context.getPlayer())) {
      return Component.translatable("message.craftingdeadimmerse.base_already_owned");
    }
    if ("base_center_blueprint".equals(blueprintName)
        && BaseSecurity.findOverlappingBase(context.getLevel(), getBuildPos(context)).isPresent()) {
      return Component.translatable("message.craftingdeadimmerse.base_overlaps");
    }
    if ("base_center_blueprint".equals(blueprintName)) {
      return Component.translatable("message.craftingdeadimmerse.base_place_denied");
    }
    return Component.translatable("message.craftingdeadimmerse.blueprint_place_denied");
  }

  public static List<Placement> getPlacements(String blueprintName, UseOnContext context) {
    if (context == null || context.getLevel() == null || getTargetBlock(blueprintName) == null) {
      return List.of();
    }
    return getPlacements(blueprintName, context.getLevel(), getBuildPos(context),
        context.getHorizontalDirection().getOpposite());
  }

  public static List<Placement> getPlacements(String blueprintName, Level level, BlockPos origin,
      Direction facing) {
    if (level == null || origin == null || facing == null || getTargetBlock(blueprintName) == null) {
      return List.of();
    }

    Block block = getTargetBlock(blueprintName);
    List<Placement> placements = new ArrayList<>();
    if (isWallBlueprint(blueprintName)) {
      Direction side = facing.getClockWise();
      BlockState state = block.defaultBlockState();
      for (int x = -1; x <= 1; x++) {
        for (int y = 0; y <= 2; y++) {
          addPlacement(level, placements, origin.relative(side, x).above(y), state);
        }
      }
      return placements;
    }

    if (isPlatformBlueprint(blueprintName)) {
      Direction side = facing.getClockWise();
      BlockState state = block.defaultBlockState();
      if (state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
        state = state.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
      }
      for (int forward = -1; forward <= 1; forward++) {
        for (int x = -1; x <= 1; x++) {
          addPlacement(level, placements, origin.relative(facing, forward).relative(side, x), state);
        }
      }
      return placements;
    }

    if (block instanceof DoorBlock) {
      Direction side = facing.getClockWise();
      BlockState wall = getDoorWallBlock(blueprintName).defaultBlockState();
      for (int x = -1; x <= 1; x++) {
        for (int y = 0; y <= 2; y++) {
          if (!List.of("wood_door_blueprint", "metal_door_blueprint", "steel_door_blueprint").contains(blueprintName)
              && (x != 0 || y == 2)) {
            addPlacement(level, placements, origin.relative(side, x).above(y), wall);
          }
        }
      }
      BlockState lower = block.defaultBlockState()
          .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
          .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
      BlockState upper = lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
      addPlacement(level, placements, origin, lower);
      addPlacement(level, placements, origin.above(), upper);
      return placements;
    }

    if (block instanceof net.minecraft.world.level.block.BedBlock) {
      var foot = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
          .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT);
      addPlacement(level, placements, origin, foot);
      addPlacement(level, placements, origin.relative(facing), foot.setValue(
          net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
      return placements;
    }
    BlockState state = block.defaultBlockState();
    if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
      state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
    }
    addPlacement(level, placements, origin, state);
    return placements;
  }

  private static void addPlacement(Level level, List<Placement> placements, BlockPos pos,
      BlockState state) {
    placements.add(new Placement(pos, state,
        level.isInWorldBounds(pos) && canReplace(level, pos)));
  }

  private static boolean canReplace(Level level, BlockPos pos) {
    BlockState state = level.getBlockState(pos);
    return state.isAir() || state.canBeReplaced();
  }

  private static BlockPos getBuildPos(UseOnContext context) {
    BlockPos clickedPos = context.getClickedPos();
    return canReplace(context.getLevel(), clickedPos)
        ? clickedPos
        : clickedPos.relative(context.getClickedFace());
  }

  private static boolean isWallBlueprint(String blueprintName) {
    return blueprintName != null
        && blueprintName.endsWith("_wall_blueprint")
        && !blueprintName.endsWith("_door_wall_blueprint")
        && !"barbed_wire_wall_blueprint".equals(blueprintName);
  }

  private static boolean isPlatformBlueprint(String blueprintName) {
    return blueprintName != null && blueprintName.endsWith("_platform_blueprint");
  }

  private static Block getDoorWallBlock(String blueprintName) {
    if (blueprintName.startsWith("metal_")) return ImmerseBlocks.BLOCKS.get("metal_block").get();
    if (blueprintName.startsWith("concrete_")) return ImmerseBlocks.BLOCKS.get("concrete_block").get();
    if ("wood_door_wall_blueprint".equals(blueprintName)) {
      return ImmerseBlocks.BLOCKS.get("wood_block").get();
    }
    if ("stone_door_wall_blueprint".equals(blueprintName)) {
      return ImmerseBlocks.BLOCKS.get("stone_block").get();
    }
    if (blueprintName != null) {
      for (var entry : WOOD_BLOCKS.entrySet()) {
        if (blueprintName.startsWith(entry.getKey() + "_door_")) {
          return entry.getValue();
        }
      }
    }
    return Blocks.OAK_PLANKS;
  }

  public static Block getTargetBlock(String blueprintName) {
    if (blueprintName == null) return null;
    for (String material : List.of("metal", "concrete")) {
      if (blueprintName.equals(material + "_door_wall_blueprint")) return Blocks.IRON_DOOR;
      for (String shape : List.of("block", "wall", "slab", "platform", "stairs")) {
        if (blueprintName.equals(material + "_" + shape + "_blueprint"))
          return ImmerseBlocks.BLOCKS.get(material + "_" + (shape.equals("wall") ? "block"
              : shape.equals("platform") ? "slab" : shape)).get();
      }
    }
    Block extra = switch (blueprintName) {
      case "grinder_blueprint", "water_pump_blueprint", "cfm_stripped_oak_mail_box_blueprint",
          "cfm_stripped_spruce_chair_blueprint" -> ImmerseBlocks.BLOCKS.get(blueprintName.replace("_blueprint", "")).get();
      case "wood_door_blueprint" -> Blocks.OAK_DOOR;
      case "metal_door_blueprint", "steel_door_blueprint" -> Blocks.IRON_DOOR;
      case "furnace_blueprint" -> Blocks.FURNACE;
      case "wood_fence_blueprint" -> Blocks.OAK_FENCE;
      case "wood_fence_gate_blueprint" -> Blocks.OAK_FENCE_GATE;
      case "wood_sign_blueprint" -> Blocks.OAK_SIGN;
      case "wired_fence_blueprint" -> Blocks.IRON_BARS;
      case "reinforced_barbed_wire_blueprint" -> ImmerseBlocks.BLOCKS.get("reinforced_barbed_wire").get();
      case "sandbag_slab_blueprint" -> ImmerseBlocks.BLOCKS.get("sandbag_slab").get();
      case "siege_ladder_blueprint" -> Blocks.LADDER;
      case "sleeping_bag_blueprint" -> Blocks.RED_BED;
      default -> null;
    };
    if (extra != null) return extra;
    if ("base_center_blueprint".equals(blueprintName)) {
      return ImmerseBlocks.BLOCKS.get("base_center").get();
    }
    if ("campfire_blueprint".equals(blueprintName)) {
      return Blocks.CAMPFIRE;
    }
    if ("chest_blueprint".equals(blueprintName)) {
      return Blocks.CHEST;
    }
    if ("barbed_wire_wall_blueprint".equals(blueprintName)) {
      return ImmerseBlocks.BLOCKS.get("barbed_wire").get();
    }
    if ("stone_brick_wall_blueprint".equals(blueprintName)) {
      return Blocks.STONE_BRICKS;
    }
    if ("stone_brick_platform_blueprint".equals(blueprintName)) {
      return Blocks.STONE_BRICK_SLAB;
    }
    if ("glass_wall_blueprint".equals(blueprintName)) {
      return Blocks.GLASS;
    }
    if ("cobblestone_wall_blueprint".equals(blueprintName)) {
      return Blocks.COBBLESTONE;
    }
    if ("wood_block_blueprint".equals(blueprintName)
        || "wood_wall_blueprint".equals(blueprintName)) {
      return ImmerseBlocks.BLOCKS.get("wood_block").get();
    }
    if ("wood_platform_blueprint".equals(blueprintName)) {
      return ImmerseBlocks.BLOCKS.get("wood_slab").get();
    }
    if ("wood_slab_blueprint".equals(blueprintName)) {
      return ImmerseBlocks.BLOCKS.get("wood_slab").get();
    }
    if ("wood_stairs_blueprint".equals(blueprintName)) {
      return ImmerseBlocks.BLOCKS.get("wood_stairs").get();
    }
    if ("wood_door_wall_blueprint".equals(blueprintName)) {
      return Blocks.OAK_DOOR;
    }
    if ("stone_block_blueprint".equals(blueprintName)
        || "stone_wall_blueprint".equals(blueprintName)) {
      return ImmerseBlocks.BLOCKS.get("stone_block").get();
    }
    if ("stone_platform_blueprint".equals(blueprintName)) {
      return ImmerseBlocks.BLOCKS.get("stone_slab").get();
    }
    if ("stone_slab_blueprint".equals(blueprintName)) {
      return ImmerseBlocks.BLOCKS.get("stone_slab").get();
    }
    if ("stone_stairs_blueprint".equals(blueprintName)) {
      return ImmerseBlocks.BLOCKS.get("stone_stairs").get();
    }
    if ("stone_door_wall_blueprint".equals(blueprintName)) {
      return Blocks.IRON_DOOR;
    }
    for (String wood : WOOD_BLOCKS.keySet()) {
      if ((wood + "_plank_wall_blueprint").equals(blueprintName)) {
        return WOOD_BLOCKS.get(wood);
      }
      if ((wood + "_plank_platform_blueprint").equals(blueprintName)) {
        return SLAB_BLOCKS.get(wood);
      }
      if ((wood + "_door_blueprint").equals(blueprintName)) {
        return DOOR_BLOCKS.get(wood);
      }
    }
    return null;
  }
}

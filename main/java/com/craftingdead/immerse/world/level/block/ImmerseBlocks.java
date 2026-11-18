package com.craftingdead.immerse.world.level.block;

import com.craftingdead.immerse.CraftingDeadImmerse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ImmerseBlocks {

  public static final DeferredRegister<Block> deferredRegister =
      DeferredRegister.create(Registries.BLOCK, CraftingDeadImmerse.ID);

  private static final List<String> BLOCK_NAMES = List.of(
      "base_center");

  public static final Map<String, Supplier<Block>> BLOCKS = new LinkedHashMap<>();

  static {
    for (String utility : List.of("water_pump", "grinder", "cfm_stripped_oak_mail_box"))
      BLOCKS.put(utility, deferredRegister.register(utility,
          () -> new BaseUtilityBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().strength(8F))));
    BLOCKS.put("cfm_stripped_spruce_chair", deferredRegister.register("cfm_stripped_spruce_chair",
        () -> new StairBlock(Blocks.SPRUCE_PLANKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_STAIRS))));
    for (String name : BLOCK_NAMES) {
      BLOCKS.put(name, deferredRegister.register(name,
          () -> new BaseCenterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
              .noOcclusion())));
    }
    BLOCKS.put("barbed_wire", deferredRegister.register("barbed_wire",
        () -> new BarbedWireBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBWEB)
            .strength(2.0F)
            .noCollission()
            .noOcclusion())));
    BLOCKS.put("wood_block", deferredRegister.register("wood_block",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(8.0F))));
    BLOCKS.put("wood_slab", deferredRegister.register("wood_slab",
        () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB).strength(8.0F))));
    BLOCKS.put("wood_stairs", deferredRegister.register("wood_stairs",
        () -> new StairBlock(BLOCKS.get("wood_block").get().defaultBlockState(),
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS).strength(8.0F))));
    for (String material : List.of("metal", "concrete")) {
      Block template = material.equals("metal") ? Blocks.IRON_BLOCK : Blocks.GRAY_CONCRETE;
      float strength = material.equals("metal") ? 25F : 35F;
      BLOCKS.put(material + "_block", deferredRegister.register(material + "_block",
          () -> new Block(BlockBehaviour.Properties.ofFullCopy(template).strength(strength))));
      BLOCKS.put(material + "_slab", deferredRegister.register(material + "_slab",
          () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(template).strength(strength))));
      BLOCKS.put(material + "_stairs", deferredRegister.register(material + "_stairs",
          () -> new StairBlock(BLOCKS.get(material + "_block").get().defaultBlockState(),
              BlockBehaviour.Properties.ofFullCopy(template).strength(strength))));
    }
    BLOCKS.put("sandbag_slab", deferredRegister.register("sandbag_slab",
        () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SANDSTONE).strength(12F))));
    BLOCKS.put("reinforced_barbed_wire", deferredRegister.register("reinforced_barbed_wire",
        () -> new BarbedWireBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBWEB)
            .strength(12F).noCollission().noOcclusion())));
    BLOCKS.put("stone_block", deferredRegister.register("stone_block",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).strength(15.0F))));
    BLOCKS.put("stone_slab", deferredRegister.register("stone_slab",
        () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_SLAB)
            .strength(15.0F))));
    BLOCKS.put("stone_stairs", deferredRegister.register("stone_stairs",
        () -> new StairBlock(BLOCKS.get("stone_block").get().defaultBlockState(),
            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_STAIRS).strength(15.0F))));
  }

  private ImmerseBlocks() {}
}

package com.craftingdead.survival.world.level.block;

import com.craftingdead.survival.CraftingDeadSurvival;
import com.craftingdead.survival.particles.SurvivalParticleTypes;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SurvivalBlocks {

  public static final DeferredRegister<Block> deferredRegister =
      DeferredRegister.create(Registries.BLOCK, CraftingDeadSurvival.ID);

  private static final List<String> BLOCK_NAMES = List.of(
      "civilian_loot",
      "civilian_loot_gen",
      "civilian_rare_loot",
      "civilian_rare_loot_gen",
      "medic_loot",
      "medic_loot_gen",
      "military_loot",
      "military_loot_gen",
      "police_loot",
      "police_loot_gen");

  public static final Map<String, Supplier<Block>> BLOCKS = new LinkedHashMap<>();

  static {
    for (String name : BLOCK_NAMES) {
      BLOCKS.put(name, deferredRegister.register(name, () -> createBlock(name)));
    }
  }

  private static Block createBlock(String name) {
    if (name.endsWith("_gen")) {
      var lootName = name.substring(0, name.length() - 4);
      return new LootGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRUCTURE_VOID)
          .strength(5.0F)
          .noCollission()
          .noOcclusion(),
          () -> BLOCKS.get(lootName).get(),
          () -> particleType(name).get(),
          refreshDelayTicks(name));
    }
    return new LootBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST)
        .strength(5.0F)
        .noOcclusion());
  }

  private static int refreshDelayTicks(String name) {
    return switch (name) {
      case "medic_loot_gen" -> 20 * 60 * 6;
      case "police_loot_gen" -> 20 * 60 * 8;
      case "civilian_rare_loot_gen" -> 20 * 60 * 9;
      case "military_loot_gen" -> 20 * 60 * 10;
      default -> 20 * 60 * 5;
    };
  }

  private static Supplier<? extends net.minecraft.core.particles.ParticleOptions> particleType(
      String name) {
    return switch (name) {
      case "military_loot_gen" -> SurvivalParticleTypes.MILITARY_LOOT_GEN;
      case "medic_loot_gen" -> SurvivalParticleTypes.MEDIC_LOOT_GEN;
      case "civilian_loot_gen" -> SurvivalParticleTypes.CIVILIAN_LOOT_GEN;
      case "civilian_rare_loot_gen" -> SurvivalParticleTypes.CIVILIAN_RARE_LOOT_GEN;
      case "police_loot_gen" -> SurvivalParticleTypes.POLICE_LOOT_GEN;
      default -> () -> null;
    };
  }

  private SurvivalBlocks() {}
}

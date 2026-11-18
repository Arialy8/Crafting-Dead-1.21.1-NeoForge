package com.craftingdead.survival.world.entity;

import com.craftingdead.survival.CraftingDeadSurvival;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

public final class SurvivalEntityTypes {

  public static final DeferredRegister<EntityType<?>> deferredRegister =
      DeferredRegister.create(Registries.ENTITY_TYPE, CraftingDeadSurvival.ID);

  private static final List<String> ZOMBIE_NAMES = List.of(
      "advanced_zombie",
      "alfa_zombie",
      "bounty_hunter_zombie",
      "desert_raider_zombie",
      "doctor_zombie",
      "fast_zombie",
      "firefighter_zombie",
      "giant_zombie",
      "hazmat_zombie",
      "juggernaut_zombie",
      "miner_zombie",
      "ninja_zombie",
      "pilot_zombie",
      "police_zombie",
      "scout_zombie",
      "sniper_zombie",
      "soldier_zombie",
      "swat_zombie",
      "tank_zombie",
      "thrower_zombie",
      "rocket_zombie",
      "weak_zombie");

  public static final Map<String, Supplier<EntityType<Zombie>>> ZOMBIES = new LinkedHashMap<>();
  public static final Supplier<EntityType<SupplyDrop>> SUPPLY_DROP = deferredRegister.register(
      "supply_drop", () -> EntityType.Builder.<SupplyDrop>of(SupplyDrop::new, MobCategory.MISC)
          .sized(2.0F, 1.5F)
          .clientTrackingRange(10)
          .updateInterval(3)
          .build(ResourceLocation.fromNamespaceAndPath(CraftingDeadSurvival.ID,
              "supply_drop").toString()));
  public static final Supplier<EntityType<PipeBomb>> PIPE_BOMB = deferredRegister.register(
      "pipe_bomb", () -> EntityType.Builder.<PipeBomb>of(PipeBomb::new, MobCategory.MISC)
          .sized(0.25F, 0.5F)
          .clientTrackingRange(64)
          .updateInterval(4)
          .setShouldReceiveVelocityUpdates(false)
          .build(ResourceLocation.fromNamespaceAndPath(CraftingDeadSurvival.ID,
              "pipe_bomb").toString()));

  static {
    for (String name : ZOMBIE_NAMES) {
      ZOMBIES.put(name, deferredRegister.register(name, () -> createZombie(name)));
    }
  }

  private static EntityType<Zombie> createZombie(String name) {
    return EntityType.Builder.<Zombie>of(SurvivalZombie::new, MobCategory.MONSTER)
        .sized(zombieWidth(name), zombieHeight(name))
        .clientTrackingRange(8)
        .build(ResourceLocation.fromNamespaceAndPath(CraftingDeadSurvival.ID, name).toString());
  }

  private static float zombieWidth(String name) {
    return switch (name) {
      case "giant_zombie" -> 1.2F;
      default -> 0.6F;
    };
  }

  private static float zombieHeight(String name) {
    return switch (name) {
      case "giant_zombie" -> 3.9F;
      default -> 1.95F;
    };
  }

  public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
    for (var entityType : ZOMBIES.values()) {
      event.register(entityType.get(), SpawnPlacementTypes.ON_GROUND,
          Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
          Zombie::checkMonsterSpawnRules,
          RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
  }

  private SurvivalEntityTypes() {}
}

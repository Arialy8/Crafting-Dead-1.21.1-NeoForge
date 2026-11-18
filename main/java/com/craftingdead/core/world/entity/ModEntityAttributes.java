/*
 * Crafting Dead
 * Copyright (C) 2022 NexusNode LTD
 */

package com.craftingdead.core.world.entity;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.survival.world.entity.SurvivalEntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Forge 1.21.1 entity attribute registration entry point.
 *
 * <p>The current core module mainly registers grenade/projectile entities, which do not need mob
 * attributes. Survival/NPC modules should register zombie/NPC attributes here instead of using the
 * removed GlobalEntityTypeAttributes API from older Minecraft versions.
 */
@EventBusSubscriber(modid = CraftingDead.ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntityAttributes {

  private static boolean survivalAttributesRegistered;

  private ModEntityAttributes() {
  }

  @SubscribeEvent
  public static void registerAttributes(EntityAttributeCreationEvent event) {
    if (survivalAttributesRegistered) {
      return;
    }
    survivalAttributesRegistered = true;

    for (var entry : SurvivalEntityTypes.ZOMBIES.entrySet()) {
      var attributes = Zombie.createAttributes();
      switch (entry.getKey()) {
        case "doctor_zombie", "miner_zombie", "firefighter_zombie" -> {
          attributes.add(Attributes.MAX_HEALTH, 20.0D);
          attributes.add(Attributes.ATTACK_DAMAGE, 3.0D);
          attributes.add(Attributes.FOLLOW_RANGE, 24.0D);
          attributes.add(Attributes.MOVEMENT_SPEED, 0.23D);
        }
        case "police_zombie", "bounty_hunter_zombie", "desert_raider_zombie" -> {
          attributes.add(Attributes.MAX_HEALTH, 24.0D);
          attributes.add(Attributes.ATTACK_DAMAGE, 4.0D);
          attributes.add(Attributes.FOLLOW_RANGE, 28.0D);
          attributes.add(Attributes.MOVEMENT_SPEED, 0.25D);
        }
        case "hazmat_zombie", "pilot_zombie", "soldier_zombie", "swat_zombie",
            "sniper_zombie" -> {
          attributes.add(Attributes.MAX_HEALTH, 30.0D);
          attributes.add(Attributes.ATTACK_DAMAGE, 5.0D);
          attributes.add(Attributes.FOLLOW_RANGE, 32.0D);
          attributes.add(Attributes.MOVEMENT_SPEED, 0.26D);
        }
        case "advanced_zombie", "alfa_zombie" -> {
          attributes.add(Attributes.MAX_HEALTH, 40.0D);
          attributes.add(Attributes.ATTACK_DAMAGE, 6.0D);
          attributes.add(Attributes.FOLLOW_RANGE, 36.0D);
          attributes.add(Attributes.MOVEMENT_SPEED, 0.28D);
          attributes.add(Attributes.ARMOR, 4.0D);
        }
        case "rocket_zombie" -> {
          attributes.add(Attributes.FOLLOW_RANGE, 40.0D);
          attributes.add(Attributes.MAX_HEALTH, 20.0D);
          attributes.add(Attributes.ARMOR, 0.0D);
        }
        case "thrower_zombie" -> {
          attributes.add(Attributes.FOLLOW_RANGE, 24.0D);
          attributes.add(Attributes.MAX_HEALTH, 20.0D);
        }
        case "fast_zombie" -> {
          attributes.add(Attributes.MAX_HEALTH, 10.0D);
          attributes.add(Attributes.ATTACK_DAMAGE, 1.0D);
          attributes.add(Attributes.FOLLOW_RANGE, 30.0D);
          attributes.add(Attributes.MOVEMENT_SPEED, 0.33D);
          attributes.add(Attributes.ATTACK_KNOCKBACK, 1.5D);
        }
        case "ninja_zombie", "scout_zombie" ->
            attributes.add(Attributes.MOVEMENT_SPEED, 0.33D);
        case "tank_zombie" -> {
          attributes.add(Attributes.FOLLOW_RANGE, 20.0D);
          attributes.add(Attributes.MAX_HEALTH, 100.0D);
          attributes.add(Attributes.ATTACK_DAMAGE, 15.0D);
          attributes.add(Attributes.MOVEMENT_SPEED, 0.19D);
          attributes.add(Attributes.ATTACK_SPEED, 2.0D);
        }
        case "juggernaut_zombie" -> {
          attributes.add(Attributes.MAX_HEALTH, 40.0D);
          attributes.add(Attributes.ATTACK_DAMAGE, 5.0D);
        }
        case "giant_zombie" -> {
          attributes.add(Attributes.MAX_HEALTH, 100.0D);
          attributes.add(Attributes.ATTACK_DAMAGE, 50.0D);
          attributes.add(Attributes.MOVEMENT_SPEED, 0.5D);
        }
        case "weak_zombie" -> {
          attributes.add(Attributes.FOLLOW_RANGE, 25.0D);
          attributes.add(Attributes.MAX_HEALTH, 5.0D);
          attributes.add(Attributes.ATTACK_DAMAGE, 2.0D);
        }
        default -> {
        }
      }
      event.put(entry.getValue().get(), attributes.build());
    }
  }
}

/*
 * Crafting Dead - 1.21.1 migration helper.
 */
package com.craftingdead.core.world.item.attribute;

import com.craftingdead.core.CraftingDead;
import net.minecraft.resources.ResourceLocation;

/**
 * Central ResourceLocation ids for item attribute modifiers.
 *
 * <p>Minecraft 1.21.x replaced the old UUID based AttributeModifier constructor with
 * ResourceLocation based ids. Keeping the ids here avoids scattering string literals across
 * equipment/gun/melee item registration code.</p>
 */
public final class ModAttributeModifierIds {

  public static final ResourceLocation MELEE_ATTACK_DAMAGE = id("melee_attack_damage");
  public static final ResourceLocation MELEE_ATTACK_SPEED = id("melee_attack_speed");
  public static final ResourceLocation HAT_ARMOR = id("hat_armor");
  public static final ResourceLocation HAT_ARMOR_TOUGHNESS = id("hat_armor_toughness");
  public static final ResourceLocation STORAGE_ARMOR = id("storage_armor");
  public static final ResourceLocation STORAGE_ARMOR_TOUGHNESS = id("storage_armor_toughness");
  public static final ResourceLocation CLOTHING_ARMOR = id("clothing_armor");
  public static final ResourceLocation CLOTHING_ARMOR_TOUGHNESS = id("clothing_armor_toughness");
  public static final ResourceLocation CLOTHING_MAX_HEALTH = id("clothing_max_health");
  public static final ResourceLocation CLOTHING_MOVEMENT_SPEED = id("clothing_movement_speed");

  private ModAttributeModifierIds() {}

  public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, path);
  }
}

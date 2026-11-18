/*
 * Crafting Dead 1.21.1 port helper.
 */
package com.craftingdead.core.world.item.attribute;

import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Item attribute component helpers for the 1.21.x port.
 *
 * <p>Minecraft 1.20.5+/1.21 moved item attribute modifiers out of the old
 * {@code Item#getAttributeModifiers(EquipmentSlot, ItemStack)} override path and into the
 * {@code minecraft:attribute_modifiers} data component. Keeping the builders here makes the weapon
 * item constructors small and avoids spreading component boilerplate across every melee/tool item.</p>
 */
public final class ModItemAttributeUtil {

  private ModItemAttributeUtil() {}

  public static ItemAttributeModifiers mainHandWeapon(double attackDamage, double attackSpeed) {
    return ItemAttributeModifiers.builder()
        .add(Attributes.ATTACK_DAMAGE,
            new AttributeModifier(ModAttributeModifierIds.MELEE_ATTACK_DAMAGE, attackDamage,
                AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND)
        .add(Attributes.ATTACK_SPEED,
            new AttributeModifier(ModAttributeModifierIds.MELEE_ATTACK_SPEED, attackSpeed,
                AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND)
        .build();
  }
}

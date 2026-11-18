package com.craftingdead.survival.world.effect;

import com.craftingdead.survival.CraftingDeadSurvival;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class BrokenLegMobEffect extends MobEffect {

  public BrokenLegMobEffect() {
    super(MobEffectCategory.HARMFUL, 0x7A6A58);
    this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
        ResourceLocation.fromNamespaceAndPath(CraftingDeadSurvival.ID, "broken_leg_movement_speed"),
        -0.45D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
  }
}

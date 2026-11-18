package com.craftingdead.survival.world;

import com.craftingdead.core.world.item.MeleeWeaponItem;
import com.craftingdead.survival.world.effect.SurvivalMobEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Server-side effects for enchanted Crafting Dead melee weapons. */
public final class SurvivalCombatEvents {

  @SubscribeEvent
  public void onMeleeDamage(LivingDamageEvent.Post event) {
    if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)
        || !(event.getEntity() instanceof LivingEntity target)
        || !(attacker.getMainHandItem().getItem() instanceof MeleeWeaponItem)) {
      return;
    }

    var stack = attacker.getMainHandItem();
    var registry = attacker.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    if (has(registry, SurvivalEnchantments.INFECTION, stack)
        && attacker.getRandom().nextFloat() < 0.60F) {
      target.addEffect(new MobEffectInstance(
          SurvivalMobEffects.holder(SurvivalMobEffects.INFECTION), 20 * 60 * 5));
    } else if (has(registry, SurvivalEnchantments.BURNING, stack)
        && attacker.getRandom().nextFloat() < 0.20F) {
      target.igniteForSeconds(2.0F);
    } else if (has(registry, SurvivalEnchantments.BLINDING, stack)
        && attacker.getRandom().nextFloat() < 0.15F) {
      target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 3));
    } else if (has(registry, SurvivalEnchantments.SLOWING, stack)
        && attacker.getRandom().nextFloat() < 0.50F) {
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 3));
    }
  }

  private static boolean has(net.minecraft.core.Registry<Enchantment> registry,
      net.minecraft.resources.ResourceKey<Enchantment> key,
      net.minecraft.world.item.ItemStack stack) {
    var enchantment = registry.getHolder(key).orElse(null);
    return enchantment != null && EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) > 0;
  }
}

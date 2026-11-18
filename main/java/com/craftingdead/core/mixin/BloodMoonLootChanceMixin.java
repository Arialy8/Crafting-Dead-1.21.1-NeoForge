package com.craftingdead.core.mixin;

import com.craftingdead.survival.world.BloodMoonManager;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LootItemRandomChanceCondition.class)
public abstract class BloodMoonLootChanceMixin {
  @Inject(method = "test(Lnet/minecraft/world/level/storage/loot/LootContext;)Z",
      at = @At("HEAD"), cancellable = true, require = 1)
  private void bloodMoonChance(LootContext context, CallbackInfoReturnable<Boolean> callback) {
    // Death loot only: storage contents and chest generation must not be multiplied.
    if (!context.hasParam(LootContextParams.DAMAGE_SOURCE)) return;
    var entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
    float chance = ((LootItemRandomChanceCondition) (Object) this).chance().getFloat(context);
    float adjusted = BloodMoonManager.dropChance(entity, chance);
    if (adjusted != chance)
      callback.setReturnValue(context.getRandom().nextFloat() < adjusted);
  }
}

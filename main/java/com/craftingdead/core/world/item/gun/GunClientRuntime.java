package com.craftingdead.core.world.item.gun;

import com.craftingdead.core.world.entity.extension.LivingExtension;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Common-side bridge whose signature contains no Minecraft client classes. */
public interface GunClientRuntime {
  void execute(Runnable task);
  void startReload(boolean removingMagazine);
  void cancelReload();
  float getPartialTick();
  void handleTick(LivingExtension<?, ?> living);
  void handleShoot(LivingExtension<?, ?> living);
  void handleHitEntityPre(LivingExtension<?, ?> living, Entity entity, Vec3 hitPos,
      long randomSeed);
  void handleHitEntityPost(LivingExtension<?, ?> living, Entity entity, Vec3 hitPos,
      boolean playSound, boolean headshot);
  void handleHitBlock(LivingExtension<?, ?> living, BlockHitResult result,
      BlockState state, boolean playSound);
  void handleToggleSecondaryAction(LivingExtension<?, ?> living);
}

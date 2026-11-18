package com.craftingdead.core.world.entity.ai;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.mixin.MobAccessor;
import com.craftingdead.survival.world.entity.SurvivalZombie;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = CraftingDead.ID)
public final class VanillaZombieGrenadeAttraction {

  private VanillaZombieGrenadeAttraction() {}

  @SubscribeEvent
  public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Zombie zombie)
        || zombie instanceof SurvivalZombie) {
      return;
    }
    ((MobAccessor) zombie).craftingDead$getGoalSelector()
        .addGoal(1, new FollowAttractiveGrenadeGoal(zombie, 1.15D));
  }
}

package com.craftingdead.survival;

import com.craftingdead.survival.particles.SurvivalParticleTypes;
import com.craftingdead.survival.world.effect.SurvivalMobEffects;
import com.craftingdead.survival.world.entity.SurvivalEntityTypes;
import com.craftingdead.survival.world.item.SurvivalItems;
import com.craftingdead.survival.world.level.block.SurvivalBlocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import com.craftingdead.survival.world.SurvivalCommands;
import com.craftingdead.survival.world.FriendCommand;
import com.craftingdead.survival.world.SurvivalGameRules;
import com.craftingdead.survival.world.inventory.SurvivalMenuTypes;
import com.craftingdead.survival.world.BloodMoonEvents;
import com.craftingdead.survival.world.SurvivalHordeEvents;
import com.craftingdead.survival.world.SurvivalFractureEvents;
import com.craftingdead.survival.world.SurvivalCombatEvents;
import com.craftingdead.survival.world.SurvivalTeamSystem;
import com.craftingdead.survival.world.AutoSupplyDropManager;
import com.craftingdead.survival.world.SurvivalZombieSpawning;

public final class CraftingDeadSurvival {

  public static final String ID = "craftingdeadsurvival";

  private CraftingDeadSurvival() {}

  public static void register(IEventBus modEventBus) {
    SurvivalGameRules.init();
    modEventBus.addListener(SurvivalEntityTypes::registerSpawnPlacements);
    SurvivalEntityTypes.deferredRegister.register(modEventBus);
    SurvivalBlocks.deferredRegister.register(modEventBus);
    SurvivalItems.deferredRegister.register(modEventBus);
    SurvivalMenuTypes.REGISTER.register(modEventBus);
    SurvivalItems.tabDeferredRegister.register(modEventBus);
    SurvivalParticleTypes.deferredRegister.register(modEventBus);
    SurvivalMobEffects.deferredRegister.register(modEventBus);
    NeoForge.EVENT_BUS.register(new SurvivalCommands());
    NeoForge.EVENT_BUS.register(new FriendCommand());
    NeoForge.EVENT_BUS.register(new SurvivalZombieSpawning());
    NeoForge.EVENT_BUS.register(new BloodMoonEvents());
    NeoForge.EVENT_BUS.register(new SurvivalFractureEvents());
    NeoForge.EVENT_BUS.register(new SurvivalCombatEvents());
    NeoForge.EVENT_BUS.register(new SurvivalTeamSystem());
    NeoForge.EVENT_BUS.register(new AutoSupplyDropManager.Events());
  }
}

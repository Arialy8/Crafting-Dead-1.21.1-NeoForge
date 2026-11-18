package com.craftingdead.immerse;

import com.craftingdead.immerse.world.action.ImmerseActionTypes;
import com.craftingdead.immerse.world.base.ImmerseBaseEvents;
import com.craftingdead.immerse.world.base.BlueprintRestrictionCommands;
import com.craftingdead.immerse.world.item.ImmerseItems;
import com.craftingdead.immerse.world.inventory.ImmerseMenuTypes;
import com.craftingdead.immerse.world.level.block.ImmerseBlocks;
import com.craftingdead.immerse.world.level.block.entity.ImmerseBlockEntityTypes;
import com.craftingdead.immerse.world.airdrop.RankAirdropCommands;
import com.craftingdead.immerse.world.stats.RankCommands;
import com.craftingdead.immerse.world.resource.ResourceHarvestEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public final class CraftingDeadImmerse {

  public static final String ID = "craftingdeadimmerse";

  private CraftingDeadImmerse() {}

  public static void register(IEventBus modEventBus) {
    ImmerseBlocks.deferredRegister.register(modEventBus);
    ImmerseBlockEntityTypes.deferredRegister.register(modEventBus);
    ImmerseItems.deferredRegister.register(modEventBus);
    ImmerseItems.tabDeferredRegister.register(modEventBus);
    ImmerseMenuTypes.deferredRegister.register(modEventBus);
    ImmerseActionTypes.deferredRegister.register(modEventBus);
    NeoForge.EVENT_BUS.register(new ImmerseBaseEvents());
    NeoForge.EVENT_BUS.register(new ResourceHarvestEvents());
    NeoForge.EVENT_BUS.register(new BlueprintRestrictionCommands());
    NeoForge.EVENT_BUS.register(new RankCommands());
    NeoForge.EVENT_BUS.register(new RankAirdropCommands());
  }
}

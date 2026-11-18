package com.craftingdead.immerse.world.action;

import com.craftingdead.core.world.action.ActionType;
import com.craftingdead.core.world.action.ActionTypes;
import com.craftingdead.core.world.action.item.BlockItemActionType;
import com.craftingdead.immerse.CraftingDeadImmerse;
import com.craftingdead.immerse.world.item.BuildBlueprintItem;
import com.craftingdead.immerse.world.item.ImmerseItems;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ImmerseActionTypes {

  public static final DeferredRegister<ActionType<?>> deferredRegister =
      DeferredRegister.create(ActionTypes.REGISTRY_KEY, CraftingDeadImmerse.ID);

  public static final Map<String, Supplier<BlockItemActionType>> BUILD_ACTIONS =
      new LinkedHashMap<>();

  static {
    for (String blueprintName : ImmerseItems.BLUEPRINT_NAMES) {
      String actionName = "build_" + blueprintName.replace("_blueprint", "");
      BUILD_ACTIONS.put(blueprintName, deferredRegister.register(actionName,
          () -> BlockItemActionType.builder()
              .forItem(() -> ImmerseItems.ITEMS.get(blueprintName).get())
              .forBlock(state -> true)
              .durationSeconds(3.0F)
              .maxDistance(6.0D)
              .customAction(action -> BuildBlueprintItem.build(blueprintName, action.getContext()))
              .build()));
    }
  }

  public static Supplier<BlockItemActionType> getBuildAction(String blueprintName) {
    return BUILD_ACTIONS.get(blueprintName);
  }

  private ImmerseActionTypes() {}
}

package com.craftingdead.core.world.vehicle;

import net.minecraft.world.level.GameRules;

/** Game rules for the self-contained vehicle port. */
public final class VehicleGameRules {

  /** Disabled by default so tanks never damage a world unless an operator opts in. */
  public static final GameRules.Key<GameRules.BooleanValue> TANK_BLOCK_DESTRUCTION =
      GameRules.register("craftingDeadTankBlockDestruction", GameRules.Category.MISC,
          GameRules.BooleanValue.create(false));

  /** Forces static rule registration during mod construction. */
  public static void init() {
  }

  private VehicleGameRules() {
  }
}

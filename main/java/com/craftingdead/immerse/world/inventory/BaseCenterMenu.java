package com.craftingdead.immerse.world.inventory;

import com.craftingdead.immerse.world.base.BaseSecurity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** Menu token for the TECU-style list screen. Crafting is handled server-side. */
public class BaseCenterMenu extends AbstractContainerMenu {

  private final BlockPos basePos;

  public BaseCenterMenu(int id, Inventory inventory) {
    this(id, inventory, BlockPos.ZERO);
  }

  public BaseCenterMenu(int id, Inventory inventory, BlockPos basePos) {
    super(ImmerseMenuTypes.BASE_CENTER.get(), id);
    this.basePos = basePos;
  }

  public BlockPos getBasePos() {
    return this.basePos;
  }

  @Override
  public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
    return ItemStack.EMPTY;
  }

  @Override
  public boolean stillValid(@NotNull Player player) {
    return this.basePos.equals(BlockPos.ZERO)
        || BaseSecurity.canCraft(player.level(), this.basePos, player);
  }
}

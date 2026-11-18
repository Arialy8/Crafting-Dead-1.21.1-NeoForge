package com.craftingdead.immerse.world.inventory;

import com.craftingdead.immerse.world.airdrop.RankRewardData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Ghost slots copy reward templates, never consuming or handing out actual items. */
public final class RankRewardMenu extends AbstractContainerMenu {
  private final SimpleContainer templates = new SimpleContainer(54);
  private final ContainerData state = new SimpleContainerData(3);
  private ServerPlayer editor;
  private int rank;

  public RankRewardMenu(int id, Inventory inventory) {
    super(ImmerseMenuTypes.RANK_REWARDS.get(), id);
    for (int row = 0; row < 6; row++) {
      for (int col = 0; col < 9; col++) {
        addSlot(new Slot(templates, row * 9 + col, 36 + col * 18, 24 + row * 18) {
          @Override public boolean mayPickup(Player player) { return false; }
          @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
      }
    }
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 9; col++)
        addSlot(new Slot(inventory, col + row * 9 + 9, 36 + col * 18, 156 + row * 18));
    for (int col = 0; col < 9; col++)
      addSlot(new Slot(inventory, col, 36 + col * 18, 214));
    addDataSlots(state);
  }

  public RankRewardMenu(int id, Inventory inventory, ServerPlayer editor, int rank) {
    this(id, inventory);
    this.editor = editor;
    this.rank = rank;
    refresh();
  }

  public int page() { return state.get(0); }
  public int pages() { return Math.max(1, state.get(1)); }
  public boolean modified() { return state.get(2) != 0; }

  private void refresh() {
    if (editor == null) return;
    var rewards = RankRewardData.get(editor.server).rewards(editor.server, rank);
    state.set(1, rewards.pages());
    state.set(2, rewards.modified ? 1 : 0);
    for (int slot = 0; slot < 54; slot++) {
      var tier = rewards.tiers.get(slot / 18);
      int index = page() * 18 + slot % 18;
      templates.setItem(slot, index < tier.size() ? tier.get(index).copy() : ItemStack.EMPTY);
    }
  }

  @Override
  public void broadcastChanges() {
    refresh();
    super.broadcastChanges();
  }

  @Override
  public boolean clickMenuButton(Player player, int button) {
    if (!stillValid(player)) return false;
    if (button == 0 && page() > 0) state.set(0, page() - 1);
    else if (button == 1 && page() < Math.min(63, pages())) state.set(0, page() + 1);
    else return false;
    broadcastChanges();
    return true;
  }

  @Override
  public void clicked(int slot, int button, ClickType type, Player player) {
    if (!stillValid(player) || editor == null) return;
    if (slot >= 0 && slot < 54) {
      refresh();
      ItemStack value;
      if (type == ClickType.QUICK_MOVE) {
        value = ItemStack.EMPTY;
      } else if (type == ClickType.SWAP && button >= 0 && button < 9) {
        value = player.getInventory().getItem(button).copy();
      } else if (type == ClickType.PICKUP) {
        value = getCarried().copy();
        if (value.isEmpty() && button == 1) {
          value = templates.getItem(slot).copy();
          value.shrink(1);
        }
      } else return;
      RankRewardData.get(editor.server).edit(editor.server, rank, slot / 18,
          page() * 18 + slot % 18, value);
      broadcastChanges();
      sendAllDataToRemote();
    } else if (type != ClickType.QUICK_CRAFT && type != ClickType.PICKUP_ALL
        && type != ClickType.QUICK_MOVE) {
      super.clicked(slot, button, type, player);
    }
  }

  @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
  @Override public boolean stillValid(Player player) {
    return player.hasPermissions(2) && (editor == null || player == editor);
  }
}

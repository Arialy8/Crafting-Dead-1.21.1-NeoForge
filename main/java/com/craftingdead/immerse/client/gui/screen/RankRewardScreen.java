package com.craftingdead.immerse.client.gui.screen;

import com.craftingdead.immerse.world.inventory.RankRewardMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class RankRewardScreen extends AbstractContainerScreen<RankRewardMenu> {
  public RankRewardScreen(RankRewardMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    imageWidth = 230;
    imageHeight = 238;
    inventoryLabelX = 36;
    inventoryLabelY = 151;
  }

  @Override protected void init() {
    super.init();
    addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(0))
        .bounds(leftPos + 36, topPos + 132, 24, 12).build());
    addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1))
        .bounds(leftPos + 174, topPos + 132, 24, 12).build());
  }

  private void turn(int direction) {
    if (minecraft.gameMode != null)
      minecraft.gameMode.handleInventoryButtonClick(menu.containerId, direction);
  }

  @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {}

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    graphics.fill(0, 0, width, height, 0x77000000);
    super.render(graphics, mouseX, mouseY, delta);
    renderTooltip(graphics, mouseX, mouseY);
    if (hoveredSlot != null && hoveredSlot.index < 54 && !hoveredSlot.hasItem()) {
      graphics.renderTooltip(font, Component.literal(
          "模板：数量=上限；持物点击复制；空手左键删除，右键减1"), mouseX, mouseY);
    }
  }

  @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
    graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF20252C);
    int[] colors = {0xFF44885A, 0xFF426C9C, 0xFF965D72};
    for (int i = 0; i < menu.slots.size(); i++) {
      var slot = menu.slots.get(i);
      int color = i < 54 ? colors[i / 18] : 0xFF5E6670;
      graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1,
          leftPos + slot.x + 17, topPos + slot.y + 17, color);
      graphics.fill(leftPos + slot.x, topPos + slot.y,
          leftPos + slot.x + 16, topPos + slot.y + 16, 0xFF101419);
    }
  }

  @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    graphics.drawString(font, title, 8, 6, 0xFFFFFFFF, false);
    String[] labels = {"高", "中", "低"};
    String[] chances = {"60%", "30%", "10%"};
    for (int i = 0; i < 3; i++) {
      graphics.drawString(font, labels[i], 12, 29 + i * 36, 0xFFFFFFFF, false);
      graphics.drawString(font, menu.modified() ? chances[i] : "原值",
          6, 42 + i * 36, 0xFFBBBBBB, false);
    }
    graphics.drawCenteredString(font, (menu.page() + 1) + "/" + Math.max(menu.page() + 1, menu.pages()),
        117, 134, 0xFFFFFFFF);
    graphics.drawString(font, menu.modified() ? "已自动保存" : "原奖励规则（未修改）",
        36, 146, 0xFFAAAAAA, false);
  }
}

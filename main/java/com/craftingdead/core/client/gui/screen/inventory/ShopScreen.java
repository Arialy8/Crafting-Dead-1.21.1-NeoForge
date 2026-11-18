package com.craftingdead.core.client.gui.screen.inventory;

import com.craftingdead.core.world.inventory.ShopMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 内置商店客户端界面：复用原版 6 行箱子背景（类 ChestCommand 商店外观）。
 */
public class ShopScreen extends AbstractContainerScreen<ShopMenu> {

  private static final ResourceLocation CONTAINER_BACKGROUND =
      ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

  private final int containerRows;

  public ShopScreen(ShopMenu menu, Inventory playerInventory, Component title) {
    super(menu, playerInventory, title);
    this.containerRows = menu.getRowCount();
    this.imageHeight = 114 + this.containerRows * 18;
    this.inventoryLabelY = this.imageHeight - 94;
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    // Vanilla clone handling is creative-only; explicitly send middle clicks in survival too.
    if (button == 2 && this.hoveredSlot != null && this.hoveredSlot.index < 54) {
      this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 2, net.minecraft.world.inventory.ClickType.CLONE);
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    super.render(guiGraphics, mouseX, mouseY, partialTick);
    this.renderTooltip(guiGraphics, mouseX, mouseY);
  }

  @Override
  protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    int left = (this.width - this.imageWidth) / 2;
    int top = (this.height - this.imageHeight) / 2;
    guiGraphics.blit(CONTAINER_BACKGROUND, left, top, 0, 0, this.imageWidth,
        this.containerRows * 18 + 17);
    guiGraphics.blit(CONTAINER_BACKGROUND, left, top + this.containerRows * 18 + 17, 0, 126,
        this.imageWidth, 96);
  }
}

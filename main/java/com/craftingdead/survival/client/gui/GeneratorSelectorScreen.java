package com.craftingdead.survival.client.gui;

import com.craftingdead.survival.world.inventory.GeneratorSelectorMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GeneratorSelectorScreen extends AbstractContainerScreen<GeneratorSelectorMenu> {
  private static final String[] NAMES = {"总体数量", "平民占比", "稀有平民", "医疗占比", "军用占比", "警察占比"};
  private final List<EditBox> inputs = new ArrayList<>();
  private String error = "";

  public GeneratorSelectorScreen(GeneratorSelectorMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    this.imageWidth = 230;
    this.imageHeight = 190;
  }

  @Override
  protected void init() {
    super.init();
    this.inputs.clear();
    int left = (this.width - this.imageWidth) / 2;
    int top = (this.height - this.imageHeight) / 2;
    for (int i = 0; i < 6; i++) {
      EditBox input = new EditBox(this.font, left + 132, top + 23 + i * 21, 72, 18,
          Component.literal(NAMES[i]));
      input.setMaxLength(3);
      input.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
      input.setValue(Integer.toString(this.menu.getValue(i)));
      this.inputs.add(this.addRenderableWidget(input));
    }
    this.addRenderableWidget(Button.builder(Component.literal("保存配置"), button -> this.save())
        .bounds(left + 65, top + 155, 100, 20).build());
  }

  private void save() {
    int[] values = new int[6];
    try {
      for (int i = 0; i < values.length; i++) {
        values[i] = Integer.parseInt(this.inputs.get(i).getValue());
      }
    } catch (NumberFormatException exception) {
      this.error = "所有输入框都必须填写数字";
      return;
    }
    if (values[0] < 1 || values[0] > 500) {
      this.error = "总体数量必须在 1 到 500 之间";
      return;
    }
    int total = 0;
    for (int i = 1; i < values.length; i++) {
      if (values[i] < 0 || values[i] > 100) {
        this.error = "单项占比必须在 0% 到 100% 之间";
        return;
      }
      total += values[i];
    }
    if (total > 100) {
      this.error = "五类占比总和不能超过 100%";
      return;
    }
    if (total == 0) {
      this.error = "至少需要为一种生成器填写占比";
      return;
    }

    this.sendValue(0, values[0]);
    for (int i = 1; i < values.length; i++) {
      if (values[i] <= this.menu.getValue(i)) {
        this.sendValue(i, values[i]);
      }
    }
    for (int i = 1; i < values.length; i++) {
      if (values[i] > this.menu.getValue(i)) {
        this.sendValue(i, values[i]);
      }
    }
    this.error = "配置已保存，占比合计 " + total + "%";
  }

  private void sendValue(int field, int value) {
    this.minecraft.gameMode.handleInventoryButtonClick(
        this.menu.containerId, 1000 + field * 501 + value);
  }

  @Override
  protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    int x = (this.width - this.imageWidth) / 2;
    int y = (this.height - this.imageHeight) / 2;
    graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xE0181C20);
    graphics.fill(x, y, x + this.imageWidth, y + 2, 0xFF6E7D86);
  }

  @Override
  protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    graphics.drawString(this.font, this.title, 10, 7, 0xFFFFFFFF, false);
    for (int i = 0; i < NAMES.length; i++) {
      graphics.drawString(this.font, NAMES[i], 14, 29 + i * 21, 0xFFD8DEE2, false);
    }
    if (!this.error.isEmpty()) {
      int color = this.error.startsWith("配置已保存") ? 0xFF78D878 : 0xFFFF6868;
      graphics.drawCenteredString(this.font, this.error, this.imageWidth / 2, 143, color);
    }
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    this.renderBackground(graphics, mouseX, mouseY, partialTick);
    super.render(graphics, mouseX, mouseY, partialTick);
  }
}

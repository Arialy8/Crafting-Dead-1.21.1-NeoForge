package com.craftingdead.core.client.gui;
import com.craftingdead.core.network.message.play.RenameChalkMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
public final class ChalkScreen extends Screen {
  private final InteractionHand hand;
  private EditBox name;
  public ChalkScreen(InteractionHand hand) { super(Component.literal("粉笔命名")); this.hand = hand; }
  @Override protected void init() {
    name = addRenderableWidget(new EditBox(font, width / 2 - 100, height / 2 - 10, 200, 20, Component.literal("名称")));
    name.setMaxLength(64);
    if (minecraft.player.getItemInHand(hand).has(net.minecraft.core.component.DataComponents.CUSTOM_NAME))
      name.setValue(minecraft.player.getItemInHand(hand).getHoverName().getString());
    var confirm = addRenderableWidget(Button.builder(Component.literal("确认"), b -> {
      PacketDistributor.sendToServer(new RenameChalkMessage(hand, name.getValue()));
      onClose();
    }).bounds(width / 2 - 100, height / 2 + 20, 95, 20).build());
    confirm.active = !name.getValue().isBlank();
    name.setResponder(value -> confirm.active = !value.isBlank());
    addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
        .bounds(width / 2 + 5, height / 2 + 20, 95, 20).build());
    setInitialFocus(name);
  }
  @Override public void render(GuiGraphics graphics, int x, int y, float tick) {
    renderBackground(graphics, x, y, tick);
    super.render(graphics, x, y, tick);
    graphics.drawCenteredString(font, title, width / 2, height / 2 - 40, 0xFFFFFF);
    graphics.drawCenteredString(font, "命名后，与武器或背包放入合成栏转移名称", width / 2, height / 2 + 55, 0xBBBBBB);
  }
  @Override public boolean isPauseScreen() { return false; }
}

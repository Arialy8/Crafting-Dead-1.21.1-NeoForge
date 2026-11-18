package com.craftingdead.survival.client.gui.screen;

import com.craftingdead.core.network.NetworkChannel;
import com.craftingdead.core.network.message.play.C2SFriendActionMessage;
import com.craftingdead.core.network.message.play.C2SFriendRequestActionMessage;
import com.craftingdead.core.network.message.play.C2SFriendTeamActionMessage;
import com.craftingdead.survival.client.friend.FriendClientData;
import com.craftingdead.survival.world.friend.FriendEntry;
import com.craftingdead.survival.world.friend.FriendTab;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class FriendScreen extends Screen {

  private static final int LIST_START_Y = 80;
  private static final int ENTRY_HEIGHT = 28;
  private static final int SCROLLBAR_WIDTH = 6;
  private static final int TAB_WIDTH = 71;
  private static final int TAB_GAP = 7;

  private FriendTab currentTab = FriendTab.FRIENDS;
  private EditBox searchBox;
  private int scrollOffset;
  private final List<AbstractWidget> entryWidgets = new ArrayList<>();
  private boolean draggingScrollbar;

  public FriendScreen() {
    super(Component.literal("好友"));
  }

  @Override
  protected void init() {
    super.init();
    this.buildFullUI();
  }

  private void buildFullUI() {
    this.clearWidgets();
    this.entryWidgets.clear();
    this.scrollOffset = 0;

    int centerX = this.width / 2;
    int topY = 25;
    int totalTabsWidth = 4 * TAB_WIDTH + 3 * TAB_GAP;
    int startX = centerX - totalTabsWidth / 2;
    int tabStep = TAB_WIDTH + TAB_GAP;

    this.addTabButton(startX, topY, FriendTab.ONLINE,
        Component.literal("在线玩家"));
    this.addTabButton(startX + tabStep, topY, FriendTab.FRIENDS,
        Component.literal("好友列表"));
    this.addTabButton(startX + tabStep * 2, topY, FriendTab.REQUESTS,
        Component.literal("好友请求"));
    this.addTabButton(startX + tabStep * 3, topY, FriendTab.TEAM,
        Component.literal("组队"));

    String savedSearch = this.searchBox != null ? this.searchBox.getValue() : "";
    this.searchBox = new EditBox(this.font, centerX - 150, 50, 300, 20,
        Component.literal("搜索"));
    this.searchBox.setSuggestion(savedSearch.isEmpty() ? "搜索..." : "");
    this.searchBox.setMaxLength(32);
    this.searchBox.setValue(savedSearch);
    this.searchBox.setResponder(text -> {
      this.searchBox.setSuggestion(text.isEmpty() ? "搜索..." : "");
      this.scrollOffset = 0;
      this.rebuildEntries();
    });
    this.addRenderableWidget(this.searchBox);

    this.rebuildEntries();
  }

  @Override
  public void removed() {
    super.removed();
    if (this.searchBox != null) {
      this.searchBox.setFocused(false);
    }
  }

  @Override
  public boolean charTyped(char codePoint, int modifiers) {
    if (this.searchBox != null && this.searchBox.charTyped(codePoint, modifiers)) {
      return true;
    }
    return super.charTyped(codePoint, modifiers);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (this.searchBox != null && this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  private void addTabButton(int posX, int posY, FriendTab tab, Component label) {
    final boolean selected = this.currentTab == tab;
    Button tabButton = Button.builder(label, button -> {
      if (this.currentTab != tab) {
        this.switchTab(tab);
      }
    }).bounds(posX, posY, TAB_WIDTH, 20).build();
    tabButton.active = !selected;
    this.addRenderableWidget(tabButton);
  }

  private void addEntry(FriendEntry entry, int posX, int posY) {
    Component nameText;
    if (this.currentTab == FriendTab.FRIENDS || this.currentTab == FriendTab.TEAM) {
      Component status = entry.online()
          ? Component.literal("在线").withStyle(ChatFormatting.GREEN)
          : Component.literal("离线").withStyle(ChatFormatting.RED);
      if (this.currentTab == FriendTab.TEAM
          && entry.uuid().equals(FriendClientData.getOwnerUuid())) {
        nameText = Component.literal("\u00a76[队长] \u00a7f" + entry.name() + " \u00a78- ")
            .copy().append(status);
      } else {
        nameText = Component.literal("\u00a7f" + entry.name() + " \u00a78- ")
            .copy().append(status);
      }
    } else {
      nameText = Component.literal("\u00a7f" + entry.name());
    }

    Button playerButton = Button.builder(nameText, button -> {})
        .bounds(posX, posY, 180, 20).build();
    playerButton.active = false;
    this.addEntryWidget(playerButton);

    if (this.currentTab == FriendTab.ONLINE) {
      this.addEntryWidget(Button.builder(Component.literal("添加好友"), button -> {
        NetworkChannel.PLAY.sendToServer(
            new C2SFriendActionMessage(FriendTab.ONLINE, entry.uuid()));
      }).bounds(posX + 190, posY, 60, 20).build());
      Button inviteButton = Button.builder(Component.literal("邀请组队"), button -> {
        NetworkChannel.PLAY.sendToServer(
            new C2SFriendTeamActionMessage(
                C2SFriendTeamActionMessage.TeamActionType.INVITE, entry.uuid()));
      }).bounds(posX + 255, posY, 60, 20).build();
      inviteButton.active = FriendClientData.isInSquad();
      this.addEntryWidget(inviteButton);
    }

    if (this.currentTab == FriendTab.FRIENDS) {
      this.addEntryWidget(Button.builder(Component.literal("移除好友"), button -> {
        NetworkChannel.PLAY.sendToServer(
            new C2SFriendActionMessage(FriendTab.FRIENDS, entry.uuid()));
      }).bounds(posX + 190, posY, 90, 20).build());
    }

    if (this.currentTab == FriendTab.REQUESTS) {
      this.addEntryWidget(Button.builder(Component.literal("接受"), button -> {
        NetworkChannel.PLAY.sendToServer(
            new C2SFriendRequestActionMessage(entry.uuid(), true));
      }).bounds(posX + 190, posY, 70, 20).build());
      this.addEntryWidget(Button.builder(Component.literal("拒绝"), button -> {
        NetworkChannel.PLAY.sendToServer(
            new C2SFriendRequestActionMessage(entry.uuid(), false));
      }).bounds(posX + 265, posY, 70, 20).build());
    }

    if (this.currentTab == FriendTab.TEAM) {
      UUID ownerUuid = FriendClientData.getOwnerUuid();
      boolean viewerIsOwner = Minecraft.getInstance().player != null
          && Minecraft.getInstance().player.getUUID().equals(ownerUuid);
      boolean isTargetOwner = entry.uuid().equals(ownerUuid);
      if (viewerIsOwner && !isTargetOwner) {
        this.addEntryWidget(Button.builder(Component.literal("踢出"), button -> {
          NetworkChannel.PLAY.sendToServer(
              new C2SFriendTeamActionMessage(
                  C2SFriendTeamActionMessage.TeamActionType.KICK, entry.uuid()));
        }).bounds(posX + 190, posY, 60, 20).build());
        this.addEntryWidget(Button.builder(Component.literal("转让"), button -> {
          NetworkChannel.PLAY.sendToServer(
              new C2SFriendTeamActionMessage(
                  C2SFriendTeamActionMessage.TeamActionType.TRANSFER, entry.uuid()));
        }).bounds(posX + 255, posY, 60, 20).build());
      }
    }
  }

  private void addEntryWidget(AbstractWidget widget) {
    this.entryWidgets.add(widget);
    this.addRenderableWidget(widget);
  }

  public void refresh() {
    this.buildFullUI();
  }

  private void rebuildEntries() {
    for (AbstractWidget widget : this.entryWidgets) {
      this.removeWidget(widget);
    }
    this.entryWidgets.clear();

    int centerX = this.width / 2;
    List<FriendEntry> entries = this.getFilteredEntries();
    int maxVisible = (this.height - LIST_START_Y - 20) / ENTRY_HEIGHT;
    int maxScroll = Math.max(0, entries.size() - maxVisible);
    if (this.scrollOffset > maxScroll) {
      this.scrollOffset = maxScroll;
    }
    for (int i = this.scrollOffset;
        i < entries.size() && i < this.scrollOffset + maxVisible; i++) {
      int posY = LIST_START_Y + (i - this.scrollOffset) * ENTRY_HEIGHT;
      this.addEntry(entries.get(i), centerX - 150, posY);
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY,
      double scrollX, double scrollY) {
    List<FriendEntry> entries = this.getFilteredEntries();
    int maxVisible = (this.height - LIST_START_Y - 20) / ENTRY_HEIGHT;
    int maxScroll = Math.max(0, entries.size() - maxVisible);
    if (scrollY < 0 && this.scrollOffset < maxScroll) {
      this.scrollOffset++;
      this.rebuildEntries();
      return true;
    }
    if (scrollY > 0 && this.scrollOffset > 0) {
      this.scrollOffset--;
      this.rebuildEntries();
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (button == 0 && this.isMouseOverScrollbar(mouseX, mouseY)) {
      this.draggingScrollbar = true;
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    this.draggingScrollbar = false;
    return super.mouseReleased(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int button,
      double dragX, double dragY) {
    if (!this.draggingScrollbar) {
      return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    List<FriendEntry> entries = this.getFilteredEntries();
    int maxVisible = (this.height - LIST_START_Y - 20) / ENTRY_HEIGHT;
    int maxScroll = entries.size() - maxVisible;
    if (maxScroll <= 0) {
      return true;
    }
    int scrollbarHeight = maxVisible * ENTRY_HEIGHT;
    int thumbHeight = Math.max(20, scrollbarHeight * maxVisible / entries.size());
    float progress = (float) ((mouseY - LIST_START_Y - (double) thumbHeight / 2.0)
        / (double) (scrollbarHeight - thumbHeight));
    progress = Mth.clamp(progress, 0.0F, 1.0F);
    this.scrollOffset = Math.round(progress * (float) maxScroll);
    this.rebuildEntries();
    return true;
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    this.renderBackground(graphics, mouseX, mouseY, partialTick);
    int centerX = this.width / 2;
    graphics.drawCenteredString(this.font, this.getCurrentTitle(), centerX, 10, 0xFFFFFF);

    if (this.searchBox != null) {
      graphics.blit(
          net.minecraft.resources.ResourceLocation
              .withDefaultNamespace("textures/gui/resource_packs.png"),
          centerX - 145, 54, 227.0F, 3.0F, 12, 12, 256, 256);
    }

    if (this.getFilteredEntries().isEmpty()) {
      String emptyMessage = this.currentTab == FriendTab.TEAM && !FriendClientData.isInSquad()
          ? "你还没有加入小队" : "暂无数据";
      graphics.drawCenteredString(this.font,
          Component.literal(emptyMessage), centerX, 90, 0xAAAAAA);
    }

    this.renderScrollbar(graphics);
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  private List<FriendEntry> getCurrentEntries() {
    return switch (this.currentTab) {
      case ONLINE -> FriendClientData.getOnlinePlayers();
      case FRIENDS -> FriendClientData.getFriends();
      case REQUESTS -> FriendClientData.getRequests();
      case TEAM -> FriendClientData.isInSquad() ? FriendClientData.getTeamMembers() : List.of();
    };
  }

  private Component getCurrentTitle() {
    return switch (this.currentTab) {
      case ONLINE -> Component.literal("在线玩家");
      case FRIENDS -> Component.literal("好友列表");
      case REQUESTS -> Component.literal("好友请求");
      case TEAM -> Component.literal("组队");
    };
  }

  private void switchTab(FriendTab tab) {
    this.currentTab = tab;
    this.scrollOffset = 0;
    if (this.searchBox != null) {
      this.searchBox.setValue("");
    }
    this.buildFullUI();
  }

  private List<FriendEntry> getFilteredEntries() {
    String searchText = this.searchBox != null
        ? this.searchBox.getValue().toLowerCase() : "";
    List<FriendEntry> entries = this.getCurrentEntries();
    if (searchText.isEmpty()) {
      return entries;
    }
    List<FriendEntry> filtered = new ArrayList<>();
    for (FriendEntry entry : entries) {
      if (entry.name().toLowerCase().contains(searchText)) {
        filtered.add(entry);
      }
    }
    return filtered;
  }

  private boolean hasTwoActionButtons() {
    return this.currentTab == FriendTab.REQUESTS
        || this.currentTab == FriendTab.TEAM
        || this.currentTab == FriendTab.ONLINE;
  }

  private void renderScrollbar(GuiGraphics graphics) {
    List<FriendEntry> entries = this.getFilteredEntries();
    int maxVisible = (this.height - LIST_START_Y - 20) / ENTRY_HEIGHT;
    if (entries.size() <= maxVisible) {
      return;
    }
    int centerX = this.width / 2;
    int scrollbarOffset = this.hasTwoActionButtons() ? 205 : 170;
    int scrollbarX = centerX + scrollbarOffset;
    int scrollbarY = LIST_START_Y;
    int scrollbarHeight = maxVisible * ENTRY_HEIGHT;

    graphics.fill(scrollbarX, scrollbarY,
        scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight,
        0xFF202020);

    int maxScroll = entries.size() - maxVisible;
    int thumbHeight = Math.max(20, scrollbarHeight * maxVisible / entries.size());
    int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * this.scrollOffset / maxScroll;
    graphics.fill(scrollbarX, thumbY,
        scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight,
        0xFF808080);
  }

  private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
    List<FriendEntry> entries = this.getFilteredEntries();
    int maxVisible = (this.height - LIST_START_Y - 20) / ENTRY_HEIGHT;
    if (entries.size() <= maxVisible) {
      return false;
    }
    int centerX = this.width / 2;
    int scrollbarOffset = this.hasTwoActionButtons() ? 205 : 170;
    int scrollbarX = centerX + scrollbarOffset;
    int scrollbarHeight = maxVisible * ENTRY_HEIGHT;
    return mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH
        && mouseY >= LIST_START_Y && mouseY <= LIST_START_Y + scrollbarHeight;
  }
}

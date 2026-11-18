package com.craftingdead.immerse.client.gui.screen;

import com.craftingdead.immerse.CraftingDeadImmerse;
import com.craftingdead.immerse.world.base.BaseMemberRole;
import com.craftingdead.immerse.world.base.BasePermission;
import com.craftingdead.immerse.world.base.BaseRecipe;
import com.craftingdead.immerse.world.base.BaseSecurity;
import com.craftingdead.immerse.world.inventory.BaseCenterMenu;
import com.craftingdead.immerse.world.item.ImmerseItems;
import com.craftingdead.immerse.world.level.block.entity.BaseCenterBlockEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 1.18-style base recipe/member screen. */
public class BaseCenterScreen extends AbstractContainerScreen<BaseCenterMenu> {

  public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      CraftingDeadImmerse.ID, "textures/gui/container/base.png");
  private static final int VISIBLE_ROWS = 4;
  private static final int ROW_X = 6;
  private static final int ROW_Y = 19;
  private static final int ROW_WIDTH = 143;
  private static final int ROW_HEIGHT = 35;
  private static final int SCROLLER_X = 156;
  private static final int SCROLLER_Y = 20;
  private static final int SCROLLER_HEIGHT = 125;
  private static final String[] PERMISSION_KEYS = {
      "build", "break", "container", "craft", "administrator"
  };
  private static final BasePermission[] PERMISSIONS = {
      BasePermission.BUILD, BasePermission.BREAK, BasePermission.CONTAINER, BasePermission.CRAFT
  };

  private enum Mode { RECIPES, MEMBERS, INFORMATION }

  private record StorageCell(Item item, int count, int type) {}

  private Mode mode = Mode.RECIPES;
  private EditBox input;
  private String search = "";
  private int scroll;
  private boolean draggingScroller;

  public BaseCenterScreen(BaseCenterMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    this.imageWidth = 176;
    this.imageHeight = 165;
  }

  @Override
  protected void init() {
    super.init();
    this.rebuildInput();
  }

  private void rebuildInput() {
    if (this.input != null) {
      this.removeWidget(this.input);
    }
    Component hint = Component.translatable(this.mode == Mode.MEMBERS
        ? "container.craftingdeadimmerse.base.member_name"
        : "container.craftingdeadimmerse.base.search");
    this.input = new EditBox(this.font, this.leftPos + 10, this.topPos + 8, 78, 8, hint);
    this.input.setBordered(false);
    this.input.setTextColor(0xFFFFFF);
    this.input.setMaxLength(this.mode == Mode.MEMBERS ? 16 : 40);
    if (this.mode == Mode.RECIPES) {
      this.input.setValue(this.search);
      this.input.setResponder(value -> {
        this.search = value;
        this.scroll = 0;
      });
    }
    this.input.visible = this.mode != Mode.INFORMATION;
    this.addRenderableWidget(this.input);
  }

  @Override
  public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    this.renderTransparentBackground(graphics);
    super.render(graphics, mouseX, mouseY, partialTick);
    this.renderTooltip(graphics, mouseX, mouseY);
    this.renderMemberActionTooltip(graphics, mouseX, mouseY);
  }

  @Override
  protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX,
      int mouseY) {
    graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    this.renderTabs(graphics);
    switch (this.mode) {
      case RECIPES -> this.renderRecipes(graphics, mouseX, mouseY);
      case MEMBERS -> this.renderMembers(graphics, mouseX, mouseY);
      case INFORMATION -> this.renderInformation(graphics);
    }
  }

  @Override
  protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {}

  private void renderTabs(GuiGraphics graphics) {
    for (int i = 0; i < 3; i++) {
      boolean selected = this.mode.ordinal() == i;
      int x = this.leftPos + i * 30;
      graphics.blit(TEXTURE, x, this.topPos - 28, i == 0 ? 183 : 211,
          selected ? 0 : 36, i == 0 ? 28 : 29, selected ? 35 : 28);
    }
    graphics.renderFakeItem(new ItemStack(Items.BOOK), this.leftPos + 6, this.topPos - 20);
    graphics.renderFakeItem(new ItemStack(Items.PLAYER_HEAD), this.leftPos + 36,
        this.topPos - 22);
    graphics.renderFakeItem(new ItemStack(ImmerseItems.ITEMS.get("base_center").get()),
        this.leftPos + 66, this.topPos - 22);
  }

  private void renderRecipes(GuiGraphics graphics, int mouseX, int mouseY) {
    graphics.fill(this.leftPos + 6, this.topPos + 6, this.leftPos + 102,
        this.topPos + 17, 0xFF202020);
    List<BaseRecipe> recipes = this.filteredRecipes();
    this.clampScroll(recipes.size());
    this.renderScroller(graphics, recipes.size());
    for (int row = 0; row < VISIBLE_ROWS; row++) {
      int index = this.scroll + row;
      if (index >= recipes.size()) {
        break;
      }
      BaseRecipe recipe = recipes.get(index);
      int x = this.leftPos + ROW_X;
      int y = this.topPos + ROW_Y + row * ROW_HEIGHT;
      this.renderRecipeRow(graphics, recipe, x, y,
          this.isOver(x + 111, y + 3, 27, 10, mouseX, mouseY));
    }
  }

  private void renderRecipeRow(GuiGraphics graphics, BaseRecipe recipe, int x, int y,
      boolean craftHovered) {
    graphics.blit(TEXTURE, x, y, 0, 165, ROW_WIDTH, ROW_HEIGHT);
    ItemStack output = new ItemStack(ImmerseItems.ITEMS.get(recipe.output()).get());
    BaseCenterBlockEntity base = this.getBaseCenter();
    boolean craftable = this.minecraft != null && this.minecraft.player != null
        && recipe.canCraft(this.minecraft.player, base);
    String name = this.font.plainSubstrByWidth(output.getHoverName().getString(), 91);
    graphics.drawString(this.font, name, x + 4, y + 5, craftable ? 0x00CC00 : 0xCC0000, false);
    graphics.drawString(this.font,
        Component.translatable("player_recipes.craftingdeadimmerse.craft"), x + 112, y + 5,
        craftHovered ? 0xFFDD00 : 0xFFFFFF, false);
    graphics.renderFakeItem(output, x + 3, y + 16);
    int inputX = x + 52;
    for (BaseRecipe.Cost cost : recipe.costs()) {
      ItemStack stack = new ItemStack(cost.item(), cost.count());
      int have = this.minecraft == null || this.minecraft.player == null ? 0
          : recipe.count(this.minecraft.player, cost.item(), base);
      graphics.renderFakeItem(stack, inputX, y + 16);
      graphics.fill(inputX, y + 16, inputX + 16, y + 32,
          have >= cost.count() ? 0x2200FF00 : 0x44FF0000);
      graphics.renderItemDecorations(this.font, stack, inputX, y + 16);
      inputX += 18;
    }
  }

  private void renderMembers(GuiGraphics graphics, int mouseX, int mouseY) {
    BaseCenterBlockEntity base = this.getBaseCenter();
    if (base == null) {
      return;
    }
    List<UUID> members = this.memberRows(base);
    this.clampScroll(members.size());
    this.renderScroller(graphics, members.size());
    boolean canManage = this.canManage(base);
    if (canManage) {
      graphics.fill(this.leftPos + 6, this.topPos + 6, this.leftPos + 88,
          this.topPos + 17, 0xFF202020);
      if (this.input != null && this.input.getValue().isEmpty() && !this.input.isFocused()) {
        graphics.drawString(this.font,
            Component.translatable("container.craftingdeadimmerse.base.member_name"),
            this.leftPos + 10, this.topPos + 8, 0x888888, false);
      }
      this.drawSmallButton(graphics, this.leftPos + 91, this.topPos + 6, 13, 11,
          "+", false, this.isOver(this.leftPos + 91, this.topPos + 6, 13, 11, mouseX, mouseY));
    }
    for (int row = 0; row < VISIBLE_ROWS; row++) {
      int index = this.scroll + row;
      if (index >= members.size()) {
        break;
      }
      UUID id = members.get(index);
      int x = this.leftPos + ROW_X;
      int y = this.topPos + ROW_Y + row * ROW_HEIGHT;
      graphics.blit(TEXTURE, x, y, 0, 201, ROW_WIDTH, ROW_HEIGHT);
      this.renderPlayerHead(graphics, id, x + 4, y + 16, 18);
      BaseMemberRole role = base.getRole(id);
      int roleColor = this.roleColor(role);
      String display = this.font.plainSubstrByWidth(this.resolveDisplayName(id), 68);
      graphics.drawString(this.font, display, x + 4, y + 5, roleColor, false);
      graphics.drawString(this.font,
          Component.translatable("container.craftingdeadimmerse.base.role."
              + role.name().toLowerCase(Locale.ROOT)), x + 76, y + 5, roleColor, false);
      if (role != BaseMemberRole.OWNER) {
        Set<BasePermission> granted = base.getPermissions(id);
        for (int action = 0; action < PERMISSION_KEYS.length; action++) {
          int bx = x + 29 + action * 22;
          boolean active = action == 4 ? base.isAdministrator(id)
              : granted.contains(PERMISSIONS[action]);
          boolean actionable = action < 4 ? canManage : this.canSetAdministrator(base);
          String label = Component.translatable("container.craftingdeadimmerse.base.permission."
              + PERMISSION_KEYS[action] + ".short").getString();
          this.drawSmallButton(graphics, bx, y + 16, 20, 16, label, active,
              actionable && this.isOver(bx, y + 16, 20, 16, mouseX, mouseY));
        }
      }
    }
  }

  private void renderInformation(GuiGraphics graphics) {
    BaseCenterBlockEntity base = this.getBaseCenter();
    if (base == null) {
      return;
    }
    int x = this.leftPos + 6;
    int y = this.topPos + 19;
    graphics.blit(TEXTURE, x, y, 0, 201, ROW_WIDTH, ROW_HEIGHT);
    if (base.getOwner() != null) {
      this.renderPlayerHead(graphics, base.getOwner(), x + 4, y + 15, 18);
      graphics.drawString(this.font, this.resolveDisplayName(base.getOwner()), x + 28, y + 7,
          0xFFDD00, false);
    }
    String state = base.isBreached() ? "breached"
        : base.isUnderSiege() ? "siege" : base.isRecovering() ? "recovering" : "secure";
    graphics.drawString(this.font, Component.translatable(
        "container.craftingdeadimmerse.base.raid." + state, base.getRaidHealth(),
        BaseCenterBlockEntity.MAX_RAID_HEALTH, base.getRaidSecondsRemaining()), x + 28, y + 20,
        base.isUnderSiege() ? 0xFF7777 : 0x77FF99, false);
    graphics.drawString(this.font,
        Component.translatable("container.craftingdeadimmerse.base.repair_storage"),
        x + 3, y + 39, 0xFFFFFF, false);
    this.drawSmallButton(graphics, x + 91, y + 36, 24, 14,
        Component.translatable("container.craftingdeadimmerse.base.import.short").getString(),
        false, false);
    this.drawSmallButton(graphics, x + 117, y + 36, 24, 14,
        Component.translatable("container.craftingdeadimmerse.base.repair.short").getString(),
        false, false);
    List<StorageCell> cells = this.storageCells(base);
    int rows = Math.max(1, (cells.size() + 7) / 8);
    this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, rows - 4)));
    for (int row = 0; row < 4; row++) {
      int storageRow = this.scroll + row;
      int rowY = y + 53 + row * 21;
      graphics.blit(TEXTURE, x, rowY, 0, 236, 144, 18);
      for (int slot = 0; slot < 8; slot++) {
        int index = storageRow * 8 + slot;
        if (index >= cells.size()) {
          break;
        }
        StorageCell cell = cells.get(index);
        ItemStack stack = new ItemStack(cell.item(), cell.count());
        graphics.renderFakeItem(stack, x + 1 + slot * 18, rowY + 1);
        graphics.renderItemDecorations(this.font, stack, x + 1 + slot * 18, rowY + 1);
      }
    }
    this.renderScroller(graphics, rows + VISIBLE_ROWS - 4);
  }

  private void drawSmallButton(GuiGraphics graphics, int x, int y, int width, int height,
      String text, boolean active, boolean hovered) {
    int background = active ? 0xFF397A46 : hovered ? 0xFF777777 : 0xFF4A4A4A;
    graphics.fill(x, y, x + width, y + height, 0xFF111111);
    graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, background);
    int textX = x + (width - this.font.width(text)) / 2;
    graphics.drawString(this.font, text, textX, y + (height - 8) / 2, 0xFFFFFF, false);
  }

  private void renderScroller(GuiGraphics graphics, int count) {
    if (count <= VISIBLE_ROWS) {
      return;
    }
    int max = count - VISIBLE_ROWS;
    int thumbHeight = Math.max(12, SCROLLER_HEIGHT * VISIBLE_ROWS / count);
    int offset = max == 0 ? 0 : (SCROLLER_HEIGHT - thumbHeight) * this.scroll / max;
    int x = this.leftPos + SCROLLER_X;
    int y = this.topPos + SCROLLER_Y;
    graphics.fill(x, y, x + 12, y + SCROLLER_HEIGHT, 0xFF303030);
    graphics.fill(x + 2, y + offset, x + 10, y + offset + thumbHeight, 0xFF777777);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (button == 0) {
      for (int tab = 0; tab < 3; tab++) {
        if (this.isOver(this.leftPos + tab * 30, this.topPos - 28, 29, 28, mouseX, mouseY)) {
          this.mode = Mode.values()[tab];
          this.scroll = 0;
          this.playClick();
          this.rebuildInput();
          return true;
        }
      }
      int count = this.scrollEntryCount();
      if (count > VISIBLE_ROWS && this.isOver(this.leftPos + SCROLLER_X,
          this.topPos + SCROLLER_Y, 13, SCROLLER_HEIGHT, mouseX, mouseY)) {
        this.draggingScroller = true;
        this.updateScrollFromMouse(mouseY, count);
        return true;
      }
      if (this.mode == Mode.RECIPES && this.handleRecipeClick(mouseX, mouseY)) {
        return true;
      }
      if (this.mode == Mode.MEMBERS && this.handleMemberClick(mouseX, mouseY)) {
        return true;
      }
      if (this.mode == Mode.INFORMATION && this.handleInformationClick(mouseX, mouseY)) {
        return true;
      }
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  private boolean handleRecipeClick(double mouseX, double mouseY) {
    List<BaseRecipe> recipes = this.filteredRecipes();
    for (int row = 0; row < VISIBLE_ROWS; row++) {
      int index = this.scroll + row;
      int y = this.topPos + ROW_Y + row * ROW_HEIGHT;
      if (index < recipes.size() && this.isOver(this.leftPos + 117, y + 3, 27, 10,
          mouseX, mouseY)) {
        this.sendCommand("base craft " + recipes.get(index).id());
        this.playClick();
        return true;
      }
    }
    return false;
  }

  private boolean handleMemberClick(double mouseX, double mouseY) {
    BaseCenterBlockEntity base = this.getBaseCenter();
    if (base == null || !this.canManage(base)) {
      return false;
    }
    if (this.isOver(this.leftPos + 91, this.topPos + 6, 13, 11, mouseX, mouseY)
        && this.input != null && !this.input.getValue().trim().isEmpty()) {
      this.sendCommand("base add " + this.input.getValue().trim());
      this.input.setValue("");
      return true;
    }
    List<UUID> members = this.memberRows(base);
    for (int row = 0; row < VISIBLE_ROWS; row++) {
      int index = this.scroll + row;
      if (index >= members.size()) {
        break;
      }
      UUID id = members.get(index);
      if (base.getRole(id) == BaseMemberRole.OWNER) {
        continue;
      }
      String name = this.resolveCommandName(id);
      if (name == null) {
        continue;
      }
      int y = this.topPos + ROW_Y + row * ROW_HEIGHT + 16;
      for (int action = 0; action < PERMISSION_KEYS.length; action++) {
        int x = this.leftPos + ROW_X + 29 + action * 22;
        if (this.isOver(x, y, 20, 16, mouseX, mouseY)) {
          if (action == 4 && !this.canSetAdministrator(base)) {
            return true;
          }
          this.sendCommand(action == 4 ? "base administrator " + name
              : "base permission " + name + " "
                  + PERMISSIONS[action].name().toLowerCase(Locale.ROOT));
          this.playClick();
          return true;
        }
      }
    }
    return false;
  }

  private boolean handleInformationClick(double mouseX, double mouseY) {
    BaseCenterBlockEntity base = this.getBaseCenter();
    if (base == null) {
      return false;
    }
    int x = this.leftPos + 6;
    int y = this.topPos + 19;
    if (this.isOver(x + 91, y + 36, 24, 14, mouseX, mouseY)) {
      this.sendCommand("base storage import");
      this.playClick();
      return true;
    }
    if (this.isOver(x + 117, y + 36, 24, 14, mouseX, mouseY)) {
      this.sendCommand("base storage repair");
      this.playClick();
      return true;
    }
    List<StorageCell> cells = this.storageCells(base);
    for (int row = 0; row < 4; row++) {
      int rowY = y + 53 + row * 21;
      for (int slot = 0; slot < 8; slot++) {
        int index = (this.scroll + row) * 8 + slot;
        if (index < cells.size()
            && this.isOver(x + 1 + slot * 18, rowY + 1, 16, 16, mouseX, mouseY)) {
          this.sendCommand("base storage export " + cells.get(index).type());
          this.playClick();
          return true;
        }
      }
    }
    return false;
  }

  private void renderMemberActionTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
    if (this.mode != Mode.MEMBERS) {
      return;
    }
    BaseCenterBlockEntity base = this.getBaseCenter();
    if (base == null) {
      return;
    }
    List<UUID> members = this.memberRows(base);
    for (int row = 0; row < VISIBLE_ROWS && this.scroll + row < members.size(); row++) {
      if (base.getRole(members.get(this.scroll + row)) == BaseMemberRole.OWNER) {
        continue;
      }
      int y = this.topPos + ROW_Y + row * ROW_HEIGHT + 16;
      for (int action = 0; action < PERMISSION_KEYS.length; action++) {
        int x = this.leftPos + ROW_X + 29 + action * 22;
        if (this.isOver(x, y, 20, 16, mouseX, mouseY)) {
          graphics.renderTooltip(this.font, Component.translatable(
              "container.craftingdeadimmerse.base.permission." + PERMISSION_KEYS[action]),
              mouseX, mouseY);
          return;
        }
      }
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    int count = this.scrollEntryCount();
    int old = this.scroll;
    this.scroll = Math.max(0, Math.min(Math.max(0, count - VISIBLE_ROWS),
        this.scroll + (scrollY < 0 ? 1 : scrollY > 0 ? -1 : 0)));
    return old != this.scroll || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX,
      double dragY) {
    if (this.draggingScroller) {
      int count = this.scrollEntryCount();
      this.updateScrollFromMouse(mouseY, count);
      return true;
    }
    return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    this.draggingScroller = false;
    return super.mouseReleased(mouseX, mouseY, button);
  }

  private void updateScrollFromMouse(double mouseY, int count) {
    int max = Math.max(0, count - VISIBLE_ROWS);
    double progress = (mouseY - (this.topPos + SCROLLER_Y)) / SCROLLER_HEIGHT;
    this.scroll = Math.max(0, Math.min(max, (int) Math.round(progress * max)));
  }

  private List<BaseRecipe> filteredRecipes() {
    String query = this.search.toLowerCase(Locale.ROOT).trim();
    return BaseRecipe.baseRecipes().stream().filter(recipe -> query.isEmpty()
        || new ItemStack(ImmerseItems.ITEMS.get(recipe.output()).get()).getHoverName().getString()
            .toLowerCase(Locale.ROOT).contains(query)).toList();
  }

  private int scrollEntryCount() {
    BaseCenterBlockEntity base = this.getBaseCenter();
    return switch (this.mode) {
      case RECIPES -> this.filteredRecipes().size();
      case MEMBERS -> base == null ? 0 : this.memberRows(base).size();
      case INFORMATION -> base == null ? 0
          : Math.max(1, (this.storageCells(base).size() + 7) / 8);
    };
  }

  private List<StorageCell> storageCells(BaseCenterBlockEntity base) {
    List<StorageCell> cells = new ArrayList<>();
    this.addStorageCells(cells, ImmerseItems.ITEMS.get(ImmerseItems.SPLIT_LOG).get(),
        base.getStoredSplitLogs(), 0);
    this.addStorageCells(cells, ImmerseItems.ITEMS.get(ImmerseItems.STONE_PEBBLE).get(),
        base.getStoredStonePebbles(), 1);
    this.addStorageCells(cells, com.craftingdead.core.world.item.ModItems.SCRAP_METAL.get(),
        base.getStoredScrapMetal(), 2);
    return cells;
  }

  private void addStorageCells(List<StorageCell> cells, Item item, int count, int type) {
    int remaining = count;
    while (remaining > 0) {
      int stackSize = Math.min(64, remaining);
      cells.add(new StorageCell(item, stackSize, type));
      remaining -= stackSize;
    }
  }

  private List<UUID> memberRows(BaseCenterBlockEntity base) {
    List<UUID> rows = new ArrayList<>();
    if (base.getOwner() != null) {
      rows.add(base.getOwner());
    }
    rows.addAll(base.getMembers().stream().sorted(Comparator.comparing(this::resolveDisplayName))
        .toList());
    return rows;
  }

  private boolean canManage(BaseCenterBlockEntity base) {
    return this.minecraft != null && this.minecraft.player != null
        && (base.isOwner(this.minecraft.player)
            || base.isAdministrator(this.minecraft.player.getUUID()));
  }

  private boolean canSetAdministrator(BaseCenterBlockEntity base) {
    return this.minecraft != null && this.minecraft.player != null
        && base.isOwner(this.minecraft.player);
  }

  @Nullable
  private BaseCenterBlockEntity getBaseCenter() {
    if (this.minecraft == null || this.minecraft.level == null || this.minecraft.player == null) {
      return null;
    }
    if (this.minecraft.level.getBlockEntity(this.menu.getBasePos())
        instanceof BaseCenterBlockEntity exact) {
      return exact;
    }
    return BaseSecurity.findBase(this.minecraft.level, this.minecraft.player.blockPosition())
        .orElse(null);
  }

  private void renderPlayerHead(GuiGraphics graphics, UUID id, int x, int y, int size) {
    ResourceLocation skin = this.resolveSkin(id);
    graphics.blit(skin, x, y, size, size, 8, 8, 8, 8, 64, 64);
    graphics.blit(skin, x, y, size, size, 40, 8, 8, 8, 64, 64);
  }

  private ResourceLocation resolveSkin(UUID id) {
    PlayerInfo info = this.playerInfo(id);
    return info == null ? DefaultPlayerSkin.get(id).texture() : info.getSkin().texture();
  }

  private String resolveDisplayName(UUID id) {
    PlayerInfo info = this.playerInfo(id);
    return info == null ? id.toString().substring(0, 8) : info.getProfile().getName();
  }

  @Nullable
  private String resolveCommandName(UUID id) {
    PlayerInfo info = this.playerInfo(id);
    return info == null ? null : info.getProfile().getName();
  }

  @Nullable
  private PlayerInfo playerInfo(UUID id) {
    return this.minecraft == null || this.minecraft.getConnection() == null
        ? null : this.minecraft.getConnection().getPlayerInfo(id);
  }

  private int roleColor(BaseMemberRole role) {
    return switch (role) {
      case OWNER -> 0xFFDD00;
      case ADMIN -> 0xFF5555;
      case TRUSTED -> 0x55FFFF;
      case MEMBER -> 0x55FF55;
    };
  }

  private void clampScroll(int count) {
    this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, count - VISIBLE_ROWS)));
  }

  private boolean isOver(int x, int y, int width, int height, double mouseX, double mouseY) {
    return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
  }

  private void sendCommand(String command) {
    if (this.minecraft != null && this.minecraft.getConnection() != null) {
      this.minecraft.getConnection().sendCommand(command);
    }
  }

  private void playClick() {
    if (this.minecraft != null && this.minecraft.player != null) {
      this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.2F, 1.0F);
    }
  }
}

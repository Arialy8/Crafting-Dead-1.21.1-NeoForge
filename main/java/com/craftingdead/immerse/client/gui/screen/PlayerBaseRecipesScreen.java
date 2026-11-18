package com.craftingdead.immerse.client.gui.screen;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.network.NetworkChannel;
import com.craftingdead.core.network.message.play.OpenCraftingMenuMessage;
import com.craftingdead.core.network.message.play.OpenEquipmentMenuMessage;
import com.craftingdead.immerse.world.base.BaseRecipe;
import com.craftingdead.immerse.world.item.ImmerseItems;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** Player-inventory base recipes, laid out like TECU 1.18. */
public class PlayerBaseRecipesScreen extends Screen {

  private static final ResourceLocation EQUIPMENT = ResourceLocation.fromNamespaceAndPath(
      CraftingDead.ID, "textures/gui/container/equipment.png");
  private static final int IMAGE_WIDTH = 176;
  private static final int IMAGE_HEIGHT = 165;
  private static final int VISIBLE_ROWS = 4;
  private int leftPos;
  private int topPos;
  private int scroll;
  private String search = "";
  private EditBox searchBox;
  private boolean draggingScroller;

  public PlayerBaseRecipesScreen() {
    super(Component.translatable("screen.craftingdeadimmerse.player_base_recipes"));
  }

  public static void open() {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player != null) {
      minecraft.player.closeContainer();
    }
    minecraft.setScreen(new PlayerBaseRecipesScreen());
  }

  @Override
  protected void init() {
    this.leftPos = (this.width - IMAGE_WIDTH) / 2;
    this.topPos = (this.height - IMAGE_HEIGHT) / 2;
    this.searchBox = new EditBox(this.font, this.leftPos + 10, this.topPos + 8, 78, 8,
        Component.translatable("container.craftingdeadimmerse.base.search"));
    this.searchBox.setBordered(false);
    this.searchBox.setTextColor(0xFFFFFF);
    this.searchBox.setMaxLength(40);
    this.searchBox.setValue(this.search);
    this.searchBox.setResponder(value -> {
      this.search = value;
      this.scroll = 0;
    });
    this.addRenderableWidget(this.searchBox);
  }

  @Override
  public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    this.renderTransparentBackground(graphics);
    graphics.blit(BaseCenterScreen.TEXTURE, this.leftPos, this.topPos, 0, 0,
        IMAGE_WIDTH, IMAGE_HEIGHT);
    this.renderTabs(graphics);
    List<BaseRecipe> recipes = this.filteredRecipes();
    this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, recipes.size() - VISIBLE_ROWS)));
    this.renderScroller(graphics, recipes.size());
    for (int row = 0; row < VISIBLE_ROWS; row++) {
      int index = this.scroll + row;
      if (index >= recipes.size()) {
        break;
      }
      this.renderRecipe(graphics, recipes.get(index), row, mouseX, mouseY);
    }
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  /** Screen.render() invokes this again after our panel is drawn; keep it empty to avoid
   * applying the 1.21 post-process blur over the complete recipe interface. */
  @Override
  public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY,
      float partialTick) {}

  private void renderTabs(GuiGraphics graphics) {
    graphics.blit(EQUIPMENT, this.leftPos, this.topPos - 28, 183, 36, 28, 28);
    graphics.blit(EQUIPMENT, this.leftPos + 30, this.topPos - 28, 211, 36, 29, 28);
    graphics.blit(EQUIPMENT, this.leftPos + 60, this.topPos - 28, 211, 0, 29, 35);
    graphics.renderFakeItem(new ItemStack(com.craftingdead.core.world.item.ModItems
        .MEDIUM_BLUE_BACKPACK.get()), this.leftPos + 6, this.topPos - 20);
    graphics.renderFakeItem(new ItemStack(com.craftingdead.core.world.item.ModItems.PICKAXE.get()),
        this.leftPos + 36, this.topPos - 22);
    graphics.renderFakeItem(new ItemStack(ImmerseItems.ITEMS.get("base_center").get()),
        this.leftPos + 66, this.topPos - 22);
  }

  private void renderRecipe(GuiGraphics graphics, BaseRecipe recipe, int row, int mouseX,
      int mouseY) {
    int x = this.leftPos + 6;
    int y = this.topPos + 19 + row * 35;
    graphics.blit(BaseCenterScreen.TEXTURE, x, y, 0, 165, 143, 35);
    ItemStack output = new ItemStack(ImmerseItems.ITEMS.get(recipe.output()).get());
    boolean craftable = this.minecraft != null && this.minecraft.player != null
        && recipe.canCraft(this.minecraft.player);
    graphics.drawString(this.font, this.font.plainSubstrByWidth(output.getHoverName().getString(), 91),
        x + 4, y + 5, craftable ? 0x00CC00 : 0xCC0000, false);
    boolean hovered = this.isOver(x + 111, y + 3, 27, 10, mouseX, mouseY);
    graphics.drawString(this.font,
        Component.translatable("player_recipes.craftingdeadimmerse.craft"), x + 112, y + 5,
        hovered ? 0xFFDD00 : 0xFFFFFF, false);
    graphics.renderFakeItem(output, x + 3, y + 16);
    int inputX = x + 52;
    for (BaseRecipe.Cost cost : recipe.costs()) {
      ItemStack input = new ItemStack(cost.item(), cost.count());
      int have = this.minecraft == null || this.minecraft.player == null ? 0
          : recipe.count(this.minecraft.player, cost.item());
      graphics.renderFakeItem(input, inputX, y + 16);
      graphics.fill(inputX, y + 16, inputX + 16, y + 32,
          have >= cost.count() ? 0x2200FF00 : 0x44FF0000);
      graphics.renderItemDecorations(this.font, input, inputX, y + 16);
      inputX += 18;
    }
  }

  private void renderScroller(GuiGraphics graphics, int count) {
    if (count <= VISIBLE_ROWS) {
      return;
    }
    int max = count - VISIBLE_ROWS;
    int thumbHeight = Math.max(12, 125 * VISIBLE_ROWS / count);
    int offset = (125 - thumbHeight) * this.scroll / max;
    int x = this.leftPos + 156;
    int y = this.topPos + 20;
    graphics.fill(x, y, x + 12, y + 125, 0xFF303030);
    graphics.fill(x + 2, y + offset, x + 10, y + offset + thumbHeight, 0xFF777777);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (button == 0) {
      if (this.isOver(this.leftPos, this.topPos - 28, 29, 28, mouseX, mouseY)) {
        NetworkChannel.PLAY.sendToServer(new OpenEquipmentMenuMessage());
        this.playClick();
        return true;
      }
      if (this.isOver(this.leftPos + 30, this.topPos - 28, 29, 28, mouseX, mouseY)) {
        NetworkChannel.PLAY.sendToServer(new OpenCraftingMenuMessage());
        this.playClick();
        return true;
      }
      List<BaseRecipe> recipes = this.filteredRecipes();
      if (recipes.size() > VISIBLE_ROWS && this.isOver(this.leftPos + 156, this.topPos + 20,
          13, 125, mouseX, mouseY)) {
        this.draggingScroller = true;
        this.updateScroll(mouseY, recipes.size());
        return true;
      }
      for (int row = 0; row < VISIBLE_ROWS; row++) {
        int index = this.scroll + row;
        int y = this.topPos + 19 + row * 35;
        if (index < recipes.size() && this.isOver(this.leftPos + 117, y + 3, 27, 10,
            mouseX, mouseY)) {
          if (this.minecraft != null && this.minecraft.getConnection() != null) {
            this.minecraft.getConnection().sendCommand("base craft " + recipes.get(index).id());
          }
          this.playClick();
          return true;
        }
      }
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    int max = Math.max(0, this.filteredRecipes().size() - VISIBLE_ROWS);
    int old = this.scroll;
    this.scroll = Math.max(0, Math.min(max,
        this.scroll + (scrollY < 0 ? 1 : scrollY > 0 ? -1 : 0)));
    return old != this.scroll || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX,
      double dragY) {
    if (this.draggingScroller) {
      this.updateScroll(mouseY, this.filteredRecipes().size());
      return true;
    }
    return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    this.draggingScroller = false;
    return super.mouseReleased(mouseX, mouseY, button);
  }

  private void updateScroll(double mouseY, int count) {
    int max = Math.max(0, count - VISIBLE_ROWS);
    double progress = (mouseY - (this.topPos + 20)) / 125.0D;
    this.scroll = Math.max(0, Math.min(max, (int) Math.round(progress * max)));
  }

  private List<BaseRecipe> filteredRecipes() {
    String query = this.search.toLowerCase(Locale.ROOT).trim();
    return BaseRecipe.playerRecipes().stream().filter(recipe -> query.isEmpty()
        || new ItemStack(ImmerseItems.ITEMS.get(recipe.output()).get()).getHoverName().getString()
            .toLowerCase(Locale.ROOT).contains(query)).toList();
  }

  private boolean isOver(int x, int y, int width, int height, double mouseX, double mouseY) {
    return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
  }

  private void playClick() {
    if (this.minecraft != null && this.minecraft.player != null) {
      this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.2F, 1.0F);
    }
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}

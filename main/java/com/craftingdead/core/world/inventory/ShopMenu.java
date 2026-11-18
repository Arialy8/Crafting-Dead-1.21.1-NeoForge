package com.craftingdead.core.world.inventory;

import com.craftingdead.core.world.shop.ShopCatalog;
import com.craftingdead.core.world.shop.ShopCategory;
import com.craftingdead.core.world.shop.ShopProduct;
import com.craftingdead.core.world.shop.ShopServer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * 商店容器菜单（类 ChestCommand 风格）：6 行箱子界面，四周用五彩（彩虹渐变）玻璃板装饰，
 * 商品区商品与灰色玻璃板交替摆放（4 列 × 4 行），第 6 行为导航栏。左键购买、
 * Shift+左键尽量多买、右键出售全部；右键/Shift+左键背包物品可直接出售。
 */
public class ShopMenu extends ChestMenu {

  private static final int ROWS = 6;
  /** 商店区槽位数（0..53），即 ChestCommand 风格的商店“箱子”。 */
  private static final int SHOP_SLOTS = ROWS * 9;
  /** 商品区行数（四周玻璃板边框内）。 */
  private static final int PRODUCT_ROWS = 4;
  /** 每行商品数：商品与灰色玻璃板交替摆放（商品占奇数列 1/3/5/7）。 */
  private static final int PRODUCT_COLS = 4;
  /** 单页最多商品数。 */
  private static final int PRODUCTS_PER_PAGE = PRODUCT_ROWS * PRODUCT_COLS;

  private final SimpleContainer shopContainer;
  private final Inventory playerInventory;

  /** -1 = 分类主页；否则为当前分类下标。 */
  private int categoryIndex = -1;
  private int page;

  public ShopMenu(int id, Inventory inventory) {
    this(id, inventory, new SimpleContainer(SHOP_SLOTS));
  }

  private ShopMenu(int id, Inventory inventory, SimpleContainer container) {
    super(ModMenuTypes.SHOP.get(), id, inventory, container, ROWS);
    this.shopContainer = container;
    this.playerInventory = inventory;
    for (int i = 0; i < SHOP_SLOTS; i++) {
      var old = this.slots.get(i);
      var locked = new net.minecraft.world.inventory.Slot(container, i, old.x, old.y) {
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
      };
      locked.index = i;
      this.slots.set(i, locked);
    }
    this.refreshAll();
  }

  @Override
  public void clicked(int slotId, int button, ClickType clickType, Player player) {
    if (slotId >= 0 && slotId < SHOP_SLOTS) {
      // 商店区是展示/按钮槽：任何点击都由服务端按“购买/出售/翻页”处理，绝不转移物品。
      if (!player.level().isClientSide()) {
        this.handleShopSlot(slotId, button, clickType, (ServerPlayer) player);
      }
      return;
    }
    // 背包槽右键（光标为空时）：若该物品可回收则直接出售；客户端同样拦截，避免拾取导致不同步
    if (slotId >= SHOP_SLOTS && slotId < this.slots.size()
        && clickType == ClickType.PICKUP && button == 1
        && this.getCarried().isEmpty()) {
      if (!player.level().isClientSide()) {
        this.sellInventorySlot(slotId, (ServerPlayer) player);
      }
      return;
    }
    super.clicked(slotId, button, clickType, player);
  }

  /** 出售背包槽位上的物品（卖光全部匹配物品）。 */
  private void sellInventorySlot(int slotId, ServerPlayer player) {
    ShopCatalog.ProductRef ref = ShopCatalog.findSellable(this.slots.get(slotId).getItem());
    if (ref == null) {
      return; // 非商店回收商品：忽略该点击
    }
    ShopServer.handleSell(player, ref.categoryIndex(), ref.productIndex(),
        ShopServer.QUANTITY_MAX);
    this.refreshAll();
    this.broadcastChanges();
  }

  /**
   * Shift+左键背包物品：若该物品是商店可回收商品，直接出售全部匹配物品。
   */
  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    if (player.level().isClientSide()) {
      return ItemStack.EMPTY;
    }
    if (index >= SHOP_SLOTS && index < this.slots.size()) {
      ShopCatalog.ProductRef ref = ShopCatalog.findSellable(this.slots.get(index).getItem());
      if (ref != null) {
        ShopServer.handleSell((ServerPlayer) player, ref.categoryIndex(), ref.productIndex(),
            ShopServer.QUANTITY_MAX);
        this.refreshAll();
        this.broadcastChanges();
      }
    }
    return ItemStack.EMPTY;
  }

  private void handleShopSlot(int slotId, int button, ClickType clickType, ServerPlayer player) {
    if (clickType != ClickType.PICKUP && clickType != ClickType.QUICK_MOVE && clickType != ClickType.CLONE) return;
    if (slotId >= PRODUCT_ROWS * 9 + 9) {
      // 第 6 行导航栏
      this.handleNavSlot(slotId, player);
      return;
    }
    if (!isProductSlot(slotId)) {
      return; // 装饰玻璃板，无操作
    }
    int index = pageIndexFromSlot(slotId);
    if (index < 0) {
      return;
    }

    if (this.categoryIndex == -1) {
      // 主页：点击分类图标进入分类
      List<ShopCategory> categories = ShopCatalog.categories();
      if (index < categories.size()) {
        this.categoryIndex = index;
        this.page = 0;
        this.refreshAll();
        this.broadcastChanges();
      }
      return;
    }

    // 分类页：商品槽点击 = 购买/出售
    ShopCategory category = this.currentCategory();
    int productIndex = this.page * PRODUCTS_PER_PAGE + index;
    if (category == null || productIndex >= category.products().size()) {
      return;
    }
    ShopProduct product = category.products().get(productIndex);
    if (clickType == ClickType.CLONE && button == 2) {
      com.craftingdead.core.world.shop.MaBalanceData.get(player.server)
          .toggleFavourite(player.getUUID(), product.itemId());
      this.refreshAll();
      this.broadcastChanges();
      return;
    }
    if (clickType == ClickType.CLONE) return;
    int actualCategory = this.categoryIndex;
    int actualProduct = productIndex;
    if (this.categoryIndex == -2) {
      var ref = this.findProduct(product.itemId());
      if (ref == null) return;
      actualCategory = ref.categoryIndex();
      actualProduct = ref.productIndex();
    }
    boolean shift = clickType == ClickType.QUICK_MOVE;
    if (button == 0) {
      ShopServer.handleBuy(player, actualCategory, actualProduct,
          shift ? ShopServer.QUANTITY_MAX : ShopServer.QUANTITY_SINGLE);
    } else if (button == 1) {
      // 右键 = 出售全部该商品（Shift 与否一样）
      ShopServer.handleSell(player, actualCategory, actualProduct,
          ShopServer.QUANTITY_MAX);
    } else {
      return;
    }
    this.refreshAll();
    this.broadcastChanges();
  }

  private void handleNavSlot(int slotId, ServerPlayer player) {
    if (slotId == 48) {
      this.categoryIndex = -2;
      this.page = 0;
      this.refreshAll();
      this.broadcastChanges();
      return;
    }
    if (slotId == 53) {
      player.closeContainer();
      return;
    }
    if (this.categoryIndex == -1) {
      return; // 主页导航栏只有余额与操作说明
    }
    if (slotId == 45) {
      this.page = Math.max(0, this.page - 1);
    } else if (slotId == 46) {
      this.categoryIndex = -1;
      this.page = 0;
    } else if (slotId == 52) {
      ShopCategory category = this.currentCategory();
      int totalPages = category == null ? 1
          : Math.max(1, (category.products().size() + PRODUCTS_PER_PAGE - 1)
              / PRODUCTS_PER_PAGE);
      this.page = Math.min(totalPages - 1, this.page + 1);
    } else {
      return; // 余额/操作说明等纯展示槽
    }
    this.refreshAll();
    this.broadcastChanges();
  }

  /** 重建全部商店展示槽（标题行/五彩玻璃边框/商品/分类/导航/余额）。服务端与客户端都会调用。 */
  private void refreshAll() {
    // 顶部标题行：五彩玻璃板（彩虹渐变）+ 居中标题物品（带附魔光效）
    for (int i = 0; i < 9; i++) {
      this.shopContainer.setItem(i, i < 4 ? rainbowGlass(i) : rainbowGlass(i - 1));
    }
    this.shopContainer.setItem(4, this.titleIcon());
    // 商品区：左右边缘五彩玻璃板，商品之间统一灰色玻璃板
    for (int row = 1; row <= PRODUCT_ROWS; row++) {
      this.shopContainer.setItem(row * 9, rainbowGlass(row));
      this.shopContainer.setItem(row * 9 + 8, rainbowGlass(PRODUCT_ROWS + row));
      for (int col = 2; col <= 6; col += 2) {
        this.shopContainer.setItem(row * 9 + col, fillerGlass());
      }
    }
    // 商品区：商品格默认灰色玻璃板，避免界面空洞
    for (int i = 0; i < PRODUCTS_PER_PAGE; i++) {
      this.shopContainer.setItem(productSlot(i), fillerGlass());
    }
    if (this.categoryIndex == -1) {
      List<ShopCategory> categories = ShopCatalog.categories();
      for (int i = 0; i < categories.size() && i < PRODUCTS_PER_PAGE; i++) {
        this.shopContainer.setItem(productSlot(i), this.categoryIcon(categories.get(i)));
      }
    } else {
      ShopCategory category = this.currentCategory();
      if (category == null) {
        this.categoryIndex = -1;
        this.page = 0;
        this.refreshAll();
        return;
      }
      int totalPages = Math.max(1, (category.products().size() + PRODUCTS_PER_PAGE - 1)
          / PRODUCTS_PER_PAGE);
      this.page = Math.min(this.page, totalPages - 1);
      int start = this.page * PRODUCTS_PER_PAGE;
      List<ShopProduct> products = category.products();
      for (int i = 0; i < PRODUCTS_PER_PAGE; i++) {
        int productIndex = start + i;
        if (productIndex < products.size()) {
          this.shopContainer.setItem(productSlot(i), this.productIcon(products.get(productIndex)));
        }
      }
    }

    // 第 6 行导航栏：45 上一页 / 46 返回主页 / 49 余额 / 52 下一页 / 53 操作说明（其余五彩玻璃板）
    for (int i = PRODUCT_ROWS * 9 + 9; i < SHOP_SLOTS; i++) {
      this.shopContainer.setItem(i, rainbowGlass(i));
    }
    this.shopContainer.setItem(48, button(Items.HEART_OF_THE_SEA, "我的收藏", "鼠标中键商品：收藏 / 取消收藏"));
    this.shopContainer.setItem(49, this.balanceIcon());
    this.shopContainer.setItem(50, helpIcon());
    this.shopContainer.setItem(53, button(Items.BARRIER, "关闭商店"));
    if (this.categoryIndex != -1) {
      String pageText = this.pageText();
      this.shopContainer.setItem(45,
          button(Items.ARROW, "上一页", pageText, "点击查看上一页"));
      this.shopContainer.setItem(46,
          button(Items.OAK_SIGN, "返回主页", "点击回到商店主页"));
      this.shopContainer.setItem(52,
          button(Items.ARROW, "下一页", pageText, "点击查看下一页"));
    }
  }

  /**
   * 标题行中央的展示物品：主页显示商店名（绿宝石），分类页显示分类图标与页数。
   */
  private ItemStack titleIcon() {
    ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
    stack.set(DataComponents.PROFILE, new net.minecraft.world.item.component.ResolvableProfile(
        this.playerInventory.player.getGameProfile()));
    stack.set(DataComponents.CUSTOM_NAME, Component.literal(
        this.playerInventory.player.getGameProfile().getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
    stack.set(DataComponents.LORE, new ItemLore(List.of(
        Component.literal("商店 · " + this.pageText()),
        Component.literal("左键购买 · 右键出售 · Shift 全部"))));
    return stack;
  }

  private java.util.Set<String> favouriteIds() {
    return this.playerInventory.player instanceof ServerPlayer player
        ? com.craftingdead.core.world.shop.MaBalanceData.get(player.server).favourites(player.getUUID())
        : java.util.Set.of();
  }

  private ShopCatalog.ProductRef findProduct(String itemId) {
    for (int c = 0; c < ShopCatalog.categories().size(); c++) {
      var products = ShopCatalog.categories().get(c).products();
      for (int i = 0; i < products.size(); i++)
        if (products.get(i).itemId().equals(itemId)) return new ShopCatalog.ProductRef(c, i, products.get(i));
    }
    return null;
  }

  private ShopCategory currentCategory() {
    if (this.categoryIndex != -2) return ShopCatalog.getCategory(this.categoryIndex);
    var ids = this.favouriteIds();
    var products = ShopCatalog.categories().stream().flatMap(c -> c.products().stream())
        .filter(product -> ids.contains(product.itemId())).toList();
    return new ShopCategory("favourites", "我的收藏", "minecraft:heart_of_the_sea", products);
  }

  private String pageText() {
    ShopCategory category = this.currentCategory();
    int totalPages = category == null ? 1
        : Math.max(1, (category.products().size() + PRODUCTS_PER_PAGE - 1)
            / PRODUCTS_PER_PAGE);
    return (category == null ? "主页" : category.label()) + " · 第 " + (this.page + 1) + " / " + totalPages + " 页";
  }

  private static boolean isProductSlot(int slotId) {
    int row = slotId / 9;
    int col = slotId % 9;
    return row >= 1 && row <= PRODUCT_ROWS
        && col >= 1 && col <= 7 && col % 2 == 1;
  }

  /** 商品区页内下标 -> 槽位号（第 1~4 行、第 1/3/5/7 列，与灰玻璃交替）。 */
  private static int productSlot(int index) {
    int row = index / PRODUCT_COLS;
    int col = index % PRODUCT_COLS * 2 + 1;
    return (row + 1) * 9 + col;
  }

  /** 槽位号 -> 商品区页内下标；非商品槽返回 -1。 */
  private static int pageIndexFromSlot(int slotId) {
    if (!isProductSlot(slotId)) {
      return -1;
    }
    int row = slotId / 9;
    int col = slotId % 9;
    return (row - 1) * PRODUCT_COLS + (col - 1) / 2;
  }

  private ItemStack productIcon(ShopProduct product) {
    ItemStack stack = ShopCatalog.resolve(product.itemId(), Math.min(product.amount(), 64));
    if (stack.isEmpty()) {
      return ItemStack.EMPTY;
    }
    stack.set(DataComponents.CUSTOM_NAME,
        stack.getHoverName().copy().withStyle(style -> style.withItalic(false)));
    stack.set(DataComponents.LORE, new ItemLore(List.of(
        Component.literal(product.buyable() ? "购买价：" + product.priceMa() + " ＄ / 份" : "仅出售 · 不可购买").withStyle(ChatFormatting.GOLD),
        Component.literal("回收价：" + product.sellPricePerItem() + " ＄ / 个")
            .withStyle(ChatFormatting.GRAY),
        Component.literal("左键购买 · Shift+左键 尽量多买").withStyle(ChatFormatting.GRAY),
        Component.literal("右键出售全部").withStyle(ChatFormatting.DARK_GRAY),
        Component.literal(this.favouriteIds().contains(product.itemId())
            ? "★ 已收藏 · 中键取消" : "中键收藏").withStyle(ChatFormatting.YELLOW))));
    return stack;
  }

  private ItemStack categoryIcon(ShopCategory category) {
    ItemStack stack = ShopCatalog.resolve(category.iconItemId(), 1);
    if (stack.isEmpty()) {
      stack = new ItemStack(Items.CHEST);
    }
    stack.set(DataComponents.CUSTOM_NAME,
        Component.literal(category.label()).withStyle(style -> style.withItalic(false)));
    stack.set(DataComponents.LORE, new ItemLore(List.of(
        Component.literal("点击进入分类").withStyle(ChatFormatting.GRAY))));
    return stack;
  }

  private ItemStack balanceIcon() {
    ItemStack stack = new ItemStack(Items.NETHER_STAR);
    int balance = this.playerInventory.player instanceof ServerPlayer player
        ? ShopServer.getBalance(player)
        : 0;
    stack.set(DataComponents.CUSTOM_NAME,
        Component.literal("余额：" + balance + " ＄").withStyle(ChatFormatting.GOLD));
    return stack;
  }

  private static ItemStack helpIcon() {
    ItemStack stack = new ItemStack(Items.BOOK);
    stack.set(DataComponents.CUSTOM_NAME,
        Component.literal("操作说明").withStyle(style -> style.withItalic(false)));
    stack.set(DataComponents.LORE, new ItemLore(List.of(
        Component.literal("左键商品：购买 1 份").withStyle(ChatFormatting.GRAY),
        Component.literal("Shift+左键：尽量多买").withStyle(ChatFormatting.GRAY),
        Component.literal("右键商品：出售全部该商品").withStyle(ChatFormatting.GRAY),
        Component.literal("右键背包物品：直接出售").withStyle(ChatFormatting.GRAY),
        Component.literal("鼠标中键商品：收藏 / 取消收藏").withStyle(ChatFormatting.YELLOW),
        Component.literal("Shift+右键商品：出售全部").withStyle(ChatFormatting.DARK_GRAY))));
    return stack;
  }

  private static ItemStack button(Item item, String name, String... lore) {
    ItemStack stack = new ItemStack(item);
    stack.set(DataComponents.CUSTOM_NAME,
        Component.literal(name).withStyle(style -> style.withItalic(false)));
    if (lore.length > 0) {
      List<Component> lines = new java.util.ArrayList<>(lore.length);
      for (String line : lore) {
        lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
      }
      stack.set(DataComponents.LORE, new ItemLore(lines));
    }
    return stack;
  }

  /** 五彩玻璃板：按彩虹色序循环取色（红橙黄绿 → 天蓝蓝紫品红）。 */
  private static final Item[] RAINBOW_PANES = {
      Items.RED_STAINED_GLASS_PANE,
      Items.ORANGE_STAINED_GLASS_PANE,
      Items.YELLOW_STAINED_GLASS_PANE,
      Items.LIME_STAINED_GLASS_PANE,
      Items.LIGHT_BLUE_STAINED_GLASS_PANE,
      Items.BLUE_STAINED_GLASS_PANE,
      Items.PURPLE_STAINED_GLASS_PANE,
      Items.MAGENTA_STAINED_GLASS_PANE
  };

  private static ItemStack rainbowGlass(int index) {
    ItemStack stack = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
    stack.set(DataComponents.CUSTOM_NAME,
        Component.literal(" ").withStyle(style -> style.withItalic(false)));
    return stack;
  }

  /** 商品区统一灰色玻璃板（商品间隔与空位填充）。 */
  private static ItemStack fillerGlass() {
    ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
    stack.set(DataComponents.CUSTOM_NAME,
        Component.literal(" ").withStyle(style -> style.withItalic(false)));
    return stack;
  }
}

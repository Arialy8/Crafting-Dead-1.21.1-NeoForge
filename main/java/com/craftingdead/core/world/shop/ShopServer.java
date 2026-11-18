package com.craftingdead.core.world.shop;

import com.craftingdead.core.world.inventory.ShopMenu;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 商店服务端逻辑：打开商店、处理购买请求（服务端权威校验扣款发货）。
 */
public final class ShopServer {

  /** 购买数量：1 = 一份；0 = 在余额/背包允许范围内尽量多买。 */
  public static final int QUANTITY_SINGLE = 1;
  public static final int QUANTITY_MAX = 0;

  private ShopServer() {}

  /**
   * 给玩家打开商店（类 ChestCommand 的箱子界面）。
   */
  public static void openFor(ServerPlayer player) {
    player.openMenu(new SimpleMenuProvider(
        (id, inv, p) -> new ShopMenu(id, inv),
        Component.literal("商店")));
  }

  /**
   * 读取玩家 ＄ 余额（默认给新玩家一笔初始金额便于体验）。
   */
  public static int getBalance(ServerPlayer player) {
    MaBalanceData data = MaBalanceData.get(player.server);
    UUID uuid = player.getUUID();
    // 只有从未领取过默认额度的新玩家才赠送，花光后余额保持 0
    return data.getBalance(uuid);
  }

  /**
   * 管理员加钱（amount 可为负），返回新余额。
   */
  public static int addMoney(ServerPlayer player, int amount) {
    return MaBalanceData.get(player.server).addBalance(player.getUUID(), amount);
  }

  /**
   * 处理购买请求：校验商品与余额，能买多少买多少（背包放不下则停止）。
   */
  public static void handleBuy(ServerPlayer player, int categoryIndex, int productIndex,
      int quantity) {
    ShopProduct product = ShopCatalog.getProduct(categoryIndex, productIndex);
    if (product == null) {
      sendResult(player, 0, 0, "商品不存在或已下架。");
      return;
    }
    ItemStack stack = ShopCatalog.resolve(product.itemId(), product.amount());
    if (!product.buyable()) {
      sendResult(player, getBalance(player), 0, "该商品仅支持出售，不能购买。");
      return;
    }
    if (stack.isEmpty()) {
      sendResult(player, 0, 0, "商品数据无效，请联系管理员。");
      return;
    }
    int price = product.priceMa();
    if (price <= 0) {
      sendResult(player, 0, 0, "商品价格无效，请联系管理员。");
      return;
    }

    int balance = getBalance(player);
    if (balance < price) {
      sendResult(player, balance, 0,
          "余额不足：需要 " + price + " ＄，还差 " + (price - balance) + " ＄。");
      return;
    }

    boolean buyMax = quantity == QUANTITY_MAX;
    int bought = 0;
    int cost = 0;
    MaBalanceData data = MaBalanceData.get(player.server);
    // 逐份尝试放入背包，钱不够或背包放不下即停止
    while ((buyMax || bought < 1) && balance - cost >= price) {
      if (!canFit(player.getInventory(), stack)) break;
      ItemStack toGive = stack.copy();
      if (!player.getInventory().add(toGive)) {
        break;
      }
      bought++;
      cost += price;
    }

    if (bought <= 0) {
      sendResult(player, balance, 0, "背包已满，无法购买。");
      return;
    }
    int newBalance = data.addBalance(player.getUUID(), -cost);
    player.containerMenu.broadcastChanges();
    sendResult(player, newBalance, bought,
        "成功购买 " + bought + " 份" + stack.getHoverName().getString()
            + "，花费 " + cost + " ＄，余额 " + newBalance + " ＄。");
  }

  /**
   * 处理出售请求：从背包回收与商品同种的物品。
   * quantity = 1 卖 1 个；QUANTITY_MAX 卖全部匹配物品。
   */
  public static void handleSell(ServerPlayer player, int categoryIndex, int productIndex,
      int quantity) {
    ShopProduct product = ShopCatalog.getProduct(categoryIndex, productIndex);
    if (product == null) {
      sendResult(player, 0, 0, "商品不存在或已下架。");
      return;
    }
    ItemStack template = ShopCatalog.resolve(product.itemId(), 1);
    if (template.isEmpty()) {
      sendResult(player, 0, 0, "商品数据无效，请联系管理员。");
      return;
    }
    int sellPrice = product.sellPricePerItem();
    if (sellPrice <= 0) {
      sendResult(player, 0, 0, "该商品暂不支持回收。");
      return;
    }

    boolean sellAll = quantity == QUANTITY_MAX;
    int wanted = sellAll ? Integer.MAX_VALUE : 1;
    Inventory inventory = player.getInventory();
    int sold = 0;
    for (int i = 0; i < inventory.items.size() && sold < wanted; i++) {
      ItemStack stack = inventory.getItem(i);
      if (stack.isEmpty() || !stack.is(template.getItem())) {
        continue;
      }
      int take = Math.min(stack.getCount(), wanted - sold);
      stack.shrink(take);
      sold += take;
    }

    if (sold <= 0) {
      sendResult(player, getBalance(player), 0,
          "背包里没有可出售的" + template.getHoverName().getString() + "。");
      return;
    }
    int gained = sold * sellPrice;
    int newBalance = MaBalanceData.get(player.server).addBalance(player.getUUID(), gained);
    player.containerMenu.broadcastChanges();
    sendResult(player, newBalance, sold,
        "已出售 " + sold + " 个" + template.getHoverName().getString()
            + "，获得 " + gained + " ＄，余额 " + newBalance + " ＄。");
  }

  private static void sendResult(ServerPlayer player, int balance, int count, String message) {
    player.displayClientMessage(Component.literal(message), true);
  }

  /** Check the whole purchase before inserting: Inventory.add may otherwise insert only part. */
  private static boolean canFit(Inventory inventory, ItemStack stack) {
    int remaining = stack.getCount();
    for (ItemStack existing : inventory.items) {
      if (existing.isEmpty()) remaining -= Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize());
      else if (ItemStack.isSameItemSameComponents(existing, stack))
        remaining -= Math.max(0, Math.min(inventory.getMaxStackSize(), existing.getMaxStackSize())
            - existing.getCount());
      if (remaining <= 0) return true;
    }
    return false;
  }
}

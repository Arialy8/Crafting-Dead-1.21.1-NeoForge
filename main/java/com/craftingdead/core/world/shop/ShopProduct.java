package com.craftingdead.core.world.shop;

/**
 * 商店单个商品：由真实物品 ID 标识，一份 = {@code amount} 个物品。
 *
 * @param itemId   物品 ID（如 {@code craftingdead:ak47}）
 * @param amount   每次购买一份包含的物品数量
 * @param priceMa  单份价格（MA）
 */
public record ShopProduct(String itemId, int amount, int priceMa, int resale, boolean buyable) {

  public ShopProduct(String itemId, int amount, int priceMa) {
    this(itemId, amount, priceMa, Math.max(1, (int) Math.floor(priceMa / (double) Math.max(1, amount) * 0.4)), true);
  }

  public ShopProduct {
    if (amount < 1) {
      amount = 1;
    }
    if (priceMa < 0) {
      priceMa = 0;
    }
  }

  public static ShopProduct of(String itemId, int priceMa) {
    return new ShopProduct(itemId, 1, priceMa);
  }

  /**
   * 单件回收价（MA）：按单价约 40% 回收，至少 1，防刷与倒卖差价。</
   */
  public int sellPricePerItem() {
    return Math.max(0, this.resale);
  }
}

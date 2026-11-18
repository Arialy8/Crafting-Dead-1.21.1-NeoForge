package com.craftingdead.core.world.shop;

import java.util.List;

/**
 * 商店分类。
 *
 * @param id          分类 ID
 * @param label       分类名（中文，与服务端消息一致）
 * @param iconItemId  分类图标物品 ID（客户端渲染用）
 * @param products    该分类商品列表
 */
public record ShopCategory(
    String id,
    String label,
    String iconItemId,
    List<ShopProduct> products) {}
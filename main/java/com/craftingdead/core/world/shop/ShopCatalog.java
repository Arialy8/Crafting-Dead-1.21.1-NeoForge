package com.craftingdead.core.world.shop;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 内置商店商品目录（服务端与客户端共用同一份，价格与上架以本表为唯一来源）。
 *
 * <p>首版把目录直接写在代码里保证双端一致与开箱可用；后续如需热更，再整体迁移到
 * {@code data/craftingdead/shop/*.json} 数据包（见设计文档第六节）。</p>
 */
public final class ShopCatalog {

  /** 商店主页网格中一行显示的分类数。 */
  public static final int GRID_WIDTH = 9;
  /** 商品区总行数（第 0~4 行商品，第 5 行导航）。 */
  public static final int GRID_ROWS = 6;
  /** 单页最多商品数。 */
  public static final int PRODUCTS_PER_PAGE = GRID_WIDTH * (GRID_ROWS - 1);

  private static final List<ShopCategory> CATEGORIES = List.of(
      category("guns", "枪械", "craftingdead:m4a1",
          p("craftingdead:crossbow", 1800),
          // 手枪
          p("craftingdead:p250", 700),
          p("craftingdead:fn57", 800),
          p("craftingdead:m9", 850),
          p("craftingdead:m1911", 900),
          p("craftingdead:taser", 900),
          p("craftingdead:g18", 900),
          p("craftingdead:magnum", 1500),
          p("craftingdead:desert_eagle", 1800),
          // 冲锋枪
          p("craftingdead:mac10", 1600),
          p("craftingdead:mp5a5", 1800),
          p("craftingdead:vector", 2000),
          p("craftingdead:p90", 2200),
          // 步枪/霰弹枪
          p("craftingdead:sporter22", 1200),
          p("craftingdead:trench_gun", 2000),
          p("craftingdead:mossberg", 2200),
          p("craftingdead:m1garand", 2400),
          p("craftingdead:g36c", 2600),
          p("craftingdead:ak47", 2900),
          p("craftingdead:m4a1", 3000),
          p("craftingdead:fnfal", 3200),
          p("craftingdead:mpt55", 3300),
          p("craftingdead:scarl", 3500),
          p("craftingdead:qbz97_girls_frontline", 3600),
          p("craftingdead:hk417", 3800),
          p("craftingdead:acr", 3900),
          // 狙击/机枪/重武
          p("craftingdead:dmr", 4500),
          p("craftingdead:rpk", 5000),
          p("craftingdead:m240b", 6000),
          p("craftingdead:mk48mod", 6800),
          p("craftingdead:awp", 7500),
          p("craftingdead:m107", 8500),
          p("craftingdead:as50", 9000),
          p("craftingdead:minigun", 11000)),

      category("melee", "近战武器", "craftingdead:scythe",
          p("craftingdead:broken_bottle", 40),
          p("craftingdead:frying_pan", 60),
          p("craftingdead:rusty_pipe", 80),
          p("craftingdead:pipe", 100),
          p("craftingdead:golf_club", 90),
          p("craftingdead:bo_staff", 120),
          p("craftingdead:cleaver", 150),
          p("craftingdead:bat", 150),
          p("craftingdead:nail_bat", 250),
          p("craftingdead:steel_bat", 300),
          p("craftingdead:night_stick", 250),
          p("craftingdead:wrench", 250),
          p("craftingdead:hatchet", 250),
          p("craftingdead:combat_knife", 350),
          p("craftingdead:bowie_knife", 400),
          p("craftingdead:shovel", 250),
          p("craftingdead:pickaxe", 350),
          p("craftingdead:crowbar", 300),
          p("craftingdead:sledgehammer", 350),
          p("craftingdead:machete", 500),
          p("craftingdead:fire_axe", 550),
          p("craftingdead:broadsword", 600),
          p("craftingdead:scythe", 700),
          p("craftingdead:weaponized_scythe", 800),
          p("craftingdead:katana", 900),
          p("craftingdead:chainsaw", 1200)),

      category("ammo", "弹药/弹匣", "craftingdead:stanag_box_magazine",
          p("craftingdead:crossbow_bolt", 35),
          p("craftingdead:fn57_magazine", 120),
          p("craftingdead:p250_magazine", 130),
          p("craftingdead:m9_magazine", 150),
          p("craftingdead:m1911_magazine", 150),
          p("craftingdead:g18_magazine", 180),
          p("craftingdead:taser_cartridge", 100),
          p("craftingdead:desert_eagle_magazine", 400),
          p("craftingdead:magnum_ammunition", 400),
          p("craftingdead:mp5a5_21_round_magazine", 200),
          p("craftingdead:mp5a5_35_round_magazine", 260),
          p("craftingdead:mac10_magazine", 180),
          p("craftingdead:mac10_extended_magazine", 230),
          p("craftingdead:p90_magazine", 250),
          p("craftingdead:vector_magazine", 280),
          p("craftingdead:sporter22_magazine", 200),
          p("craftingdead:m1garand_ammunition", 350),
          p("craftingdead:stanag_20_round_magazine", 400),
          p("craftingdead:stanag_30_round_magazine", 500),
          p("craftingdead:stanag_drum_magazine", 900),
          p("craftingdead:stanag_box_magazine", 1500),
          p("craftingdead:ak47_30_round_magazine", 400),
          p("craftingdead:g36c_magazine", 420),
          p("craftingdead:fnfal_magazine", 420),
          p("craftingdead:mpt55_magazine", 480),
          p("craftingdead:hk417_magazine", 520),
          p("craftingdead:acr_magazine", 550),
          p("craftingdead:dmr_magazine", 700),
          p("craftingdead:m240b_magazine", 800),
          p("craftingdead:mk48mod_magazine", 900),
          p("craftingdead:rpk_magazine", 500),
          p("craftingdead:rpk_drum_magazine", 1100),
          p("craftingdead:minigun_magazine", 1500),
          p("craftingdead:awp_magazine", 1100),
          p("craftingdead:m107_magazine", 1100),
          p("craftingdead:as50_magazine", 1200),
          p("craftingdead:mossberg_shells", 250),
          p("craftingdead:trench_gun_shells", 200)),

      category("attachment", "配件/瞄具", "craftingdead:acog_sight",
          p("craftingdead:tactical_grip", 300),
          p("craftingdead:red_dot_sight", 350),
          p("craftingdead:bipod", 400),
          p("craftingdead:eotech_sight", 600),
          p("craftingdead:lp_scope", 700),
          p("craftingdead:acog_sight", 800),
          p("craftingdead:suppressor", 800),
          p("craftingdead:hp_scope", 1000)),

      category("medical", "医疗", "craftingdead:first_aid_kit",
          p("craftingdeadsurvival:splint", 180),
          p("craftingdeadsurvival:cure_syringe", 600),
          p("craftingdead:dirty_rag", 15),
          p("craftingdead:bloody_rag", 30),
          p("craftingdead:clean_rag", 40),
          p("craftingdead:bandage", 80),
          p("craftingdead:syringe", 120),
          p("craftingdead:blood_syringe", 250),
          p("craftingdead:adrenaline_syringe", 400),
          p("craftingdead:first_aid_kit", 450)),

      category("food", "食物/饮料", "craftingdeadsurvival:canned_beans",
          p("craftingdeadsurvival:chips", 30),
          p("craftingdeadsurvival:cheesy_chips", 45),
          p("craftingdeadsurvival:flake_cereal", 60),
          p("craftingdeadsurvival:emerald_cereal", 100),
          p("craftingdeadsurvival:canned_beans", 90),
          p("craftingdeadsurvival:canned_custard", 90),
          p("craftingdeadsurvival:canned_pickles", 90),
          p("craftingdeadsurvival:canned_sweetcorn", 90),
          p("craftingdeadsurvival:canned_tuna", 100),
          p("craftingdeadsurvival:canned_tomato_soup", 100),
          p("craftingdeadsurvival:canned_pasta", 110),
          p("craftingdeadsurvival:canned_peaches", 110),
          p("craftingdeadsurvival:canned_corned_beef", 120),
          p("craftingdeadsurvival:canned_dog_food", 60),
          p("craftingdeadsurvival:iced_tea", 50),
          p("craftingdeadsurvival:iron_brew", 60),
          p("craftingdeadsurvival:milk_carton", 60),
          p("craftingdeadsurvival:zombie_energy", 70),
          p("craftingdeadsurvival:water_bottle", 100),
          p("craftingdeadsurvival:water_canteen", 150),
          p("craftingdeadsurvival:flask", 200),
          p("craftingdeadsurvival:can_opener", 150)),

      category("clothing", "服装", "craftingdead:army_medic_clothing",
          p("craftingdead:casual_green_clothing", 350),
          p("craftingdead:business_clothing", 400),
          p("craftingdead:builder_clothing", 450),
          p("craftingdead:contractor_clothing", 500),
          p("craftingdead:doctor_clothing", 700),
          p("craftingdead:army_medic_clothing", 1300),
          p("craftingdead:police_clothing", 1200),
          p("craftingdead:fireman_clothing", 1400),
          p("craftingdead:camo_clothing", 1600),
          p("craftingdead:army_clothing", 1800),
          p("craftingdead:army_desert_clothing", 1900),
          p("craftingdead:combat_bdu_clothing", 2200),
          p("craftingdead:scuba_clothing", 1800),
          p("craftingdead:pilot_clothing", 2000),
          p("craftingdead:ninja_clothing", 2500),
          p("craftingdead:sas_clothing", 2500),
          p("craftingdead:swat_clothing", 2800),
          p("craftingdead:hazmat_clothing", 2200),
          p("craftingdead:mil_hazmat_clothing", 2400),
          p("craftingdead:tac_ghillie_clothing", 3000),
          p("craftingdead:full_ghillie_clothing", 3200),
          p("craftingdead:juggernaut_clothing", 3500)),

      category("hats", "头盔/帽子", "craftingdead:bunny_hat",
          p("craftingdead:beanie_hat", 150),
          p("craftingdead:bunny_hat", 200),
          p("craftingdead:top_hat", 400),
          p("craftingdead:santa_hat", 250),
          p("craftingdead:trapper_hat", 250),
          p("craftingdead:ushanka_hat", 300),
          p("craftingdead:radar_cap", 300),
          p("craftingdead:cow_mask", 200),
          p("craftingdead:creeper_mask", 200),
          p("craftingdead:cookie_mask", 250),
          p("craftingdead:zombie_mask", 300),
          p("craftingdead:ski_mask", 300),
          p("craftingdead:deadpool_mask", 450),
          p("craftingdead:hacker_mask", 450),
          p("craftingdead:doctor_mask", 250),
          p("craftingdead:blue_hard_hat", 350),
          p("craftingdead:green_hard_hat", 350),
          p("craftingdead:orange_hard_hat", 350),
          p("craftingdead:yellow_hard_hat", 350),
          p("craftingdead:fireman_hat", 700),
          p("craftingdead:chief_fireman_hat", 1000),
          p("craftingdead:ghillie_hat", 900),
          p("craftingdead:ninja_hat", 800),
          p("craftingdead:riot_hat", 1500),
          p("craftingdead:hazmat_hat", 1200),
          p("craftingdead:scuba_mask", 1600),
          p("craftingdead:gas_mask", 2000),
          p("craftingdead:army_helmet", 1600),
          p("craftingdead:grey_army_helmet", 1500),
          p("craftingdead:green_army_helmet", 1500),
          p("craftingdead:camo_helmet", 1600),
          p("craftingdead:combat_bdu_helmet", 1800),
          p("craftingdead:pilot_helmet", 1800),
          p("craftingdead:spetsnaz_helmet", 2200),
          p("craftingdead:military_hazmat_hat", 1600),
          p("craftingdead:knight_hat", 2500),
          p("craftingdead:black_ballistic_hat", 2600),
          p("craftingdead:green_ballistic_helmet", 2600),
          p("craftingdead:nv_goggles_hat", 3000),
          p("craftingdead:juggernaut_helmet", 3200)),

      category("gear", "背心/背包", "craftingdead:black_tactical_vest",
          p("craftingdead:tan_gun_bag", 1600),
          p("craftingdead:grey_gun_bag", 1600),
          p("craftingdead:small_green_backpack", 300),
          p("craftingdead:small_red_backpack", 300),
          p("craftingdead:small_orange_backpack", 300),
          p("craftingdead:small_yellow_backpack", 300),
          p("craftingdead:small_blue_backpack", 300),
          p("craftingdead:small_purple_backpack", 300),
          p("craftingdead:medium_black_backpack", 500),
          p("craftingdead:medium_blue_backpack", 500),
          p("craftingdead:medium_green_backpack", 500),
          p("craftingdead:medium_grey_backpack", 500),
          p("craftingdead:medium_red_backpack", 500),
          p("craftingdead:large_black_backpack", 800),
          p("craftingdead:large_grey_backpack", 800),
          p("craftingdead:large_tan_backpack", 800),
          p("craftingdead:medium_ghillie_backpack", 900),
          p("craftingdead:black_tactical_vest", 1200),
          p("craftingdead:grey_tactical_vest", 1200),
          p("craftingdead:green_tactical_vest", 1200),
          p("craftingdead:tan_tactical_vest", 1400),
          p("craftingdead:ghillie_tactical_vest", 2800),
          p("craftingdead:riot_vest", 3000)),

      category("tools", "工具/杂物", "craftingdead:crowbar",
          p("craftingdead:flamethrower", 6500),
          p("craftingdead:flamethrower_fuel", 400),
          p("craftingdead:chalk", 120),
          p("craftingdead:vulcan_paint", 800),
          p("craftingdead:asmo_paint", 800),
          p("craftingdead:candy_apple_paint", 800),
          p("craftingdead:cyrex_paint", 800),
          p("craftingdead:diamond_paint", 800),
          p("craftingdead:dragon_paint", 800),
          p("craftingdead:fade_paint", 800),
          p("craftingdead:fury_paint", 800),
          p("craftingdead:gem_paint", 800),
          p("craftingdead:inferno_paint", 800),
          p("craftingdead:ruby_paint", 800),
          p("craftingdead:scorched_paint", 800),
          p("craftingdead:slaughter_paint", 800),
          p("craftingdead:uv_paint", 800),
          p("craftingdead:hyper_beast_paint", 800),
          p("craftingdead:emperor_dragon_paint", 800),
          p("craftingdead:nuclear_winter_paint", 800),
          p("craftingdead:monarch_paint", 800),
          p("craftingdead:lovelace_paint", 800),
          p("craftingdead:crowbar", 300),
          p("craftingdead:bolt_cutters", 400),
          p("craftingdead:parachute", 2500)),
      category("vehicle", "载具弹药/维修", "craftingdead:bullet762",
          p("craftingdead:bullet762", 20),
          p("craftingdead:125mm_he", 600),
          p("craftingdead:125mm_ap", 750),
          p("craftingdead:iron_wrench", 500),
          p("craftingdead:40mm_smoke", 250),
          p("craftingdead:blowtorch", 800)),
      new ShopCategory("vanilla", "原版物品", "minecraft:grass_block", VanillaShopProducts.products()));

  private ShopCatalog() {}

  public static List<ShopCategory> categories() {
    return CATEGORIES;
  }

  /**
   * 取第 index 个分类；越界返回 null。
   */
  @Nullable
  public static ShopCategory getCategory(int index) {
    return index >= 0 && index < CATEGORIES.size() ? CATEGORIES.get(index) : null;
  }

  /**
   * 解析商品；目录、分类或商品越界时返回 null。
   */
  @Nullable
  public static ShopProduct getProduct(int categoryIndex, int productIndex) {
    ShopCategory category = getCategory(categoryIndex);
    if (category == null || productIndex < 0 || productIndex >= category.products().size()) {
      return null;
    }
    return category.products().get(productIndex);
  }

  /**
   * 解析物品栈（数量 1）；ID 无效返回空栈（渲染/发放时防御）。
   */
  public static ItemStack resolveItem(String itemId) {
    return resolve(itemId, 1);
  }

  /**
   * 解析物品栈；ID 无效返回空栈（渲染/发放时防御）。
   */
  public static ItemStack resolve(String itemId, int amount) {
    var item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(itemId));
    return item == null || item == net.minecraft.world.item.Items.AIR
        ? ItemStack.EMPTY
        : new ItemStack(item, amount);
  }

  /**
   * 在目录中查找可回收该物品的商品，用于背包右键/Shift+左键直接出售。
   */
  @Nullable
  public static ProductRef findSellable(ItemStack stack) {
    if (stack.isEmpty()) {
      return null;
    }
    for (int c = 0; c < CATEGORIES.size(); c++) {
      List<ShopProduct> products = CATEGORIES.get(c).products();
      for (int p = 0; p < products.size(); p++) {
        ShopProduct product = products.get(p);
        if (product.sellPricePerItem() <= 0) {
          continue;
        }
        ItemStack template = resolve(product.itemId(), 1);
        if (!template.isEmpty() && template.is(stack.getItem())) {
          return new ProductRef(c, p, product);
        }
      }
    }
    return null;
  }

  /** 目录定位结果：分类下标 + 商品下标 + 商品本身。 */
  public record ProductRef(int categoryIndex, int productIndex, ShopProduct product) {}

  private static ShopCategory category(String id, String label, String iconItemId,
      ShopProduct... products) {
    return new ShopCategory(id, label, iconItemId, List.of(products));
  }

  private static ShopProduct p(String itemId, int priceMa) {
    return ShopProduct.of(itemId, priceMa);
  }
}

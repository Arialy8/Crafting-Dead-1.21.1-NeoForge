package com.craftingdead.core.world.shop;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 玩家 MA 余额存储（服务端主世界 SavedData）。
 *
 * <p>默认实现为内置虚拟币，便于开箱即用；若要对接服务器已有的 MA 经济系统，
 * 只需在 {@link ShopServer} 的余额读取/扣减处替换实现即可。</p>
 */
public final class MaBalanceData extends SavedData {

  private static final String DATA_NAME = "craftingdead_ma_balance";
  private static final String KEY_BALANCES = "balances";

  private final Map<UUID, Integer> balances = new HashMap<>();
  private final Map<UUID, java.util.Set<String>> favourites = new HashMap<>();

  public java.util.Set<String> favourites(UUID player) {
    return java.util.Set.copyOf(this.favourites.getOrDefault(player, java.util.Set.of()));
  }

  public void toggleFavourite(UUID player, String itemId) {
    var entries = this.favourites.computeIfAbsent(player, id -> new java.util.HashSet<>());
    if (!entries.remove(itemId)) entries.add(itemId);
    this.setDirty();
  }

  public MaBalanceData() {
    super();
  }

  public static MaBalanceData load(CompoundTag tag, HolderLookup.Provider registries) {
    MaBalanceData data = new MaBalanceData();
    CompoundTag favourites = tag.getCompound("shopFavourites");
    for (String key : favourites.getAllKeys()) {
      try {
        var entries = new java.util.HashSet<String>();
        var list = favourites.getList(key, net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) entries.add(list.getString(i));
        data.favourites.put(UUID.fromString(key), entries);
      } catch (IllegalArgumentException ignored) {}
    }
    CompoundTag balances = tag.getCompound(KEY_BALANCES);
    for (String key : balances.getAllKeys()) {
      try {
        data.balances.put(UUID.fromString(key), balances.getInt(key));
      } catch (IllegalArgumentException ignored) {
        // 跳过损坏的存档条目
      }
    }
    return data;
  }

  public static MaBalanceData get(MinecraftServer server) {
    return get(server.overworld());
  }

  public static MaBalanceData get(ServerLevel level) {
    return level.getServer().overworld().getDataStorage()
        .computeIfAbsent(new Factory<>(MaBalanceData::new, MaBalanceData::load), DATA_NAME);
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    CompoundTag balancesTag = new CompoundTag();
    this.balances.forEach((uuid, amount) -> balancesTag.putInt(uuid.toString(), amount));
    tag.put(KEY_BALANCES, balancesTag);
    CompoundTag favouritesTag = new CompoundTag();
    this.favourites.forEach((uuid, entries) -> {
      var list = new net.minecraft.nbt.ListTag();
      entries.stream().sorted().forEach(id -> list.add(net.minecraft.nbt.StringTag.valueOf(id)));
      favouritesTag.put(uuid.toString(), list);
    });
    tag.put("shopFavourites", favouritesTag);
    return tag;
  }

  public int getBalance(UUID playerUuid) {
    if (!this.balances.containsKey(playerUuid)) {
      this.balances.put(playerUuid, defaultBalance());
      this.setDirty();
    }
    return this.balances.get(playerUuid);
  }

  public boolean hasBalance(UUID playerUuid) {
    return this.balances.containsKey(playerUuid);
  }

  /**
   * 增加余额并返回新余额；amount 可为负（扣款）。
   */
  public int addBalance(UUID playerUuid, int amount) {
    int current = this.getBalance(playerUuid);
    int next = (int) Math.clamp((long) current + amount, 0L, Integer.MAX_VALUE);
    if (next != current || !this.balances.containsKey(playerUuid)) {
      this.balances.put(playerUuid, next);
      this.setDirty();
    }
    return next;
  }

  /**
   * 设置余额并返回新余额。
   */
  public int setBalance(UUID playerUuid, int amount) {
    int next = Math.max(0, amount);
    this.balances.put(playerUuid, next);
    this.setDirty();
    return next;
  }

  /**
   * 玩家无存档记录时为默认余额（新玩家赠送，便于上手体验）。
   */
  public static int defaultBalance() {
    return com.craftingdead.core.ServerConfig.configSpec.isLoaded()
        ? com.craftingdead.core.ServerConfig.instance.initialBalance.get() : 1000;
  }
}

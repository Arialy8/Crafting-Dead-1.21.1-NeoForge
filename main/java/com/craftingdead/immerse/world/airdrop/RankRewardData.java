package com.craftingdead.immerse.world.airdrop;

import com.google.gson.*;
import java.io.Reader;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;

/** World-wide reward templates. Unedited ranks continue using their original loot tables. */
public final class RankRewardData extends SavedData {
  private static final Factory<RankRewardData> FACTORY =
      new Factory<>(RankRewardData::new, RankRewardData::load, DataFixTypes.LEVEL);
  private final Map<Integer, Rewards> ranks = new HashMap<>();
  public static final double[] CHANCES = {0.60, 0.30, 0.10};

  public static final class Rewards {
    public boolean modified;
    public final List<List<ItemStack>> tiers = List.of(new ArrayList<>(),
        new ArrayList<>(), new ArrayList<>());
    public int pages() {
      return Math.max(1, (tiers.stream().mapToInt(List::size).max().orElse(0) + 17) / 18);
    }
  }

  public static RankRewardData get(MinecraftServer server) {
    return server.overworld().getDataStorage().computeIfAbsent(FACTORY,
        "craftingdeadimmerse_rank_rewards");
  }

  public Rewards rewards(MinecraftServer server, int rank) {
    Rewards existing = ranks.get(rank);
    if (existing != null) return existing;
    Rewards imported = new Rewards();
    ResourceLocation file = ResourceLocation.fromNamespaceAndPath("craftingdeadimmerse",
        "loot_table/rank_drops/rank" + rank + ".json");
    try (Reader reader = server.getResourceManager().getResourceOrThrow(file).openAsReader()) {
      JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
      for (JsonElement poolElement : json.getAsJsonArray("pools")) {
        JsonObject pool = poolElement.getAsJsonObject();
        JsonArray entries = pool.getAsJsonArray("entries");
        double total = 0;
        for (JsonElement element : entries) total += weight(element.getAsJsonObject());
        double rolls = maximum(pool.get("rolls"), 1);
        double chance = 1;
        if (pool.has("conditions")) {
          for (JsonElement condition : pool.getAsJsonArray("conditions")) {
            JsonObject c = condition.getAsJsonObject();
            if (c.has("chance")) chance *= c.get("chance").getAsDouble();
          }
        }
        for (JsonElement element : entries) {
          JsonObject entry = element.getAsJsonObject();
          if (!"minecraft:item".equals(entry.get("type").getAsString()))
            throw new IllegalStateException("Unsupported reward entry: " + entry);
          ResourceLocation id = ResourceLocation.parse(entry.get("name").getAsString());
          if (!BuiltInRegistries.ITEM.containsKey(id))
            throw new IllegalStateException("Unknown reward item: " + id);
          ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
          int count = 1;
          if (entry.has("functions")) {
            for (JsonElement function : entry.getAsJsonArray("functions")) {
              JsonObject f = function.getAsJsonObject();
              if ("minecraft:set_count".equals(f.get("function").getAsString()))
                count = (int) maximum(f.get("count"), 1);
            }
          }
          stack.setCount(Math.max(1, Math.min(99, count)));
          double probability = chance * (1 - Math.pow(1 - weight(entry) / total, rolls));
          imported.tiers.get(probability >= .25 ? 0 : probability >= .08 ? 1 : 2).add(stack);
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Cannot import rank " + rank + " rewards", e);
    }
    ranks.put(rank, imported);
    setDirty();
    return imported;
  }

  private static double weight(JsonObject entry) {
    return entry.has("weight") ? entry.get("weight").getAsDouble() : 1;
  }

  private static double maximum(JsonElement value, double fallback) {
    if (value == null) return fallback;
    return value.isJsonPrimitive() ? value.getAsDouble()
        : value.getAsJsonObject().get("max").getAsDouble();
  }

  public void edit(MinecraftServer server, int rank, int tier, int index, ItemStack value) {
    Rewards rewards = rewards(server, rank);
    List<ItemStack> entries = rewards.tiers.get(tier);
    while (entries.size() <= index) entries.add(ItemStack.EMPTY);
    entries.set(index, value.copy());
    while (!entries.isEmpty() && entries.get(entries.size() - 1).isEmpty())
      entries.remove(entries.size() - 1);
    rewards.modified = true;
    setDirty();
  }

  /** Independent per-entry rolls; an item's template count is its maximum total for this entry. */
  public boolean fillCustom(int rank, Container container, RandomSource random) {
    Rewards rewards = ranks.get(rank);
    if (rewards == null || !rewards.modified) return false;
    List<ItemStack> rolled = new ArrayList<>();
    for (int tier = 0; tier < 3; tier++) {
      for (ItemStack template : rewards.tiers.get(tier)) {
        if (!template.isEmpty() && random.nextDouble() < CHANCES[tier]) {
          int count = 1 + random.nextInt(template.getCount());
          while (count > 0) {
            int part = Math.min(count, template.getMaxStackSize());
            rolled.add(template.copyWithCount(part));
            count -= part;
          }
        }
      }
    }
    // Randomize before the physical 54-slot capacity limit, so lower tiers are not cut first.
    for (int i = rolled.size() - 1; i > 0; i--) {
      Collections.swap(rolled, i, random.nextInt(i + 1));
    }
    for (int i = 0; i < Math.min(rolled.size(), container.getContainerSize()); i++)
      container.setItem(i, rolled.get(i));
    return true;
  }

  private static RankRewardData load(CompoundTag tag, HolderLookup.Provider lookup) {
    RankRewardData data = new RankRewardData();
    ListTag list = tag.getList("Ranks", Tag.TAG_COMPOUND);
    for (int i = 0; i < list.size(); i++) {
      CompoundTag rankTag = list.getCompound(i);
      int rank = rankTag.getInt("Rank");
      if (rank < 1 || rank > 19) continue;
      Rewards rewards = new Rewards();
      rewards.modified = rankTag.getBoolean("Modified");
      for (int tier = 0; tier < 3; tier++) {
        ListTag items = rankTag.getList("Tier" + tier, Tag.TAG_COMPOUND);
        for (int j = 0; j < items.size(); j++)
          rewards.tiers.get(tier).add(ItemStack.parseOptional(lookup, items.getCompound(j)));
      }
      data.ranks.put(rank, rewards);
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
    ListTag list = new ListTag();
    for (var entry : ranks.entrySet()) {
      CompoundTag rankTag = new CompoundTag();
      rankTag.putInt("Rank", entry.getKey());
      rankTag.putBoolean("Modified", entry.getValue().modified);
      for (int tier = 0; tier < 3; tier++) {
        ListTag items = new ListTag();
        for (ItemStack stack : entry.getValue().tiers.get(tier))
          items.add(stack.saveOptional(lookup));
        rankTag.put("Tier" + tier, items);
      }
      list.add(rankTag);
    }
    tag.put("Ranks", list);
    return tag;
  }
}

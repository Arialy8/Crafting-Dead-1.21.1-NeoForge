package com.craftingdead.immerse.world.airdrop;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.loot.LootTable;
import com.craftingdead.survival.world.item.SupplyDropRadioItem;

/** Wall-clock deadlines survive world saves and do not rely on TickTask's queue timestamp. */
public final class RankDropSchedule extends SavedData {
  private static final Factory<RankDropSchedule> FACTORY =
      new Factory<>(RankDropSchedule::new, RankDropSchedule::load, DataFixTypes.LEVEL);
  private record Pending(ResourceKey<Level> dimension, BlockPos pos,
      ResourceKey<LootTable> loot, long due, java.util.UUID caller, String name,
      boolean hideCoords, int lastNotice) {}
  private final List<Pending> pending = new ArrayList<>();

  public static RankDropSchedule get(MinecraftServer server) {
    return server.overworld().getDataStorage().computeIfAbsent(FACTORY,
        "craftingdeadimmerse_pending_rank_drops");
  }

  public void schedule(ServerLevel level, BlockPos pos, ResourceKey<LootTable> loot, int seconds,
      net.minecraft.server.level.ServerPlayer caller, boolean hideCoords) {
    pending.add(new Pending(level.dimension(), pos.immutable(), loot,
        System.currentTimeMillis() + seconds * 1000L, caller.getUUID(),
        caller.getGameProfile().getName(), hideCoords, seconds));
    setDirty();
  }

  public void tick(MinecraftServer server) {
    long now = System.currentTimeMillis();
    var iterator = pending.listIterator();
    while (iterator.hasNext()) {
      Pending drop = iterator.next();
      if (now < drop.due()) {
        int remaining = (int) ((drop.due() - now + 999) / 1000);
        int threshold = remaining <= 10 ? 10 : remaining <= 60 ? 60 : Integer.MAX_VALUE;
        if (drop.lastNotice() > threshold) {
          RankAirdrop.announce(server, drop.caller(), drop.name(),
              drop.dimension().location().toString(), drop.pos(), drop.hideCoords(),
              "还有 " + RankAirdrop.duration(remaining) + " 开始降落");
          iterator.set(new Pending(drop.dimension(), drop.pos(), drop.loot(), drop.due(),
              drop.caller(), drop.name(), drop.hideCoords(), threshold));
          setDirty();
        }
        continue;
      }
      ServerLevel level = server.getLevel(drop.dimension());
      if (level != null && SupplyDropRadioItem.spawnSupplyDrop(level, drop.pos(), drop.loot())) {
        RankAirdrop.announce(server, drop.caller(), drop.name(),
            drop.dimension().location().toString(), drop.pos(), drop.hideCoords(), "已开始降落");
        iterator.remove();
        setDirty();
      }
    }
  }

  private static RankDropSchedule load(CompoundTag tag, HolderLookup.Provider lookup) {
    RankDropSchedule data = new RankDropSchedule();
    ListTag list = tag.getList("Pending", Tag.TAG_COMPOUND);
    for (int i = 0; i < list.size(); i++) {
      CompoundTag value = list.getCompound(i);
      ResourceLocation dimension = ResourceLocation.tryParse(value.getString("Dimension"));
      ResourceLocation loot = ResourceLocation.tryParse(value.getString("Loot"));
      if (dimension != null && loot != null)
        data.pending.add(new Pending(ResourceKey.create(Registries.DIMENSION, dimension),
            BlockPos.of(value.getLong("Pos")), ResourceKey.create(Registries.LOOT_TABLE, loot),
            value.getLong("Due"), value.hasUUID("Caller") ? value.getUUID("Caller") : null,
            value.contains("Name") ? value.getString("Name") : "玩家",
            !value.contains("HideCoords") || value.getBoolean("HideCoords"),
            value.contains("LastNotice") ? value.getInt("LastNotice") : Integer.MAX_VALUE));
    }
    return data;
  }

  @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
    ListTag list = new ListTag();
    for (Pending drop : pending) {
      CompoundTag value = new CompoundTag();
      value.putString("Dimension", drop.dimension().location().toString());
      value.putString("Loot", drop.loot().location().toString());
      value.putLong("Pos", drop.pos().asLong());
      value.putLong("Due", drop.due());
      if (drop.caller() != null) value.putUUID("Caller", drop.caller());
      value.putString("Name", drop.name());
      value.putBoolean("HideCoords", drop.hideCoords());
      value.putInt("LastNotice", drop.lastNotice());
      list.add(value);
    }
    tag.put("Pending", list);
    return tag;
  }
}

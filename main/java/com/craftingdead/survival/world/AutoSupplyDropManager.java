package com.craftingdead.survival.world;

import com.craftingdead.survival.CraftingDeadSurvival;
import com.craftingdead.survival.world.item.SupplyDropRadioItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class AutoSupplyDropManager extends SavedData {

  private static final String DATA_NAME = "craftingdeadsurvival_auto_supply_drop";
  private static final SavedData.Factory<AutoSupplyDropManager> FACTORY =
      new SavedData.Factory<>(AutoSupplyDropManager::new,
          AutoSupplyDropManager::load, DataFixTypes.LEVEL);

  private ResourceKey<Level> dimension = Level.OVERWORLD;
  private BlockPos position;
  private String type = "medical";
  private int count = 1;
  private int interval = 12000;
  private long nextSpawnTick;
  private boolean enabled;

  public static AutoSupplyDropManager get(MinecraftServer server) {
    return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
  }

  public void setPosition(ServerLevel level, BlockPos position) {
    this.dimension = level.dimension();
    this.position = position.immutable();
    this.setDirty();
  }

  public void setType(String type) {
    this.type = "military".equals(type) ? "military" : "medical";
    this.setDirty();
  }

  public void setCount(int count) {
    this.count = Math.max(1, Math.min(5, count));
    this.setDirty();
  }

  public void setInterval(int interval, MinecraftServer server) {
    this.interval = Math.max(1, Math.min(12000, interval));
    this.nextSpawnTick = server.overworld().getGameTime() + this.interval;
    this.setDirty();
  }

  public void setEnabled(boolean enabled, MinecraftServer server) {
    this.enabled = enabled;
    if (enabled) {
      this.nextSpawnTick = server.overworld().getGameTime() + this.interval;
    }
    this.setDirty();
  }

  public void clearPosition() {
    this.position = null;
    this.enabled = false;
    this.nextSpawnTick = 0L;
    this.setDirty();
  }

  public Component status() {
    String pos = this.position == null ? "not set"
        : this.position.getX() + ", " + this.position.getY() + ", " + this.position.getZ();
    return Component.translatable("command.craftingdeadsurvival.auto_supply_drop.status",
        this.enabled,
        Component.translatable("command.craftingdeadsurvival.auto_supply_drop.type." + this.type),
        this.count, this.interval, pos, this.dimension.location());
  }

  private void tick(MinecraftServer server) {
    long gameTime = server.overworld().getGameTime();
    if (!this.enabled || this.position == null || gameTime < this.nextSpawnTick) {
      return;
    }
    this.nextSpawnTick = gameTime + this.interval;
    this.setDirty();

    ServerLevel level = server.getLevel(this.dimension);
    if (level == null) {
      return;
    }
    ResourceKey<LootTable> loot = ResourceKey.create(Registries.LOOT_TABLE,
        ResourceLocation.fromNamespaceAndPath(CraftingDeadSurvival.ID,
            "supply_drops/" + this.type));
    int spawned = 0;
    for (int i = 0; i < this.count; i++) {
      int offsetX = level.getRandom().nextInt(11) - 5;
      int offsetZ = level.getRandom().nextInt(11) - 5;
      BlockPos target = this.position.offset(offsetX, 0, offsetZ);
      if (SupplyDropRadioItem.spawnSupplyDrop(level, target, loot)) {
        spawned++;
      }
    }
    server.getPlayerList().broadcastSystemMessage(Component.translatable(
        "command.craftingdeadsurvival.auto_supply_drop.announcement",
        Component.translatable("command.craftingdeadsurvival.auto_supply_drop.type." + this.type),
        spawned, this.position.getX(), this.position.getY(), this.position.getZ()), false);
  }

  private static AutoSupplyDropManager load(CompoundTag tag,
      HolderLookup.Provider registries) {
    AutoSupplyDropManager data = new AutoSupplyDropManager();
    data.enabled = tag.getBoolean("Enabled");
    data.type = "military".equals(tag.getString("Type")) ? "military" : "medical";
    data.count = Math.max(1, Math.min(5, tag.getInt("Count")));
    int savedInterval = tag.getInt("Interval");
    data.interval = savedInterval <= 0 ? 12000 : Math.min(12000, savedInterval);
    data.nextSpawnTick = tag.getLong("NextSpawnTick");
    if (tag.getBoolean("HasPosition")) {
      data.position = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
    }
    ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("Dimension"));
    if (dimensionId != null) {
      data.dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    tag.putBoolean("Enabled", this.enabled);
    tag.putString("Type", this.type);
    tag.putInt("Count", this.count);
    tag.putInt("Interval", this.interval);
    tag.putLong("NextSpawnTick", this.nextSpawnTick);
    tag.putString("Dimension", this.dimension.location().toString());
    tag.putBoolean("HasPosition", this.position != null);
    if (this.position != null) {
      tag.putInt("X", this.position.getX());
      tag.putInt("Y", this.position.getY());
      tag.putInt("Z", this.position.getZ());
    }
    return tag;
  }

  public static final class Events {

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
      AutoSupplyDropManager.get(event.getServer()).tick(event.getServer());
    }
  }
}

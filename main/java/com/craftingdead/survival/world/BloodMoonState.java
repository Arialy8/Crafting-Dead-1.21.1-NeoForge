package com.craftingdead.survival.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/** World-owned event state; reconnecting cannot reset waves or reroll a night. */
public final class BloodMoonState extends SavedData {
  private static final Factory<BloodMoonState> FACTORY = new Factory<>(
      BloodMoonState::new, BloodMoonState::load, DataFixTypes.LEVEL);
  public long forcedDay = Long.MIN_VALUE;
  public long stoppedDay = Long.MIN_VALUE;
  public long warnedDay = Long.MIN_VALUE;
  public long nextWave;
  public boolean running;
  public long progressionAnnouncedCycle = -1;
  public long eventDay = Long.MIN_VALUE;
  public long settledDay = Long.MIN_VALUE;
  public boolean administrative;
  public final java.util.Map<java.util.UUID, Integer> deaths = new java.util.HashMap<>();
  public final java.util.Map<java.util.UUID, Integer> participation = new java.util.HashMap<>();

  public static int reward(int deaths, int ticks) {
    if (ticks < 9800) return 200;
    return deaths == 0 ? 1000 : deaths == 1 ? 500 : 200;
  }

  public void begin(long day, boolean admin) {
    eventDay = day;
    administrative = admin;
    deaths.clear();
    participation.clear();
    setDirty();
  }

  public java.util.Map<java.util.UUID, Integer> claimRewards() {
    if (administrative || eventDay == Long.MIN_VALUE || eventDay <= settledDay)
      return java.util.Map.of();
    var rewards = new java.util.HashMap<java.util.UUID, Integer>();
    participation.forEach((uuid, ticks) -> rewards.put(uuid,
        reward(deaths.getOrDefault(uuid, 0), ticks)));
    settledDay = eventDay;
    setDirty();
    return rewards;
  }

  public void observe(java.util.UUID player) {
    participation.merge(player, 20, (a, b) -> Math.min(10000, a + b));
    deaths.putIfAbsent(player, 0);
    setDirty();
  }

  public void recordDeath(java.util.UUID player) {
    if (participation.containsKey(player)) {
      deaths.merge(player, 1, (a, b) -> Math.min(1000, a + b));
      setDirty();
    }
  }
  public static BloodMoonState get(ServerLevel level) {
    return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY,
        "craftingdeadsurvival_bloodmoon");
  }
  static BloodMoonState load(CompoundTag tag, HolderLookup.Provider lookup) {
    BloodMoonState state = new BloodMoonState();
    state.forcedDay = tag.contains("Forced") ? tag.getLong("Forced") : Long.MIN_VALUE;
    state.stoppedDay = tag.contains("Stopped") ? tag.getLong("Stopped") : Long.MIN_VALUE;
    state.warnedDay = tag.contains("Warned") ? tag.getLong("Warned") : Long.MIN_VALUE;
    state.nextWave = tag.getLong("NextWave");
    state.running = tag.getBoolean("Running");
    state.progressionAnnouncedCycle = tag.contains("ProgressionAnnouncedCycle")
        ? tag.getLong("ProgressionAnnouncedCycle") : -1;
    state.eventDay = tag.contains("EventDay") ? tag.getLong("EventDay") : Long.MIN_VALUE;
    state.settledDay = tag.contains("SettledDay") ? tag.getLong("SettledDay") : Long.MIN_VALUE;
    state.administrative = tag.getBoolean("Administrative");
    CompoundTag participants = tag.getCompound("Participants");
    for (String id : participants.getAllKeys()) {
      try {
        java.util.UUID uuid = java.util.UUID.fromString(id);
        CompoundTag data = participants.getCompound(id);
        state.deaths.put(uuid, Math.max(0, data.getInt("Deaths")));
        state.participation.put(uuid, Math.clamp(data.getInt("Ticks"), 0, 10000));
      } catch (IllegalArgumentException ignored) { }
    }
    return state;
  }
  @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
    tag.putLong("Forced", forcedDay);
    tag.putLong("Stopped", stoppedDay);
    tag.putLong("Warned", warnedDay);
    tag.putLong("NextWave", nextWave);
    tag.putBoolean("Running", running);
    tag.putLong("ProgressionAnnouncedCycle", progressionAnnouncedCycle);
    tag.putLong("EventDay", eventDay);
    tag.putLong("SettledDay", settledDay);
    tag.putBoolean("Administrative", administrative);
    CompoundTag participants = new CompoundTag();
    participation.forEach((uuid, ticks) -> {
      CompoundTag data = new CompoundTag();
      data.putInt("Ticks", ticks);
      data.putInt("Deaths", deaths.getOrDefault(uuid, 0));
      participants.put(uuid.toString(), data);
    });
    tag.put("Participants", participants);
    return tag;
  }
}

package com.craftingdead.survival.world;

import com.craftingdead.core.network.NetworkChannel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 服务端统一管理预警、尸潮、禁睡、状态同步及自然血月结算。
 */
public final class BloodMoonEvents {

  private final ServerBossEvent bar = new ServerBossEvent(Component.literal("血月"),
      BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
  private MinecraftServer lastServer;

  @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
  public void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      BloodMoonState state = BloodMoonState.get(player.serverLevel());
      long day = BloodMoonManager.currentDay(player.server.overworld());
      if (BloodMoonManager.isBloodMoonNight(player.server.overworld()) && state.eventDay != day)
        state.begin(day, state.forcedDay == day);
      if (BloodMoonManager.isBloodMoonNight(player.server.overworld()) && !state.administrative) {
        if (player.serverLevel() == player.server.overworld() && !player.isCreative() && !player.isSpectator()
            && !state.participation.containsKey(player.getUUID())) state.observe(player.getUUID());
        state.recordDeath(player.getUUID());
      }
    }
  }

  private void settle(ServerLevel level, BloodMoonState state) {
    var money = com.craftingdead.core.world.shop.MaBalanceData.get(level);
    state.claimRewards().forEach((uuid, amount) -> {
      money.addBalance(uuid, amount);
      ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
      if (player != null) player.sendSystemMessage(Component.literal("[血月结算] 获得 " + amount
          + " 游戏币，死亡 " + state.deaths.getOrDefault(uuid, 0) + " 次。余额："
          + money.getBalance(uuid) + "，使用 /shop 打开商店。"));
    });
  }

  @SubscribeEvent
  public void onSleep(CanPlayerSleepEvent event) {
    ServerLevel level = event.getEntity().serverLevel();
    if (BloodMoonManager.isBloodMoonDay(level)
        && Math.floorMod(level.getDayTime(), 24000) < 23000
        && level.getGameRules().getBoolean(SurvivalGameRules.BLOOD_MOON_NO_SLEEP)) {
      event.setProblem(net.minecraft.world.entity.player.Player.BedSleepingProblem.OTHER_PROBLEM);
      event.getEntity().displayClientMessage(Component.literal("血月笼罩，今晚无法入睡。"), true);
    }
  }

  @SubscribeEvent
  public void onServerTick(ServerTickEvent.Post event) {
    MinecraftServer server = event.getServer();
    if (server.getTickCount() % 20 != 0) return;
    if (lastServer != server) {
      bar.removeAllPlayers();
      lastServer = server;
    }
    ServerLevel level = server.overworld();
    BloodMoonState state = BloodMoonState.get(level);
    long day = BloodMoonManager.currentDay(level);
    long time = Math.floorMod(level.getDayTime(), 24000);
    boolean active = BloodMoonManager.isBloodMoonNight(level);
    if (state.running && (!active || state.eventDay != day)) {
      if (BloodMoonManager.isEnabled(level) && state.stoppedDay != state.eventDay
          && level.getDayTime() >= state.eventDay * 24000 + 23000)
        settle(level, state);
    }
    if (active && state.eventDay != day)
      state.begin(day, state.forcedDay == day);
    if (active && !state.administrative) {
      for (ServerPlayer player : level.players())
        if (!player.isCreative() && !player.isSpectator()) state.observe(player.getUUID());
    }
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      NetworkChannel.PLAY.sendToPlayer(player,
          BloodMoonManager.createSyncMessage(player.serverLevel()));
    }
    if (BloodMoonManager.isBloodMoonDay(level) && time >= 10000 && time < 13000
        && state.warnedDay != day) {
      state.warnedDay = day;
      state.setDirty();
      announce(level, "血月将至", "今晚无法安眠，请准备弹药并返回基地！");
    }
    if (state.running != active) {
      state.running = active;
      state.nextWave = 0;
      state.setDirty();
      announce(level, active ? "血月降临" : "血月结束",
          active ? "尸潮即将袭来，坚持到黎明！" : "血色褪去，尸潮停止增援。");
    }
    if (!active) {
      bar.removeAllPlayers();
      return;
    }
    int seconds = (int) ((23000 - time + 19) / 20);
    bar.setName(Component.literal("血月 · 距黎明 " + seconds / 60 + "分" + seconds % 60 + "秒"));
    bar.setProgress(Math.clamp((23000 - time) / 10000F, 0F, 1F));
    bar.setDarkenScreen(true);
    var visible = new HashSet<UUID>();
    for (ServerPlayer player : level.players()) {
      if (player.isSpectator()) continue;
      visible.add(player.getUUID());
      bar.addPlayer(player);
      if (player.isSleeping()
          && level.getGameRules().getBoolean(SurvivalGameRules.BLOOD_MOON_NO_SLEEP))
        player.stopSleepInBed(true, true);
    }
    for (ServerPlayer player : new ArrayList<>(bar.getPlayers()))
      if (!visible.contains(player.getUUID())) bar.removePlayer(player);
    if (level.getGameTime() < state.nextWave) return;
    state.nextWave = level.getGameTime() + 20L * Math.clamp(
        level.getGameRules().getInt(SurvivalGameRules.BLOOD_MOON_WAVE_SECONDS), 30, 600);
    state.setDirty();
    if (level.getDifficulty() == Difficulty.PEACEFUL
        || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
    int mobs = 0;
    for (var entity : level.getAllEntities())
      if (entity.isAlive() && entity.getTags().contains("craftingdead_bloodmoon")) mobs++;
    var centers = new ArrayList<ServerPlayer>();
    for (ServerPlayer player : level.players()) {
      if (mobs + 14 > 240) break;
      if (!player.isAlive() || player.isCreative() || player.isSpectator()) continue;
      if (centers.stream().anyMatch(center -> center.distanceToSqr(player) < 64 * 64)) continue;
      centers.add(player);
      SurvivalHordeEvents.spawnHorde(level, player, true);
      mobs += 14;
    }
  }

  private static void announce(ServerLevel level, String title, String subtitle) {
    for (ServerPlayer player : level.players()) {
      player.sendSystemMessage(Component.literal("[血月] " + title + "：" + subtitle)
          .withStyle(net.minecraft.ChatFormatting.RED));
      player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 70, 20));
      player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
      player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
          Component.literal(title).withStyle(net.minecraft.ChatFormatting.DARK_RED)));
    }
  }

  @SubscribeEvent
  public void onCommands(RegisterCommandsEvent event) {
    event.getDispatcher().register(Commands.literal("bloodmoon")
        .then(Commands.literal("wave").requires(source -> source.hasPermission(2))
            .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                .then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100))
                    .executes(context -> {
                      ServerPlayer player = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "player");
                      int count = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count");
                      int actual = SurvivalHordeEvents.spawnBloodWave(player.serverLevel(), player, count);
                      context.getSource().sendSuccess(() -> Component.literal("已在 "
                          + player.getScoreboardName() + " 附近生成 " + actual + "/" + count
                          + " 只僵尸（受安全位置、难度和数量上限限制）。"), true);
                      return actual;
                    }))))
        .then(Commands.literal("status").executes(context -> {
          ServerLevel level = context.getSource().getServer().overworld();
          context.getSource().sendSuccess(() -> Component.literal(
              (BloodMoonManager.isBloodMoonNight(level) ? "血月进行中" : "当前非血月夜")
              + "，距血月天：" + BloodMoonManager.daysUntilBloodMoon(level)
              + "，尸潮间隔：" + Math.clamp(level.getGameRules().getInt(
                  SurvivalGameRules.BLOOD_MOON_WAVE_SECONDS), 30, 600) + "秒"), false);
          return 1;
        }))
        .then(Commands.literal("start").requires(source -> source.hasPermission(2)).executes(context -> {
          ServerLevel level = context.getSource().getServer().overworld();
          BloodMoonState state = BloodMoonState.get(level);
          state.forcedDay = BloodMoonManager.currentDay(level);
          state.administrative = true;
          state.stoppedDay = Long.MIN_VALUE;
          state.nextWave = 0;
          state.setDirty();
          level.getGameRules().getRule(SurvivalGameRules.BLOOD_MOON).set(true, level.getServer());
          level.setDayTime(state.forcedDay * 24000 + 13000);
          context.getSource().sendSuccess(() -> Component.literal("已开始本夜血月。"), true);
          return 1;
        }))
        .then(Commands.literal("stop").requires(source -> source.hasPermission(2)).executes(context -> {
          ServerLevel level = context.getSource().getServer().overworld();
          BloodMoonState state = BloodMoonState.get(level);
          state.stoppedDay = BloodMoonManager.currentDay(level);
          state.setDirty();
          context.getSource().sendSuccess(() -> Component.literal("已停止本次血月，后续周期不变。"), true);
          return 1;
        })));
  }
}

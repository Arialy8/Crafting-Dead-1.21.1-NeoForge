package com.craftingdead.survival.world;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.ServerConfig;
import com.craftingdead.core.world.shop.ShopServer;
import com.craftingdead.survival.world.hydration.HydrationData;
import com.craftingdead.survival.CraftingDeadSurvival;
import com.craftingdead.survival.world.item.SupplyDropRadioItem;
import java.util.Collection;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class SurvivalCommands {
  @SubscribeEvent
  public void registerCommands(RegisterCommandsEvent event) {
    event.getDispatcher().register(Commands.literal("hydrate")
        .requires(source -> source.hasPermission(2))
        .executes(context -> hydrate(context.getSource(),
            java.util.List.of(context.getSource().getPlayerOrException())))
        .then(Commands.argument("targets", EntityArgument.players())
            .executes(context -> hydrate(context.getSource(),
                EntityArgument.getPlayers(context, "targets")))));
    event.getDispatcher().register(Commands.literal("shop")
        .executes(context -> openShop(context.getSource())));
    event.getDispatcher().register(Commands.literal("balance").executes(context -> {
      ServerPlayer player = context.getSource().getPlayerOrException();
      context.getSource().sendSuccess(() -> Component.literal("balance：" + ShopServer.getBalance(player) + " ＄")
          .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), false);
      return 1;
    }));
    event.getDispatcher().register(Commands.literal("craftingdead")
        .requires(source -> source.hasPermission(2))
        .then(Commands.literal("explosion").then(Commands.literal("terrain")
            .executes(context -> showTerrainDamage(context.getSource()))
            .then(Commands.argument("enabled", BoolArgumentType.bool())
                .executes(context -> setTerrainDamage(context.getSource(),
                    BoolArgumentType.getBool(context, "enabled"))))))
        .then(Commands.literal("zombies")
            .then(Commands.literal("natural")
                .executes(context -> showNaturalZombies(context.getSource()))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> setNaturalZombies(context.getSource(),
                        BoolArgumentType.getBool(context, "enabled")))))
            .then(Commands.literal("rate")
                .executes(context -> showZombieRate(context.getSource()))
                .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                    .executes(context -> setZombieRate(context.getSource(),
                        IntegerArgumentType.getInteger(context, "percent")))))
            .then(Commands.literal("rare")
                .executes(context -> showRareZombies(context.getSource()))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> setRareZombies(context.getSource(),
                        BoolArgumentType.getBool(context, "enabled"))))))
        .then(Commands.literal("horde")
            .then(Commands.literal("enabled")
                .executes(context -> showHordes(context.getSource()))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> setHordes(context.getSource(),
                        BoolArgumentType.getBool(context, "enabled")))))
            .then(Commands.literal("chance")
                .executes(context -> showHordeChance(context.getSource()))
                .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                    .executes(context -> setHordeChance(context.getSource(),
                        IntegerArgumentType.getInteger(context, "percent"))))))
        .then(Commands.literal("bloodmoon")
            .then(Commands.literal("enabled")
                .executes(context -> showBloodMoonEnabled(context.getSource()))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> setBloodMoonEnabled(context.getSource(),
                        BoolArgumentType.getBool(context, "enabled")))))
            .then(Commands.literal("cycle")
                .executes(context -> showBloodMoonCycle(context.getSource()))
                .then(Commands.argument("days", IntegerArgumentType.integer(2, 31))
                    .executes(context -> setBloodMoonCycle(context.getSource(),
                        IntegerArgumentType.getInteger(context, "days")))))
            .then(Commands.literal("omen")
                .executes(context -> showBloodMoonOmen(context.getSource()))
                .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                    .executes(context -> setBloodMoonOmen(context.getSource(),
                        IntegerArgumentType.getInteger(context, "percent")))))
            .then(Commands.literal("status")
                .executes(context -> showBloodMoonStatus(context.getSource())))
            .then(Commands.literal("test")
                .executes(context -> testBloodMoon(context.getSource())))
            .then(Commands.literal("testomen")
                .executes(context -> testBloodMoonOmen(context.getSource()))))
        .then(Commands.literal("magazine")
            .then(Commands.literal("empty")
                .executes(context -> showEmptyMagazineMode(context.getSource()))
                .then(Commands.literal("discard")
                    .executes(context -> setEmptyMagazineMode(context.getSource(), true)))
                .then(Commands.literal("return")
                    .executes(context -> setEmptyMagazineMode(context.getSource(), false)))))
        .then(Commands.literal("supplydrop")
            .then(supplyDropType("medical"))
            .then(supplyDropType("military"))
            .then(autoSupplyDropCommands()))
        .then(Commands.literal("shop")
            .then(Commands.literal("money")
                .then(Commands.literal("give")
                    .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 10_000_000))
                            .executes(context -> giveShopMoney(context.getSource(),
                                EntityArgument.getPlayers(context, "targets"),
                                IntegerArgumentType.getInteger(context, "amount"))))))
                .then(Commands.literal("balance")
                    .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> showShopBalance(context.getSource(),
                            EntityArgument.getPlayers(context, "targets")))))))
        .then(Commands.literal("generator")
            .then(Commands.literal("pos1")
                .executes(context -> setGeneratorPos(context.getSource(), 1)))
            .then(Commands.literal("pos2")
                .executes(context -> setGeneratorPos(context.getSource(), 2)))
            .then(Commands.literal("confirm")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> confirmGeneratorArea(context.getSource(),
                        StringArgumentType.getString(context, "name")))))
            .then(Commands.literal("generate")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> confirmGeneratorArea(context.getSource(),
                        StringArgumentType.getString(context, "name")))))
            .then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> deleteGeneratorArea(context.getSource(),
                        StringArgumentType.getString(context, "name")))))
            .then(Commands.literal("list")
                .executes(context -> listGeneratorAreas(context.getSource())))));
  }

  private static int showEmptyMagazineMode(CommandSourceStack source) {
    boolean discard = ServerConfig.instance.reloadDestroyMagWhenEmpty.get();
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.empty_magazine_mode", discard ? "discard" : "return"), false);
    return discard ? 1 : 0;
  }

  private static int setEmptyMagazineMode(CommandSourceStack source, boolean discard) {
    ServerConfig.instance.reloadDestroyMagWhenEmpty.set(discard);
    ServerConfig.instance.reloadDestroyMagWhenEmpty.save();
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.empty_magazine_mode_set",
        discard ? "discard" : "return"), true);
    return 1;
  }

  private static int hydrate(CommandSourceStack source, Collection<ServerPlayer> targets) {
    int hydrated = 0;
    for (ServerPlayer player : targets) {
      if (!player.isAlive()) {
        continue;
      }
      int current = player.getData(com.craftingdead.core.ModAttachmentTypes.getHydration())
          .getWater();
      CraftingDead.restoreHydration(player, HydrationData.MAX_WATER - current);
      player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK,
          SoundSource.PLAYERS, 1.0F, 1.0F);
      hydrated++;
    }
    int count = hydrated;
    source.sendSuccess(() -> Component.literal("已补满 " + count + " 名玩家的口渴值。"), false);
    return hydrated;
  }

  private static int showTerrainDamage(CommandSourceStack source) {
    boolean enabled = source.getLevel().getGameRules()
        .getBoolean(SurvivalGameRules.EXPLOSION_TERRAIN_DAMAGE);
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.explosion_terrain", enabled), false);
    return enabled ? 1 : 0;
  }

  private static int setTerrainDamage(CommandSourceStack source, boolean enabled) {
    source.getLevel().getGameRules().getRule(SurvivalGameRules.EXPLOSION_TERRAIN_DAMAGE)
        .set(enabled, source.getServer());
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.explosion_terrain_set", enabled), true);
    return 1;
  }

  private static int showNaturalZombies(CommandSourceStack source) {
    boolean enabled = source.getLevel().getGameRules()
        .getBoolean(SurvivalGameRules.NATURAL_SPECIAL_ZOMBIES);
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.natural_zombies", enabled), false);
    return enabled ? 1 : 0;
  }

  private static int setNaturalZombies(CommandSourceStack source, boolean enabled) {
    source.getLevel().getGameRules().getRule(SurvivalGameRules.NATURAL_SPECIAL_ZOMBIES)
        .set(enabled, source.getServer());
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.natural_zombies_set", enabled), true);
    return 1;
  }

  private static int showZombieRate(CommandSourceStack source) {
    int rate = source.getLevel().getGameRules().getInt(SurvivalGameRules.SPECIAL_ZOMBIE_RATE);
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.zombie_rate", rate), false);
    return rate;
  }

  private static int setZombieRate(CommandSourceStack source, int rate) {
    source.getLevel().getGameRules().getRule(SurvivalGameRules.SPECIAL_ZOMBIE_RATE)
        .set(rate, source.getServer());
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.zombie_rate_set", rate), true);
    return rate;
  }

  private static int showRareZombies(CommandSourceStack source) {
    boolean enabled = source.getLevel().getGameRules()
        .getBoolean(SurvivalGameRules.RARE_SPECIAL_ZOMBIES);
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.rare_zombies", enabled), false);
    return enabled ? 1 : 0;
  }

  private static int setRareZombies(CommandSourceStack source, boolean enabled) {
    source.getLevel().getGameRules().getRule(SurvivalGameRules.RARE_SPECIAL_ZOMBIES)
        .set(enabled, source.getServer());
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.rare_zombies_set", enabled), true);
    return 1;
  }

  private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack>
      supplyDropType(String type) {
    return Commands.literal(type)
        .then(Commands.argument("count", IntegerArgumentType.integer(1, 5))
            .executes(context -> summonSupplyDrops(context.getSource(), type,
                IntegerArgumentType.getInteger(context, "count"))));
  }

  private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack>
      autoSupplyDropCommands() {
    return Commands.literal("auto")
        .then(Commands.literal("here")
            .executes(context -> setAutoSupplyDropHere(context.getSource())))
        .then(Commands.literal("pos")
            .then(Commands.argument("position", BlockPosArgument.blockPos())
                .executes(context -> setAutoSupplyDropPosition(context.getSource(),
                    BlockPosArgument.getBlockPos(context, "position")))))
        .then(Commands.literal("type")
            .then(Commands.literal("medical")
                .executes(context -> setAutoSupplyDropType(context.getSource(), "medical")))
            .then(Commands.literal("military")
                .executes(context -> setAutoSupplyDropType(context.getSource(), "military"))))
        .then(Commands.literal("count")
            .then(Commands.argument("count", IntegerArgumentType.integer(1, 5))
                .executes(context -> setAutoSupplyDropCount(context.getSource(),
                    IntegerArgumentType.getInteger(context, "count")))))
        .then(Commands.literal("interval")
            .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 12000))
                .executes(context -> setAutoSupplyDropInterval(context.getSource(),
                    IntegerArgumentType.getInteger(context, "ticks")))))
        .then(Commands.literal("enabled")
            .then(Commands.argument("enabled", BoolArgumentType.bool())
                .executes(context -> setAutoSupplyDropEnabled(context.getSource(),
                    BoolArgumentType.getBool(context, "enabled")))))
        .then(Commands.literal("delete")
            .executes(context -> deleteAutoSupplyDropPosition(context.getSource())))
        .then(Commands.literal("status")
            .executes(context -> showAutoSupplyDropStatus(context.getSource())));
  }

  private static int setAutoSupplyDropHere(CommandSourceStack source)
      throws com.mojang.brigadier.exceptions.CommandSyntaxException {
    return setAutoSupplyDropPosition(source, source.getPlayerOrException().blockPosition());
  }

  private static int setAutoSupplyDropPosition(CommandSourceStack source,
      net.minecraft.core.BlockPos position) {
    AutoSupplyDropManager.get(source.getServer()).setPosition(source.getLevel(), position);
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.auto_supply_drop.position",
        position.getX(), position.getY(), position.getZ()), true);
    return 1;
  }

  private static int setAutoSupplyDropType(CommandSourceStack source, String type) {
    AutoSupplyDropManager.get(source.getServer()).setType(type);
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.auto_supply_drop.type_set",
        Component.translatable("command.craftingdeadsurvival.auto_supply_drop.type." + type)),
        true);
    return 1;
  }

  private static int setAutoSupplyDropCount(CommandSourceStack source, int count) {
    AutoSupplyDropManager.get(source.getServer()).setCount(count);
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.auto_supply_drop.count", count), true);
    return count;
  }

  private static int setAutoSupplyDropInterval(CommandSourceStack source, int ticks) {
    AutoSupplyDropManager.get(source.getServer()).setInterval(ticks, source.getServer());
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.auto_supply_drop.interval", ticks), true);
    return ticks;
  }

  private static int setAutoSupplyDropEnabled(CommandSourceStack source, boolean enabled) {
    AutoSupplyDropManager.get(source.getServer()).setEnabled(enabled, source.getServer());
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.auto_supply_drop.enabled", enabled), true);
    return enabled ? 1 : 0;
  }

  private static int showAutoSupplyDropStatus(CommandSourceStack source) {
    source.sendSuccess(() -> AutoSupplyDropManager.get(source.getServer()).status(), false);
    return 1;
  }

  private static int deleteAutoSupplyDropPosition(CommandSourceStack source) {
    AutoSupplyDropManager.get(source.getServer()).clearPosition();
    source.sendSuccess(() -> Component.translatable(
        "command.craftingdeadsurvival.auto_supply_drop.deleted"), true);
    return 1;
  }

  private static int summonSupplyDrops(CommandSourceStack source, String type, int count) {
    ServerPlayer player;
    try {
      player = source.getPlayerOrException();
    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
      source.sendFailure(Component.literal("该指令需要由游戏中的玩家执行。"));
      return 0;
    }
    ResourceKey<LootTable> loot = ResourceKey.create(Registries.LOOT_TABLE,
        ResourceLocation.fromNamespaceAndPath(CraftingDeadSurvival.ID, "supply_drops/" + type));
    int spawned = 0;
    for (int i = 0; i < count; i++) {
      int offsetX = source.getLevel().getRandom().nextInt(11) - 5;
      int offsetZ = source.getLevel().getRandom().nextInt(11) - 5;
      if (SupplyDropRadioItem.spawnSupplyDrop(source.getLevel(),
          player.blockPosition().offset(offsetX, 0, offsetZ), loot)) {
        spawned++;
      }
    }
    int result = spawned;
    source.sendSuccess(() -> Component.literal("已召唤 " + result + " 个" +
        ("medical".equals(type) ? "医疗" : "军用") + "空投。"), true);
    return spawned;
  }

  private static int showHordes(CommandSourceStack source) {
    boolean enabled = source.getLevel().getGameRules().getBoolean(SurvivalGameRules.NIGHT_HORDES);
    source.sendSuccess(() -> Component.literal("夜间尸潮：" + (enabled ? "开启" : "关闭")), false);
    return enabled ? 1 : 0;
  }

  private static int setHordes(CommandSourceStack source, boolean enabled) {
    source.getLevel().getGameRules().getRule(SurvivalGameRules.NIGHT_HORDES)
        .set(enabled, source.getServer());
    return showHordes(source);
  }

  private static int showHordeChance(CommandSourceStack source) {
    int chance = source.getLevel().getGameRules().getInt(SurvivalGameRules.NIGHT_HORDE_CHANCE);
    source.sendSuccess(() -> Component.literal("夜间尸潮触发概率：" + chance + "%"), false);
    return chance;
  }

  private static int setHordeChance(CommandSourceStack source, int chance) {
    source.getLevel().getGameRules().getRule(SurvivalGameRules.NIGHT_HORDE_CHANCE)
        .set(chance, source.getServer());
    return showHordeChance(source);
  }

  private static int showBloodMoonEnabled(CommandSourceStack source) {
    boolean enabled = source.getLevel().getGameRules().getBoolean(SurvivalGameRules.BLOOD_MOON);
    source.sendSuccess(() -> Component.literal("血月：" + (enabled ? "开启" : "关闭")), false);
    return enabled ? 1 : 0;
  }

  private static int setBloodMoonEnabled(CommandSourceStack source, boolean enabled) {
    source.getLevel().getGameRules().getRule(SurvivalGameRules.BLOOD_MOON)
        .set(enabled, source.getServer());
    return showBloodMoonEnabled(source);
  }

  private static int showBloodMoonCycle(CommandSourceStack source) {
    int days = source.getLevel().getGameRules().getInt(SurvivalGameRules.BLOOD_MOON_CYCLE_DAYS);
    source.sendSuccess(() -> Component.literal("血月周期：每 " + days + " 天一次"), false);
    return days;
  }

  private static int setBloodMoonCycle(CommandSourceStack source, int days) {
    source.getLevel().getGameRules().getRule(SurvivalGameRules.BLOOD_MOON_CYCLE_DAYS)
        .set(days, source.getServer());
    return showBloodMoonCycle(source);
  }

  private static int showBloodMoonOmen(CommandSourceStack source) {
    int chance = source.getLevel().getGameRules()
        .getInt(SurvivalGameRules.BLOOD_MOON_OMEN_CHANCE);
    source.sendSuccess(() -> Component.literal("血月前预兆（天空变暗）概率：" + chance + "%"), false);
    return chance;
  }

  private static int setBloodMoonOmen(CommandSourceStack source, int chance) {
    source.getLevel().getGameRules().getRule(SurvivalGameRules.BLOOD_MOON_OMEN_CHANCE)
        .set(chance, source.getServer());
    return showBloodMoonOmen(source);
  }

  private static int showBloodMoonStatus(CommandSourceStack source) {
    ServerLevel level = source.getLevel();
    source.sendSuccess(() -> Component.literal("血月状态："
        + (BloodMoonManager.isEnabled(level) ? "开启" : "关闭")
        + "，周期 " + BloodMoonManager.cycleDays(level) + " 天，当前周期第 "
        + (BloodMoonManager.dayOfCycle(level) + 1) + " 天"
        + (BloodMoonManager.isEnabled(level)
            ? "，距离血月还有 " + BloodMoonManager.daysUntilBloodMoon(level) + " 天"
            : "")), false);
    source.sendSuccess(() -> Component.literal(BloodMoonManager.isBloodMoonDay(level)
        ? "今天是血月天！夜晚将必定出现大规模尸潮。"
        : (BloodMoonManager.isOmenDay(level)
            ? "今天是预兆日，白天天空会变暗。"
            : "今天不是预兆日，天空正常。")), false);
    return 1;
  }

  /**
   * 一键测试血月：推进到本周期血月天的夜晚，并立即在玩家周围刷一波血月级尸潮。
   */
  private static int testBloodMoon(CommandSourceStack source)
      throws com.mojang.brigadier.exceptions.CommandSyntaxException {
    ServerLevel level = source.getServer().overworld();
    long targetDay = BloodMoonManager.currentDay(level)
        + (BloodMoonManager.cycleDays(level) - 1 - BloodMoonManager.dayOfCycle(level));
    level.setDayTime(targetDay * BloodMoonManager.TICKS_PER_DAY + 13000L);
    BloodMoonState state = BloodMoonState.get(level);
    state.forcedDay = targetDay;
    state.administrative = true;
    state.stoppedDay = Long.MIN_VALUE;
    state.nextWave = 0;
    state.setDirty();
    level.getGameRules().getRule(SurvivalGameRules.BLOOD_MOON).set(true, level.getServer());
    source.sendSuccess(() -> Component.literal("血月测试：已跳转到血月天夜晚，尸潮将由统一调度器生成。"), true);
    return 1;
  }

  /**
   * 一键测试预兆日：把预兆概率临时设为 100%，跳转到下一个非血月日的正午（天空最暗时刻）。
   */
  private static int testBloodMoonOmen(CommandSourceStack source) {
    ServerLevel level = source.getLevel();
    level.getGameRules().getRule(SurvivalGameRules.BLOOD_MOON_OMEN_CHANCE)
        .set(100, source.getServer());
    long day = BloodMoonManager.currentDay(level);
    long nextDay = day + 1;
    if (Math.floorMod(nextDay, BloodMoonManager.cycleDays(level))
        == BloodMoonManager.cycleDays(level) - 1) {
      nextDay++;
    }
    level.setDayTime(nextDay * BloodMoonManager.TICKS_PER_DAY + 6000L);
    source.sendSuccess(() -> Component.literal("预兆日测试：已跳转到下一个非血月日的正午，天空将变暗。"
        + "预兆概率已临时设为 100%，测试完可用 /craftingdead bloodmoon omen <数值> 恢复。"), true);
    return 1;
  }

  private static int setGeneratorPos(CommandSourceStack source, int index)
      throws com.mojang.brigadier.exceptions.CommandSyntaxException {
    ServerPlayer player = source.getPlayerOrException();
    GeneratorAreaData.setPosition(player.getUUID(), index, player.blockPosition());
    source.sendSuccess(() -> Component.literal("已设置区域坐标 " + index + "："
        + player.blockPosition().toShortString()), false);
    var draft = GeneratorAreaData.getDraft(player.getUUID());
    if (draft != null && draft.pos1() != null && draft.pos2() != null) {
      source.sendSuccess(() -> Component.literal(
          "坐标选择完成。手持生成器区域选择器输入 /craftingdead generator generate <区域名> 开始随机放置。"), false);
    }
    return 1;
  }

  private static int confirmGeneratorArea(CommandSourceStack source, String name)
      throws com.mojang.brigadier.exceptions.CommandSyntaxException {
    ServerPlayer player = source.getPlayerOrException();
    var draft = GeneratorAreaData.getDraft(player.getUUID());
    if (draft == null || draft.pos1() == null || draft.pos2() == null) {
      source.sendFailure(Component.literal("请先使用 pos1 和 pos2 设置两个区域坐标。"));
      return 0;
    }
    var selector = player.getMainHandItem();
    if (!(selector.getItem() instanceof com.craftingdead.survival.world.item.GeneratorAreaSelectorItem)) {
      source.sendFailure(Component.literal("确认区域时必须主手持有生成器区域选择器。"));
      return 0;
    }
    int placed = GeneratorAreaData.get(source.getLevel()).create(
        source.getLevel(), name, draft, selector);
    if (placed < 0) {
      source.sendFailure(Component.literal("区域名称已存在或坐标无效。"));
      return 0;
    }
    source.sendSuccess(() -> Component.literal("区域“" + name + "”已确认，成功放置 "
        + placed + " 个生成器。"), true);
    return placed;
  }

  private static int deleteGeneratorArea(CommandSourceStack source, String name) {
    int removed = GeneratorAreaData.get(source.getLevel()).delete(source.getLevel(), name);
    if (removed < 0) {
      source.sendFailure(Component.literal("未找到当前维度中的区域：“" + name + "”。"));
      return 0;
    }
    source.sendSuccess(() -> Component.literal("已删除区域“" + name + "”中的 "
        + removed + " 个生成器。"), true);
    return removed;
  }

  private static int listGeneratorAreas(CommandSourceStack source) {
    var areas = GeneratorAreaData.get(source.getLevel()).list();
    source.sendSuccess(() -> Component.literal("生成器区域数量：" + areas.size()), false);
    for (var area : areas) {
      source.sendSuccess(() -> Component.literal("- " + area.name() + " [" + area.dimension()
          + "] " + area.pos1().toShortString() + " -> " + area.pos2().toShortString()
          + "，生成器 " + area.generators().size() + " 个"), false);
    }
    return areas.size();
  }

  private static int openShop(CommandSourceStack source)
      throws com.mojang.brigadier.exceptions.CommandSyntaxException {
    ServerPlayer player = source.getPlayerOrException();
    ShopServer.openFor(player);
    return 1;
  }

  private static int giveShopMoney(CommandSourceStack source, Collection<ServerPlayer> targets,
      int amount) {
    int count = 0;
    for (ServerPlayer player : targets) {
      ShopServer.addMoney(player, amount);
      count++;
    }
    int given = count;
    source.sendSuccess(() -> Component.literal("已给 " + given + " 名玩家发放 " + amount + " ＄。"), true);
    return given;
  }

  private static int showShopBalance(CommandSourceStack source, Collection<ServerPlayer> targets) {
    for (ServerPlayer player : targets) {
      int balance = ShopServer.getBalance(player);
      source.sendSuccess(() -> Component.literal(player.getName().getString()
          + " 的 ＄ 余额：" + balance + " ＄"), false);
    }
    return targets.size();
  }
}

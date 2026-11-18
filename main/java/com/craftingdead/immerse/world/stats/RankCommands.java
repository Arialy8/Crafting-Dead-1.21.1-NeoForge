package com.craftingdead.immerse.world.stats;

import com.craftingdead.core.ModAttachmentTypes;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class RankCommands {

  private static final int TOP_LIMIT = 10;

  @SubscribeEvent
  public void handleNameFormat(PlayerEvent.NameFormat event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    RankDefinitions.Rank rank = RankDefinitions.fromScore(scoreOf(player));
    var prefix = Component.literal("[")
        .withStyle(ChatFormatting.DARK_GRAY)
        .append(Component.translatable(rank.nameKey()).withStyle(
            rank.ordinal() < 6 ? ChatFormatting.GREEN
                : rank.ordinal() < 13 ? ChatFormatting.BLUE : ChatFormatting.RED))
        .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
    event.setDisplayname(prefix.append(event.getUsername().copy().withStyle(ChatFormatting.WHITE)));
  }

  @SubscribeEvent
  public void handleRegisterCommands(RegisterCommandsEvent event) {
    event.getDispatcher().register(Commands.literal("rank")
        .executes(context -> sendSelf(context, context.getSource().getPlayerOrException()))
        .then(Commands.argument("player", EntityArgument.player())
            .executes(context -> sendOther(context, EntityArgument.getPlayer(context, "player")))));
    event.getDispatcher().register(Commands.literal("rankinfo")
        .executes(context -> sendSelf(context, context.getSource().getPlayerOrException()))
        .then(Commands.argument("player", EntityArgument.player())
            .executes(context -> sendOther(context, EntityArgument.getPlayer(context, "player")))));
    event.getDispatcher().register(Commands.literal("ranktop")
        .executes(RankCommands::sendTop));
    event.getDispatcher().register(Commands.literal("rankset")
        .requires(source -> source.hasPermission(2))
        .then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("level", IntegerArgumentType.integer(1, 19))
                .executes(context -> setRank(context, EntityArgument.getPlayer(context, "player"),
                    IntegerArgumentType.getInteger(context, "level"))))));
  }

  private static int sendSelf(CommandContext<CommandSourceStack> context, ServerPlayer player) {
    return sendRankInfo(context, player, false);
  }

  private static int sendOther(CommandContext<CommandSourceStack> context, ServerPlayer target) {
    return sendRankInfo(context, target, true);
  }

  private static int sendRankInfo(CommandContext<CommandSourceStack> context,
      ServerPlayer player, boolean includeName) {
    long score = scoreOf(player);
    RankDefinitions.Rank rank = RankDefinitions.fromScore(score);
    long next = RankDefinitions.nextLevelScore(score);
    Component name = Component.translatable(rank.nameKey());
    Component message = includeName
        ? Component.translatable(next == Long.MAX_VALUE
            ? "command.craftingdeadimmerse.rank.other_max"
            : "command.craftingdeadimmerse.rank.other",
            player.getDisplayName(), name, score, next - score)
        : Component.translatable(next == Long.MAX_VALUE
            ? "command.craftingdeadimmerse.rank.self_max"
            : "command.craftingdeadimmerse.rank.self",
            name, score, next - score);
    context.getSource().sendSuccess(() -> message, false);
    return 1;
  }

  private static int sendTop(CommandContext<CommandSourceStack> context) {
    List<ServerPlayer> players = context.getSource().getServer().getPlayerList().getPlayers()
        .stream()
        .sorted(Comparator.comparingLong(RankCommands::scoreOf).reversed()
            .thenComparing(player -> player.getGameProfile().getName()))
        .limit(TOP_LIMIT)
        .toList();
    context.getSource().sendSuccess(() -> Component.translatable(
        "command.craftingdeadimmerse.rank.top", TOP_LIMIT), false);
    int place = 1;
    for (ServerPlayer player : players) {
      int index = place++;
      context.getSource().sendSuccess(() -> Component.translatable(
          "command.craftingdeadimmerse.rank.top_entry", index, player.getDisplayName(),
          Component.translatable(RankDefinitions.fromScore(scoreOf(player)).nameKey()),
          scoreOf(player)), false);
    }
    return players.size();
  }

  private static int setRank(CommandContext<CommandSourceStack> context,
      ServerPlayer target, int level) {
    if (level < 1 || level > 19) {
      context.getSource().sendFailure(Component.translatable(
          "command.craftingdeadimmerse.rankset.invalid_level"));
      return 0;
    }
    RankDefinitions.Rank[] ranks = RankDefinitions.Rank.values();
    RankDefinitions.Rank rank = ranks[level - 1];
    long targetScore = rank.minScore();
    var stats = target.getData(ModAttachmentTypes.getSurvivalStats());
    stats.setRankScore(targetScore);
    com.craftingdead.core.CraftingDead.syncSurvivalStats(target);
    context.getSource().sendSuccess(() -> Component.translatable(
        "command.craftingdeadimmerse.rankset.success",
        target.getDisplayName(),
        Component.translatable(rank.nameKey()),
        level), true);
    return 1;
  }

  private static long scoreOf(ServerPlayer player) {
    var stats = player.getData(ModAttachmentTypes.getSurvivalStats());
    return stats.getRankScore();
  }
}

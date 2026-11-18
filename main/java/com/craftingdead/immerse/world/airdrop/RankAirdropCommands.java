package com.craftingdead.immerse.world.airdrop;

import com.craftingdead.core.ModAttachmentTypes;
import com.craftingdead.immerse.world.stats.RankDefinitions;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * The {@code /rankdrop} command family: summon the rank airdrop and query remaining daily uses.
 */
public final class RankAirdropCommands {

  @SubscribeEvent
  public void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
    if (event.getServer().getTickCount() % 20 == 0)
      RankDropSchedule.get(event.getServer()).tick(event.getServer());
  }

  @SubscribeEvent
  public void handleRegisterCommands(RegisterCommandsEvent event) {
    event.getDispatcher().register(Commands.literal("rankdrop")
        .executes(context -> summon(context.getSource()))
        .then(Commands.literal("status")
            .executes(context -> showStatus(context.getSource())))
        .then(Commands.literal("edit").requires(source -> source.hasPermission(2))
            .then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 19))
                .executes(context -> openEditor(context.getSource(),
                    com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "level"))))));
  }

  private static int openEditor(CommandSourceStack source, int rank) {
    ServerPlayer player = playerOf(source);
    if (player == null) return 0;
    try {
      RankRewardData.get(player.server).rewards(player.server, rank);
    } catch (IllegalStateException e) {
      source.sendFailure(Component.literal("原奖励表导入失败，未修改奖励：" + e.getCause()));
      return 0;
    }
    player.openMenu(new net.minecraft.world.SimpleMenuProvider(
        (id, inventory, owner) -> new com.craftingdead.immerse.world.inventory.RankRewardMenu(
            id, inventory, player, rank),
        Component.literal("军衔 " + rank + " 级 · 奖励配置")));
    player.sendSystemMessage(Component.literal(
        "每两行为高/中/低概率。持物点击复制模板（不消耗），空手左键删除、右键减1。"
        + "修改立即保存；翻页查看全部奖励。修改后每项独立按60%/30%/10%抽取，数量1至模板数量。"));
    return 1;
  }

  private static int summon(CommandSourceStack source) {
    ServerPlayer player = playerOf(source);
    if (player == null) {
      return 0;
    }
    if (!ImmerseAirdropConfig.instance.enableAirdrops.get()) {
      player.displayClientMessage(Component.translatable(
          "command.craftingdeadimmerse.rankdrop.disabled"), true);
      return 0;
    }
    RankAirdrop.SummonResult result = RankAirdrop.summon(player);
    switch (result.result()) {
      case SUMMONED -> player.displayClientMessage(Component.translatable(
          "command.craftingdeadimmerse.rankdrop.summoned",
          result.landing().getX(), result.landing().getZ()), true);
      case NO_USES_LEFT -> player.displayClientMessage(Component.translatable(
          "command.craftingdeadimmerse.rankdrop.no_uses_left"), true);
      case NO_SPACE -> player.displayClientMessage(Component.translatable(
          "command.craftingdeadimmerse.rankdrop.no_space"), true);
      case DISABLED -> player.displayClientMessage(Component.translatable(
          "command.craftingdeadimmerse.rankdrop.disabled"), true);
    }
    return result.result() == RankAirdrop.Result.SUMMONED ? 1 : 0;
  }

  private static int showStatus(CommandSourceStack source) {
    ServerPlayer player = playerOf(source);
    if (player == null) {
      return 0;
    }
    int remaining = RankAirdrop.usesRemaining(player);
    int maxDaily = RankAirdrop.dailyUsesFor(player);
    long seconds = (player.getData(ModAttachmentTypes.getSurvivalStats())
        .airdropResetRemaining(System.currentTimeMillis()) + 999) / 1000;
    player.sendSystemMessage(Component.literal("现实24小时额度：" + remaining + "/" + maxDaily
        + (seconds == 0 ? "，首次领取开始计时" : "，距刷新 "
            + seconds / 3600 + "小时 " + seconds % 3600 / 60 + "分")));
    player.displayClientMessage(Component.translatable(
        "command.craftingdeadimmerse.rankdrop.status",
        remaining, maxDaily), true);
    return 1;
  }

  private static ServerPlayer playerOf(CommandSourceStack source) {
    try {
      return source.getPlayerOrException();
    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
      source.sendFailure(Component.translatable(
          "command.craftingdeadimmerse.rankdrop.requires_player"));
      return null;
    }
  }

  private static String formatDuration(long seconds) {
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
  }
}

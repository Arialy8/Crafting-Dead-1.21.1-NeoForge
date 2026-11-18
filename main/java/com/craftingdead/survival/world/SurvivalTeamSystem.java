package com.craftingdead.survival.world;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Lightweight survival squads with invitations, five-player limit and friendly-fire protection. */
public final class SurvivalTeamSystem {
  private static final int MAX_MEMBERS = 5;
  private static final Map<String, Squad> SQUADS = new HashMap<>();
  private static final Map<UUID, String> MEMBERSHIP = new HashMap<>();
  private static final Map<UUID, String> INVITES = new HashMap<>();

  @SubscribeEvent
  public void registerCommands(RegisterCommandsEvent event) {
    event.getDispatcher().register(Commands.literal("team")
        .then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.word())
            .executes(c -> create(c.getSource(), StringArgumentType.getString(c, "name")))))
        .then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player())
            .executes(c -> invite(c.getSource(), EntityArgument.getPlayer(c, "player")))))
        .then(Commands.literal("accept").executes(c -> accept(c.getSource())))
        .then(Commands.literal("decline").executes(c -> decline(c.getSource())))
        .then(Commands.literal("kick").then(Commands.argument("player", EntityArgument.player())
            .executes(c -> kick(c.getSource(), EntityArgument.getPlayer(c, "player")))))
        .then(Commands.literal("leave").executes(c -> leave(c.getSource()))));
  }

  @SubscribeEvent
  public void preventFriendlyFire(LivingIncomingDamageEvent event) {
    if (event.getEntity() instanceof ServerPlayer victim
        && event.getSource().getEntity() instanceof ServerPlayer attacker
        && sameSquad(victim.getUUID(), attacker.getUUID())) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public void showNearbyTeammates(EntityTickEvent.Post event) {
    if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
      return;
    }
    boolean nearby = player.serverLevel().players().stream()
        .anyMatch(other -> other != player && sameSquad(player.getUUID(), other.getUUID())
            && player.distanceToSqr(other) <= 100.0D);
    player.setGlowingTag(nearby);
  }

  private static int create(CommandSourceStack source, String name) throws CommandSyntaxException {
    var player = source.getPlayerOrException();
    String key = name.toLowerCase(java.util.Locale.ROOT);
    if (MEMBERSHIP.containsKey(player.getUUID()) || SQUADS.containsKey(key)) {
      return fail(source, "你已经在小队中，或该小队名称已存在。");
    }
    var squad = new Squad(player.getUUID());
    squad.members.add(player.getUUID());
    SQUADS.put(key, squad);
    MEMBERSHIP.put(player.getUUID(), key);
    return success(source, "已创建小队：" + name);
  }

  private static int invite(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
    var player = source.getPlayerOrException();
    var squad = ownedSquad(player);
    if (squad == null) return fail(source, "只有队长可以邀请玩家。");
    if (squad.members.size() >= MAX_MEMBERS) return fail(source, "小队人数已达到5人上限。");
    if (MEMBERSHIP.containsKey(target.getUUID())) return fail(source, "该玩家已经在小队中。");
    String key = MEMBERSHIP.get(player.getUUID());
    INVITES.put(target.getUUID(), key);
    target.sendSystemMessage(Component.literal(player.getName().getString()
        + " 邀请你加入小队。使用 /team accept 或 /team decline。"));
    return success(source, "已邀请 " + target.getName().getString());
  }

  private static int accept(CommandSourceStack source) throws CommandSyntaxException {
    var player = source.getPlayerOrException();
    String key = INVITES.remove(player.getUUID());
    var squad = key == null ? null : SQUADS.get(key);
    if (squad == null || MEMBERSHIP.containsKey(player.getUUID())) return fail(source, "没有有效的小队邀请。");
    if (squad.members.size() >= MAX_MEMBERS) return fail(source, "该小队人数已满。");
    squad.members.add(player.getUUID());
    MEMBERSHIP.put(player.getUUID(), key);
    return success(source, "已加入小队：" + key);
  }

  private static int decline(CommandSourceStack source) throws CommandSyntaxException {
    var player = source.getPlayerOrException();
    return INVITES.remove(player.getUUID()) == null
        ? fail(source, "没有待处理的小队邀请。") : success(source, "已拒绝小队邀请。");
  }

  private static int kick(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
    var player = source.getPlayerOrException();
    var squad = ownedSquad(player);
    if (squad == null || target.getUUID().equals(player.getUUID())
        || !squad.members.remove(target.getUUID())) return fail(source, "无法踢出该玩家。");
    MEMBERSHIP.remove(target.getUUID());
    target.setGlowingTag(false);
    target.sendSystemMessage(Component.literal("你已被移出小队。"));
    return success(source, "已将玩家移出小队。");
  }

  private static int leave(CommandSourceStack source) throws CommandSyntaxException {
    var player = source.getPlayerOrException();
    String key = MEMBERSHIP.remove(player.getUUID());
    var squad = key == null ? null : SQUADS.get(key);
    if (squad == null) return fail(source, "你当前不在小队中。");
    squad.members.remove(player.getUUID());
    player.setGlowingTag(false);
    if (squad.owner.equals(player.getUUID()) || squad.members.isEmpty()) {
      squad.members.forEach(MEMBERSHIP::remove);
      SQUADS.remove(key);
    }
    return success(source, "已退出小队。");
  }

  public static void inviteByUuid(ServerPlayer sender, UUID targetUuid) {
    var server = sender.getServer();
    if (server == null) {
      return;
    }
    var squad = SQUADS.get(MEMBERSHIP.get(sender.getUUID()));
    if (squad == null || !squad.owner.equals(sender.getUUID())) {
      sender.sendSystemMessage(Component.literal("\u00a7c只有队长可以邀请玩家。"));
      return;
    }
    if (squad.members.size() >= MAX_MEMBERS) {
      sender.sendSystemMessage(Component.literal("\u00a7c小队人数已达到5人上限。"));
      return;
    }
    if (MEMBERSHIP.containsKey(targetUuid)) {
      sender.sendSystemMessage(Component.literal("\u00a7c该玩家已经在小队中。"));
      return;
    }
    ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
    if (target == null) {
      sender.sendSystemMessage(Component.literal("\u00a7c该玩家不在线。"));
      return;
    }
    String squadKey = MEMBERSHIP.get(sender.getUUID());
    INVITES.put(targetUuid, squadKey);
    String senderName = sender.getDisplayName().getString();
    target.sendSystemMessage(Component.literal("\u00a7f" + senderName
        + " \u00a7a邀请你加入小队。使用 /team accept 或 /team decline。"));
    sender.sendSystemMessage(Component.literal("\u00a7a已邀请 \u00a7f"
        + target.getDisplayName().getString() + " \u00a7a加入小队。"));
  }

  public static void kickByUuid(ServerPlayer sender, UUID targetUuid) {
    var squad = SQUADS.get(MEMBERSHIP.get(sender.getUUID()));
    if (squad == null || !squad.owner.equals(sender.getUUID())) {
      sender.sendSystemMessage(Component.literal("\u00a7c只有队长可以踢出玩家。"));
      return;
    }
    if (targetUuid.equals(sender.getUUID())) {
      sender.sendSystemMessage(Component.literal("\u00a7c你不能踢出自己。"));
      return;
    }
    if (!squad.members.contains(targetUuid)) {
      sender.sendSystemMessage(Component.literal("\u00a7c该玩家不在你的小队中。"));
      return;
    }
    squad.members.remove(targetUuid);
    MEMBERSHIP.remove(targetUuid);
    var server = sender.getServer();
    if (server != null) {
      ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
      if (target != null) {
        target.setGlowingTag(false);
        target.sendSystemMessage(Component.literal("\u00a7c你已被移出小队。"));
      }
    }
    sender.sendSystemMessage(Component.literal("\u00a7a已将玩家移出小队。"));
  }

  public static void transferLeadership(ServerPlayer sender, UUID targetUuid) {
    var squad = SQUADS.get(MEMBERSHIP.get(sender.getUUID()));
    if (squad == null || !squad.owner.equals(sender.getUUID())) {
      sender.sendSystemMessage(Component.literal("\u00a7c只有队长可以转让队长。"));
      return;
    }
    if (!squad.members.contains(targetUuid)) {
      sender.sendSystemMessage(Component.literal("\u00a7c该玩家不在你的小队中。"));
      return;
    }
    squad.owner = targetUuid;
    var server = sender.getServer();
    if (server != null) {
      String targetName = server.getPlayerList().getPlayer(targetUuid) != null
          ? server.getPlayerList().getPlayer(targetUuid).getDisplayName().getString()
          : server.getProfileCache().get(targetUuid).map(com.mojang.authlib.GameProfile::getName).orElse("Unknown");
      sender.sendSystemMessage(Component.literal("\u00a7a已将队长转让给 \u00a7f" + targetName + " \u00a7a。"));
      ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
      if (target != null) {
        target.sendSystemMessage(Component.literal("\u00a7a你现在是小队队长了。"));
      }
    }
  }

  public static java.util.List<UUID> getTeamMembers(ServerPlayer player) {
    String squadKey = MEMBERSHIP.get(player.getUUID());
    if (squadKey == null) {
      return java.util.List.of();
    }
    var squad = SQUADS.get(squadKey);
    return squad != null ? java.util.List.copyOf(squad.members) : java.util.List.of();
  }

  public static UUID getSquadOwnerUuid(ServerPlayer player) {
    String squadKey = MEMBERSHIP.get(player.getUUID());
    if (squadKey == null) {
      return null;
    }
    var squad = SQUADS.get(squadKey);
    return squad != null ? squad.owner : null;
  }

  public static boolean isSquadOwner(ServerPlayer player) {
    var squad = SQUADS.get(MEMBERSHIP.get(player.getUUID()));
    return squad != null && squad.owner.equals(player.getUUID());
  }

  public static boolean isInSquad(ServerPlayer player) {
    return MEMBERSHIP.containsKey(player.getUUID());
  }

  public static boolean isPlayerInAnySquad(UUID playerUuid) {
    return MEMBERSHIP.containsKey(playerUuid);
  }

  private static Squad ownedSquad(ServerPlayer player) {
    var squad = SQUADS.get(MEMBERSHIP.get(player.getUUID()));
    return squad != null && squad.owner.equals(player.getUUID()) ? squad : null;
  }

  private static boolean sameSquad(UUID first, UUID second) {
    String team = MEMBERSHIP.get(first);
    return team != null && team.equals(MEMBERSHIP.get(second));
  }

  private static int success(CommandSourceStack source, String text) {
    source.sendSuccess(() -> Component.literal(text), false);
    return 1;
  }

  private static int fail(CommandSourceStack source, String text) {
    source.sendFailure(Component.literal(text));
    return 0;
  }

  private static final class Squad {
    private UUID owner;
    private final Set<UUID> members = new LinkedHashSet<>();
    private Squad(UUID owner) { this.owner = owner; }
  }
}

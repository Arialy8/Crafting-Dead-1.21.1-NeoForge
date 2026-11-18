package com.craftingdead.immerse.world.base;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class BlueprintRestrictionCommands {

  private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

  private record Selection(ServerLevel level, BlockPos first, BlockPos second) {

    Selection withFirst(ServerLevel newLevel, BlockPos pos) {
      return new Selection(newLevel, pos.immutable(),
          this.level == newLevel ? this.second : null);
    }

    Selection withSecond(ServerLevel newLevel, BlockPos pos) {
      return new Selection(newLevel,
          this.level == newLevel ? this.first : null, pos.immutable());
    }
  }

  @SubscribeEvent
  public void handleRegisterCommands(RegisterCommandsEvent event) {
    event.getDispatcher().register(Commands.literal("basezone")
        .requires(source -> source.hasPermission(2))
        .then(Commands.literal("pos1")
            .executes(context -> setPosition(context, 1,
                context.getSource().getPlayerOrException().blockPosition()))
            .then(Commands.argument("position", BlockPosArgument.blockPos())
                .executes(context -> setPosition(context, 1,
                    BlockPosArgument.getLoadedBlockPos(context, "position")))))
        .then(Commands.literal("pos2")
            .executes(context -> setPosition(context, 2,
                context.getSource().getPlayerOrException().blockPosition()))
            .then(Commands.argument("position", BlockPosArgument.blockPos())
                .executes(context -> setPosition(context, 2,
                    BlockPosArgument.getLoadedBlockPos(context, "position")))))
        .then(Commands.literal("add")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(BlueprintRestrictionCommands::addZone)))
        .then(Commands.literal("remove")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(BlueprintRestrictionCommands::removeZone)))
        .then(Commands.literal("list")
            .executes(BlueprintRestrictionCommands::listZones)));
  }

  private static int setPosition(CommandContext<CommandSourceStack> context, int index,
      BlockPos pos) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();
    ServerLevel level = context.getSource().getLevel();
    Selection current = SELECTIONS.getOrDefault(player.getUUID(),
        new Selection(level, null, null));
    Selection updated = index == 1
        ? current.withFirst(level, pos)
        : current.withSecond(level, pos);
    SELECTIONS.put(player.getUUID(), updated);
    context.getSource().sendSuccess(() -> Component.translatable(
        "command.craftingdeadimmerse.basezone.position_set",
        index, pos.getX(), pos.getY(), pos.getZ()), false);
    return 1;
  }

  private static int addZone(CommandContext<CommandSourceStack> context)
      throws com.mojang.brigadier.exceptions.CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();
    ServerLevel level = context.getSource().getLevel();
    Selection selection = SELECTIONS.get(player.getUUID());
    if (selection == null || selection.level != level
        || selection.first == null || selection.second == null) {
      context.getSource().sendFailure(Component.translatable(
          "command.craftingdeadimmerse.basezone.selection_incomplete"));
      return 0;
    }

    String name = StringArgumentType.getString(context, "name");
    if (!BlueprintRestrictionData.add(level, name, selection.first, selection.second)) {
      context.getSource().sendFailure(Component.translatable(
          "command.craftingdeadimmerse.basezone.add_failed", name));
      return 0;
    }
    SELECTIONS.remove(player.getUUID());
    context.getSource().sendSuccess(() -> Component.translatable(
        "command.craftingdeadimmerse.basezone.added", name), true);
    return 1;
  }

  private static int removeZone(CommandContext<CommandSourceStack> context) {
    String name = StringArgumentType.getString(context, "name");
    if (!BlueprintRestrictionData.remove(context.getSource().getLevel(), name)) {
      context.getSource().sendFailure(Component.translatable(
          "command.craftingdeadimmerse.basezone.not_found", name));
      return 0;
    }
    context.getSource().sendSuccess(() -> Component.translatable(
        "command.craftingdeadimmerse.basezone.removed", name), true);
    return 1;
  }

  private static int listZones(CommandContext<CommandSourceStack> context) {
    var zones = BlueprintRestrictionData.list(context.getSource().getLevel());
    context.getSource().sendSuccess(() -> Component.translatable(
        "command.craftingdeadimmerse.basezone.count", zones.size()), false);
    for (var zone : zones) {
      context.getSource().sendSuccess(() -> Component.literal(
          zone.name() + ": " + format(zone.min()) + " -> " + format(zone.max())), false);
    }
    return zones.size();
  }

  private static String format(BlockPos pos) {
    return pos.getX() + " " + pos.getY() + " " + pos.getZ();
  }
}

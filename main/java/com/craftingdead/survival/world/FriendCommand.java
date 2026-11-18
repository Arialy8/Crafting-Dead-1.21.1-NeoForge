package com.craftingdead.survival.world;

import com.craftingdead.survival.world.friend.FriendActions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class FriendCommand {

  @SubscribeEvent
  public void registerCommands(RegisterCommandsEvent event) {
    event.getDispatcher().register(Commands.literal("friend")
        .executes(context -> openFriendMenu(context.getSource())));
  }

  private static int openFriendMenu(CommandSourceStack source) throws CommandSyntaxException {
    ServerPlayer player = source.getPlayerOrException();
    FriendActions.syncFriendMenu(player, true);
    return 1;
  }
}

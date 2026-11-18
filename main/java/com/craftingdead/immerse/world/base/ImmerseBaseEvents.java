package com.craftingdead.immerse.world.base;

import com.craftingdead.immerse.world.level.block.entity.BaseCenterBlockEntity;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.Container;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.minecraft.world.entity.item.FallingBlockEntity;

public class ImmerseBaseEvents {

  @SubscribeEvent
  public void handleRegisterCommands(RegisterCommandsEvent event) {
    event.getDispatcher().register(Commands.literal("base")
        .then(Commands.literal("add")
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> updateMember(context.getSource().getPlayerOrException(),
                    EntityArgument.getPlayer(context, "player"), true))))
        .then(Commands.literal("remove")
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> updateMember(context.getSource().getPlayerOrException(),
                    EntityArgument.getPlayer(context, "player"), false))))
        .then(Commands.literal("permission")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("permission", StringArgumentType.word())
                    .executes(context -> togglePermission(
                        context.getSource().getPlayerOrException(),
                        EntityArgument.getPlayer(context, "player"),
                        StringArgumentType.getString(context, "permission"))))))
        .then(Commands.literal("administrator")
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> toggleAdministrator(
                    context.getSource().getPlayerOrException(),
                    EntityArgument.getPlayer(context, "player")))))
        .then(Commands.literal("list")
            .executes(context -> listMembers(context.getSource().getPlayerOrException())))
        .then(Commands.literal("craft")
            .then(Commands.argument("recipe", StringArgumentType.word())
                .executes(context -> craftBaseRecipe(
                    context.getSource().getPlayerOrException(),
                    StringArgumentType.getString(context, "recipe")))))
        .then(Commands.literal("storage")
            .then(Commands.literal("import")
                .executes(context -> updateRepairStorage(
                    context.getSource().getPlayerOrException(), "import", 0)))
            .then(Commands.literal("export")
                .then(Commands.argument("type", IntegerArgumentType.integer(0, 2))
                    .executes(context -> updateRepairStorage(
                        context.getSource().getPlayerOrException(), "export",
                        IntegerArgumentType.getInteger(context, "type")))))
            .then(Commands.literal("repair")
                .executes(context -> updateRepairStorage(
                    context.getSource().getPlayerOrException(), "repair", 0)))));
  }

  @SubscribeEvent
  public void handleRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    boolean container = event.getLevel().getBlockEntity(event.getPos()) instanceof Container
        || event.getLevel().getBlockState(event.getPos())
            .getMenuProvider(event.getLevel(), event.getPos()) != null;
    boolean allowed = container
        ? BaseSecurity.canOpenContainer(event.getLevel(), event.getPos(), event.getEntity())
        : BaseSecurity.canUseProtectedPos(event.getLevel(), event.getPos(), event.getEntity());
    if (!allowed) {
      event.setCanceled(true);
      notifyDenied(event.getLevel(), event.getPos(), event.getEntity(),
          container ? BasePermission.CONTAINER : null);
    }
  }

  @SubscribeEvent
  public void handleLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
    if (!BaseSecurity.canUseProtectedPos(event.getLevel(), event.getPos(), event.getEntity())) {
      event.setCanceled(true);
      notifyDenied(event.getLevel(), event.getPos(), event.getEntity(), null);
    }
  }

  @SubscribeEvent
  public void handleEntityInteract(PlayerInteractEvent.EntityInteract event) {
    if (!BaseSecurity.canUseProtectedPos(event.getLevel(), event.getTarget().blockPosition(),
        event.getEntity())) {
      event.setCanceled(true);
      notifyDenied(event.getLevel(), event.getTarget().blockPosition(), event.getEntity(), null);
    }
  }

  @SubscribeEvent
  public void handleSpecificEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
    if (!BaseSecurity.canUseProtectedPos(event.getLevel(), event.getTarget().blockPosition(),
        event.getEntity())) {
      event.setCanceled(true);
      notifyDenied(event.getLevel(), event.getTarget().blockPosition(), event.getEntity(), null);
    }
  }

  @SubscribeEvent
  public void handleBlockBreak(BlockEvent.BreakEvent event) {
    if (!(event.getLevel() instanceof Level level)) {
      return;
    }
    if (level.getBlockEntity(event.getPos()) instanceof BaseCenterBlockEntity baseCenter
        && !baseCenter.isOwner(event.getPlayer())
        && !baseCenter.isBreached()
        && !event.getPlayer().getAbilities().instabuild) {
      event.setCanceled(true);
      notifyDenied(level, event.getPos(), event.getPlayer(), BasePermission.BREAK);
      return;
    }
    if (!BaseSecurity.canBreak(level, event.getPos(), event.getPlayer())) {
      event.setCanceled(true);
      return;
    }
    if (level.getBlockEntity(event.getPos()) instanceof BaseCenterBlockEntity baseCenter) {
      baseCenter.unregister();
    }
  }

  @SubscribeEvent
  public void handleBlockPlace(BlockEvent.EntityPlaceEvent event) {
    if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player
        && event.getLevel() instanceof Level level
        && !BaseSecurity.canBuild(level, event.getPos(), player)) {
      event.setCanceled(true);
      notifyDenied(level, event.getPos(), player, BasePermission.BUILD);
    } else if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player)
        && !(event.getEntity() instanceof FallingBlockEntity)
        && event.getLevel() instanceof Level level
        && BaseSecurity.findBase(level, event.getPos()).isPresent()) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public void handleFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
    if (event.getLevel() instanceof Level level
        && (BaseSecurity.findBase(level, event.getPos()).isPresent()
            || BaseSecurity.findBase(level, event.getLiquidPos()).isPresent())) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public void handlePiston(PistonEvent.Pre event) {
    if (!(event.getLevel() instanceof Level level)) {
      return;
    }
    var resolver = event.getStructureHelper();
    if (BaseSecurity.findBase(level, event.getPos()).isPresent()
        || BaseSecurity.findBase(level, event.getFaceOffsetPos()).isPresent()) {
      event.setCanceled(true);
      return;
    }
    if (resolver != null && resolver.resolve()) {
      boolean touchesBase = resolver.getToPush().stream()
          .anyMatch(pos -> BaseSecurity.findBase(level, pos).isPresent())
          || resolver.getToDestroy().stream()
              .anyMatch(pos -> BaseSecurity.findBase(level, pos).isPresent());
      if (touchesBase) {
        event.setCanceled(true);
      }
    }
  }

  @SubscribeEvent
  public void handleExplosionDetonate(ExplosionEvent.Detonate event) {
    var attacker = event.getExplosion().getIndirectSourceEntity();
    if (!(attacker instanceof net.minecraft.world.entity.player.Player player)) {
      event.getAffectedBlocks().removeIf(pos -> BaseSecurity.findBase(event.getLevel(), pos).isPresent());
      return;
    }

    Set<BaseCenterBlockEntity> hostileBases = new HashSet<>();
    for (var pos : event.getAffectedBlocks()) {
      BaseSecurity.findBase(event.getLevel(), pos)
          .filter(base -> !base.canUse(player))
          .ifPresent(hostileBases::add);
    }
    for (BaseCenterBlockEntity base : hostileBases) {
      boolean breached = base.damageFromRaid(150);
      player.displayClientMessage(Component.translatable(
          breached ? "message.craftingdeadimmerse.raid_breached"
              : "message.craftingdeadimmerse.raid_damaged",
          base.getRaidHealth(), BaseCenterBlockEntity.MAX_RAID_HEALTH), true);
    }
    event.getAffectedBlocks().removeIf(pos -> BaseSecurity.findBase(event.getLevel(), pos)
        .map(base -> base.canUse(player) || !base.isBreached())
        .orElse(false));
  }

  private static int craftBaseRecipe(ServerPlayer player, String recipeId) {
    BaseRecipe recipe = BaseRecipe.get(recipeId);
    if (recipe == null) {
      player.displayClientMessage(Component.translatable(
          "message.craftingdeadimmerse.base_recipe_unknown", recipeId), false);
      return 0;
    }
    BaseCenterBlockEntity baseCenter = recipe.domain() == BaseRecipe.Domain.BASE
        ? BaseSecurity.findBase(player.level(), player.blockPosition()).orElse(null) : null;
    if (recipe.domain() == BaseRecipe.Domain.BASE
        && (baseCenter == null
            || !BaseSecurity.canCraft(player.level(), player.blockPosition(), player))) {
      player.displayClientMessage(Component.translatable(
          "message.craftingdeadimmerse.base_recipe_requires_base"), false);
      return 0;
    }
    if (!(recipe.domain() == BaseRecipe.Domain.BASE
        ? recipe.craft(player, baseCenter) : recipe.craft(player))) {
      player.displayClientMessage(Component.translatable(
          "message.craftingdeadimmerse.base_recipe_missing"), false);
      return 0;
    }
    player.displayClientMessage(Component.translatable(
        "message.craftingdeadimmerse.base_recipe_crafted",
        new net.minecraft.world.item.ItemStack(
            com.craftingdead.immerse.world.item.ImmerseItems.ITEMS.get(recipe.output()).get())
            .getHoverName()), false);
    return 1;
  }

  private static int updateRepairStorage(ServerPlayer player, String action, int type) {
    BaseCenterBlockEntity baseCenter = BaseSecurity.findBase(player.level(), player.blockPosition())
        .orElse(null);
    if (baseCenter == null || !baseCenter.canUse(player)) {
      player.displayClientMessage(Component.translatable(
          "message.craftingdeadimmerse.base_no_use_permission"), false);
      return 0;
    }
    boolean allowed = switch (action) {
      case "repair" -> baseCenter.hasPermission(player, BasePermission.BUILD);
      default -> baseCenter.hasPermission(player, BasePermission.CONTAINER);
    };
    if (!allowed) {
      player.displayClientMessage(Component.translatable(
          "message.craftingdeadimmerse.base_no_"
              + (action.equals("repair") ? "build" : "container") + "_permission"), false);
      return 0;
    }
    boolean changed = switch (action) {
      case "import" -> baseCenter.importRepairResources(player);
      case "export" -> baseCenter.exportRepairStack(player, type);
      case "repair" -> baseCenter.repairFromStorage();
      default -> false;
    };
    player.displayClientMessage(Component.translatable(changed
        ? "message.craftingdeadimmerse.base_storage_changed"
        : "message.craftingdeadimmerse.base_storage_unchanged"), false);
    return changed ? 1 : 0;
  }

  private static int updateMember(ServerPlayer owner, ServerPlayer member, boolean add) {
    BaseCenterBlockEntity baseCenter = BaseSecurity.findBase(owner.level(), owner.blockPosition())
        .filter(base -> canManagePermissions(base, owner))
        .orElse(null);
    if (baseCenter == null) {
      owner.displayClientMessage(Component.translatable(
          "message.craftingdeadimmerse.base_owner_required"), false);
      return 0;
    }
    boolean changed = add
        ? baseCenter.addMember(member.getUUID())
        : baseCenter.removeMember(member.getUUID());
    owner.displayClientMessage(Component.translatable(
        changed
            ? (add
                ? "message.craftingdeadimmerse.base_member_added"
                : "message.craftingdeadimmerse.base_member_removed")
            : "message.craftingdeadimmerse.base_member_unchanged",
        member.getDisplayName()), false);
    return changed ? 1 : 0;
  }

  private static int togglePermission(ServerPlayer actor, ServerPlayer member,
      String permissionName) {
    BaseCenterBlockEntity baseCenter = BaseSecurity.findBase(actor.level(), actor.blockPosition())
        .orElse(null);
    BasePermission permission = BasePermission.byName(permissionName);
    if (baseCenter == null || permission == null || !canManagePermissions(baseCenter, actor)
        || baseCenter.isOwner(member)) {
      actor.displayClientMessage(Component.translatable(
          permission == null ? "message.craftingdeadimmerse.base_permission_invalid"
              : "message.craftingdeadimmerse.base_owner_required"), false);
      return 0;
    }
    boolean allowed = !baseCenter.getPermissions(member.getUUID()).contains(permission);
    boolean changed = baseCenter.setPermission(member.getUUID(), permission, allowed);
    actor.displayClientMessage(Component.translatable(
        changed ? "message.craftingdeadimmerse.base_permission_changed"
            : "message.craftingdeadimmerse.base_member_unchanged",
        member.getDisplayName(), permission.name().toLowerCase()), false);
    return changed ? 1 : 0;
  }

  private static int toggleAdministrator(ServerPlayer actor, ServerPlayer member) {
    BaseCenterBlockEntity baseCenter = ownedBase(actor);
    if (baseCenter == null || baseCenter.isOwner(member)) {
      actor.displayClientMessage(Component.translatable(
          "message.craftingdeadimmerse.base_owner_required"), false);
      return 0;
    }
    boolean administrator = !baseCenter.isAdministrator(member.getUUID());
    boolean changed = baseCenter.setAdministrator(member.getUUID(), administrator);
    actor.displayClientMessage(Component.translatable(
        changed ? "message.craftingdeadimmerse.base_administrator_changed"
            : "message.craftingdeadimmerse.base_member_unchanged",
        member.getDisplayName(), administrator), false);
    return changed ? 1 : 0;
  }

  private static boolean canManagePermissions(BaseCenterBlockEntity baseCenter,
      ServerPlayer actor) {
    return baseCenter.isOwner(actor) || baseCenter.isAdministrator(actor.getUUID());
  }

  private static int listMembers(ServerPlayer owner) {
    BaseCenterBlockEntity baseCenter = ownedBase(owner);
    if (baseCenter == null) {
      owner.displayClientMessage(Component.translatable(
          "message.craftingdeadimmerse.base_owner_required"), false);
      return 0;
    }
    owner.displayClientMessage(Component.translatable(
        "message.craftingdeadimmerse.base_member_count", baseCenter.getMembers().size()), false);
    for (java.util.UUID memberId : baseCenter.getMembers()) {
      String memberName = owner.server.getProfileCache().get(memberId)
          .map(com.mojang.authlib.GameProfile::getName)
          .orElse(memberId.toString());
      owner.displayClientMessage(Component.literal(memberName + " (" + memberId + ") "
          + baseCenter.getRole(memberId).name()), false);
    }
    return baseCenter.getMembers().size();
  }

  private static BaseCenterBlockEntity ownedBase(ServerPlayer owner) {
    return BaseSecurity.findBase(owner.level(), owner.blockPosition())
        .filter(base -> base.isOwner(owner)).orElse(null);
  }

  private static void notifyDenied(Level level, net.minecraft.core.BlockPos pos,
      net.minecraft.world.entity.player.Player player, BasePermission permission) {
    if (level == null || level.isClientSide() || player == null) {
      return;
    }
    BaseSecurity.findBase(level, pos).ifPresent(base -> {
      String key;
      if (!base.canUse(player)) {
        key = "message.craftingdeadimmerse.base_no_use_permission";
      } else if (permission != null && !base.hasPermission(player, permission)) {
        key = "message.craftingdeadimmerse.base_no_"
            + permission.name().toLowerCase(java.util.Locale.ROOT) + "_permission";
      } else {
        key = "message.craftingdeadimmerse.base_no_use_permission";
      }
      player.displayClientMessage(Component.translatable(key), true);
    });
  }
}

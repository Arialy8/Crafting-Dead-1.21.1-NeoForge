package com.craftingdead.immerse.world.level.block.entity;

import com.craftingdead.immerse.world.base.BaseRegistry;
import com.craftingdead.immerse.world.base.BasePermission;
import com.craftingdead.immerse.world.base.BaseMemberRole;
import com.craftingdead.immerse.world.item.ImmerseItems;
import com.craftingdead.core.world.item.ModItems;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BaseCenterBlockEntity extends BlockEntity {

  private static final String OWNER_TAG = "Owner";
  private static final String MEMBERS_TAG = "Members";
  private static final String HEALTH_TAG = "RaidHealth";
  private static final String SIEGE_UNTIL_TAG = "SiegeUntil";
  private static final String RECOVERING_UNTIL_TAG = "RecoveringUntil";
  private static final String STORED_LOGS_TAG = "StoredSplitLogs";
  private static final String STORED_STONE_TAG = "StoredStonePebbles";
  private static final String STORED_METAL_TAG = "StoredScrapMetal";
  public static final int PROTECTION_RADIUS = 12;
  public static final int MAX_RAID_HEALTH = 1000;
  public static final int SIEGE_DURATION_TICKS = 20 * 60 * 10;
  public static final int RECOVERY_DURATION_TICKS = 20 * 60 * 5;

  private UUID owner;
  private net.minecraft.world.item.component.ResolvableProfile ownerProfile;

  public net.minecraft.world.item.component.ResolvableProfile getOwnerProfile() {
    return this.ownerProfile;
  }
  private final Set<UUID> members = new HashSet<>();
  private final Map<UUID, EnumSet<BasePermission>> memberPermissions = new HashMap<>();
  private final Set<UUID> administrators = new HashSet<>();
  private int raidHealth = MAX_RAID_HEALTH;
  private long siegeUntil;
  private long recoveringUntil;
  private int storedSplitLogs;
  private int storedStonePebbles;
  private int storedScrapMetal;

  public BaseCenterBlockEntity(BlockPos pos, BlockState blockState) {
    super(ImmerseBlockEntityTypes.BASE_CENTER.get(), pos, blockState);
  }

  public void setOwner(Player player) {
    if (this.owner == null && player != null) {
      this.owner = player.getUUID();
      this.ownerProfile = new net.minecraft.world.item.component.ResolvableProfile(player.getGameProfile());
      BaseRegistry.register(player.level(), this.owner, this.worldPosition);
      this.setChanged();
    }
  }

  public UUID getOwner() {
    return this.owner;
  }

  public void unregister() {
    if (this.level != null && this.owner != null) {
      BaseRegistry.unregister(this.level, this.owner, this.worldPosition);
    }
  }

  public boolean canUse(Player player) {
    if (player == null) {
      return false;
    }
    if (player.getAbilities().instabuild) {
      return true;
    }
    UUID playerId = player.getUUID();
    return playerId.equals(this.owner) || this.members.contains(playerId);
  }

  public int getRaidHealth() {
    this.refreshRaidState();
    return this.raidHealth;
  }

  public boolean isUnderSiege() {
    return this.level != null && this.level.getGameTime() < this.siegeUntil;
  }

  public boolean isBreached() {
    return this.getRaidHealth() <= 0 && this.isUnderSiege();
  }

  public boolean isRecovering() {
    if (this.level == null) {
      return false;
    }
    long now = this.level.getGameTime();
    return now >= this.siegeUntil && now < this.recoveringUntil && this.raidHealth < MAX_RAID_HEALTH;
  }

  public int getRaidSecondsRemaining() {
    if (this.level == null) {
      return 0;
    }
    long until = this.isUnderSiege() ? this.siegeUntil : this.recoveringUntil;
    return (int) Math.max(0L, (until - this.level.getGameTime() + 19L) / 20L);
  }

  public int getStoredSplitLogs() {
    return this.storedSplitLogs;
  }

  public int getStoredStonePebbles() {
    return this.storedStonePebbles;
  }

  public int getStoredScrapMetal() {
    return this.storedScrapMetal;
  }

  public int getStoredCount(net.minecraft.world.item.Item item) {
    if (item == ImmerseItems.ITEMS.get(ImmerseItems.SPLIT_LOG).get()) {
      return this.storedSplitLogs;
    }
    if (item == ImmerseItems.ITEMS.get(ImmerseItems.STONE_PEBBLE).get()) {
      return this.storedStonePebbles;
    }
    return item == ModItems.SCRAP_METAL.get() ? this.storedScrapMetal : 0;
  }

  public int consumeStored(net.minecraft.world.item.Item item, int requested) {
    int consumed = Math.min(Math.max(0, requested), this.getStoredCount(item));
    if (consumed <= 0) {
      return 0;
    }
    if (item == ImmerseItems.ITEMS.get(ImmerseItems.SPLIT_LOG).get()) {
      this.storedSplitLogs -= consumed;
    } else if (item == ImmerseItems.ITEMS.get(ImmerseItems.STONE_PEBBLE).get()) {
      this.storedStonePebbles -= consumed;
    } else if (item == ModItems.SCRAP_METAL.get()) {
      this.storedScrapMetal -= consumed;
    }
    this.setChanged();
    return consumed;
  }

  /** Moves all 1.18 repair resources from the player's inventory into base storage. */
  public boolean importRepairResources(Player player) {
    if (player == null || this.level == null || this.level.isClientSide()) {
      return false;
    }
    int logs = removeAll(player, ImmerseItems.ITEMS.get(ImmerseItems.SPLIT_LOG).get());
    int stone = removeAll(player, ImmerseItems.ITEMS.get(ImmerseItems.STONE_PEBBLE).get());
    int metal = removeAll(player, ModItems.SCRAP_METAL.get());
    if (logs + stone + metal <= 0) {
      return false;
    }
    this.storedSplitLogs += logs;
    this.storedStonePebbles += stone;
    this.storedScrapMetal += metal;
    player.getInventory().setChanged();
    this.setChanged();
    return true;
  }

  /** Exports one visible 64-item storage stack. Type: 0 log, 1 stone, 2 metal. */
  public boolean exportRepairStack(Player player, int type) {
    if (player == null || this.level == null || this.level.isClientSide()) {
      return false;
    }
    int stored = switch (type) {
      case 0 -> this.storedSplitLogs;
      case 1 -> this.storedStonePebbles;
      case 2 -> this.storedScrapMetal;
      default -> 0;
    };
    if (stored <= 0) {
      return false;
    }
    int amount = Math.min(64, stored);
    net.minecraft.world.item.Item item = switch (type) {
      case 0 -> ImmerseItems.ITEMS.get(ImmerseItems.SPLIT_LOG).get();
      case 1 -> ImmerseItems.ITEMS.get(ImmerseItems.STONE_PEBBLE).get();
      default -> ModItems.SCRAP_METAL.get();
    };
    ItemStack stack = new ItemStack(item, amount);
    if (!player.addItem(stack)) {
      player.drop(stack, false);
    }
    if (type == 0) this.storedSplitLogs -= amount;
    if (type == 1) this.storedStonePebbles -= amount;
    if (type == 2) this.storedScrapMetal -= amount;
    this.setChanged();
    return true;
  }

  /** Repairs as much center health as the stored 1.18 base-center recipe resources allow. */
  public boolean repairFromStorage() {
    int missing = MAX_RAID_HEALTH - this.getRaidHealth();
    if (missing <= 0 || this.storedSplitLogs <= 0 || this.storedStonePebbles <= 0
        || this.storedScrapMetal <= 0) {
      return false;
    }
    double fraction = Math.min((double) missing / MAX_RAID_HEALTH,
        Math.min((double) this.storedSplitLogs / 90.0D,
            Math.min((double) this.storedStonePebbles / 200.0D,
                (double) this.storedScrapMetal / 45.0D)));
    int repaired = Math.max(1, Math.min(missing, (int) Math.floor(fraction * MAX_RAID_HEALTH)));
    double actualFraction = (double) repaired / MAX_RAID_HEALTH;
    int logs = Math.max(1, (int) Math.ceil(90.0D * actualFraction));
    int stone = Math.max(1, (int) Math.ceil(200.0D * actualFraction));
    int metal = Math.max(1, (int) Math.ceil(45.0D * actualFraction));
    if (logs > this.storedSplitLogs || stone > this.storedStonePebbles
        || metal > this.storedScrapMetal) {
      return false;
    }
    this.storedSplitLogs -= logs;
    this.storedStonePebbles -= stone;
    this.storedScrapMetal -= metal;
    this.raidHealth = Math.min(MAX_RAID_HEALTH, this.raidHealth + repaired);
    this.setChanged();
    return true;
  }

  private static int removeAll(Player player, net.minecraft.world.item.Item item) {
    int removed = 0;
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (stack.is(item)) {
        removed += stack.getCount();
        stack.setCount(0);
      }
    }
    return removed;
  }

  /** Applies one hostile blast to this base and starts (or extends) its siege window. */
  public boolean damageFromRaid(int amount) {
    if (this.level == null || this.level.isClientSide() || amount <= 0) {
      return false;
    }
    this.refreshRaidState();
    long now = this.level.getGameTime();
    this.raidHealth = Math.max(0, this.raidHealth - amount);
    this.siegeUntil = Math.max(this.siegeUntil, now + SIEGE_DURATION_TICKS);
    this.recoveringUntil = this.siegeUntil + RECOVERY_DURATION_TICKS;
    this.setChanged();
    return this.raidHealth == 0;
  }

  private void refreshRaidState() {
    if (this.level == null) {
      return;
    }
    if (this.recoveringUntil > 0L && this.level.getGameTime() >= this.recoveringUntil) {
      this.raidHealth = MAX_RAID_HEALTH;
      this.siegeUntil = 0L;
      this.recoveringUntil = 0L;
      if (!this.level.isClientSide()) {
        this.setChanged();
      }
    }
  }

  public boolean hasPermission(Player player, BasePermission permission) {
    if (player == null) {
      return false;
    }
    if (player.getAbilities().instabuild || player.getUUID().equals(this.owner)) {
      return true;
    }
    return this.memberPermissions.getOrDefault(player.getUUID(),
        EnumSet.noneOf(BasePermission.class)).contains(permission);
  }

  public Set<UUID> getMembers() {
    return Set.copyOf(this.members);
  }

  public Set<BasePermission> getPermissions(UUID memberId) {
    return Set.copyOf(this.memberPermissions.getOrDefault(memberId,
        EnumSet.noneOf(BasePermission.class)));
  }

  public BaseMemberRole getRole(UUID playerId) {
    if (playerId != null && playerId.equals(this.owner)) {
      return BaseMemberRole.OWNER;
    }
    if (this.administrators.contains(playerId)) {
      return BaseMemberRole.ADMIN;
    }
    return this.getPermissions(playerId).size() > 2
        ? BaseMemberRole.TRUSTED : BaseMemberRole.MEMBER;
  }

  public boolean isOwner(Player player) {
    return player != null && player.getUUID().equals(this.owner);
  }

  public boolean addMember(UUID memberId) {
    if (memberId == null || memberId.equals(this.owner)) {
      return false;
    }
    boolean added = this.members.add(memberId);
    if (added) {
      this.memberPermissions.put(memberId, EnumSet.noneOf(BasePermission.class));
      this.setChanged();
    }
    return added;
  }

  public boolean removeMember(UUID memberId) {
    boolean removed = this.members.remove(memberId);
    if (removed) {
      this.memberPermissions.remove(memberId);
      this.administrators.remove(memberId);
      this.setChanged();
    }
    return removed;
  }

  @Override
  public void setChanged() {
    super.setChanged();
    if (this.level != null && !this.level.isClientSide()) {
      BlockState state = this.getBlockState();
      this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
    }
  }

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    return this.saveWithoutMetadata(registries);
  }

  @Override
  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  public boolean setPermission(UUID memberId, BasePermission permission, boolean allowed) {
    if (!this.members.contains(memberId) || permission == null) {
      return false;
    }
    EnumSet<BasePermission> permissions = this.memberPermissions.computeIfAbsent(memberId,
        ignored -> EnumSet.noneOf(BasePermission.class));
    boolean changed = allowed ? permissions.add(permission) : permissions.remove(permission);
    if (changed) {
      this.setChanged();
    }
    return changed;
  }

  public boolean isAdministrator(UUID memberId) {
    return this.administrators.contains(memberId);
  }

  public boolean setAdministrator(UUID memberId, boolean administrator) {
    if (!this.members.contains(memberId)) {
      return false;
    }
    boolean changed = administrator
        ? this.administrators.add(memberId) : this.administrators.remove(memberId);
    if (administrator) {
      EnumSet<BasePermission> permissions = this.memberPermissions.computeIfAbsent(memberId,
          ignored -> EnumSet.noneOf(BasePermission.class));
      changed |= permissions.addAll(EnumSet.allOf(BasePermission.class));
    }
    if (changed) {
      this.setChanged();
    }
    return changed;
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    if (this.owner != null) {
      tag.putUUID(OWNER_TAG, this.owner);
      // Migrate existing cores and refresh the saved skin while the owner is online.
      if (this.level != null && !this.level.isClientSide) {
        Player onlineOwner = this.level.getPlayerByUUID(this.owner);
        if (onlineOwner != null) {
          this.ownerProfile = new net.minecraft.world.item.component.ResolvableProfile(onlineOwner.getGameProfile());
        }
      }
      if (this.ownerProfile != null) {
        net.minecraft.world.item.component.ResolvableProfile.CODEC
            .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, this.ownerProfile)
            .result().ifPresent(profile -> tag.put("OwnerProfile", profile));
      }
    }
    ListTag membersTag = new ListTag();
    for (UUID member : this.members) {
      CompoundTag memberTag = new CompoundTag();
      memberTag.putUUID("Id", member);
      memberTag.putBoolean("Administrator", this.administrators.contains(member));
      ListTag permissionsTag = new ListTag();
      for (BasePermission permission : this.memberPermissions.getOrDefault(member,
          EnumSet.noneOf(BasePermission.class))) {
        CompoundTag permissionTag = new CompoundTag();
        permissionTag.putString("Name", permission.name());
        permissionsTag.add(permissionTag);
      }
      memberTag.put("Permissions", permissionsTag);
      membersTag.add(memberTag);
    }
    tag.put(MEMBERS_TAG, membersTag);
    tag.putInt(HEALTH_TAG, this.raidHealth);
    tag.putLong(SIEGE_UNTIL_TAG, this.siegeUntil);
    tag.putLong(RECOVERING_UNTIL_TAG, this.recoveringUntil);
    tag.putInt(STORED_LOGS_TAG, this.storedSplitLogs);
    tag.putInt(STORED_STONE_TAG, this.storedStonePebbles);
    tag.putInt(STORED_METAL_TAG, this.storedScrapMetal);
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    this.owner = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
    this.ownerProfile = net.minecraft.world.item.component.ResolvableProfile.CODEC
        .parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.getCompound("OwnerProfile"))
        .result().filter(profile -> this.owner != null && this.owner.equals(profile.gameProfile().getId()))
        .orElse(null);
    this.members.clear();
    this.memberPermissions.clear();
    this.administrators.clear();
    this.raidHealth = tag.contains(HEALTH_TAG, Tag.TAG_INT)
        ? Math.max(0, Math.min(MAX_RAID_HEALTH, tag.getInt(HEALTH_TAG)))
        : MAX_RAID_HEALTH;
    this.siegeUntil = tag.getLong(SIEGE_UNTIL_TAG);
    this.recoveringUntil = tag.getLong(RECOVERING_UNTIL_TAG);
    this.storedSplitLogs = Math.max(0, tag.getInt(STORED_LOGS_TAG));
    this.storedStonePebbles = Math.max(0, tag.getInt(STORED_STONE_TAG));
    this.storedScrapMetal = Math.max(0, tag.getInt(STORED_METAL_TAG));
    ListTag membersTag = tag.getList(MEMBERS_TAG, Tag.TAG_COMPOUND);
    for (int i = 0; i < membersTag.size(); i++) {
      CompoundTag memberTag = membersTag.getCompound(i);
      if (memberTag.hasUUID("Id")) {
        UUID memberId = memberTag.getUUID("Id");
        this.members.add(memberId);
        EnumSet<BasePermission> permissions = EnumSet.noneOf(BasePermission.class);
        if (memberTag.contains("Permissions", Tag.TAG_LIST)) {
          ListTag permissionsTag = memberTag.getList("Permissions", Tag.TAG_COMPOUND);
          for (int j = 0; j < permissionsTag.size(); j++) {
            BasePermission permission = BasePermission.byName(
                permissionsTag.getCompound(j).getString("Name"));
            if (permission != null) {
              permissions.add(permission);
            }
          }
        }
        this.memberPermissions.put(memberId, permissions);
        if (memberTag.getBoolean("Administrator")
            || "ADMIN".equals(memberTag.getString("Role"))) {
          this.administrators.add(memberId);
        }
      }
    }
  }
}

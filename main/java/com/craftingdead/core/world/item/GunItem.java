/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 *
 * This Non-Commercial Software License Agreement (the "Agreement") is made between
 * you (the "Licensee") and NEXUSNODE (BRAD HUNTER). (the "Licensor").
 * By installing or otherwise using Crafting Dead (the "Software"), you agree to be
 * bound by the terms and conditions of this Agreement as may be revised from time
 * to time at Licensor's sole discretion.
 *
 * If you do not agree to the terms and conditions of this Agreement do not download,
 * copy, reproduce or otherwise use any of the source code available online at any time.
 *
 * https://github.com/nexusnode/crafting-dead/blob/1.18.x/LICENSE.txt
 *
 * https://craftingdead.net/terms.php
 */

package com.craftingdead.core.world.item;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.ServerConfig;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.entity.EquipmentSlot;
import com.craftingdead.core.world.item.gun.GunStackDataComponentUtil;
import com.craftingdead.core.world.item.gun.AbstractGun;
import com.craftingdead.core.network.message.play.SyncGunEquipmentSlotMessage;
import com.craftingdead.core.network.NetworkChannel;
import com.craftingdead.core.client.renderer.item.CraftingDeadItemExtensions;
import com.craftingdead.core.client.animation.Animation;
import com.craftingdead.core.world.item.combatslot.CombatSlot;
import com.craftingdead.core.world.item.gun.FireMode;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.GunAnimationEvent;
import com.craftingdead.core.world.item.gun.GunConfiguration;
import com.craftingdead.core.world.item.gun.GunConfigurations;
import com.craftingdead.core.world.item.gun.ammoprovider.AmmoProvider;
import com.craftingdead.core.world.item.gun.ammoprovider.MagazineAmmoProvider;
import com.craftingdead.core.world.item.gun.attachment.Attachment;
import com.craftingdead.core.world.item.gun.attachment.AttachmentLike;
import com.craftingdead.core.world.item.gun.skin.Paint;
import com.craftingdead.core.world.item.gun.magazine.Magazine;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

@RegisterGunColor
public abstract class GunItem extends ProjectileWeaponItem {

  private final ResourceKey<GunConfiguration> configurationKey;

  private final Map<GunAnimationEvent, Function<Gun, Animation>> animations;

  /**
   * A set of magazines that are supported by this gun.
   */
  private final Set<Supplier<? extends Item>> acceptedMagazines;

  /**
   * The default magazine that is supplied with this gun when crafted.
   */
  private final Supplier<? extends Item> defaultMagazine;

  /**
   * A set of attachments that are supported by this gun.
   */
  private final Set<Supplier<? extends Attachment>> acceptedAttachments;

  /**
   * A {@link Predicate} used to determine if the gun can shoot or not.
   */
  private final Predicate<Gun> triggerPredicate;

  private final CombatSlot combatSlot;

  protected GunItem(Builder<?> builder) {
    super(builder.properties);
    this.configurationKey = builder.configurationKey;
    this.animations = builder.animations;
    this.acceptedMagazines = builder.acceptedMagazines;
    this.defaultMagazine = builder.defaultMagazine;
    this.acceptedAttachments = builder.acceptedAttachments;
    this.triggerPredicate = builder.triggerPredicate;
    this.combatSlot = builder.combatSlot;
  }

  @Override
  public @NotNull Predicate<ItemStack> getAllSupportedProjectiles() {
    return itemStack -> this.getAcceptedMagazines()
        .stream()
        .anyMatch(itemStack.getItem()::equals);
  }

  @Override
  protected void shootProjectile(LivingEntity shooter, Projectile projectile, int projectileIndex,
      float velocity, float inaccuracy, float angle, @Nullable LivingEntity target) {
    projectile.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + angle,
        0.0F, velocity, inaccuracy);
  }

  public void initializeClient(Consumer<IClientItemExtensions> consumer) {
    consumer.accept(CraftingDeadItemExtensions.INSTANCE);
  }

  public Map<GunAnimationEvent, Function<Gun, Animation>> getAnimations() {
    return this.animations;
  }

  public AmmoProvider createAmmoProvider() {
    return new MagazineAmmoProvider(ItemStack.EMPTY);
  }

  public Set<Item> getAcceptedMagazines() {
    return this.acceptedMagazines.stream().map(Supplier::get).collect(Collectors.toSet());
  }

  public Item getDefaultMagazine() {
    return this.defaultMagazine.get();
  }

  public Set<Attachment> getAcceptedAttachments() {
    return this.acceptedAttachments.stream().map(Supplier::get).collect(Collectors.toSet());
  }

  public Predicate<Gun> getTriggerPredicate() {
    return this.triggerPredicate;
  }

  public CombatSlot getCombatSlot() {
    return this.combatSlot;
  }

  public GunConfiguration getConfiguration(RegistryAccess registryAccess) {
    GunConfiguration configuration = null;
    if (registryAccess != null) {
      try {
        configuration = registryAccess.registry(GunConfigurations.REGISTRY_KEY)
            .map(registry -> registry.get(this.configurationKey))
            .orElse(null);
      } catch (RuntimeException ignored) {
        // Some client/server runtime contexts do not expose this custom registry through
        // RegistryAccess. Fall through to the Forge registry below.
      }
    }
    if (configuration == null) {
      configuration = this.getConfigurationFromForgeRegistry();
    }
    if (configuration == null) {
      throw new IllegalStateException("Missing gun configuration: "
          + this.configurationKey.location());
    }
    return configuration;
  }

  public GunConfiguration getConfiguration(Registry<GunConfiguration> registry) {
    GunConfiguration configuration = registry == null ? null : registry.get(this.configurationKey);
    return configuration == null ? this.getConfigurationFromForgeRegistry() : configuration;
  }

  @Nullable
  private GunConfiguration getConfigurationFromForgeRegistry() {
    try {
      return GunConfigurations.registry.get(this.configurationKey.location());
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  static void syncHeldGunState(Level level, Player player, InteractionHand gunHand,
      ItemStack gunStack, Gun gun) {
    if (level == null || level.isClientSide() || player == null || gunStack == null
        || gunStack.isEmpty() || gun == null || player.getItemInHand(gunHand) != gunStack) {
      return;
    }

    if (gun instanceof AbstractGun abstractGun) {
      GunStackDataComponentUtil.writeGunState(gunStack, abstractGun.createGunStateTag());
      GunStackDataComponentUtil.writeGunState(gun.getItemStack(), abstractGun.createGunStateTag());
    }

    var slot = gunHand == InteractionHand.OFF_HAND
        ? EquipmentSlot.OFFHAND
        : EquipmentSlot.MAINHAND;
    // The held stack is already in the inventory. State synchronization must never
    // reinsert a captured stack into whichever hotbar slot happens to be selected now.

    try {
      player.getInventory().setChanged();
      player.inventoryMenu.broadcastChanges();
    } catch (RuntimeException ignored) {
    }

    PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
        new SyncGunEquipmentSlotMessage(player.getId(), slot, gun, true));
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack gunStack = player.getItemInHand(hand);
    InteractionHand modifierHand = hand == InteractionHand.MAIN_HAND
        ? InteractionHand.OFF_HAND
        : InteractionHand.MAIN_HAND;
    ItemStack modifierStack = player.getItemInHand(modifierHand);

    if (this.applyModifierToHeldGun(level, player, hand, gunStack, modifierStack)) {
      return InteractionResultHolder.sidedSuccess(gunStack, level.isClientSide());
    }

    return super.use(level, player, hand);
  }

  private boolean applyModifierToHeldGun(Level level, Player player, InteractionHand gunHand,
      ItemStack gunStack, ItemStack modifierStack) {
    if (gunStack.isEmpty() || modifierStack.isEmpty()) {
      return false;
    }

    if (modifierStack.getItem() instanceof AttachmentLike attachmentLike) {
      return this.attachHeldAttachment(level, player, gunHand, gunStack, modifierStack,
          attachmentLike.asAttachment());
    }

    if (Paint.isValid(gunStack, modifierStack)) {
      return this.applyHeldPaint(level, player, gunHand, gunStack, modifierStack);
    }

    return false;
  }

  private boolean attachHeldAttachment(Level level, Player player, InteractionHand gunHand,
      ItemStack gunStack, ItemStack attachmentStack, Attachment attachment) {
    var slot = attachment.getInventorySlot();
    if (slot == null) {
      return false;
    }

    var gun = Gun.get(gunStack).orElse(null);
    if (gun == null || !gun.isAcceptedAttachment(attachmentStack)) {
      return false;
    }

    if (!level.isClientSide()) {
      var attachments = new HashMap<>(gun.getAttachments());
      var previousAttachment = attachments.get(slot);
      if (previousAttachment == attachment) {
        return false;
      }

      attachments.put(slot, attachment);
      gun.setAttachments(attachments);

      if (!player.getAbilities().instabuild) {
        attachmentStack.shrink(1);
      }

      if (previousAttachment != null) {
        var previousStack = new ItemStack(previousAttachment);
        if (!player.getInventory().add(previousStack)) {
          player.drop(previousStack, false);
        }
      }

      syncHeldGunState(level, player, gunHand, gunStack, gun);
      player.getInventory().setChanged();
    }

    return true;
  }

  private boolean applyHeldPaint(Level level, Player player, InteractionHand gunHand,
      ItemStack gunStack, ItemStack paintStack) {
    var gun = Gun.get(gunStack).orElse(null);
    if (gun == null) {
      return false;
    }

    if (!level.isClientSide()) {
      var previousPaint = gun.getPaintStack();
      var newPaint = paintStack.copyWithCount(1);
      if (ItemStack.isSameItemSameComponents(previousPaint, newPaint)) {
        return false;
      }

      gun.setPaintStack(newPaint);

      if (!player.getAbilities().instabuild) {
        paintStack.shrink(1);
      }

      if (!previousPaint.isEmpty()) {
        var returnedPaint = previousPaint.copy();
        if (!player.getInventory().add(returnedPaint)) {
          player.drop(returnedPaint, false);
        }
      }

      syncHeldGunState(level, player, gunHand, gunStack, gun);
      player.getInventory().setChanged();
    }

    return true;
  }

  @Override
  public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
    return true;
  }

  @Override
  public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack,
      boolean slotChanged) {
    return oldStack.getItem() != newStack.getItem();
  }

  @Override
  public void appendHoverText(ItemStack itemStack, TooltipContext context,
      List<Component> lines, TooltipFlag tooltipFlag) {
    super.appendHoverText(itemStack, context, lines, tooltipFlag);

    GunConfiguration configuration = null;
    if (context.registries() != null) {
      try {
        configuration = context.registries()
            .lookupOrThrow(GunConfigurations.REGISTRY_KEY)
            .getOrThrow(this.configurationKey)
            .value();
      } catch (RuntimeException ignored) {
        // In a few client-only UI contexts, such as creative tab tooltip rendering,
        // 1.21.1 may provide a HolderLookup.Provider without this mod's custom
        // gun configuration registry. Falling back keeps tooltip rendering from
        // crashing the game.
      }
    }
    if (configuration == null) {
      try {
        configuration = GunConfigurations.registry.get(this.configurationKey.location());
      } catch (RuntimeException ignored) {
        // Configuration tooltip is optional; gameplay state is still stored on the gun capability.
      }
    }
    if (configuration != null) {
      var damageText =
          Component.literal(String.valueOf(configuration.getDamage()))
              .withStyle(ChatFormatting.RED);
      var headshotDamageText = Component.literal(
          String.valueOf((int) (configuration.getDamage()
              * ServerConfig.instance.headshotBonusDamage.get())))
          .withStyle(ChatFormatting.RED);
      var accuracyText =
          Component.literal((int) (configuration.getAccuracyPercent() * 100.0F) + "%")
              .withStyle(ChatFormatting.RED);
      var rpmText =
          Component.literal(String.valueOf(configuration.getFireRateRPM()))
              .withStyle(ChatFormatting.RED);
      var rangeText =
          Component.literal(configuration.getRange() + " blocks")
              .withStyle(ChatFormatting.RED);
      if (configuration.getRoundsPerShot() > 1) {
        var pelletsText = Component.literal(String.valueOf(configuration.getRoundsPerShot()))
            .withStyle(ChatFormatting.RED);

        lines.add(Component.translatable("gun.pellets_shot")
            .withStyle(ChatFormatting.GRAY)
            .append(pelletsText));
      }

      lines.add(Component.translatable("gun.rpm")
          .withStyle(ChatFormatting.GRAY)
          .append(rpmText));

      lines.add(Component.translatable("gun.damage")
          .withStyle(ChatFormatting.GRAY)
          .append(damageText));
      lines.add(Component.translatable("gun.headshot_damage")
          .withStyle(ChatFormatting.GRAY)
          .append(headshotDamageText));

      lines.add(Component.translatable("gun.accuracy")
          .withStyle(ChatFormatting.GRAY)
          .append(accuracyText));
      lines.add(Component.translatable("gun.range")
          .withStyle(ChatFormatting.GRAY)
          .append(rangeText));
    }

    Gun.get(itemStack).ifPresent(gun -> {
      var ammoCount = Component.literal(String.valueOf(gun.getAmmoProvider().getMagazine()
          .map(Magazine::getSize)
          .orElse(0))).withStyle(ChatFormatting.RED);

      lines.add(Component.translatable("gun.ammo_amount")
          .withStyle(ChatFormatting.GRAY)
          .append(ammoCount));

      for (var attachment : gun.getAttachments().values()) {
        Component attachmentName = attachment.getDescription()
            .plainCopy()
            .withStyle(ChatFormatting.RED);
        lines.add(Component.translatable("gun.attachment")
            .withStyle(ChatFormatting.GRAY)
            .append(attachmentName));
      }
    });
  }

  @Override
  public boolean isEnchantable(ItemStack stack) {
    return true;
  }

  @Override
  public int getEnchantmentValue() {
    return 1;
  }

  @Override
  public int getDefaultProjectileRange() {
    return 0;
  }

  public static class Builder<SELF extends Builder<SELF>> {

    private final Function<SELF, GunItem> factory;

    private final ResourceKey<GunConfiguration> configurationKey;

    private final Properties properties = new Properties().stacksTo(1);

    private final Set<FireMode> fireModes = EnumSet.noneOf(FireMode.class);

    private final Map<GunAnimationEvent, Function<Gun, Animation>> animations =
        new EnumMap<>(GunAnimationEvent.class);

    private final Set<Supplier<? extends Item>> acceptedMagazines = new HashSet<>();

    private Supplier<? extends Item> defaultMagazine;

    private final Set<Supplier<? extends Attachment>> acceptedAttachments = new HashSet<>();

    private Predicate<Gun> triggerPredicate = gun -> true;

    private CombatSlot combatSlot = CombatSlot.PRIMARY;

    public Builder(Function<SELF, GunItem> factory, ResourceKey<GunConfiguration> configurationKey) {
      this.factory = factory;
      this.configurationKey = configurationKey;
    }

    public SELF properties(Consumer<Properties> consumer) {
      consumer.accept(this.properties);
      return this.self();
    }

    public SELF addFireMode(FireMode fireMode) {
      this.fireModes.add(fireMode);
      return this.self();
    }

    public SELF putAnimation(GunAnimationEvent event, Supplier<Animation> animation) {
      return this.putAnimation(event, __ -> animation.get());
    }

    public SELF putReloadAnimation(IntFunction<Animation> animation) {
      return this.putAnimation(GunAnimationEvent.RELOAD,
          gun -> animation.apply(gun.getReloadDurationTicks()));
    }

    public SELF putAnimation(GunAnimationEvent event, Function<Gun, Animation> animation) {
      this.animations.put(event, animation);
      return this.self();
    }

    public SELF addAcceptedMagazine(Supplier<? extends Item> acceptedMagazine) {
      this.acceptedMagazines.add(acceptedMagazine);
      return this.self();
    }

    public SELF setDefaultMagazine(Supplier<? extends Item> defaultMagazine) {
      if (this.defaultMagazine != null) {
        throw new IllegalArgumentException("Default magazine already set");
      }
      this.defaultMagazine = defaultMagazine;
      return this.addAcceptedMagazine(defaultMagazine);
    }

    public SELF addAcceptedAttachment(Supplier<? extends Attachment> acceptedAttachment) {
      this.acceptedAttachments.add(acceptedAttachment);
      return this.self();
    }

    public SELF setTriggerPredicate(Predicate<Gun> triggerPredicate) {
      this.triggerPredicate = triggerPredicate;
      return this.self();
    }

    public SELF setCombatSlot(CombatSlot combatSlot) {
      this.combatSlot = combatSlot;
      return this.self();
    }

    public GunItem build() {
      return this.factory.apply(this.self());
    }

    @SuppressWarnings("unchecked")
    protected final SELF self() {
      return (SELF) this;
    }
  }
}

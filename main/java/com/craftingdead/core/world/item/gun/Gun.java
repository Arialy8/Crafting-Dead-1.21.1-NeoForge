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

package com.craftingdead.core.world.item.gun;

import com.craftingdead.core.util.RegistryNameUtil;
import com.craftingdead.core.world.item.GunItem;
import java.util.Arrays;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.network.Synched;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.craftingdead.core.world.inventory.GunCraftSlotType;
import com.craftingdead.core.world.item.combatslot.CombatSlotProvider;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.craftingdead.core.world.item.gun.aimable.AimableGun;
import com.craftingdead.core.world.item.gun.ammoprovider.AmmoProvider;
import com.craftingdead.core.world.item.gun.ammoprovider.MagazineAmmoProvider;
import com.craftingdead.core.world.item.gun.attachment.Attachment;
import com.craftingdead.core.world.item.gun.attachment.Attachment.MultiplierType;
import com.craftingdead.core.world.item.gun.minigun.Minigun;
import com.craftingdead.core.world.item.gun.minigun.MinigunItem;
import com.craftingdead.core.world.item.gun.skin.Skin;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

public interface Gun extends Equipment, CombatSlotProvider, Synched {

  /**
   * NeoForge 1.21.1 no longer gives this port the old Forge ItemStack capability instance.
   * The previous conversion recreated a new Gun object on every get() call. That split input,
   * ticking, networking and rendering across unrelated instances, so ADS, firing schedules and
   * reload animations were immediately lost. Weak keys preserve one Gun per live ItemStack while
   * still allowing discarded stacks to be garbage-collected.
   */
  Map<ItemStack, Gun> INSTANCE_CACHE =
      Collections.synchronizedMap(new WeakHashMap<>());

  static Optional<Gun> get(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return Optional.empty();
    }
    Item item = stack.getItem();
    if (!(item instanceof GunItem gunItem)) {
      return Optional.empty();
    }

    Gun cached = INSTANCE_CACHE.get(stack);
    if (cached != null) {
      return Optional.of(cached);
    }

    synchronized (INSTANCE_CACHE) {
      cached = INSTANCE_CACHE.get(stack);
      if (cached != null) {
        return Optional.of(cached);
      }

      TypedGun gun;
      if (gunItem instanceof MinigunItem) {
        gun = new Minigun(stack, gunItem);
      } else {
        gun = new AimableGun(stack, gunItem);
      }
      gun.deserializeNBT(null);

      // Pre-load the default magazine only when this stack has never carried persisted gun state.
      if (!GunStackDataComponentUtil.hasGunState(stack)
          && gun.getAmmoProvider() instanceof MagazineAmmoProvider magProvider
          && magProvider.getMagazineStack().isEmpty()) {
        ItemStack defaultMag = gun.getDefaultMagazineStack();
        if (!defaultMag.isEmpty()) {
          magProvider.setMagazineStack(defaultMag.copy());
        }
      }

      INSTANCE_CACHE.put(stack, gun);
      return Optional.of(gun);
    }
  }

  /** Remove a stale cached instance after code intentionally replaces/rebuilds a stack. */
  static void invalidate(ItemStack stack) {
    if (stack != null) {
      INSTANCE_CACHE.remove(stack);
    }
  }

  @Override
  default boolean isValidForSlot(Slot slot) {
    return slot == Slot.GUN;
  }

  /**
   * Ticked when held by the passed in {@link LivingExtension}.
   * 
   * @param living - the entity holding the gun
   */
  void tick(LivingExtension<?, ?> living);

  /**
   * Reset any actions currently being performed (e.g. secondary action or trigger).
   * 
   * @param living - the entity holding the gun
   */
  void reset(LivingExtension<?, ?> living);

  /**
   * Set the trigger pressed state.
   * 
   * @param living - the entity holding the gun
   * @param triggerPressed - whether the trigger is pressed
   * @param sendUpdate - send an update to the opposing logical side (client/server)
   */
  void setTriggerPressed(LivingExtension<?, ?> living, boolean triggerPressed, boolean sendUpdate);

  /**
   * Handle one player-driven shot request sent from the client. 1.21.1 can recreate item
   * capabilities while firing, so player auto-fire is timed on the client and mirrored to the
   * server one shot at a time. Implementations that do not need this can fall back to the old
   * trigger behaviour.
   *
   * @param living - the entity holding the gun
   */
  default void handleClientShotRequest(LivingExtension<?, ?> living) {
    this.setTriggerPressed(living, true, true);
  }

  /**
   * Prepare this gun for an authoritative state sync. Implementations may refresh their
   * AmmoProvider from the real held ItemStack before a SyncGunEquipmentSlotMessage is encoded.
   *
   * @param living - the entity holding the gun
   */
  default void prepareForMainHandStateSync(LivingExtension<?, ?> living) {
  }

  /**
   * Handle a server-side rejection of a client shot request. Implementations can clear any
   * client-only ghost ammo that was created by an animation-only reload that the server never
   * accepted.
   *
   * @param living - the entity holding the gun
   */
  default void handleClientShotRejected(LivingExtension<?, ?> living) {
    this.setTriggerPressed(living, false, false);
  }

  /**
   * Set the trigger pressed state.
   *
   * @param living - the entity holding the gun
   * @param triggerPressed - whether the trigger is pressed
   * @param sendUpdate - send an update to the opposing logical side (client/server)
   * @param accuracy - can be used to adjust the accuracy of NPC's
   */
  void setNPCTriggerPressed(LivingExtension<?, ?> living, boolean triggerPressed, boolean sendUpdate, float accuracy);

  /**
   * Whether the trigger is currently pressed.
   * 
   * @return <code>true</code> if pressed, <code>false</code> otherwise.
   */
  boolean isTriggerPressed();

  /**
   * Handles pending hit messages sent by the client.
   * 
   * @param player - the player who sent the message
   * @param hitLiving - the entity hit
   * @param pendingHit - hit data
   */
  void validatePendingHit(PlayerExtension<ServerPlayer> player,
      LivingExtension<?, ?> hitLiving, PendingHit pendingHit);

  /**
   * Get current accuracy percentage of the gun.
   * 
   * @param living - the entity holding the gun
   * @return the accuracy percentage
   */
  float getAccuracy(LivingExtension<?, ?> living);

  /**
   * Get current attachments on the gun.
   * 
   * @return the attachments
   */
  Map<GunCraftSlotType, Attachment> getAttachments();

  /**
   * Get the combined attachment multiplier of the specified {@link MultiplierType}.
   * 
   * @param multiplierType - the type of multiplier
   * @return the combined multiplier
   */
  default float getAttachmentMultiplier(Attachment.MultiplierType multiplierType) {
    return this.getAttachments().values().stream()
        .map(attachment -> attachment.getMultiplier(multiplierType))
        .reduce(1.0F, (x, y) -> x * y);
  }

  /**
   * Set the attachments on the gun.
   * 
   * @param attachments - the attachments to set
   */
  void setAttachments(Map<GunCraftSlotType, Attachment> attachments);

  /**
   * If the gun has an iron sight (no over-barrel attachments).
   * 
   * @return <code>true</code> if the gun has an iron sight, <code>false</code> otherwise
   */
  default boolean hasIronSight() {
    return !this.getAttachments().containsKey(GunCraftSlotType.OVERBARREL_ATTACHMENT);
  }

  /**
   * Get the {@link ItemStack} used to paint the gun with.
   * 
   * @return the {@link ItemStack}
   */
  ItemStack getPaintStack();

  /**
   * Paint the gun with the specified {@link ItemStack}.
   * 
   * @param paintStack - the paint {@link ItemStack}
   */
  void setPaintStack(ItemStack paintStack);

  @Nullable
  Skin getSkin();

  void setSkin(@Nullable Holder<Skin> skin);

  /**
   * Get an optional skin to be used for this gun.
   * 
   * @return the optional skin name
   */
  default Optional<ResourceLocation> getSkinName() {
    return this.getPaintStack().isEmpty()
        ? Optional.empty()
        : Optional.of(RegistryNameUtil.item(this.getPaintStack().getItem()));
  }

  /**
   * Check if the specified {@link ItemStack} is an acceptable paint or attachment for this gun.
   * 
   * @param itemStack - the {@link ItemStack} to check
   * @return <code>true</code> if accepted, <code>false</code> otherwise
   */
  boolean isAcceptedAttachment(ItemStack itemStack);

  /**
   * Get the currently selected {@link FireMode}.
   * 
   * @return the {@link FireMode}
   */
  FireMode getFireMode();

  /**
   * Set the current {@link FireMode}.
   * 
   * @param living - the entity holding the gun
   * @param fireMode - the selected {@link FireMode}
   * @param sendUpdate - send an update to the opposing logical side (client/server)
   */
  void setFireMode(LivingExtension<?, ?> living, FireMode fireMode, boolean sendUpdate);

  /**
   * Toggle the next {@link FireMode}.
   * 
   * @param living - the entity holding the gun
   * @param sendUpdate - send an update to the opposing logical side (client/server)
   */
  void toggleFireMode(LivingExtension<?, ?> living, boolean sendUpdate);

  /**
   * If the secondary action of this gun is being performed (e.g. aiming).
   * 
   * @return <code>true</code> if being performed, otherwise <code>false</code>
   */
  boolean isPerformingSecondaryAction();

  /**
   * Set the secondary action performing state.
   * 
   * @param living - the entity holding the gun
   * @param performingAction - whether to perform the action or not
   * @param sendUpdate - send an update to the opposing logical side (client/server)
   */
  void setPerformingSecondaryAction(LivingExtension<?, ?> living, boolean performingAction,
      boolean sendUpdate);

  /**
   * Get the type of trigger for the secondary action.
   * 
   * @return the {@link SecondaryActionTrigger}
   */
  SecondaryActionTrigger getSecondaryActionTrigger();

  /**
   * Get the sound to be played when reloading this gun.
   * 
   * @return an optional {@link SoundEvent}
   */
  Optional<SoundEvent> getReloadSound();

  /**
   * Get the reload duration in ticks of this gun.
   * 
   * @return the duration in ticks
   */
  int getReloadDurationTicks();

  /**
   * Get the {@link GunClient} associated with this gun. Will be null on dedicated server. <br>
   * <b>Always check <u>logical</u> side before accessing.
   * 
   * @return
   */
  GunClient getClient();

  void startClientReload();

  void startClientReload(boolean removingMagazine);

  void cancelClientReload();

  AmmoProvider getAmmoProvider();

  void setAmmoProvider(AmmoProvider ammoProvider);

  Set<? extends Item> getAcceptedMagazines();

  ItemStack getDefaultMagazineStack();

  ItemStack getItemStack();

  enum SecondaryActionTrigger implements StringRepresentable {
    HOLD("hold"), TOGGLE("toggle");

    private static final Map<String, SecondaryActionTrigger> BY_NAME = Arrays.stream(values())
        .collect(Collectors.toMap(SecondaryActionTrigger::getSerializedName, Function.identity()));
    public static final Codec<SecondaryActionTrigger> CODEC =
        StringRepresentable.fromEnum(SecondaryActionTrigger::values);

    private final String name;

    SecondaryActionTrigger(String name) {
      this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
      return name;
    }

    public static SecondaryActionTrigger byName(String name) {
      return BY_NAME.get(name);
    }
  }
}

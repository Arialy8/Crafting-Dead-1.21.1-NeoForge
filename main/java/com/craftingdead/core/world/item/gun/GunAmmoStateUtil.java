/*
 * Crafting Dead 1.21.1 migration helper.
 */
package com.craftingdead.core.world.item.gun;

import com.craftingdead.core.util.ItemStackNbtUtil;
import com.craftingdead.core.world.item.MagazineItem;
import com.craftingdead.core.world.item.gun.ammoprovider.AmmoProvider;
import com.craftingdead.core.world.item.gun.magazine.Magazine;
import com.craftingdead.core.world.item.gun.magazine.MagazineStackDataComponentUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime fallback for the 1.21.1 port while item capability sync is being stabilized.
 *
 * <p>The old code trusted the gun capability's AmmoProvider. In 1.21.1 the provider may be
 * recreated from ItemStack components at inconvenient times, so we mirror the loaded magazine
 * and its current ammo directly into the gun's CUSTOM_DATA state as a secondary source of truth.</p>
 */
public final class GunAmmoStateUtil {

  private static final Map<String, ItemStack> SYNCED_LOADED_MAGAZINES = new ConcurrentHashMap<>();

  /**
   * Client-only HUD bridge for the tiny gap between reload animation completion and the server's
   * authoritative SyncGunSlot. These previews are deliberately not used by shooting logic: they
   * only stop the HUD from flashing "empty magazine" on a reload that the server has already
   * accepted but has not synced back to the client yet.
   */
  private static final Map<String, TimedMagazinePreview> CLIENT_RELOAD_PREVIEWS = new ConcurrentHashMap<>();
  private static final long CLIENT_RELOAD_PREVIEW_TTL_MS = 1500L;

  private record TimedMagazinePreview(ItemStack magazineStack, long expiresAtMs) {}

  /**
   * Server-side authoritative fallback keyed by entity + gun item + gun instance id. This is not a
   * replacement for ItemStack CUSTOM_DATA; it is a short-lived recovery bridge for 1.21.1 hotbar
   * switches where a freshly-created capability has not restored its magazine from the stack yet.
   */
  private static final Map<String, ItemStack> AUTHORITATIVE_LOADED_MAGAZINES = new ConcurrentHashMap<>();
  private static final Map<String, ItemStack> AUTHORITATIVE_LOADED_MAGAZINES_BY_ITEM = new ConcurrentHashMap<>();

  public static final String GUN_INSTANCE_ID = "gunInstanceId";
  public static final String LOADED_AMMO = "loadedAmmo";
  public static final String LOADED_MAGAZINE_STACK = "loadedMagazineStack";

  private GunAmmoStateUtil() {}

  public static void mirrorLoadedState(ItemStack gunStack, CompoundTag gunState,
      AmmoProvider ammoProvider) {
    if (gunState == null) {
      return;
    }

    ItemStack magazineStack = ammoProvider == null ? ItemStack.EMPTY : ammoProvider.getMagazineStack();
    if (magazineStack == null || magazineStack.isEmpty()) {
      magazineStack = getLoadedMagazineStack(gunStack);
    }

    if (!magazineStack.isEmpty()) {
      writeLoadedMagazineToState(gunState, magazineStack);
    } else {
      int loadedAmmo = getLoadedAmmo(gunStack);
      if (loadedAmmo > 0) {
        gunState.putInt(LOADED_AMMO, loadedAmmo);
      }
    }
  }

  public static void writeLoadedMagazine(ItemStack gunStack, ItemStack magazineStack) {
    if (gunStack == null || gunStack.isEmpty()) {
      return;
    }

    CompoundTag gunState = GunStackDataComponentUtil.readGunState(gunStack);
    ensureGunInstanceId(gunStack, gunState);
    writeLoadedMagazineToState(gunState, magazineStack);
    GunStackDataComponentUtil.writeGunState(gunStack, gunState);
  }

  public static void writeLoadedMagazineToState(CompoundTag gunState, ItemStack magazineStack) {
    if (gunState == null || magazineStack == null || magazineStack.isEmpty()) {
      return;
    }

    magazineStack = magazineStack.copy();
    magazineStack.setCount(1);

    int ammo = getMagazineSize(magazineStack);
    // Do not auto-refill here. This method is also called after each shot while
    // persisting the loaded magazine state. Refilling when ammo reaches 0 caused
    // providerAmmo=0 to immediately become loadedAmmo=maxSize, giving infinite ammo.
    if (ammo < 0) {
      ammo = 0;
    }

    gunState.putInt(LOADED_AMMO, ammo);
    gunState.put(LOADED_MAGAZINE_STACK, ItemStackNbtUtil.save(magazineStack));

    // Also mirror into the legacy AmmoProvider shape so existing encode/decode paths can recover.
    gunState.putString(GunStackDataComponentUtil.AMMO_PROVIDER_TYPE, "craftingdead:magazine");
    CompoundTag ammoProviderTag = gunState.getCompound(GunStackDataComponentUtil.AMMO_PROVIDER);
    ammoProviderTag.put("magazineStack", ItemStackNbtUtil.save(magazineStack));
    gunState.put(GunStackDataComponentUtil.AMMO_PROVIDER, ammoProviderTag);
  }

  private static String slotPrefix(int entityId, EquipmentSlot slot) {
    return entityId + ":" + (slot == null ? EquipmentSlot.MAINHAND : slot).name() + ":";
  }

  private static String itemKey(ItemStack gunStack) {
    if (gunStack == null || gunStack.isEmpty()) {
      return "empty";
    }
    try {
      return BuiltInRegistries.ITEM.getKey(gunStack.getItem()).toString();
    } catch (RuntimeException e) {
      return String.valueOf(gunStack.getItem());
    }
  }

  private static String ensureGunInstanceId(ItemStack gunStack, CompoundTag gunState) {
    if (gunStack == null || gunStack.isEmpty()) {
      return "empty";
    }
    if (gunState == null) {
      gunState = GunStackDataComponentUtil.readGunState(gunStack);
    }
    String instanceId = gunState.getString(GUN_INSTANCE_ID);
    if (instanceId == null || instanceId.isBlank()) {
      instanceId = UUID.randomUUID().toString();
      gunState.putString(GUN_INSTANCE_ID, instanceId);
      GunStackDataComponentUtil.writeGunState(gunStack, gunState);
    }
    return instanceId;
  }

  public static String getOrCreateGunInstanceId(ItemStack gunStack) {
    if (gunStack == null || gunStack.isEmpty()) {
      return "empty";
    }
    return ensureGunInstanceId(gunStack, GunStackDataComponentUtil.readGunState(gunStack));
  }

  private static String authoritativeKey(int entityId, ItemStack gunStack) {
    return entityId + ":" + itemKey(gunStack) + ":" + getOrCreateGunInstanceId(gunStack);
  }

  private static String authoritativeItemKey(int entityId, ItemStack gunStack) {
    return entityId + ":" + itemKey(gunStack);
  }

  public static void cacheAuthoritativeLoadedMagazine(int entityId, EquipmentSlot slot,
      ItemStack gunStack, ItemStack magazineStack) {
    if (gunStack == null || gunStack.isEmpty()) {
      return;
    }
    if (magazineStack == null || magazineStack.isEmpty()) {
      return;
    }
    ItemStack copy = magazineStack.copy();
    copy.setCount(1);
    AUTHORITATIVE_LOADED_MAGAZINES.put(authoritativeKey(entityId, gunStack), copy);
    AUTHORITATIVE_LOADED_MAGAZINES_BY_ITEM.put(authoritativeItemKey(entityId, gunStack), copy.copy());
  }

  public static void cacheAuthoritativeLoadedMagazine(int entityId, EquipmentSlot slot,
      ItemStack gunStack, AmmoProvider ammoProvider) {
    ItemStack magazineStack = ammoProvider == null ? ItemStack.EMPTY : ammoProvider.getMagazineStack();
    if ((magazineStack == null || magazineStack.isEmpty()) && gunStack != null && !gunStack.isEmpty()) {
      magazineStack = getLoadedMagazineStack(gunStack);
    }
    cacheAuthoritativeLoadedMagazine(entityId, slot, gunStack, magazineStack);
  }

  public static ItemStack getAuthoritativeLoadedMagazineStack(int entityId, EquipmentSlot slot,
      ItemStack gunStack) {
    if (gunStack == null || gunStack.isEmpty()) {
      return ItemStack.EMPTY;
    }
    ItemStack stack = AUTHORITATIVE_LOADED_MAGAZINES.get(authoritativeKey(entityId, gunStack));
    if (stack == null || stack.isEmpty()) {
      stack = AUTHORITATIVE_LOADED_MAGAZINES_BY_ITEM.get(authoritativeItemKey(entityId, gunStack));
    }
    return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
  }

  private static String cacheKey(int entityId, EquipmentSlot slot, ItemStack gunStack) {
    return slotPrefix(entityId, slot) + itemKey(gunStack);
  }

  public static void clearSyncedLoadedMagazine(int entityId, EquipmentSlot slot) {
    String prefix = slotPrefix(entityId, slot);
    SYNCED_LOADED_MAGAZINES.keySet().removeIf(key -> key.startsWith(prefix));
    CLIENT_RELOAD_PREVIEWS.keySet().removeIf(key -> key.startsWith(prefix));
  }

  public static void clearSyncedLoadedMagazine(int entityId, EquipmentSlot slot, ItemStack gunStack) {
    if (gunStack == null || gunStack.isEmpty()) {
      clearSyncedLoadedMagazine(entityId, slot);
      return;
    }
    String key = cacheKey(entityId, slot, gunStack);
    SYNCED_LOADED_MAGAZINES.remove(key);
    CLIENT_RELOAD_PREVIEWS.remove(key);
  }

  public static void cacheSyncedLoadedMagazine(int entityId, EquipmentSlot slot, ItemStack gunStack,
      ItemStack magazineStack) {
    if (gunStack == null || gunStack.isEmpty()) {
      clearSyncedLoadedMagazine(entityId, slot);
      return;
    }

    String key = cacheKey(entityId, slot, gunStack);
    if (magazineStack == null || magazineStack.isEmpty()) {
      SYNCED_LOADED_MAGAZINES.remove(key);
      return;
    }
    ItemStack copy = magazineStack.copy();
    copy.setCount(1);
    SYNCED_LOADED_MAGAZINES.put(key, copy);
  }

  public static void cacheSyncedLoadedMagazine(int entityId, EquipmentSlot slot, ItemStack gunStack,
      AmmoProvider ammoProvider) {
    ItemStack magazineStack = ammoProvider == null ? ItemStack.EMPTY : ammoProvider.getMagazineStack();
    if ((magazineStack == null || magazineStack.isEmpty()) && gunStack != null && !gunStack.isEmpty()) {
      magazineStack = getLoadedMagazineStack(gunStack);
    }
    cacheSyncedLoadedMagazine(entityId, slot, gunStack, magazineStack);
  }


  public static void cacheClientReloadPreview(int entityId, EquipmentSlot slot, ItemStack gunStack,
      ItemStack magazineStack) {
    if (gunStack == null || gunStack.isEmpty() || magazineStack == null || magazineStack.isEmpty()) {
      return;
    }
    ItemStack copy = magazineStack.copy();
    copy.setCount(1);
    CLIENT_RELOAD_PREVIEWS.put(cacheKey(entityId, slot, gunStack),
        new TimedMagazinePreview(copy, System.currentTimeMillis() + CLIENT_RELOAD_PREVIEW_TTL_MS));
  }

  public static void clearClientReloadPreview(int entityId, EquipmentSlot slot, ItemStack gunStack) {
    if (gunStack == null || gunStack.isEmpty()) {
      String prefix = slotPrefix(entityId, slot);
      CLIENT_RELOAD_PREVIEWS.keySet().removeIf(key -> key.startsWith(prefix));
      return;
    }
    CLIENT_RELOAD_PREVIEWS.remove(cacheKey(entityId, slot, gunStack));
  }

  public static int getClientReloadPreviewAmmo(int entityId, EquipmentSlot slot, ItemStack gunStack) {
    TimedMagazinePreview preview = CLIENT_RELOAD_PREVIEWS.get(cacheKey(entityId, slot, gunStack));
    if (preview == null) {
      return 0;
    }
    if (preview.expiresAtMs() < System.currentTimeMillis()) {
      CLIENT_RELOAD_PREVIEWS.remove(cacheKey(entityId, slot, gunStack), preview);
      return 0;
    }
    return getMagazineSize(preview.magazineStack());
  }

  public static ItemStack getCachedLoadedMagazineStack(int entityId, EquipmentSlot slot,
      ItemStack gunStack) {
    ItemStack stack = SYNCED_LOADED_MAGAZINES.get(cacheKey(entityId, slot, gunStack));
    return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
  }

  public static int getCachedLoadedAmmo(int entityId, EquipmentSlot slot, ItemStack gunStack) {
    return getMagazineSize(getCachedLoadedMagazineStack(entityId, slot, gunStack));
  }

  @Deprecated
  public static ItemStack getCachedLoadedMagazineStack(int entityId, EquipmentSlot slot) {
    return ItemStack.EMPTY;
  }

  @Deprecated
  public static int getCachedLoadedAmmo(int entityId, EquipmentSlot slot) {
    return 0;
  }

  public static ItemStack getLoadedMagazineStack(ItemStack gunStack) {
    CompoundTag gunState = GunStackDataComponentUtil.readGunState(gunStack);
    if (gunState.contains(LOADED_MAGAZINE_STACK, Tag.TAG_COMPOUND)) {
      ItemStack stack = ItemStackNbtUtil.parseOptional(gunState.getCompound(LOADED_MAGAZINE_STACK));
      if (!stack.isEmpty()) {
        int ammo = gunState.contains(LOADED_AMMO, Tag.TAG_INT)
            ? Math.max(0, gunState.getInt(LOADED_AMMO))
            : getMagazineSize(stack);
        if (stack.getItem() instanceof MagazineItem) {
          MagazineStackDataComponentUtil.setSize(stack, ammo);
        }
        return stack;
      }
    }

    if (gunState.contains(GunStackDataComponentUtil.AMMO_PROVIDER, Tag.TAG_COMPOUND)) {
      CompoundTag ammoProviderTag = gunState.getCompound(GunStackDataComponentUtil.AMMO_PROVIDER);
      if (ammoProviderTag.contains("magazineStack", Tag.TAG_COMPOUND)) {
        return ItemStackNbtUtil.parseOptional(ammoProviderTag.getCompound("magazineStack"));
      }
    }

    return ItemStack.EMPTY;
  }

  public static int getLoadedAmmo(ItemStack gunStack) {
    CompoundTag gunState = GunStackDataComponentUtil.readGunState(gunStack);
    if (gunState.contains(LOADED_AMMO, Tag.TAG_INT)) {
      return Math.max(0, gunState.getInt(LOADED_AMMO));
    }

    ItemStack magazineStack = getLoadedMagazineStack(gunStack);
    return getMagazineSize(magazineStack);
  }

  public static int decrementLoadedAmmo(ItemStack gunStack) {
    int ammo = getLoadedAmmo(gunStack);
    if (ammo <= 0) {
      return -1;
    }

    int remaining = ammo - 1;
    CompoundTag gunState = GunStackDataComponentUtil.readGunState(gunStack);
    ItemStack magazineStack = getLoadedMagazineStack(gunStack);
    if (!magazineStack.isEmpty() && magazineStack.getItem() instanceof MagazineItem) {
      MagazineStackDataComponentUtil.setSize(magazineStack, remaining);
      gunState.put(LOADED_MAGAZINE_STACK, ItemStackNbtUtil.save(magazineStack));
      CompoundTag ammoProviderTag = gunState.getCompound(GunStackDataComponentUtil.AMMO_PROVIDER);
      ammoProviderTag.put("magazineStack", ItemStackNbtUtil.save(magazineStack));
      gunState.put(GunStackDataComponentUtil.AMMO_PROVIDER, ammoProviderTag);
    }
    gunState.putInt(LOADED_AMMO, remaining);
    GunStackDataComponentUtil.writeGunState(gunStack, gunState);
    return remaining;
  }

  public static void clearLoadedMagazine(ItemStack gunStack) {
    if (gunStack == null || gunStack.isEmpty()) {
      return;
    }
    CompoundTag gunState = GunStackDataComponentUtil.readGunState(gunStack);
    gunState.remove(LOADED_AMMO);
    gunState.remove(LOADED_MAGAZINE_STACK);
    if (gunState.contains(GunStackDataComponentUtil.AMMO_PROVIDER, Tag.TAG_COMPOUND)) {
      CompoundTag ammoProviderTag = gunState.getCompound(GunStackDataComponentUtil.AMMO_PROVIDER);
      ammoProviderTag.remove("magazineStack");
      gunState.put(GunStackDataComponentUtil.AMMO_PROVIDER, ammoProviderTag);
    }
    GunStackDataComponentUtil.writeGunState(gunStack, gunState);
  }

  public static int getMagazineSize(ItemStack magazineStack) {
    if (magazineStack == null || magazineStack.isEmpty()) {
      return 0;
    }
    if (!(magazineStack.getItem() instanceof MagazineItem magazineItem)) {
      return 0;
    }
    return Magazine.get(magazineStack)
        .map(Magazine::getSize)
        .orElseGet(() -> MagazineStackDataComponentUtil.getSize(magazineStack, magazineItem.getSize()));
  }
}

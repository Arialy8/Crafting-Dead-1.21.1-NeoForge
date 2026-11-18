package com.craftingdead.survival.world.entity;

import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.item.*;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.ammoprovider.RefillableAmmoProvider;
import com.craftingdead.survival.world.BloodMoonManager;
import com.craftingdead.survival.world.ZombieBalance;
import com.craftingdead.survival.world.ZombieProgression;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.EnumMap;

public final class ZombieLoadouts {
  private static final EnumMap<Equipment.Slot, List<Item>> POOLS = new EnumMap<>(Equipment.Slot.class);
  public static String type(SurvivalZombie zombie) {
    return BuiltInRegistries.ENTITY_TYPE.getKey(zombie.getType()).getPath();
  }
  public static ItemStack item(String id) {
    return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("craftingdead", id)));
  }
  private static ItemStack randomEquipment(SurvivalZombie zombie, Equipment.Slot slot) {
    List<Item> items = POOLS.computeIfAbsent(slot, key -> BuiltInRegistries.ITEM.stream()
        .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("craftingdead"))
        .filter(item -> Equipment.get(item.getDefaultInstance()).map(e -> e.isValidForSlot(key)).orElse(false))
        .toList());
    return items.isEmpty() ? ItemStack.EMPTY : new ItemStack(items.get(zombie.getRandom().nextInt(items.size())));
  }
  public static void equip(SurvivalZombie zombie, boolean preserveHealth) {
    var extension = LivingExtension.get(zombie);
    if (extension == null) return; // Retry after attachment initialization.
    float oldHealth = zombie.getHealth();
    var weaponClass = ZombieBalance.weaponClass(type(zombie));
    for (EquipmentSlot slot : EquipmentSlot.values()) zombie.setItemSlot(slot, ItemStack.EMPTY);
    for (Equipment.Slot slot : Equipment.Slot.values()) extension.setItemInSlot(slot, ItemStack.EMPTY);
    boolean noArmor = zombie.isBaby() || weaponClass == ZombieBalance.WeaponClass.ROCKET;
    long time = zombie.level().getDayTime();
    var difficulty = zombie.level().getDifficulty();
    boolean bloodLoadout = zombie.level() instanceof ServerLevel server && BloodMoonManager.isBloodMoonDay(server);
    if (!noArmor) {
      var slots = List.of(Equipment.Slot.CLOTHING, Equipment.Slot.HAT,
          Equipment.Slot.VEST, Equipment.Slot.BACKPACK);
      boolean full = zombie.getRandom().nextInt(100) < (bloodLoadout
          ? switch (difficulty) { case HARD -> 100; case NORMAL -> 50; case EASY -> 20; default -> 0; }
          : ZombieProgression.fullSetPercent(time, difficulty));
      if (full) {
        for (var slot : slots) extension.setItemInSlot(slot, randomEquipment(zombie, slot));
      } else if (zombie.getRandom().nextInt(100) < ZombieProgression.armorPercent(time, difficulty)) {
        var slot = slots.get(zombie.getRandom().nextInt(slots.size()));
        extension.setItemInSlot(slot, randomEquipment(zombie, slot));
      }
    }
    var health = zombie.getAttribute(Attributes.MAX_HEALTH);
    if (health != null) health.setBaseValue(net.minecraft.world.entity.ai.attributes.DefaultAttributes
        .getSupplier(SurvivalEntityTypes.ZOMBIES.get(type(zombie)).get()).getBaseValue(Attributes.MAX_HEALTH));
    zombie.removeEffect(MobEffects.GLOWING);
    boolean special = weaponClass == ZombieBalance.WeaponClass.ROCKET
        || weaponClass == ZombieBalance.WeaponClass.THROWABLE;
    List<String> weapons = ZombieBalance.weapons(weaponClass);
    boolean allowedGun = weaponClass == ZombieBalance.WeaponClass.THROWABLE
        || com.craftingdead.survival.world.NormalArmedSpawns.canArm(zombie);
    if (allowedGun && !weapons.isEmpty() && (special || ZombieBalance.armed(zombie.getRandom().nextInt(100)))) {
      ItemStack held = item(weapons.get(zombie.getRandom().nextInt(weapons.size())));
      if (held.getItem() instanceof GunItem gunItem) {
        Gun.get(held).ifPresent(gun -> gun.setAmmoProvider(new RefillableAmmoProvider(
            new ItemStack(gunItem.getDefaultMagazine()), 2, false)));
        zombie.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1));
        boolean blood = zombie.level() instanceof ServerLevel level && BloodMoonManager.isBloodMoonDay(level);
        if (health != null && weaponClass != ZombieBalance.WeaponClass.ROCKET
            && (blood || ZombieBalance.lowHealth(zombie.getRandom().nextInt(100))))
          health.setBaseValue(blood ? 5 : 10);
      } else if (health != null && weaponClass == ZombieBalance.WeaponClass.THROWABLE
          && ZombieBalance.lowHealth(zombie.getRandom().nextInt(100))) health.setBaseValue(5);
      zombie.setItemSlot(EquipmentSlot.MAINHAND, held);
    } else if (zombie.getRandom().nextInt(100) < ZombieProgression.meleePercent(time, difficulty)) {
      zombie.setItemSlot(EquipmentSlot.MAINHAND, !bloodLoadout && ZombieProgression.early(time, difficulty)
          ? item(zombie.getRandom().nextBoolean() ? "bat" : "crowbar")
          : randomEquipment(zombie, Equipment.Slot.MELEE));
    }
    if (noArmor) {
      var armor = zombie.getAttribute(Attributes.ARMOR);
      if (armor != null) armor.setBaseValue(0);
    }
    zombie.setHealth(preserveHealth ? Math.min(oldHealth, zombie.getMaxHealth()) : zombie.getMaxHealth());
    zombie.addTag("craftingdead_loadout_v3");
  }
  public static ZombieBalance.LootKind lootKind(ItemStack stack, Equipment.Slot slot) {
    String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    if (id.equals("rpg")) return ZombieBalance.LootKind.ROCKET;
    if (stack.getItem() instanceof GunItem) return ZombieBalance.LootKind.GUN;
    if (stack.getItem() instanceof MeleeWeaponItem) return ZombieBalance.LootKind.MELEE;
    if (slot == Equipment.Slot.VEST) return ZombieBalance.LootKind.VEST;
    if (slot == Equipment.Slot.BACKPACK) return ZombieBalance.LootKind.BACKPACK;
    if (slot == Equipment.Slot.CLOTHING || slot == Equipment.Slot.HAT)
      return ZombieBalance.LootKind.CLOTHING;
    return Equipment.get(stack).map(e ->
        e.isValidForSlot(Equipment.Slot.VEST) ? ZombieBalance.LootKind.VEST
        : e.isValidForSlot(Equipment.Slot.BACKPACK) ? ZombieBalance.LootKind.BACKPACK
        : e.isValidForSlot(Equipment.Slot.CLOTHING) || e.isValidForSlot(Equipment.Slot.HAT)
            ? ZombieBalance.LootKind.CLOTHING : null).orElse(null);
  }
  public static float dropChance(SurvivalZombie zombie, ItemStack stack, Equipment.Slot slot, float fallback) {
    var kind = lootKind(stack, slot);
    if (kind == null) return fallback;
    boolean blood = zombie.level() instanceof ServerLevel level && BloodMoonManager.isBloodMoonDay(level);
    return ZombieBalance.dropPercent(kind, blood) / 100F;
  }
  public static void doubleDrops(SurvivalZombie zombie, java.util.Collection<net.minecraft.world.entity.item.ItemEntity> drops) {
    if (!(zombie.level() instanceof ServerLevel level) || !BloodMoonManager.isBloodMoonDay(level)) return;
    for (var drop : new java.util.ArrayList<>(drops)) {
      var kind = lootKind(drop.getItem(), null);
      if (kind == null || kind == ZombieBalance.LootKind.ROCKET
          || !ZombieBalance.doubleDrop(zombie.getRandom().nextInt(100))) continue;
      var extra = new net.minecraft.world.entity.item.ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(),
          drop.getItem().copy());
      extra.setDefaultPickUpDelay();
      drops.add(extra);
    }
  }
  private ZombieLoadouts() {}
}

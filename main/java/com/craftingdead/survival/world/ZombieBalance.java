package com.craftingdead.survival.world;

import java.util.List;

/** Pure balance policy shared by spawn, equipment, drops and regression checks. */
public final class ZombieBalance {
  public enum WeaponClass { NONE, PISTOL, RIFLE, SNIPER, MACHINE_GUN, THROWABLE, ROCKET }
  public enum LootKind { GUN, VEST, BACKPACK, MELEE, CLOTHING, ROCKET }
  public static WeaponClass weaponClass(String type) {
    return switch (type) {
      case "soldier_zombie", "swat_zombie", "alfa_zombie", "desert_raider_zombie", "giant_zombie" -> WeaponClass.RIFLE;
      case "sniper_zombie" -> WeaponClass.SNIPER;
      case "juggernaut_zombie" -> WeaponClass.MACHINE_GUN;
      case "police_zombie", "pilot_zombie", "bounty_hunter_zombie" -> WeaponClass.PISTOL;
      case "thrower_zombie" -> WeaponClass.THROWABLE;
      case "rocket_zombie" -> WeaponClass.ROCKET;
      default -> WeaponClass.NONE;
    };
  }
  public static List<String> weapons(WeaponClass type) {
    return switch (type) {
      case PISTOL -> List.of("p250", "fn57", "m9", "m1911", "g18", "magnum", "desert_eagle");
      case RIFLE -> List.of("ak47", "m4a1", "g36c", "fnfal", "mpt55", "scarl", "hk417",
          "acr", "qbz97_girls_frontline", "sporter22", "m1garand");
      case SNIPER -> List.of("dmr", "awp", "m107", "as50");
      case MACHINE_GUN -> List.of("rpk", "m240b", "mk48mod", "minigun");
      case THROWABLE -> List.of("frag_grenade", "fire_grenade", "smoke_grenade", "flash_grenade", "decoy_grenade");
      case ROCKET -> List.of("rpg");
      default -> List.of();
    };
  }
  public static int dropPercent(LootKind kind, boolean blood) {
    if (kind == LootKind.ROCKET) return 1;
    int percent = switch (kind) {
      case GUN -> 35;
      case VEST -> 60;
      case BACKPACK -> 80;
      case MELEE -> 90;
      case CLOTHING -> 40;
      default -> 0;
    };
    return Math.max(0, percent - (blood ? 0 : 20));
  }
  public static boolean armed(int roll) { return roll < 40; }
  public static boolean lowHealth(int roll) { return roll < 90; }
  public static boolean doubleDrop(int roll) { return roll < 10; }
  public static int scrapCount(int chanceRoll, int quantityRoll) {
    return scrapCount(chanceRoll, quantityRoll, false);
  }
  public static int scrapCount(int chanceRoll, int quantityRoll, boolean bloodMoon) {
    return chanceRoll >= 0 && chanceRoll < (bloodMoon ? 40 : 20)
        ? (1 + Math.floorMod(quantityRoll, 3)) * (bloodMoon ? 2 : 1) : 0;
  }
  private ZombieBalance() {}
}

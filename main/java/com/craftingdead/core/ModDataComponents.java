package com.craftingdead.core;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {

  public static final DeferredRegister.DataComponents DATA_COMPONENTS =
      DeferredRegister.createDataComponents(CraftingDead.ID);

  private static DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> gunReg;
  private static DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> magazineReg;
  private static DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> scopeReg;
  private static DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> paintReg;
  private static DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> equipmentReg;
  private static DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> combatSlotReg;

  private static DataComponentType<CompoundTag> gun;
  private static DataComponentType<CompoundTag> magazine;
  private static DataComponentType<CompoundTag> scope;
  private static DataComponentType<CompoundTag> paint;
  private static DataComponentType<CompoundTag> equipment;
  private static DataComponentType<CompoundTag> combatSlot;

  public static DataComponentType<CompoundTag> getGun() { return gun; }
  public static DataComponentType<CompoundTag> getMagazine() { return magazine; }
  public static DataComponentType<CompoundTag> getScope() { return scope; }
  public static DataComponentType<CompoundTag> getPaint() { return paint; }
  public static DataComponentType<CompoundTag> getEquipment() { return equipment; }
  public static DataComponentType<CompoundTag> getCombatSlot() { return combatSlot; }

  static {
    registerAll();
  }

  /** Registers all data components. Call during static init (before RegisterEvent). */
  public static void registerAll() {
    gunReg = DATA_COMPONENTS.register("gun", () -> DataComponentType.<CompoundTag>builder()
        .persistent(CompoundTag.CODEC)
        .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
        .build());
    magazineReg = DATA_COMPONENTS.register("magazine", () -> DataComponentType.<CompoundTag>builder()
        .persistent(CompoundTag.CODEC)
        .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
        .build());
    scopeReg = DATA_COMPONENTS.register("scope", () -> DataComponentType.<CompoundTag>builder()
        .persistent(CompoundTag.CODEC)
        .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
        .build());
    paintReg = DATA_COMPONENTS.register("paint", () -> DataComponentType.<CompoundTag>builder()
        .persistent(CompoundTag.CODEC)
        .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
        .build());
    equipmentReg = DATA_COMPONENTS.register("equipment", () -> DataComponentType.<CompoundTag>builder()
        .persistent(CompoundTag.CODEC)
        .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
        .build());
    combatSlotReg = DATA_COMPONENTS.register("combat_slot", () -> DataComponentType.<CompoundTag>builder()
        .persistent(CompoundTag.CODEC)
        .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
        .build());
  }

  /** Resolves holders to actual values. Call after RegisterEvent. */
  public static void resolveAll() {
    gun = gunReg.get();
    magazine = magazineReg.get();
    scope = scopeReg.get();
    paint = paintReg.get();
    equipment = equipmentReg.get();
    combatSlot = combatSlotReg.get();
  }
}
